package com.example.data.remote

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class CreateRazorpaySessionRequest(
    val amount_inr: Double,
    val description: String,
    val customer_name: String,
    val customer_phone: String,
    val reference_id: String,
    val callback_url: String = "https://krishivantri.in/razorpay-callback"
)

@JsonClass(generateAdapter = true)
data class CreateRazorpaySessionResponse(
    val payment_link_id: String,
    val checkout_url: String,
    val callback_url: String? = null,
    val reference_id: String? = null
)

@JsonClass(generateAdapter = true)
data class VerifyRazorpayPaymentRequest(
    val razorpay_payment_id: String,
    val razorpay_payment_link_id: String,
    val razorpay_payment_link_reference_id: String,
    val razorpay_payment_link_status: String,
    val razorpay_signature: String
)

@JsonClass(generateAdapter = true)
data class VerifyRazorpayPaymentResponse(
    val verified: Boolean,
    val payment_id: String? = null,
    val detail: String? = null
)

sealed class RazorpaySessionResult {
    data class Success(
        val paymentLinkId: String,
        val checkoutUrl: String,
        val callbackUrl: String,
        val referenceId: String
    ) : RazorpaySessionResult()

    data class Error(val message: String) : RazorpaySessionResult()
}

sealed class RazorpayVerifyResult {
    data class Verified(val paymentId: String) : RazorpayVerifyResult()
    data class Error(val message: String) : RazorpayVerifyResult()
}

/**
 * Server-proxied Razorpay gateway client.
 * Never reads or bundles Razorpay API keys in the Android APK; all Razorpay API calls
 * and HMAC-SHA256 signature verification run strictly on the backend (Supabase Edge Function
 * or Django API).
 */
object RazorpayGatewayClient {

    const val DEFAULT_CALLBACK_URL = "https://krishivantri.in/razorpay-callback"

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val moshi: Moshi by lazy {
        Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    }

    private val httpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        OkHttpClient.Builder()
            .connectTimeout(4, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()
    }

    suspend fun createHostedCheckoutSession(
        amountInr: Double,
        description: String,
        customerName: String,
        customerPhone: String,
        referenceId: String
    ): RazorpaySessionResult = withContext(Dispatchers.IO) {
        val payload = CreateRazorpaySessionRequest(
            amount_inr = amountInr,
            description = description.take(200),
            customer_name = customerName.ifBlank { "Krishivantri Farmer" },
            customer_phone = normalizePhone(customerPhone),
            reference_id = referenceId,
            callback_url = DEFAULT_CALLBACK_URL
        )
        val payloadJson = moshi.adapter(CreateRazorpaySessionRequest::class.java).toJson(payload)

        val candidateEndpoints = buildList {
            if (SupabaseClient.isConfigured()) {
                val base = KrishivantriRuntimeConfig.supabaseUrl.trim().trimEnd('/')
                add(EndpointCandidate("$base/functions/v1/create-razorpay-checkout", isSupabase = true))
            }
            val apiBase = KrishivantriRuntimeConfig.apiBaseUrl.trim().trimEnd('/')
            if (apiBase.startsWith("https://") || apiBase.startsWith("http://")) {
                add(EndpointCandidate("$apiBase/payments/razorpay/create-session/", isSupabase = false))
            }
        }

        if (candidateEndpoints.isEmpty()) {
            return@withContext RazorpaySessionResult.Error(
                "No backend payment endpoint configured. Deploy the create-razorpay-checkout Supabase Edge Function or backend API."
            )
        }

        val errors = mutableListOf<String>()
        for (endpoint in candidateEndpoints) {
            val attempt = executeSessionRequest(endpoint, payloadJson, referenceId)
            if (attempt is RazorpaySessionResult.Success) {
                return@withContext attempt
            } else if (attempt is RazorpaySessionResult.Error) {
                errors += attempt.message
            }
        }

        val combined = errors.joinToString(" | ")
        RazorpaySessionResult.Error(
            if (combined.isNotBlank()) {
                combined
            } else {
                "Razorpay server proxy is not reachable. Deploy the create-razorpay-checkout Supabase Edge Function (--no-verify-jwt) with RAZORPAY_KEY_ID and RAZORPAY_KEY_SECRET in Supabase Secrets."
            }
        )
    }

    suspend fun verifyPayment(
        paymentId: String,
        paymentLinkId: String,
        referenceId: String,
        status: String,
        signature: String
    ): RazorpayVerifyResult = withContext(Dispatchers.IO) {
        val payload = VerifyRazorpayPaymentRequest(
            razorpay_payment_id = paymentId,
            razorpay_payment_link_id = paymentLinkId,
            razorpay_payment_link_reference_id = referenceId,
            razorpay_payment_link_status = status,
            razorpay_signature = signature
        )
        val payloadJson = moshi.adapter(VerifyRazorpayPaymentRequest::class.java).toJson(payload)

        val candidateEndpoints = buildList {
            if (SupabaseClient.isConfigured()) {
                val base = KrishivantriRuntimeConfig.supabaseUrl.trim().trimEnd('/')
                add(EndpointCandidate("$base/functions/v1/verify-razorpay-payment", isSupabase = true))
            }
            val apiBase = KrishivantriRuntimeConfig.apiBaseUrl.trim().trimEnd('/')
            if (apiBase.startsWith("https://") || apiBase.startsWith("http://")) {
                add(EndpointCandidate("$apiBase/payments/razorpay/verify/", isSupabase = false))
            }
        }

        var lastError: String? = null
        for (endpoint in candidateEndpoints) {
            val attempt = executeVerifyRequest(endpoint, payloadJson, paymentId)
            if (attempt is RazorpayVerifyResult.Verified) {
                return@withContext attempt
            } else if (attempt is RazorpayVerifyResult.Error) {
                lastError = attempt.message
            }
        }
        RazorpayVerifyResult.Error(lastError ?: "Server payment verification failed.")
    }

    private fun executeSessionRequest(
        endpoint: EndpointCandidate,
        payloadJson: String,
        fallbackReferenceId: String
    ): RazorpaySessionResult {
        return runCatching {
            val reqBuilder = Request.Builder()
                .url(endpoint.url)
                .post(payloadJson.toRequestBody(jsonMediaType))
                .header("Content-Type", "application/json")
            if (endpoint.isSupabase) {
                val key = KrishivantriRuntimeConfig.supabaseAnonKey.trim()
                reqBuilder.header("apikey", key)
                if (key.count { it == '.' } == 2) {
                    reqBuilder.header("Authorization", "Bearer $key")
                }
            }
            httpClient.newCall(reqBuilder.build()).execute().use { response ->
                val rawBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val detail = extractErrorMessage(rawBody)
                    val hint = when {
                        endpoint.isSupabase && response.code == 404 ->
                            "Supabase Edge Function 'create-razorpay-checkout' is not deployed yet. Deploy it with --no-verify-jwt and set RAZORPAY_KEY_ID & RAZORPAY_KEY_SECRET in Supabase Secrets."
                        endpoint.isSupabase && response.code == 401 ->
                            "Supabase Edge Function rejected auth (401). Redeploy 'create-razorpay-checkout' with --no-verify-jwt."
                        else ->
                            "Payment server (${response.code}): ${detail ?: "Server Razorpay configuration missing."}"
                    }
                    return RazorpaySessionResult.Error(hint)
                }
                val parsed = moshi.adapter(CreateRazorpaySessionResponse::class.java).fromJson(rawBody)
                if (parsed == null || parsed.checkout_url.isBlank()) {
                    return RazorpaySessionResult.Error("Invalid checkout session response from payment server.")
                }
                RazorpaySessionResult.Success(
                    paymentLinkId = parsed.payment_link_id,
                    checkoutUrl = parsed.checkout_url,
                    callbackUrl = parsed.callback_url?.takeIf { it.isNotBlank() } ?: DEFAULT_CALLBACK_URL,
                    referenceId = parsed.reference_id?.takeIf { it.isNotBlank() } ?: fallbackReferenceId
                )
            }
        }.getOrElse { err ->
            RazorpaySessionResult.Error(
                if (endpoint.isSupabase) {
                    "Supabase payment call failed: ${err.localizedMessage ?: "network error"}"
                } else {
                    "Backend (${endpoint.url}) unreachable"
                }
            )
        }
    }

    private fun executeVerifyRequest(
        endpoint: EndpointCandidate,
        payloadJson: String,
        paymentId: String
    ): RazorpayVerifyResult {
        return runCatching {
            val reqBuilder = Request.Builder()
                .url(endpoint.url)
                .post(payloadJson.toRequestBody(jsonMediaType))
                .header("Content-Type", "application/json")
            if (endpoint.isSupabase) {
                val key = KrishivantriRuntimeConfig.supabaseAnonKey.trim()
                reqBuilder.header("apikey", key)
                if (key.count { it == '.' } == 2) {
                    reqBuilder.header("Authorization", "Bearer $key")
                }
            }
            httpClient.newCall(reqBuilder.build()).execute().use { response ->
                val rawBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val detail = extractErrorMessage(rawBody)
                    return RazorpayVerifyResult.Error(detail ?: "Verification failed (${response.code}).")
                }
                val parsed = moshi.adapter(VerifyRazorpayPaymentResponse::class.java).fromJson(rawBody)
                if (parsed?.verified == true) {
                    RazorpayVerifyResult.Verified(parsed.payment_id ?: paymentId)
                } else {
                    RazorpayVerifyResult.Error(parsed?.detail ?: "Razorpay signature verification failed.")
                }
            }
        }.getOrElse { err ->
            RazorpayVerifyResult.Error(err.localizedMessage ?: "Verification request failed.")
        }
    }

    private fun extractErrorMessage(rawBody: String): String? {
        if (rawBody.isBlank()) return null
        return runCatching {
            val obj = JSONObject(rawBody)
            obj.optString("error").takeIf { it.isNotBlank() }
                ?: obj.optString("detail").takeIf { it.isNotBlank() }
                ?: obj.optString("message").takeIf { it.isNotBlank() }
        }.getOrNull()
    }

    private fun normalizePhone(raw: String): String {
        val digits = raw.filter { it.isDigit() }
        return when {
            digits.length == 10 -> "+91$digits"
            digits.length > 10 -> "+$digits"
            else -> "+919999999999"
        }
    }

    private data class EndpointCandidate(
        val url: String,
        val isSupabase: Boolean
    )
}
