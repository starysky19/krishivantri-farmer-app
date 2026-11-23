package com.example.domain

import android.util.Log
import java.io.IOException
import java.util.Locale

/**
 * Maps internal technical failures, payment SDK codes, and network errors
 * into safe, user-friendly UI messages while keeping technical diagnostics in logs only.
 */
object AppErrorMapper {

    const val PAYMENT_CANCELLED = "Payment cancelled."
    const val CONNECTION_ISSUE = "Connection issue. Please check your internet and try again."
    const val UNABLE_TO_COMPLETE = "Unable to complete your request. Please try again."
    const val SOMETHING_WENT_WRONG = "Something went wrong. Please try again."
    const val SESSION_EXPIRED = "Session expired. Please log in again."
    const val SERVICE_UNAVAILABLE = "Service is temporarily unavailable. Please try again later."
    const val MAP_LOAD_UNAVAILABLE = "Unable to load the map right now. Please check your internet and try again."
    const val OFFLINE_MAP_UNAVAILABLE = "Unable to save offline map. Please try again."

    private const val RAZORPAY_CODE_PAYMENT_CANCELED = 0
    private const val RAZORPAY_CODE_NETWORK_ERROR = 2

    private val TECHNICAL_PATTERNS = listOf(
        "http://",
        "https://",
        "http ",
        "status code",
        "exception",
        "stack",
        "supabase",
        "postgrest",
        "sql",
        "rls",
        "anon_key",
        "api_key",
        "secret",
        "authentication failed",
        "authentication rejected",
        "bad_request",
        "gateway_error",
        "razorpay",
        "checkout incomplete",
        "unauthorized",
        "forbidden",
        "internal server",
        "maplibre",
        "tile limit",
        "offline cache failed",
        "retrofit",
        "okhttp",
        "sockettimeout",
        "unknownhost",
        "{",
        "}"
    )

    fun logWarning(tag: String, message: String, throwable: Throwable? = null) {
        runCatching {
            if (throwable != null) {
                Log.w(tag, message, throwable)
            } else {
                Log.w(tag, message)
            }
        }
    }

    fun logError(tag: String, message: String, throwable: Throwable? = null) {
        runCatching {
            if (throwable != null) {
                Log.e(tag, message, throwable)
            } else {
                Log.e(tag, message)
            }
        }
    }

    /**
     * Translates Razorpay checkout exit/cancellation and gateway error callbacks
     * into safe, user-friendly messages without exposing SDK codes or raw payloads.
     */
    fun mapRazorpayError(code: Int, rawDescription: String?): String {
        logWarning(
            tag = "KrishivantriPayment",
            message = "Razorpay checkout did not complete (code=$code, detail=${rawDescription ?: "none"})"
        )

        val normalized = rawDescription.orEmpty().lowercase(Locale.ROOT)
        val isExplicitCancel = normalized.contains("cancel") ||
            normalized.contains("exit") ||
            normalized.contains("dismiss") ||
            normalized.contains("abort") ||
            normalized.contains("closed by user") ||
            normalized.contains("payment_cancelled")

        return when {
            code == RAZORPAY_CODE_PAYMENT_CANCELED || isExplicitCancel -> PAYMENT_CANCELLED
            code == RAZORPAY_CODE_NETWORK_ERROR -> CONNECTION_ISSUE
            normalized.contains("network") || normalized.contains("internet") || normalized.contains("timeout") ->
                CONNECTION_ISSUE
            else -> UNABLE_TO_COMPLETE
        }
    }

    fun mapThrowable(
        throwable: Throwable,
        tag: String = "KrishivantriApp",
        fallback: String = SOMETHING_WENT_WRONG
    ): String {
        logError(tag, "Operation failed", throwable)
        return if (throwable is IOException) CONNECTION_ISSUE else fallback
    }

    fun sanitizeUiError(
        rawMessage: String?,
        fallback: String = SOMETHING_WENT_WRONG,
        tag: String = "KrishivantriApp"
    ): String {
        val trimmed = rawMessage?.trim().orEmpty()
        if (trimmed.isEmpty()) return fallback
        val lower = trimmed.lowercase(Locale.ROOT)
        if (TECHNICAL_PATTERNS.any { lower.contains(it) }) {
            logWarning(tag, "Suppressed technical UI message: $trimmed")
            return fallback
        }
        return trimmed
    }
}
