package com.example

import com.example.domain.AppErrorMapper
import com.example.ui.components.OnlinePaymentOption
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun onlinePaymentOptions_mapToExpectedRazorpayMethods() {
    assertEquals("upi", OnlinePaymentOption.GPAY.razorpayMethod)
    assertEquals("upi", OnlinePaymentOption.PHONEPE.razorpayMethod)
    assertEquals("upi", OnlinePaymentOption.PAYTM.razorpayMethod)
    assertEquals("upi", OnlinePaymentOption.UPI_QR.razorpayMethod)
    assertEquals("card", OnlinePaymentOption.CARDS.razorpayMethod)
    assertEquals("netbanking", OnlinePaymentOption.NETBANKING.razorpayMethod)
    assertEquals(6, OnlinePaymentOption.entries.size)
  }

  @Test
  fun mapRazorpayError_returnsPaymentCancelled_onExitOrCancel() {
    assertEquals(AppErrorMapper.PAYMENT_CANCELLED, AppErrorMapper.mapRazorpayError(0, null))
    assertEquals(AppErrorMapper.PAYMENT_CANCELLED, AppErrorMapper.mapRazorpayError(0, "Yes, Exit"))
    assertEquals(
      AppErrorMapper.PAYMENT_CANCELLED,
      AppErrorMapper.mapRazorpayError(0, "{\"error\":{\"description\":\"Authentication Failed\"}}")
    )
    assertEquals(
      AppErrorMapper.PAYMENT_CANCELLED,
      AppErrorMapper.mapRazorpayError(5, "{\"error\":{\"reason\":\"payment_cancelled\",\"description\":\"Payment processing cancelled by user\"}}")
    )
  }

  @Test
  fun mapRazorpayError_hidesTechnicalErrorsAndCodes() {
    val authFailedResult = AppErrorMapper.mapRazorpayError(5, "Authentication Failed")
    assertEquals(AppErrorMapper.UNABLE_TO_COMPLETE, authFailedResult)
    assertFalse(authFailedResult.contains("Authentication Failed", ignoreCase = true))
    assertFalse(authFailedResult.contains("5"))

    val jsonResult = AppErrorMapper.mapRazorpayError(-1, "{\"error\":{\"code\":\"BAD_REQUEST_ERROR\"}}")
    assertEquals(AppErrorMapper.UNABLE_TO_COMPLETE, jsonResult)
    assertFalse(jsonResult.contains("BAD_REQUEST_ERROR"))
    assertFalse(jsonResult.contains("{"))

    assertEquals(AppErrorMapper.CONNECTION_ISSUE, AppErrorMapper.mapRazorpayError(2, "Network error"))
  }

  @Test
  fun sanitizeUiError_blocksTechnicalStrings_andPreservesFriendlyMessages() {
    assertEquals(
      AppErrorMapper.SOMETHING_WENT_WRONG,
      AppErrorMapper.sanitizeUiError("HTTP 500 Internal Server Error from https://example.supabase.co")
    )
    assertEquals(
      AppErrorMapper.SOMETHING_WENT_WRONG,
      AppErrorMapper.sanitizeUiError("Authentication Failed: {\"code\":401}")
    )
    assertEquals(
      AppErrorMapper.SOMETHING_WENT_WRONG,
      AppErrorMapper.sanitizeUiError("SocketTimeoutException at OkHttp")
    )
    assertEquals(
      "Enter a valid 10-digit mobile number.",
      AppErrorMapper.sanitizeUiError("Enter a valid 10-digit mobile number.")
    )
    assertEquals(
      AppErrorMapper.CONNECTION_ISSUE,
      AppErrorMapper.mapThrowable(IOException("Connection reset by peer"))
    )
    assertEquals(
      AppErrorMapper.SOMETHING_WENT_WRONG,
      AppErrorMapper.mapThrowable(IllegalStateException("SQLiteConstraintException"))
    )
  }

  @Test
  fun razorpaySheetState_preservesPurposeWhenHidingModalForGatewayLaunch() {
    val initial = com.example.ui.viewmodel.RazorpaySheetState(
      title = "Krishivantri Spray Booking",
      subtitle = "2.5 acres • Nashik",
      amount = 1125.0,
      purpose = com.example.ui.viewmodel.RazorpayPurpose.AgriStoreCheckout(
        itemCount = 2,
        summaryLabel = "2x Neem"
      )
    )
    assertFalse(initial.isAwaitingGatewayResult)
    val launched = initial.copy(isAwaitingGatewayResult = true)
    assertTrue(launched.isAwaitingGatewayResult)
    assertEquals(1125.0, launched.amount, 0.001)
    assertEquals(initial.purpose, launched.purpose)
  }
}

