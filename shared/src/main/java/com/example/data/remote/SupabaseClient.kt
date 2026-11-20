package com.example.data.remote

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class SupabaseUserDto(
    val id: String,
    val phone: String,
    val name: String,
    val role: String,
    val created_at: Long,
    val email: String? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseUserLegacyDto(
    val id: String,
    val phone: String,
    val name: String,
    val role: String,
    val created_at: Long
)

@JsonClass(generateAdapter = true)
data class SupabaseFarmerProfileDto(
    val user_id: String,
    val farmer_id_number: String,
    val coins: Int = 0
)

@JsonClass(generateAdapter = true)
data class SupabasePilotProfileDto(
    val user_id: String,
    val drone_type: String = "quadcopter",
    val tank_capacity: Double = 10.0,
    val license_document_uri: String = "",
    val is_license_verified: Boolean = false,
    val bank_upi_id: String = "",
    val wallet_balance: Double = 0.0,
    val is_online: Boolean = false,
    val rating_average: Double = 5.0,
    val rating_count: Int = 0,
    val current_lat: Double? = null,
    val current_lng: Double? = null,
    val location_updated_at: Long = 0L
)

@JsonClass(generateAdapter = true)
data class SupabaseOrderDto(
    val id: String,
    val farmer_id: String,
    val assigned_pilot_id: String? = null,
    val field_polygon_coordinates: List<SupabaseCoordDto>,
    val acres_to_spray: Double,
    val location_address: String,
    val total_amount: Double,
    val admin_commission: Double,
    val pilot_earnings: Double,
    val payment_method: String,
    val payment_status: String,
    val order_status: String,
    val pilot_eta_minutes: Int = 15,
    val initial_eta_minutes: Int = 15,
    val distance_km: Double? = null,
    val field_center_lat: Double = 0.0,
    val field_center_lng: Double = 0.0,
    val accepted_at: Long? = null,
    val pilot_rating: Int? = null,
    val razorpay_payment_id: String? = null,
    val created_at: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
data class SupabaseOrderRatingPatchDto(
    val pilot_rating: Int
)

@JsonClass(generateAdapter = true)
data class SupabaseUserPatchDto(
    val name: String,
    val phone: String
)

@JsonClass(generateAdapter = true)
data class SupabaseFarmerIdPatchDto(
    val farmer_id_number: String
)

@JsonClass(generateAdapter = true)
data class SupabaseFarmerCoinsPatchDto(
    val coins: Int
)

@JsonClass(generateAdapter = true)
data class SupabasePilotWalletPatchDto(
    val wallet_balance: Double
)

@JsonClass(generateAdapter = true)
data class SupabaseOrderStatusPatchDto(
    val order_status: String,
    val payment_status: String? = null,
    val pilot_eta_minutes: Int? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseOrderAcceptedPatchDto(
    val assigned_pilot_id: String,
    val order_status: String,
    val pilot_eta_minutes: Int,
    val initial_eta_minutes: Int,
    val distance_km: Double? = null,
    val field_center_lat: Double,
    val field_center_lng: Double,
    val accepted_at: Long
)

@JsonClass(generateAdapter = true)
data class SupabaseOrderDistancePatchDto(
    val distance_km: Double,
    val pilot_eta_minutes: Int,
    val field_center_lat: Double,
    val field_center_lng: Double
)

@JsonClass(generateAdapter = true)
data class SupabasePilotOnlinePatchDto(
    val is_online: Boolean
)

@JsonClass(generateAdapter = true)
data class SupabasePilotLocationPatchDto(
    val current_lat: Double,
    val current_lng: Double,
    val location_updated_at: Long
)

@JsonClass(generateAdapter = true)
data class SupabasePilotLicensePatchDto(
    val is_license_verified: Boolean
)

@JsonClass(generateAdapter = true)
data class SupabasePilotRatingPatchDto(
    val rating_average: Double,
    val rating_count: Int
)

@JsonClass(generateAdapter = true)
data class SupabasePilotAccountPatchDto(
    val drone_type: String,
    val tank_capacity: Double,
    val bank_upi_id: String,
    val license_document_uri: String
)

@JsonClass(generateAdapter = true)
data class SupabaseUserNamePatchDto(
    val name: String
)

@JsonClass(generateAdapter = true)
data class SupabaseIdOnlyDto(
    val id: String
)

@JsonClass(generateAdapter = true)
data class SupabaseFarmBoundaryDto(
    val id: String,
    val farmer_id: String,
    val farm_name: String,
    val polygon_coordinates: List<SupabaseCoordDto>,
    val area_acres: Double,
    val area_hectares: Double,
    val center_lat: Double,
    val center_lng: Double,
    val created_at: String? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseCoordDto(
    val lat: Double,
    val lng: Double
)

@JsonClass(generateAdapter = true)
data class SupabaseOperatorLocationDto(
    val id: String,
    val name: String,
    val role_type: String, // 'drone_operator' | 'drone_mitra'
    val lat: Double,
    val lng: Double,
    val drone_type: String = "quadcopter",
    val is_available: Boolean = true,
    val heading_degrees: Float = 0f,
    val updated_at: String? = null
)

interface SupabasePostgrestApi {
    // --- Users & Profiles ---
    @GET("rest/v1/users?select=id&limit=1")
    suspend fun pingUsers(): Response<List<SupabaseIdOnlyDto>>

    @GET("rest/v1/users?select=*")
    suspend fun getAllUsers(): Response<List<SupabaseUserDto>>

    @Headers("Prefer: resolution=merge-duplicates,return=representation")
    @POST("rest/v1/users")
    suspend fun upsertUser(@Body body: SupabaseUserDto): Response<List<SupabaseUserDto>>

    @Headers("Prefer: resolution=merge-duplicates,return=representation")
    @POST("rest/v1/users")
    suspend fun upsertUserLegacy(@Body body: SupabaseUserLegacyDto): Response<List<SupabaseUserLegacyDto>>

    @GET("rest/v1/users?select=*")
    suspend fun getUsersByPhone(@Query("phone") phoneFilter: String): Response<List<SupabaseUserDto>>

    @GET("rest/v1/users?select=*")
    suspend fun getUsersByEmail(@Query("email") emailFilter: String): Response<List<SupabaseUserDto>>

    @GET("rest/v1/users?select=*")
    suspend fun getUsersById(@Query("id") idFilter: String): Response<List<SupabaseUserDto>>

    @Headers("Prefer: return=representation")
    @PATCH("rest/v1/users")
    suspend fun patchUserCredentials(
        @Query("id") idFilter: String,
        @Body body: SupabaseUserPatchDto
    ): Response<List<SupabaseUserDto>>

    @Headers("Prefer: return=representation")
    @PATCH("rest/v1/users")
    suspend fun patchUserName(
        @Query("id") idFilter: String,
        @Body body: SupabaseUserNamePatchDto
    ): Response<List<SupabaseUserDto>>

    @Headers("Prefer: resolution=merge-duplicates,return=representation")
    @POST("rest/v1/farmer_profiles")
    suspend fun upsertFarmerProfile(
        @Body body: SupabaseFarmerProfileDto
    ): Response<List<SupabaseFarmerProfileDto>>

    @Headers("Prefer: return=representation")
    @PATCH("rest/v1/farmer_profiles")
    suspend fun patchFarmerIdNumber(
        @Query("user_id") userIdFilter: String,
        @Body body: SupabaseFarmerIdPatchDto
    ): Response<List<SupabaseFarmerProfileDto>>

    @Headers("Prefer: return=representation")
    @PATCH("rest/v1/farmer_profiles")
    suspend fun patchFarmerCoins(
        @Query("user_id") userIdFilter: String,
        @Body body: SupabaseFarmerCoinsPatchDto
    ): Response<List<SupabaseFarmerProfileDto>>

    @GET("rest/v1/farmer_profiles?select=*")
    suspend fun getFarmerProfile(
        @Query("user_id") userIdFilter: String
    ): Response<List<SupabaseFarmerProfileDto>>

    @GET("rest/v1/pilot_profiles?select=*")
    suspend fun getAllPilots(): Response<List<SupabasePilotProfileDto>>

    @GET("rest/v1/pilot_profiles?select=*")
    suspend fun getPilotProfile(
        @Query("user_id") userIdFilter: String
    ): Response<List<SupabasePilotProfileDto>>

    @Headers("Prefer: resolution=merge-duplicates,return=representation")
    @POST("rest/v1/pilot_profiles")
    suspend fun upsertPilotProfile(
        @Body body: SupabasePilotProfileDto
    ): Response<List<SupabasePilotProfileDto>>

    @Headers("Prefer: return=representation")
    @PATCH("rest/v1/pilot_profiles")
    suspend fun patchPilotWalletBalance(
        @Query("user_id") userIdFilter: String,
        @Body body: SupabasePilotWalletPatchDto
    ): Response<List<SupabasePilotProfileDto>>

    @Headers("Prefer: return=representation")
    @PATCH("rest/v1/pilot_profiles")
    suspend fun patchPilotOnline(
        @Query("user_id") userIdFilter: String,
        @Body body: SupabasePilotOnlinePatchDto
    ): Response<List<SupabasePilotProfileDto>>

    @Headers("Prefer: return=representation")
    @PATCH("rest/v1/pilot_profiles")
    suspend fun patchPilotLocation(
        @Query("user_id") userIdFilter: String,
        @Body body: SupabasePilotLocationPatchDto
    ): Response<List<SupabasePilotProfileDto>>

    @Headers("Prefer: return=representation")
    @PATCH("rest/v1/pilot_profiles")
    suspend fun patchPilotLicense(
        @Query("user_id") userIdFilter: String,
        @Body body: SupabasePilotLicensePatchDto
    ): Response<List<SupabasePilotProfileDto>>

    @Headers("Prefer: return=representation")
    @PATCH("rest/v1/pilot_profiles")
    suspend fun patchPilotRating(
        @Query("user_id") userIdFilter: String,
        @Body body: SupabasePilotRatingPatchDto
    ): Response<List<SupabasePilotProfileDto>>

    @Headers("Prefer: return=representation")
    @PATCH("rest/v1/pilot_profiles")
    suspend fun patchPilotAccount(
        @Query("user_id") userIdFilter: String,
        @Body body: SupabasePilotAccountPatchDto
    ): Response<List<SupabasePilotProfileDto>>

    // --- Spray Orders ---
    @Headers("Prefer: resolution=merge-duplicates,return=representation")
    @POST("rest/v1/orders")
    suspend fun upsertOrder(@Body body: SupabaseOrderDto): Response<List<SupabaseOrderDto>>

    @GET("rest/v1/orders?order=created_at.desc&select=*")
    suspend fun getAllOrders(): Response<List<SupabaseOrderDto>>

    @GET("rest/v1/orders?order=created_at.desc&select=*")
    suspend fun getOrdersForFarmer(
        @Query("farmer_id") farmerIdFilter: String
    ): Response<List<SupabaseOrderDto>>

    @Headers("Prefer: return=representation")
    @PATCH("rest/v1/orders")
    suspend fun patchOrderAccepted(
        @Query("id") orderIdFilter: String,
        @Body body: SupabaseOrderAcceptedPatchDto
    ): Response<List<SupabaseOrderDto>>

    @Headers("Prefer: return=representation")
    @PATCH("rest/v1/orders")
    suspend fun patchOrderDistance(
        @Query("id") orderIdFilter: String,
        @Body body: SupabaseOrderDistancePatchDto
    ): Response<List<SupabaseOrderDto>>

    @Headers("Prefer: return=representation")
    @PATCH("rest/v1/orders")
    suspend fun patchOrderRating(
        @Query("id") orderIdFilter: String,
        @Body body: SupabaseOrderRatingPatchDto
    ): Response<List<SupabaseOrderDto>>

    @Headers("Prefer: return=representation")
    @PATCH("rest/v1/orders")
    suspend fun patchOrderStatus(
        @Query("id") orderIdFilter: String,
        @Body body: SupabaseOrderStatusPatchDto
    ): Response<List<SupabaseOrderDto>>

    // --- Farm Boundaries ---
    @Headers("Prefer: resolution=merge-duplicates,return=representation")
    @POST("rest/v1/farm_boundaries")
    suspend fun upsertFarmBoundary(
        @Body body: SupabaseFarmBoundaryDto
    ): Response<List<SupabaseFarmBoundaryDto>>

    @GET("rest/v1/farm_boundaries?order=created_at.desc")
    suspend fun getFarmBoundariesForFarmer(
        @Query("farmer_id") farmerIdFilter: String
    ): Response<List<SupabaseFarmBoundaryDto>>

    @DELETE("rest/v1/farm_boundaries")
    suspend fun deleteFarmBoundary(
        @Query("id") idFilter: String
    ): Response<Unit>

    // --- Drone Operators / Pilots Live Location ---
    @GET("rest/v1/operator_locations?select=*")
    suspend fun getOperatorLocations(): Response<List<SupabaseOperatorLocationDto>>

    @Headers("Prefer: resolution=merge-duplicates,return=representation")
    @POST("rest/v1/operator_locations")
    suspend fun upsertOperatorLocation(
        @Body body: SupabaseOperatorLocationDto
    ): Response<List<SupabaseOperatorLocationDto>>
}

@JsonClass(generateAdapter = true)
data class SupabaseAuthSignUpRequest(
    val email: String,
    val password: String,
    val data: Map<String, String> = emptyMap()
)

/**
 * Intentionally omits email_redirect_to / code_challenge so Supabase Auth
 * generates a 6-digit numeric OTP token ({{ .Token }}) instead of a redirect/PKCE link.
 */
@JsonClass(generateAdapter = true)
data class SupabaseOtpRequest(
    val email: String,
    val create_user: Boolean = false
)

@JsonClass(generateAdapter = true)
data class SupabaseVerifyOtpRequest(
    val email: String,
    val token: String,
    val type: String = "email"
)

@JsonClass(generateAdapter = true)
data class SupabaseRefreshTokenRequest(
    val refresh_token: String
)

@JsonClass(generateAdapter = true)
data class SupabaseAuthUserDto(
    val id: String,
    val email: String? = null,
    val user_metadata: Map<String, Any?>? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseAuthSessionResponse(
    val access_token: String? = null,
    val token_type: String? = null,
    val expires_in: Long? = null,
    val expires_at: Long? = null,
    val refresh_token: String? = null,
    val user: SupabaseAuthUserDto? = null,
    val id: String? = null,
    val email: String? = null
)

interface SupabaseAuthApi {
    @POST("auth/v1/signup")
    suspend fun signUp(@Body body: SupabaseAuthSignUpRequest): Response<SupabaseAuthSessionResponse>

    @POST("auth/v1/otp")
    suspend fun requestOtp(@Body body: SupabaseOtpRequest): Response<Unit>

    @POST("auth/v1/verify")
    suspend fun verifyOtp(@Body body: SupabaseVerifyOtpRequest): Response<SupabaseAuthSessionResponse>

    @POST("auth/v1/token?grant_type=refresh_token")
    suspend fun refreshSession(@Body body: SupabaseRefreshTokenRequest): Response<SupabaseAuthSessionResponse>

    @GET("auth/v1/user")
    suspend fun getCurrentAuthUser(
        @Header("Authorization") bearerToken: String
    ): Response<SupabaseAuthUserDto>

    @POST("auth/v1/logout")
    suspend fun logout(
        @Header("Authorization") bearerToken: String
    ): Response<Unit>
}

sealed class SupabaseConnectionStatus {
    data object Checking : SupabaseConnectionStatus()
    data class Connected(val endpoint: String) : SupabaseConnectionStatus()
    data class MissingConfig(val missingKey: String, val message: String) : SupabaseConnectionStatus()
    data class Error(val httpCode: Int?, val message: String) : SupabaseConnectionStatus()
}

object SupabaseClient {

    @Volatile
    private var cachedSignature: String? = null

    @Volatile
    private var cachedApi: SupabasePostgrestApi? = null

    @Volatile
    private var cachedAuthApi: SupabaseAuthApi? = null

    fun normalizedBaseUrl(): String {
        var base = KrishivantriRuntimeConfig.supabaseUrl.trim().trimEnd('/')
        if (base.endsWith("/rest/v1", ignoreCase = true)) {
            base = base.dropLast("/rest/v1".length).trimEnd('/')
        }
        return base
    }

    fun isUrlConfigured(): Boolean {
        val url = normalizedBaseUrl()
        return url.startsWith("https://") &&
            !url.contains("your-project") &&
            !url.contains("MY_SUPABASE")
    }

    fun isKeyConfigured(): Boolean {
        val key = KrishivantriRuntimeConfig.supabaseAnonKey.trim()
        return key.isNotBlank() && !key.startsWith("MY_SUPABASE")
    }

    fun isConfigured(): Boolean = isUrlConfigured() && isKeyConfigured()

    fun realtimeWebSocketUrl(): String {
        val base = normalizedBaseUrl()
        val wsBase = base.replace("https://", "wss://").replace("http://", "ws://")
        return "$wsBase/realtime/v1/websocket?apikey=${KrishivantriRuntimeConfig.supabaseAnonKey.trim()}&vsn=1.0.0"
    }

    suspend fun checkConnection(): SupabaseConnectionStatus = withContext(Dispatchers.IO) {
        if (!isUrlConfigured()) {
            return@withContext SupabaseConnectionStatus.MissingConfig(
                missingKey = "SUPABASE_URL",
                message = "SUPABASE_URL is missing or placeholder in AI Studio Secrets."
            )
        }
        if (!isKeyConfigured()) {
            return@withContext SupabaseConnectionStatus.MissingConfig(
                missingKey = "SUPABASE_ANON_KEY",
                message = "SUPABASE_ANON_KEY is missing in AI Studio Secrets. Add your Supabase anon/publishable key to enable cloud sync."
            )
        }
        val endpoint = normalizedBaseUrl()
        val service = api ?: return@withContext SupabaseConnectionStatus.Error(
            httpCode = null,
            message = "Invalid Supabase URL: $endpoint"
        )
        try {
            val response = service.pingUsers()
            if (response.isSuccessful) {
                SupabaseConnectionStatus.Connected(endpoint = endpoint)
            } else {
                val rawErr = runCatching { response.errorBody()?.string()?.trim() }.getOrNull()
                    ?.take(180)
                    .orEmpty()
                val detailSuffix = if (rawErr.isNotBlank()) " ($rawErr)" else ""
                val msg = when (response.code()) {
                    401, 403 -> "Authentication rejected (HTTP ${response.code()}). Verify SUPABASE_ANON_KEY and RLS policies.$detailSuffix"
                    404 -> "Table 'users' not found (HTTP 404). Run backend/supabase_schema.sql in Supabase SQL Editor.$detailSuffix"
                    else -> "Supabase returned HTTP ${response.code()}: ${response.message()}$detailSuffix"
                }
                SupabaseConnectionStatus.Error(httpCode = response.code(), message = msg)
            }
        } catch (e: Exception) {
            SupabaseConnectionStatus.Error(
                httpCode = null,
                message = "Cannot reach Supabase ($endpoint): ${e.localizedMessage ?: "Network error"}"
            )
        }
    }

    val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val key = KrishivantriRuntimeConfig.supabaseAnonKey.trim()
                val reqBuilder = chain.request().newBuilder()
                    .header("apikey", key)
                    .header("Content-Type", "application/json")
                // Legacy Supabase anon keys are 3-segment JWTs and require Authorization Bearer;
                // newer sb_publishable_ keys authenticate via apikey header alone.
                // Never overwrite an explicit user Bearer token (e.g. /auth/v1/user, /auth/v1/logout).
                if (chain.request().header("Authorization") == null && key.count { it == '.' } == 2) {
                    reqBuilder.header("Authorization", "Bearer $key")
                }
                chain.proceed(reqBuilder.build())
            }
            .addInterceptor(logging)
            .build()
    }

    val api: SupabasePostgrestApi?
        get() {
            ensureClientsInitialized()
            return cachedApi
        }

    val authApi: SupabaseAuthApi?
        get() {
            ensureClientsInitialized()
            return cachedAuthApi
        }

    private fun ensureClientsInitialized() {
        if (!isConfigured()) return
        val baseUrl = normalizedBaseUrl() + "/"
        val signature = "$baseUrl|${KrishivantriRuntimeConfig.supabaseAnonKey.trim()}"
        if (cachedApi != null && cachedAuthApi != null && cachedSignature == signature) {
            return
        }
        synchronized(this) {
            if (cachedApi != null && cachedAuthApi != null && cachedSignature == signature) {
                return
            }
            val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
            val retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
            cachedApi = retrofit.create(SupabasePostgrestApi::class.java)
            cachedAuthApi = retrofit.create(SupabaseAuthApi::class.java)
            cachedSignature = signature
        }
    }
}
