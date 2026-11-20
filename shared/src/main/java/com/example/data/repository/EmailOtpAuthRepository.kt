package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.AuthSessionStore
import com.example.data.local.KrishivantriDatabase
import com.example.data.model.AuthSession
import com.example.data.model.FarmerProfileEntity
import com.example.data.model.UserEntity
import com.example.data.model.UserRole
import com.example.data.remote.SupabaseAuthSessionResponse
import com.example.data.remote.SupabaseAuthSignUpRequest
import com.example.data.remote.SupabaseClient
import com.example.data.remote.SupabaseFarmerProfileDto
import com.example.data.remote.SupabaseOtpRequest
import com.example.data.remote.SupabaseRefreshTokenRequest
import com.example.data.remote.SupabaseUserDto
import com.example.data.remote.SupabaseUserLegacyDto
import com.example.data.remote.SupabaseVerifyOtpRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.security.SecureRandom
import java.util.Base64
import java.util.Locale
import java.util.UUID

sealed class EmailAuthResult {
    data class Success(val user: UserEntity, val session: AuthSession) : EmailAuthResult()
    data class Error(val message: String) : EmailAuthResult()
}

sealed class OtpRequestResult {
    data class Sent(
        val cooldownSeconds: Int = DEFAULT_COOLDOWN_SECONDS,
        val expiresInSeconds: Int = DEFAULT_EXPIRY_SECONDS,
        val message: String
    ) : OtpRequestResult()

    data class RateLimited(
        val message: String,
        val cooldownSeconds: Int = DEFAULT_COOLDOWN_SECONDS
    ) : OtpRequestResult()

    data class Error(val message: String) : OtpRequestResult()

    companion object {
        const val DEFAULT_COOLDOWN_SECONDS = 60
        const val DEFAULT_EXPIRY_SECONDS = 300 // 5 minutes
    }
}

sealed class BackendTokenValidation {
    data class Valid(val updatedSession: AuthSession? = null) : BackendTokenValidation()
    data object InvalidOrRevoked : BackendTokenValidation()
    data object NetworkUnavailable : BackendTokenValidation()
}

interface EmailOtpBackend {
    suspend fun registerAuthAccount(
        email: String,
        name: String,
        phone: String,
        farmerIdNumber: String
    ): BackendSignUpOutcome

    suspend fun requestOtp(email: String, allowCreateUser: Boolean): OtpRequestResult

    suspend fun verifyOtp(email: String, otp: String): BackendVerifyOutcome

    suspend fun validateOrRefreshSession(session: AuthSession): BackendTokenValidation

    suspend fun revokeSession(accessToken: String)
}

sealed class BackendSignUpOutcome {
    data class Created(
        val authUserId: String?,
        val session: AuthSession?
    ) : BackendSignUpOutcome()

    data object AlreadyRegistered : BackendSignUpOutcome()
    data class Error(val message: String) : BackendSignUpOutcome()
}

sealed class BackendVerifyOutcome {
    data class Verified(
        val userId: String,
        val email: String,
        val metadata: Map<String, Any?>,
        val accessToken: String,
        val refreshToken: String,
        val expiresAtEpochSec: Long
    ) : BackendVerifyOutcome()

    data object InvalidOrExpired : BackendVerifyOutcome()
    data class Error(val message: String) : BackendVerifyOutcome()
}

class SupabaseEmailOtpBackend : EmailOtpBackend {

    override suspend fun registerAuthAccount(
        email: String,
        name: String,
        phone: String,
        farmerIdNumber: String
    ): BackendSignUpOutcome = withContext(Dispatchers.IO) {
        val authApi = SupabaseClient.authApi
            ?: return@withContext BackendSignUpOutcome.Created(authUserId = null, session = null)

        val randomSecret = generateRandomSecret()
        val response = try {
            authApi.signUp(
                SupabaseAuthSignUpRequest(
                    email = email,
                    password = randomSecret,
                    data = mapOf(
                        "name" to name,
                        "phone" to phone,
                        "farmer_id_number" to farmerIdNumber,
                        "role" to UserRole.FARMER.apiValue
                    )
                )
            )
        } catch (_: IOException) {
            return@withContext BackendSignUpOutcome.Error(
                "Network error while creating account. Please check your internet connection."
            )
        } catch (e: Exception) {
            return@withContext BackendSignUpOutcome.Error(
                e.localizedMessage ?: "Unable to complete registration."
            )
        }

        if (response.isSuccessful) {
            val body = response.body()
            val authUserId = body?.user?.id ?: body?.id
            val session = buildAuthSessionFromResponse(body, email)
            return@withContext BackendSignUpOutcome.Created(
                authUserId = authUserId,
                session = session
            )
        }

        val errBody = runCatching { response.errorBody()?.string().orEmpty() }.getOrDefault("")
        val normalizedErr = errBody.lowercase(Locale.ROOT)
        if (response.code() == 422 ||
            normalizedErr.contains("already registered") ||
            normalizedErr.contains("user_already_exists") ||
            normalizedErr.contains("email_exists")
        ) {
            return@withContext BackendSignUpOutcome.AlreadyRegistered
        }

        if (response.code() == 429 || normalizedErr.contains("rate_limit")) {
            return@withContext BackendSignUpOutcome.Error(
                "Too many registration attempts. Please wait a minute and try again."
            )
        }

        return@withContext BackendSignUpOutcome.Error(
            "Registration could not be completed right now (HTTP ${response.code()}). Please try again."
        )
    }

    override suspend fun requestOtp(email: String, allowCreateUser: Boolean): OtpRequestResult =
        withContext(Dispatchers.IO) {
            val authApi = SupabaseClient.authApi
                ?: return@withContext OtpRequestResult.Error(
                    "Cloud authentication service is not configured."
                )

            val response = try {
                authApi.requestOtp(
                    SupabaseOtpRequest(
                        email = email,
                        create_user = allowCreateUser
                    )
                )
            } catch (_: IOException) {
                return@withContext OtpRequestResult.Error(
                    "Unable to reach authentication server. Check your internet connection."
                )
            } catch (_: Exception) {
                return@withContext OtpRequestResult.Error(
                    "Unable to send OTP right now. Please try again."
                )
            }

            if (response.isSuccessful) {
                return@withContext OtpRequestResult.Sent(
                    cooldownSeconds = OtpRequestResult.DEFAULT_COOLDOWN_SECONDS,
                    expiresInSeconds = OtpRequestResult.DEFAULT_EXPIRY_SECONDS,
                    message = "If an account is registered with this email, a 6-digit OTP has been sent. It expires in 5 minutes."
                )
            }

            val errBody = runCatching { response.errorBody()?.string().orEmpty() }.getOrDefault("")
            val lower = errBody.lowercase(Locale.ROOT)

            if (response.code() == 429 || lower.contains("over_email_send_rate_limit") || lower.contains("rate_limit")) {
                return@withContext OtpRequestResult.RateLimited(
                    message = "Please wait 60 seconds before requesting another OTP.",
                    cooldownSeconds = OtpRequestResult.DEFAULT_COOLDOWN_SECONDS
                )
            }

            // Do not reveal whether the email address is registered when signups are disabled for OTP
            if (response.code() == 422 && (lower.contains("otp_disabled") || lower.contains("not found") || lower.contains("signups not allowed"))) {
                return@withContext OtpRequestResult.Sent(
                    cooldownSeconds = OtpRequestResult.DEFAULT_COOLDOWN_SECONDS,
                    expiresInSeconds = OtpRequestResult.DEFAULT_EXPIRY_SECONDS,
                    message = "If an account is registered with this email, a 6-digit OTP has been sent. It expires in 5 minutes."
                )
            }

            return@withContext OtpRequestResult.Error(
                "Unable to send OTP email right now. Please try again shortly."
            )
        }

    override suspend fun verifyOtp(email: String, otp: String): BackendVerifyOutcome =
        withContext(Dispatchers.IO) {
            val authApi = SupabaseClient.authApi
                ?: return@withContext BackendVerifyOutcome.Error(
                    "Cloud authentication service is not configured."
                )

            val typesToTry = listOf("email", "signup", "magiclink")
            for (otpType in typesToTry) {
                val response = try {
                    authApi.verifyOtp(
                        SupabaseVerifyOtpRequest(
                            email = email,
                            token = otp,
                            type = otpType
                        )
                    )
                } catch (_: IOException) {
                    return@withContext BackendVerifyOutcome.Error(
                        "Unable to reach authentication server. Check your internet connection."
                    )
                } catch (_: Exception) {
                    return@withContext BackendVerifyOutcome.Error(
                        "Unable to verify OTP right now. Please try again."
                    )
                }

                if (response.isSuccessful) {
                    val body = response.body()
                    val token = body?.access_token
                    val authUser = body?.user
                    if (!token.isNullOrBlank() && authUser != null) {
                        val nowSec = System.currentTimeMillis() / 1000L
                        val expiresAt = body.expires_at ?: (nowSec + (body.expires_in ?: 3600L))
                        return@withContext BackendVerifyOutcome.Verified(
                            userId = authUser.id,
                            email = authUser.email?.trim()?.lowercase(Locale.ROOT) ?: email,
                            metadata = authUser.user_metadata.orEmpty(),
                            accessToken = token,
                            refreshToken = body.refresh_token.orEmpty(),
                            expiresAtEpochSec = expiresAt
                        )
                    }
                }
            }

            BackendVerifyOutcome.InvalidOrExpired
        }

    override suspend fun validateOrRefreshSession(session: AuthSession): BackendTokenValidation =
        withContext(Dispatchers.IO) {
            if (session.accessToken.isBlank() || session.accessToken.startsWith(LOCAL_TOKEN_PREFIX)) {
                return@withContext BackendTokenValidation.Valid()
            }
            val authApi = SupabaseClient.authApi
                ?: return@withContext BackendTokenValidation.NetworkUnavailable

            val nowSec = System.currentTimeMillis() / 1000L
            val needsRefresh = session.expiresAtEpochSec > 0L && (session.expiresAtEpochSec - nowSec) <= 60L

            if (!needsRefresh) {
                val userCheck = try {
                    authApi.getCurrentAuthUser("Bearer ${session.accessToken}")
                } catch (_: IOException) {
                    return@withContext BackendTokenValidation.NetworkUnavailable
                } catch (_: Exception) {
                    return@withContext BackendTokenValidation.NetworkUnavailable
                }

                if (userCheck.isSuccessful) {
                    return@withContext BackendTokenValidation.Valid()
                }
                if (userCheck.code() !in setOf(401, 403)) {
                    return@withContext BackendTokenValidation.NetworkUnavailable
                }
            }

            if (session.refreshToken.isBlank()) {
                return@withContext BackendTokenValidation.InvalidOrRevoked
            }

            val refreshResp = try {
                authApi.refreshSession(SupabaseRefreshTokenRequest(session.refreshToken))
            } catch (_: IOException) {
                return@withContext BackendTokenValidation.NetworkUnavailable
            } catch (_: Exception) {
                return@withContext BackendTokenValidation.NetworkUnavailable
            }

            if (refreshResp.isSuccessful) {
                val body = refreshResp.body()
                val nextAccess = body?.access_token
                if (!nextAccess.isNullOrBlank()) {
                    val nextExpiresAt = body.expires_at ?: ((System.currentTimeMillis() / 1000L) + (body.expires_in ?: 3600L))
                    val refreshed = session.copy(
                        accessToken = nextAccess,
                        refreshToken = body.refresh_token?.takeIf { it.isNotBlank() } ?: session.refreshToken,
                        expiresAtEpochSec = nextExpiresAt
                    )
                    return@withContext BackendTokenValidation.Valid(updatedSession = refreshed)
                }
            }

            if (refreshResp.code() in setOf(400, 401, 403)) {
                BackendTokenValidation.InvalidOrRevoked
            } else {
                BackendTokenValidation.NetworkUnavailable
            }
        }

    override suspend fun revokeSession(accessToken: String) {
        if (accessToken.isBlank() || accessToken.startsWith(LOCAL_TOKEN_PREFIX)) return
        withContext(Dispatchers.IO) {
            runCatching {
                SupabaseClient.authApi?.logout("Bearer $accessToken")
            }
        }
    }

    private fun buildAuthSessionFromResponse(
        body: SupabaseAuthSessionResponse?,
        email: String
    ): AuthSession? {
        val access = body?.access_token ?: return null
        val userId = body.user?.id ?: body.id ?: return null
        val nowSec = System.currentTimeMillis() / 1000L
        return AuthSession(
            userId = userId,
            email = email,
            role = UserRole.FARMER.apiValue,
            accessToken = access,
            refreshToken = body.refresh_token.orEmpty(),
            expiresAtEpochSec = body.expires_at ?: (nowSec + (body.expires_in ?: 3600L))
        )
    }

    private fun generateRandomSecret(): String {
        val bytes = ByteArray(24)
        SecureRandom().nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    companion object {
        const val LOCAL_TOKEN_PREFIX = "local_"
    }
}

class EmailOtpAuthRepository(
    private val database: KrishivantriDatabase,
    private val sessionStore: AuthSessionStore,
    private val backend: EmailOtpBackend = SupabaseEmailOtpBackend()
) {
    private val dao = database.dao()

    companion object {
        private val EMAIL_REGEX = Regex(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
        )
        const val MAX_OTP_ATTEMPTS = 5

        fun normalizeEmail(raw: String): String = raw.trim().lowercase(Locale.ROOT)

        fun isValidEmail(raw: String): Boolean {
            val clean = normalizeEmail(raw)
            return clean.isNotEmpty() && clean.length <= 254 && EMAIL_REGEX.matches(clean)
        }
    }

    suspend fun registerFarmerWithEmail(
        name: String,
        email: String,
        phone: String,
        farmerIdNumber: String
    ): EmailAuthResult {
        val cleanName = name.trim()
        val cleanEmail = normalizeEmail(email)
        val cleanPhone = phone.trim()
        val cleanFarmerId = farmerIdNumber.trim().uppercase(Locale.getDefault())

        if (cleanName.isBlank()) {
            return EmailAuthResult.Error("Enter your full name.")
        }
        if (cleanEmail.isBlank()) {
            return EmailAuthResult.Error("Email address is required.")
        }
        if (!isValidEmail(cleanEmail)) {
            return EmailAuthResult.Error("Enter a valid email address.")
        }
        if (cleanFarmerId.isBlank()) {
            return EmailAuthResult.Error("Enter your Farmer ID / Registration number.")
        }
        if (cleanPhone.length < 10) {
            return EmailAuthResult.Error("Enter a valid 10-digit mobile number.")
        }

        // Prevent duplicate registration locally
        if (dao.getUserByEmail(cleanEmail) != null) {
            return EmailAuthResult.Error("This email address is already registered. Please sign in with Email OTP.")
        }
        if (dao.getUserByPhone(cleanPhone) != null) {
            return EmailAuthResult.Error("This mobile number is already registered. Please sign in with Email OTP.")
        }

        // Prevent duplicate registration in Supabase public.users
        val duplicateCheckError = withContext(Dispatchers.IO) {
            val api = SupabaseClient.api ?: return@withContext null
            val existingByEmail = runCatching {
                val resp = api.getUsersByEmail("eq.$cleanEmail")
                if (resp.isSuccessful) resp.body()?.firstOrNull() else null
            }.getOrNull()
            if (existingByEmail != null) {
                return@withContext "This email address is already registered. Please sign in with Email OTP."
            }
            val existingByPhone = runCatching {
                val resp = api.getUsersByPhone("eq.$cleanPhone")
                if (resp.isSuccessful) resp.body()?.firstOrNull() else null
            }.getOrNull()
            if (existingByPhone != null) {
                return@withContext "This mobile number is already registered. Please sign in with Email OTP."
            }
            null
        }
        if (duplicateCheckError != null) {
            return EmailAuthResult.Error(duplicateCheckError)
        }

        val signUpOutcome = backend.registerAuthAccount(
            email = cleanEmail,
            name = cleanName,
            phone = cleanPhone,
            farmerIdNumber = cleanFarmerId
        )

        val resolvedUserId: String
        val resolvedAuthSession: AuthSession?
        when (signUpOutcome) {
            is BackendSignUpOutcome.AlreadyRegistered -> {
                return EmailAuthResult.Error(
                    "This email address is already registered. Please sign in with Email OTP."
                )
            }
            is BackendSignUpOutcome.Error -> {
                return EmailAuthResult.Error(signUpOutcome.message)
            }
            is BackendSignUpOutcome.Created -> {
                resolvedUserId = signUpOutcome.authUserId?.takeIf { it.isNotBlank() }
                    ?: UUID.randomUUID().toString()
                resolvedAuthSession = signUpOutcome.session
            }
        }

        val user = UserEntity(
            id = resolvedUserId,
            phone = cleanPhone,
            name = cleanName,
            role = UserRole.FARMER.apiValue,
            email = cleanEmail
        )
        val profile = FarmerProfileEntity(
            userId = user.id,
            farmerIdNumber = cleanFarmerId,
            coins = 0
        )

        database.withTransaction {
            dao.insertUser(user)
            dao.insertFarmerProfile(profile)
        }

        withContext(Dispatchers.IO) {
            runCatching {
                SupabaseClient.api?.let { api ->
                    val dto = SupabaseUserDto(
                        id = user.id,
                        phone = user.phone,
                        name = user.name,
                        role = user.role,
                        created_at = user.createdAt,
                        email = user.email
                    )
                    val userResp = api.upsertUser(dto)
                    if (!userResp.isSuccessful) {
                        api.upsertUserLegacy(
                            SupabaseUserLegacyDto(
                                id = user.id,
                                phone = user.phone,
                                name = user.name,
                                role = user.role,
                                created_at = user.createdAt
                            )
                        )
                    }
                    api.upsertFarmerProfile(
                        SupabaseFarmerProfileDto(
                            user_id = profile.userId,
                            farmer_id_number = profile.farmerIdNumber,
                            coins = profile.coins
                        )
                    )
                }
            }
        }

        val session = (resolvedAuthSession ?: AuthSession(
            userId = user.id,
            email = cleanEmail,
            role = UserRole.FARMER.apiValue,
            accessToken = "${SupabaseEmailOtpBackend.LOCAL_TOKEN_PREFIX}${UUID.randomUUID()}"
        )).copy(userId = user.id, email = cleanEmail, role = UserRole.FARMER.apiValue)

        sessionStore.saveSession(session)
        return EmailAuthResult.Success(user, session)
    }

    suspend fun requestLoginOtp(email: String): OtpRequestResult {
        val cleanEmail = normalizeEmail(email)
        if (cleanEmail.isBlank()) {
            return OtpRequestResult.Error("Enter your email address.")
        }
        if (!isValidEmail(cleanEmail)) {
            return OtpRequestResult.Error("Enter a valid email address.")
        }

        // Only allow auto-create in auth.users if the farmer already exists in public.users / Room
        val knownFarmerInPublicDb = dao.getUserByEmail(cleanEmail) != null || withContext(Dispatchers.IO) {
            runCatching {
                val resp = SupabaseClient.api?.getUsersByEmail("eq.$cleanEmail")
                resp?.isSuccessful == true && !resp.body().isNullOrEmpty()
            }.getOrDefault(false)
        }

        return backend.requestOtp(email = cleanEmail, allowCreateUser = knownFarmerInPublicDb)
    }

    suspend fun verifyLoginOtp(email: String, otp: String): EmailAuthResult {
        val cleanEmail = normalizeEmail(email)
        val cleanOtp = otp.trim()
        if (!isValidEmail(cleanEmail)) {
            return EmailAuthResult.Error("Enter a valid email address.")
        }
        if (cleanOtp.length != 6 || !cleanOtp.all { it.isDigit() }) {
            return EmailAuthResult.Error("Enter the 6-digit OTP sent to your email.")
        }

        return when (val outcome = backend.verifyOtp(cleanEmail, cleanOtp)) {
            is BackendVerifyOutcome.Verified -> {
                val user = hydrateVerifiedFarmer(outcome)
                    ?: return EmailAuthResult.Error(
                        "Unable to sign in to the Farmer Portal with this account."
                    )

                val session = AuthSession(
                    userId = user.id,
                    email = outcome.email,
                    role = user.role,
                    accessToken = outcome.accessToken,
                    refreshToken = outcome.refreshToken,
                    expiresAtEpochSec = outcome.expiresAtEpochSec
                )
                sessionStore.saveSession(session)
                EmailAuthResult.Success(user, session)
            }

            is BackendVerifyOutcome.InvalidOrExpired -> {
                EmailAuthResult.Error("Invalid or expired OTP. Please check the code and try again.")
            }

            is BackendVerifyOutcome.Error -> {
                EmailAuthResult.Error(outcome.message)
            }
        }
    }

    suspend fun restorePersistentSession(): EmailAuthResult? {
        val saved = sessionStore.loadSession() ?: return null
        if (saved.userId.isBlank() || saved.email.isBlank()) {
            sessionStore.clearSession()
            return null
        }

        val validation = backend.validateOrRefreshSession(saved)
        val activeSession = when (validation) {
            is BackendTokenValidation.InvalidOrRevoked -> {
                sessionStore.clearSession()
                return null
            }
            is BackendTokenValidation.Valid -> {
                val updated = validation.updatedSession
                if (updated != null) {
                    sessionStore.saveSession(updated)
                    updated
                } else {
                    saved
                }
            }
            is BackendTokenValidation.NetworkUnavailable -> saved
        }

        val user = findOrHydrateUserByIdOrEmail(activeSession.userId, activeSession.email)
            ?: return null

        return EmailAuthResult.Success(user, activeSession)
    }

    suspend fun logout() {
        val existing = sessionStore.loadSession()
        sessionStore.clearSession()
        if (existing != null) {
            backend.revokeSession(existing.accessToken)
        }
    }

    suspend fun revokeAccessToken(accessToken: String?) {
        if (!accessToken.isNullOrBlank()) {
            backend.revokeSession(accessToken)
        }
    }

    private suspend fun hydrateVerifiedFarmer(verified: BackendVerifyOutcome.Verified): UserEntity? {
        val metaName = (verified.metadata["name"] as? String)?.trim().orEmpty()
        val metaPhone = (verified.metadata["phone"] as? String)?.trim().orEmpty()
        val metaFarmerId = (verified.metadata["farmer_id_number"] as? String)?.trim().orEmpty()
        val metaRole = (verified.metadata["role"] as? String)?.trim().orEmpty()

        // 1. Check local Room by email, id, or metadata phone
        var user = dao.getUserByEmail(verified.email)
            ?: dao.getUserById(verified.userId)
            ?: metaPhone.takeIf { it.isNotBlank() }?.let { dao.getUserByPhone(it) }

        // 2. Check Supabase public.users if not cached locally
        if (user == null) {
            user = withContext(Dispatchers.IO) {
                runCatching {
                    val api = SupabaseClient.api ?: return@runCatching null
                    val byEmail = runCatching {
                        val r = api.getUsersByEmail("eq.${verified.email}")
                        if (r.isSuccessful) r.body()?.firstOrNull() else null
                    }.getOrNull()
                    val byId = byEmail ?: api.getUsersById("eq.${verified.userId}").body()?.firstOrNull()
                    val remote = byId ?: metaPhone.takeIf { it.isNotBlank() }?.let {
                        api.getUsersByPhone("eq.$it").body()?.firstOrNull()
                    } ?: return@runCatching null

                    val entity = UserEntity(
                        id = remote.id,
                        phone = remote.phone,
                        name = remote.name,
                        role = remote.role,
                        email = remote.email?.takeIf { it.isNotBlank() } ?: verified.email,
                        createdAt = remote.created_at
                    )
                    dao.insertUser(entity)
                    if (entity.role.equals(UserRole.FARMER.apiValue, ignoreCase = true)) {
                        api.getFarmerProfile("eq.${entity.id}").body()?.firstOrNull()?.let { fp ->
                            dao.insertFarmerProfile(
                                FarmerProfileEntity(
                                    userId = fp.user_id,
                                    farmerIdNumber = fp.farmer_id_number,
                                    coins = fp.coins
                                )
                            )
                        }
                    }
                    entity
                }.getOrNull()
            }
        }

        // 3. If authenticated via Supabase Auth metadata but not yet in public.users, materialize Farmer record
        if (user == null) {
            val resolvedRole = metaRole.ifBlank { UserRole.FARMER.apiValue }
            if (!resolvedRole.equals(UserRole.FARMER.apiValue, ignoreCase = true)) {
                return null
            }
            val newUser = UserEntity(
                id = verified.userId,
                phone = metaPhone,
                name = metaName.ifBlank { verified.email.substringBefore("@") },
                role = UserRole.FARMER.apiValue,
                email = verified.email
            )
            val newProfile = FarmerProfileEntity(
                userId = newUser.id,
                farmerIdNumber = metaFarmerId.ifBlank { "FRM-${newUser.id.take(6).uppercase(Locale.ROOT)}" },
                coins = 0
            )
            database.withTransaction {
                dao.insertUser(newUser)
                dao.insertFarmerProfile(newProfile)
            }
            withContext(Dispatchers.IO) {
                runCatching {
                    SupabaseClient.api?.let { api ->
                        val dto = SupabaseUserDto(
                            id = newUser.id,
                            phone = newUser.phone,
                            name = newUser.name,
                            role = newUser.role,
                            created_at = newUser.createdAt,
                            email = newUser.email
                        )
                        val resp = api.upsertUser(dto)
                        if (!resp.isSuccessful) {
                            api.upsertUserLegacy(
                                SupabaseUserLegacyDto(
                                    id = newUser.id,
                                    phone = newUser.phone,
                                    name = newUser.name,
                                    role = newUser.role,
                                    created_at = newUser.createdAt
                                )
                            )
                        }
                        api.upsertFarmerProfile(
                            SupabaseFarmerProfileDto(
                                user_id = newProfile.userId,
                                farmer_id_number = newProfile.farmerIdNumber,
                                coins = newProfile.coins
                            )
                        )
                    }
                }
            }
            return newUser
        }

        if (!user.role.equals(UserRole.FARMER.apiValue, ignoreCase = true) &&
            !user.role.equals(UserRole.ADMIN.apiValue, ignoreCase = true)
        ) {
            return null
        }

        // Ensure email is populated locally if missing
        val normalizedUser = if (user.email.isBlank()) {
            val updated = user.copy(email = verified.email)
            dao.insertUser(updated)
            updated
        } else {
            user
        }

        // Ensure FarmerProfile exists locally (hydrate from Supabase if missing)
        if (normalizedUser.role.equals(UserRole.FARMER.apiValue, ignoreCase = true) &&
            dao.getFarmerProfile(normalizedUser.id) == null
        ) {
            withContext(Dispatchers.IO) {
                runCatching {
                    val fp = SupabaseClient.api?.getFarmerProfile("eq.${normalizedUser.id}")
                        ?.body()?.firstOrNull()
                    dao.insertFarmerProfile(
                        FarmerProfileEntity(
                            userId = normalizedUser.id,
                            farmerIdNumber = fp?.farmer_id_number
                                ?: metaFarmerId.ifBlank { "FRM-ID" },
                            coins = fp?.coins ?: 0
                        )
                    )
                }
            }
        }

        return normalizedUser
    }

    private suspend fun findOrHydrateUserByIdOrEmail(userId: String, email: String): UserEntity? {
        val local = dao.getUserById(userId) ?: dao.getUserByEmail(email)
        if (local != null) {
            return if (local.email.isBlank()) {
                val updated = local.copy(email = email)
                dao.insertUser(updated)
                updated
            } else {
                local
            }
        }

        return withContext(Dispatchers.IO) {
            runCatching {
                val api = SupabaseClient.api ?: return@runCatching null
                val remote = api.getUsersById("eq.$userId").body()?.firstOrNull()
                    ?: runCatching {
                        val r = api.getUsersByEmail("eq.$email")
                        if (r.isSuccessful) r.body()?.firstOrNull() else null
                    }.getOrNull()
                    ?: return@runCatching null
                val entity = UserEntity(
                    id = remote.id,
                    phone = remote.phone,
                    name = remote.name,
                    role = remote.role,
                    email = remote.email?.takeIf { it.isNotBlank() } ?: email,
                    createdAt = remote.created_at
                )
                dao.insertUser(entity)
                if (entity.role.equals(UserRole.FARMER.apiValue, ignoreCase = true)) {
                    api.getFarmerProfile("eq.${entity.id}").body()?.firstOrNull()?.let { fp ->
                        dao.insertFarmerProfile(
                            FarmerProfileEntity(
                                userId = fp.user_id,
                                farmerIdNumber = fp.farmer_id_number,
                                coins = fp.coins
                            )
                        )
                    }
                }
                entity
            }.getOrNull()
        }
    }
}
