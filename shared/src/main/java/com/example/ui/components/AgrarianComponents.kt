package com.example.ui.components

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
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.LatLngPoint
import com.example.ui.theme.AlertRust
import com.example.ui.theme.AlertRustBg
import com.example.ui.theme.EmeraldPulse
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.HarvestAmber
import com.example.ui.theme.HarvestGoldBg
import com.example.ui.theme.MintHighlight
import com.example.ui.theme.SoftSage
import com.example.ui.viewmodel.InsufficientWalletAlert
import com.example.ui.viewmodel.RazorpayPurpose
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

enum class OnlinePaymentOption(
    val label: String,
    val razorpayMethod: String,
    val testTagId: String
) {
    GPAY("GPay", "upi", "payment_option_gpay"),
    PHONEPE("PhonePe", "upi", "payment_option_phonepe"),
    PAYTM("Paytm", "upi", "payment_option_paytm"),
    UPI_QR("UPI QR", "upi", "payment_option_upi_qr"),
    CARDS("Cards", "card", "payment_option_cards"),
    NETBANKING("Netbanking", "netbanking", "payment_option_netbanking");

    fun icon(): ImageVector = when (this) {
        GPAY, PHONEPE, PAYTM -> Icons.Rounded.AccountBalanceWallet
        UPI_QR -> Icons.Rounded.QrCode2
        CARDS -> Icons.Rounded.CreditCard
        NETBANKING -> Icons.Rounded.AccountBalance
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RazorpayCheckoutSheet(
    state: RazorpaySheetState,
    onConfirmPayment: (OnlinePaymentOption) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedOption by remember { mutableStateOf(OnlinePaymentOption.GPAY) }
    val optionRows = remember { OnlinePaymentOption.entries.chunked(3) }

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
                text = "Select Payment Method",
                style = MaterialTheme.typography.labelLarge
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                optionRows.forEach { rowOptions ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowOptions.forEach { option ->
                            val isSelected = selectedOption == option
                            val bgColor by animateColorAsState(
                                if (isSelected) SoftSage else MaterialTheme.colorScheme.surfaceVariant,
                                label = "paymentOptionBgColor"
                            )
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(54.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { selectedOption = option }
                                    .border(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) ForestGreen else MaterialTheme.colorScheme.outline,
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .testTag(option.testTagId),
                                color = bgColor
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = option.icon(),
                                        contentDescription = null,
                                        tint = if (isSelected) ForestGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = option.label,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) ForestGreen else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (state.purpose is RazorpayPurpose.FarmerOrderCheckout) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = HarvestGoldBg,
                    border = BorderStroke(1.dp, HarvestAmber.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Non-cancellable after booking & payment. Pilot payout is released only after you click Job Done.",
                        style = MaterialTheme.typography.labelSmall,
                        color = ForestGreen,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }

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
                val gatewayLabel = if (com.example.data.remote.KrishivantriRuntimeConfig.isRazorpayConfigured()) {
                    "Razorpay Checkout • UPI, QR, Cards & Netbanking"
                } else {
                    "Secure Checkout • UPI, QR, Cards & Netbanking"
                }
                Text(
                    text = gatewayLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = { onConfirmPayment(selectedOption) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("razorpay_pay_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen)
            ) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Pay ${formatInr(state.amount)} via ${selectedOption.label}",
                    style = MaterialTheme.typography.labelLarge
                )
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
