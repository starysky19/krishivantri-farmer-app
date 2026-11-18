package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.LatLngPoint
import com.example.data.remote.RazorpayGatewayClient
import com.example.ui.theme.AlertRust
import com.example.ui.theme.AlertRustBg
import com.example.ui.theme.EmeraldPulse
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.MintHighlight
import com.example.ui.theme.SoftSage
import com.example.ui.viewmodel.HostedCheckoutSession
import com.example.ui.viewmodel.InsufficientWalletAlert
import com.example.ui.viewmodel.RazorpaySheetState
import java.text.NumberFormat
import java.util.Locale

fun formatInr(amount: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale("en", "IN"))
    formatter.maximumFractionDigits = if (amount % 1.0 == 0.0) 0 else 2
    formatter.minimumFractionDigits = 0
    return "₹${formatter.format(amount)}"
}

@Composable
fun KrishivantriBrandMark(
    modifier: Modifier = Modifier,
    title: String = "Krishivantri",
    subtitle: String = "Precision Drone Agritech"
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.linearGradient(listOf(ForestGreen, EmeraldPulse))
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Eco,
                contentDescription = "$title emblem",
                tint = SoftSage,
                modifier = Modifier.size(22.dp)
            )
        }
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun MiniPolygonPreview(
    points: List<LatLngPoint>,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.size(64.dp),
        shape = RoundedCornerShape(14.dp),
        color = SoftSage.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(10.dp)) {
            if (points.size < 3) {
                drawCircle(color = EmeraldPulse, radius = size.minDimension / 4f)
                return@Canvas
            }
            val minLat = points.minOf { it.lat }
            val maxLat = points.maxOf { it.lat }
            val minLng = points.minOf { it.lng }
            val maxLng = points.maxOf { it.lng }
            val latSpan = (maxLat - minLat).coerceAtLeast(1e-6)
            val lngSpan = (maxLng - minLng).coerceAtLeast(1e-6)

            val offsets = points.map { pt ->
                val x = ((pt.lng - minLng) / lngSpan).toFloat() * size.width
                val y = (1f - ((pt.lat - minLat) / latSpan).toFloat()) * size.height
                Offset(x, y)
            }
            val path = Path().apply {
                moveTo(offsets.first().x, offsets.first().y)
                offsets.drop(1).forEach { lineTo(it.x, it.y) }
                close()
            }
            drawPath(path, color = MintHighlight.copy(alpha = 0.45f))
            drawPath(
                path,
                color = ForestGreen,
                style = Stroke(width = 3.5f, join = StrokeJoin.Round)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RazorpayCheckoutSheet(
    state: RazorpaySheetState,
    onConfirmPayment: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedUpiApp by remember { mutableStateOf("GPay UPI") }
    var upiHandle by remember { mutableStateOf("farmer@okicici") }
    val upiOptions = listOf("GPay UPI", "PhonePe", "Paytm", "UPI ID")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Razorpay Trusted Checkout",
                        style = MaterialTheme.typography.labelMedium,
                        color = EmeraldPulse
                    )
                    Text(
                        text = state.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = state.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SoftSage
                ) {
                    Text(
                        text = formatInr(state.amount),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = ForestGreen,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }

            Text(
                text = "Select UPI Method",
                style = MaterialTheme.typography.labelLarge
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                upiOptions.forEach { option ->
                    val isSelected = selectedUpiApp == option
                    val bgColor by animateColorAsState(
                        if (isSelected) SoftSage else MaterialTheme.colorScheme.surfaceVariant,
                        label = "upiChipColor"
                    )
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { selectedUpiApp = option }
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) ForestGreen else MaterialTheme.colorScheme.outline,
                                shape = RoundedCornerShape(12.dp)
                            ),
                        color = bgColor
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = option.replace(" UPI", ""),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) ForestGreen else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = upiHandle,
                onValueChange = { upiHandle = it },
                label = { Text("UPI VPA / Handle") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )

            val isSdkKeyConfigured = com.example.data.remote.KrishivantriRuntimeConfig.isRazorpayConfigured()
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = null,
                    tint = EmeraldPulse,
                    modifier = Modifier.size(16.dp)
                )
                val gatewayLabel = if (isSdkKeyConfigured) {
                    "Razorpay Checkout • Key ID from AI Studio Secrets (Secret stays server-only)"
                } else {
                    "Razorpay Server Checkout • Set RAZORPAY_KEY_ID in AI Studio Secrets or deploy Edge Function"
                }
                Text(
                    text = gatewayLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            state.errorMessage?.let { errorMsg ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = AlertRustBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMsg,
                        style = MaterialTheme.typography.bodySmall,
                        color = AlertRust,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Text(
                text = "Bookings are final and cannot be cancelled after payment. Payout is released to the pilot only after you tap Job Done.",
                style = MaterialTheme.typography.labelSmall,
                color = AlertRust
            )

            Button(
                onClick = onConfirmPayment,
                enabled = !state.isCreatingSession,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("razorpay_pay_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen)
            ) {
                if (state.isCreatingSession) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "Creating Secure Checkout...",
                        style = MaterialTheme.typography.labelLarge
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Pay ${formatInr(state.amount)} via $selectedUpiApp",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun RazorpayHostedCheckoutDialog(
    session: HostedCheckoutSession,
    onPaymentCallback: (
        paymentId: String,
        paymentLinkId: String,
        referenceId: String,
        status: String,
        signature: String
    ) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var isPageLoading by remember { mutableStateOf(true) }
    var callbackDispatched by remember(session.checkoutUrl) { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = session.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${formatInr(session.amount)} • Hosted by Razorpay",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Close Razorpay Checkout"
                            )
                        }
                    }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                var webLoadError by remember { mutableStateOf<String?>(null) }

                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.javaScriptCanOpenWindowsAutomatically = true
                            settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                            CookieManager.getInstance().setAcceptCookie(true)
                            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                            webViewClient = object : WebViewClient() {
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    isPageLoading = false
                                }

                                override fun onReceivedError(
                                    view: WebView?,
                                    request: WebResourceRequest?,
                                    error: WebResourceError?
                                ) {
                                    if (request?.isForMainFrame == true) {
                                        isPageLoading = false
                                        webLoadError = error?.description?.toString()
                                            ?: "Unable to load Razorpay checkout page."
                                    }
                                }

                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): Boolean {
                                    val url = request?.url?.toString().orEmpty()
                                    val uri = request?.url ?: return false

                                    val isCallback = url.startsWith(session.callbackUrl) ||
                                        url.startsWith(RazorpayGatewayClient.DEFAULT_CALLBACK_URL) ||
                                        uri.scheme == "krishivantri"

                                    if (isCallback) {
                                        if (!callbackDispatched) {
                                            callbackDispatched = true
                                            val paymentId = uri.getQueryParameter("razorpay_payment_id").orEmpty()
                                            val linkId = uri.getQueryParameter("razorpay_payment_link_id").orEmpty()
                                            val refId = uri.getQueryParameter("razorpay_payment_link_reference_id").orEmpty()
                                            val status = uri.getQueryParameter("razorpay_payment_link_status").orEmpty()
                                            val signature = uri.getQueryParameter("razorpay_signature").orEmpty()
                                            onPaymentCallback(paymentId, linkId, refId, status, signature)
                                        }
                                        return true
                                    }

                                    val scheme = uri.scheme?.lowercase(Locale.ROOT).orEmpty()
                                    if (scheme == "intent") {
                                        runCatching {
                                            val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                                            if (intent.resolveActivity(context.packageManager) != null) {
                                                context.startActivity(intent)
                                            } else {
                                                val fallbackUrl = intent.getStringExtra("browser_fallback_url")
                                                if (!fallbackUrl.isNullOrBlank()) {
                                                    view?.loadUrl(fallbackUrl)
                                                }
                                            }
                                        }
                                        return true
                                    }
                                    if (scheme != "http" && scheme != "https") {
                                        runCatching {
                                            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                        }
                                        return true
                                    }
                                    return false
                                }
                            }
                            loadUrl(session.checkoutUrl)
                        }
                    }
                )

                webLoadError?.let { errText ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = AlertRustBg,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = errText,
                            style = MaterialTheme.typography.bodySmall,
                            color = AlertRust,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                if (isPageLoading || session.isVerifying) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter),
                        color = ForestGreen
                    )
                }
            }
        }
    }
}

@Composable
fun InsufficientWalletDialog(
    alert: InsufficientWalletAlert,
    onTopUpNow: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    val shortfall = (alert.requiredCommission - alert.currentBalance).coerceAtLeast(100.0)
    val recommendedTopUp = kotlin.math.ceil(shortfall / 100.0) * 100.0

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(AlertRustBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.WarningAmber,
                    contentDescription = null,
                    tint = AlertRust
                )
            }
        },
        title = {
            Text(
                text = "Wallet Balance Too Low for Cash Order",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Cash on Field orders deduct the 10% platform commission immediately upon acceptance.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Required 10% Commission: ${formatInr(alert.requiredCommission)}",
                            style = MaterialTheme.typography.labelLarge,
                            color = AlertRust
                        )
                        Text(
                            text = "Current Wallet Balance: ${formatInr(alert.currentBalance)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onTopUpNow(recommendedTopUp.coerceAtLeast(500.0)) },
                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                modifier = Modifier.testTag("wallet_alert_topup_button")
            ) {
                Text("Add ${formatInr(recommendedTopUp.coerceAtLeast(500.0))}")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
