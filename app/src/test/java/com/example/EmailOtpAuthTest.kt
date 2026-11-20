package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.KrishivantriDatabase
import com.example.data.local.PersistentAuthSessionStore
import com.example.data.model.AuthSession
import com.example.data.repository.BackendSignUpOutcome
import com.example.data.repository.BackendTokenValidation
import com.example.data.repository.BackendVerifyOutcome
import com.example.data.repository.EmailAuthResult
import com.example.data.repository.EmailOtpAuthRepository
import com.example.data.repository.EmailOtpBackend
import com.example.data.repository.OtpRequestResult
import com.example.ui.screens.AuthScreen
import com.example.ui.theme.KrishivantriTheme
import com.example.ui.viewmodel.AuthUiState
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class EmailOtpAuthTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var database: KrishivantriDatabase
    private lateinit var sessionStore: PersistentAuthSessionStore
    private lateinit var fakeBackend: FakeEmailOtpBackend
    private lateinit var authRepository: EmailOtpAuthRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, KrishivantriDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        sessionStore = PersistentAuthSessionStore(context)
        sessionStore.clearSession()
        fakeBackend = FakeEmailOtpBackend()
        authRepository = EmailOtpAuthRepository(database, sessionStore, fakeBackend)
    }

    @After
    fun tearDown() {
        sessionStore.clearSession()
        database.close()
    }

    @Test
    fun `registration requires valid email and rejects duplicate email`() = runBlocking {
        val missingEmail = authRepository.registerFarmerWithEmail(
            name = "Ramesh Patel",
            email = "   ",
            phone = "9876543210",
            farmerIdNumber = "MH-FRM-100"
        )
        assertTrue(missingEmail is EmailAuthResult.Error)
        assertTrue((missingEmail as EmailAuthResult.Error).message.contains("required", ignoreCase = true))

        val invalidEmail = authRepository.registerFarmerWithEmail(
            name = "Ramesh Patel",
            email = "ramesh-invalid-email",
            phone = "9876543210",
            farmerIdNumber = "MH-FRM-100"
        )
        assertTrue(invalidEmail is EmailAuthResult.Error)
        assertTrue((invalidEmail as EmailAuthResult.Error).message.contains("valid email", ignoreCase = true))

        val firstSuccess = authRepository.registerFarmerWithEmail(
            name = "Ramesh Patel",
            email = "Ramesh@Example.COM ",
            phone = "9876543210",
            farmerIdNumber = "mh-frm-100"
        )
        assertTrue(firstSuccess is EmailAuthResult.Success)
        val registeredUser = (firstSuccess as EmailAuthResult.Success).user
        assertEquals("ramesh@example.com", registeredUser.email)
        assertEquals("Ramesh Patel", registeredUser.name)

        // Duplicate registration with same email (case-insensitive) must be blocked
        val duplicateEmail = authRepository.registerFarmerWithEmail(
            name = "Another Person",
            email = "ramesh@example.com",
            phone = "9998887776",
            farmerIdNumber = "MH-FRM-200"
        )
        assertTrue(duplicateEmail is EmailAuthResult.Error)
        assertTrue((duplicateEmail as EmailAuthResult.Error).message.contains("already registered", ignoreCase = true))

        // Duplicate reported by remote backend even if local DB is empty
        val cleanDb = Room.inMemoryDatabaseBuilder(context, KrishivantriDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            fakeBackend.alreadyRegisteredEmails.add("existing.remote@example.com")
            val remoteRepo = EmailOtpAuthRepository(cleanDb, sessionStore, fakeBackend)
            val remoteDup = remoteRepo.registerFarmerWithEmail(
                name = "Remote Farmer",
                email = "existing.remote@example.com",
                phone = "9111222333",
                farmerIdNumber = "MH-FRM-300"
            )
            assertTrue(remoteDup is EmailAuthResult.Error)
            assertTrue((remoteDup as EmailAuthResult.Error).message.contains("already registered", ignoreCase = true))
        } finally {
            cleanDb.close()
        }
    }

    @Test
    fun `otp request hides whether email exists and otp verify authenticates valid code only`() = runBlocking {
        // Request OTP for unregistered email -> non-enumerating neutral Sent message, allowCreateUser=false
        val unknownReq = authRepository.requestLoginOtp("unknown.farmer@example.com")
        assertTrue(unknownReq is OtpRequestResult.Sent)
        assertFalse(fakeBackend.lastAllowCreateUser)

        // Register farmer then verify OTP login flow
        authRepository.registerFarmerWithEmail(
            name = "Suresh Deshmukh",
            email = "suresh@example.com",
            phone = "9822001122",
            farmerIdNumber = "MH-FRM-404"
        )
        sessionStore.clearSession()

        val reqResult = authRepository.requestLoginOtp("suresh@example.com")
        assertTrue(reqResult is OtpRequestResult.Sent)
        assertTrue(fakeBackend.lastAllowCreateUser)
        assertEquals("suresh@example.com", fakeBackend.lastOtpEmail)

        // Invalid / non-6-digit OTPs rejected
        fakeBackend.expectedOtp = "654321"
        val shortOtp = authRepository.verifyLoginOtp("suresh@example.com", "65432")
        assertTrue(shortOtp is EmailAuthResult.Error)
        val longOtp = authRepository.verifyLoginOtp("suresh@example.com", "6543210")
        assertTrue(longOtp is EmailAuthResult.Error)
        val badOtp = authRepository.verifyLoginOtp("suresh@example.com", "000000")
        assertTrue(badOtp is EmailAuthResult.Error)
        assertNull(sessionStore.loadSession())

        // Valid 6-digit OTP succeeds and saves persistent session
        val goodOtp = authRepository.verifyLoginOtp("suresh@example.com", "654321")
        assertTrue(goodOtp is EmailAuthResult.Success)
        val success = goodOtp as EmailAuthResult.Success
        assertEquals("suresh@example.com", success.user.email)
        assertNotNull(sessionStore.loadSession())
        assertEquals(success.session.accessToken, sessionStore.loadSession()?.accessToken)
    }

    @Test
    fun `persistent session survives store recreation and clears permanently on logout`() = runBlocking {
        val regResult = authRepository.registerFarmerWithEmail(
            name = "Kavita Shinde",
            email = "kavita@example.com",
            phone = "9811223344",
            farmerIdNumber = "MH-FRM-505"
        )
        assertTrue(regResult is EmailAuthResult.Success)

        // Simulate app restart with a fresh PersistentAuthSessionStore & Repository
        val restartedStore = PersistentAuthSessionStore(context)
        val restartedRepo = EmailOtpAuthRepository(database, restartedStore, fakeBackend)
        val restored = restartedRepo.restorePersistentSession()
        assertNotNull(restored)
        assertTrue(restored is EmailAuthResult.Success)
        assertEquals("kavita@example.com", (restored as EmailAuthResult.Success).user.email)

        // Network offline preserves valid persisted session
        fakeBackend.tokenValidationMode = BackendTokenValidation.NetworkUnavailable
        val restoredOffline = restartedRepo.restorePersistentSession()
        assertTrue(restoredOffline is EmailAuthResult.Success)

        // Explicit Log Out revokes token and clears storage
        restartedRepo.logout()
        assertEquals(1, fakeBackend.revokedTokens.size)
        assertNull(restartedStore.loadSession())
        assertNull(restartedRepo.restorePersistentSession())
    }

    @Test
    fun `revoked backend token clears local session during startup restore`() = runBlocking {
        authRepository.registerFarmerWithEmail(
            name = "Nilesh More",
            email = "nilesh@example.com",
            phone = "9844556677",
            farmerIdNumber = "MH-FRM-606"
        )
        assertNotNull(sessionStore.loadSession())

        fakeBackend.tokenValidationMode = BackendTokenValidation.InvalidOrRevoked
        val restored = authRepository.restorePersistentSession()
        assertNull(restored)
        assertNull(sessionStore.loadSession())
    }

    @Test
    fun `authScreen renders Email OTP login, OTP verification state, and mandatory email signup`() {
        var capturedEmail: String? = null
        var capturedOtp: String? = null
        val otpStateHolder = androidx.compose.runtime.mutableStateOf(
            AuthUiState(isSignUpMode = false, isRestoringSession = false)
        )

        composeRule.setContent {
            KrishivantriTheme {
                AuthScreen(
                    state = otpStateHolder.value,
                    onToggleSignUp = { isSignUp ->
                        otpStateHolder.value = otpStateHolder.value.copy(isSignUpMode = isSignUp)
                    },
                    onRegisterFarmer = { _, _, _, _ -> },
                    onRequestLoginOtp = { email -> capturedEmail = email },
                    onVerifyLoginOtp = { otp -> capturedOtp = otp },
                    onEditLoginEmail = {
                        otpStateHolder.value = otpStateHolder.value.copy(otpRequestedEmail = null)
                    },
                    onSubmitAdmin = { _, _, _ -> }
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("farmer_login_email_input").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("farmer_login_email_input").performTextInput("farmer@example.com")
        composeRule.onNodeWithTag("farmer_get_otp_button").performScrollTo().assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        assertEquals("farmer@example.com", capturedEmail)

        // Switch to OTP Verification step
        otpStateHolder.value = otpStateHolder.value.copy(
            otpRequestedEmail = "farmer@example.com",
            otpInfoMessage = "A 6-digit OTP has been sent.",
            otpResendCooldownSeconds = 45,
            otpExpiresInSeconds = 295,
            otpFailedAttempts = 1
        )
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("farmer_otp_code_input").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("farmer_verify_otp_button").performScrollTo().assertIsDisplayed().assertIsNotEnabled()
        composeRule.onNodeWithTag("farmer_resend_otp_button").performScrollTo().assertIsDisplayed().assertIsNotEnabled()
        composeRule.onNodeWithText("Resend OTP in 45s").assertIsDisplayed()
        composeRule.onNodeWithText("Expires in 04:55").assertIsDisplayed()

        composeRule.onNodeWithTag("farmer_otp_code_input").performScrollTo().performTextInput("123456")
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("farmer_verify_otp_button").performScrollTo().assertIsEnabled().performClick()
        assertEquals("123456", capturedOtp)

        // Switch to Sign Up tab -> verify mandatory Email field
        composeRule.onNodeWithTag("farmer_auth_tab_signup").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("farmer_auth_email_input").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("farmer_auth_name_input").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("farmer_auth_id_input").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("farmer_auth_phone_input").performScrollTo().assertIsDisplayed()
    }

    private class FakeEmailOtpBackend : EmailOtpBackend {
        val alreadyRegisteredEmails = mutableSetOf<String>()
        val registeredAccounts = mutableMapOf<String, Map<String, Any?>>()
        var lastOtpEmail: String? = null
        var lastAllowCreateUser: Boolean = false
        var expectedOtp: String = "123456"
        var tokenValidationMode: BackendTokenValidation = BackendTokenValidation.Valid()
        val revokedTokens = mutableListOf<String>()

        override suspend fun registerAuthAccount(
            email: String,
            name: String,
            phone: String,
            farmerIdNumber: String
        ): BackendSignUpOutcome {
            if (email in alreadyRegisteredEmails || email in registeredAccounts) {
                return BackendSignUpOutcome.AlreadyRegistered
            }
            val uid = "user_${registeredAccounts.size + 1}"
            registeredAccounts[email] = mapOf(
                "name" to name,
                "phone" to phone,
                "farmer_id_number" to farmerIdNumber,
                "role" to "farmer"
            )
            return BackendSignUpOutcome.Created(
                authUserId = uid,
                session = AuthSession(
                    userId = uid,
                    email = email,
                    role = "farmer",
                    accessToken = "jwt_access_$uid",
                    refreshToken = "jwt_refresh_$uid",
                    expiresAtEpochSec = (System.currentTimeMillis() / 1000L) + 3600L
                )
            )
        }

        override suspend fun requestOtp(email: String, allowCreateUser: Boolean): OtpRequestResult {
            lastOtpEmail = email
            lastAllowCreateUser = allowCreateUser
            return OtpRequestResult.Sent(
                cooldownSeconds = OtpRequestResult.DEFAULT_COOLDOWN_SECONDS,
                expiresInSeconds = OtpRequestResult.DEFAULT_EXPIRY_SECONDS,
                message = "If an account is registered with this email, a 6-digit OTP has been sent."
            )
        }

        override suspend fun verifyOtp(email: String, otp: String): BackendVerifyOutcome {
            val meta = registeredAccounts[email] ?: return BackendVerifyOutcome.InvalidOrExpired
            if (otp != expectedOtp) {
                return BackendVerifyOutcome.InvalidOrExpired
            }
            val uid = "user_${email.hashCode()}"
            return BackendVerifyOutcome.Verified(
                userId = uid,
                email = email,
                metadata = meta,
                accessToken = "otp_jwt_${System.currentTimeMillis()}",
                refreshToken = "otp_refresh_${System.currentTimeMillis()}",
                expiresAtEpochSec = (System.currentTimeMillis() / 1000L) + 3600L
            )
        }

        override suspend fun validateOrRefreshSession(session: AuthSession): BackendTokenValidation {
            return tokenValidationMode
        }

        override suspend fun revokeSession(accessToken: String) {
            revokedTokens.add(accessToken)
        }
    }
}
