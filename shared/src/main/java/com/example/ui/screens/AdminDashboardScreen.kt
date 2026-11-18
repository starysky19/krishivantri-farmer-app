package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.DailyOrderVolume
import com.example.ui.components.formatInr
import com.example.ui.theme.EmeraldPulse
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.HarvestAmber
import com.example.ui.theme.HarvestGoldBg
import com.example.ui.theme.SoftSage
import com.example.ui.theme.VerifiedTeal
import com.example.ui.theme.VerifiedTealBg
import com.example.ui.viewmodel.KrishivantriViewModel.AdminOverviewState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    overview: AdminOverviewState,
    onVerifyPilotLicense: (pilotId: String, verified: Boolean) -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedSection by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ForestGreen
                ),
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AdminPanelSettings,
                            contentDescription = null,
                            tint = SoftSage
                        )
                        Column {
                            Text(
                                text = "Krishivantri Admin Console",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = SoftSage
                            )
                            Text(
                                text = "Restricted Operations & Commission Ledger",
                                style = MaterialTheme.typography.labelSmall,
                                color = SoftSage.copy(alpha = 0.8f)
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = onSignOut,
                        modifier = Modifier.testTag("admin_sign_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Logout,
                            contentDescription = "Sign Out",
                            tint = SoftSage
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Platform Overview Grid
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AdminStatCard(
                            label = "Registered Pilots",
                            value = "${overview.totalPilots}",
                            sub = "${overview.verifiedPilots} Verified",
                            modifier = Modifier.weight(1f)
                        )
                        AdminStatCard(
                            label = "Registered Farmers",
                            value = "${overview.totalFarmers}",
                            sub = "Farmer IDs Active",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AdminStatCard(
                            label = "Cumulative Orders",
                            value = "${overview.totalOrders}",
                            sub = "GMV ${formatInr(overview.totalGmv)}",
                            modifier = Modifier.weight(1f)
                        )
                        AdminStatCard(
                            label = "10% Admin Commission",
                            value = formatInr(overview.totalPlatformCommission),
                            sub = "Settled Ledger",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Daily Order Volume Breakdown
                item {
                    DailyOrderVolumeCard(volumes = overview.dailyVolumes)
                }

                // Section Switcher
                item {
                    TabRow(
                        selectedTabIndex = selectedSection,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = ForestGreen,
                        modifier = Modifier.clip(RoundedCornerShape(14.dp))
                    ) {
                        Tab(
                            selected = selectedSection == 0,
                            onClick = { selectedSection = 0 },
                            text = { Text("Pilots (${overview.pilotStats.size})") }
                        )
                        Tab(
                            selected = selectedSection == 1,
                            onClick = { selectedSection = 1 },
                            text = { Text("Farmers (${overview.farmers.size})") }
                        )
                        Tab(
                            selected = selectedSection == 2,
                            onClick = { selectedSection = 2 },
                            text = { Text("Commissions (${overview.commissionLogs.size})") }
                        )
                    }
                }

                when (selectedSection) {
                    0 -> {
                        if (overview.pilotStats.isEmpty()) {
                            item { EmptyAdminNotice("No drone pilots registered yet.") }
                        } else {
                            items(overview.pilotStats, key = { it.pilotId }) { stat ->
                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            if (stat.licenseDocumentUri.isNotBlank()) {
                                                AsyncImage(
                                                    model = stat.licenseDocumentUri,
                                                    contentDescription = "Pilot license document",
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier
                                                        .size(50.dp)
                                                        .clip(RoundedCornerShape(12.dp))
                                                )
                                            } else {
                                                Box(
                                                    modifier = Modifier
                                                        .size(50.dp)
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .background(SoftSage),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Verified,
                                                        contentDescription = null,
                                                        tint = ForestGreen
                                                    )
                                                }
                                            }
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = stat.pilotName,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "${stat.droneType.label} (${stat.tankCapacity.toInt()}L) • ★ ${String.format(Locale.US, "%.1f", stat.ratingAverage)}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = "Completed: ${stat.completedOrdersCount} jobs • 90% Earned: ${formatInr(stat.totalEarnings)}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = EmeraldPulse
                                                )
                                            }
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = if (stat.isLicenseVerified) VerifiedTealBg else HarvestGoldBg
                                            ) {
                                                Text(
                                                    text = if (stat.isLicenseVerified) "License Verified" else "Pending Verification",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (stat.isLicenseVerified) VerifiedTeal else HarvestAmber,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                                )
                                            }
                                            if (stat.isLicenseVerified) {
                                                OutlinedButton(
                                                    onClick = { onVerifyPilotLicense(stat.pilotId, false) }
                                                ) {
                                                    Text("Revoke")
                                                }
                                            } else {
                                                Button(
                                                    onClick = { onVerifyPilotLicense(stat.pilotId, true) },
                                                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                                                    modifier = Modifier.testTag("verify_pilot_${stat.pilotId}")
                                                ) {
                                                    Icon(
                                                        Icons.Rounded.CheckCircle,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(Modifier.size(6.dp))
                                                    Text("Verify License")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    1 -> {
                        if (overview.farmers.isEmpty()) {
                            item { EmptyAdminNotice("No farmers registered yet.") }
                        } else {
                            items(overview.farmers, key = { it.user.id }) { farmer ->
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = farmer.user.name,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "${farmer.user.phone} • Farmer ID: ${farmer.profile.farmerIdNumber}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Surface(shape = RoundedCornerShape(10.dp), color = HarvestGoldBg) {
                                            Text(
                                                text = "${farmer.profile.coins} Coins",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = HarvestAmber,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    2 -> {
                        if (overview.commissionLogs.isEmpty()) {
                            item { EmptyAdminNotice("No 10% commission entries recorded yet.") }
                        } else {
                            val dateFmt = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
                            items(overview.commissionLogs, key = { it.id }) { log ->
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = log.note,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = "${log.paymentMethod.uppercase(Locale.US)} • ${dateFmt.format(Date(log.createdAt))}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text(
                                            text = "+${formatInr(log.amount)}",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = ForestGreen
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminStatCard(
    label: String,
    value: String,
    sub: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = ForestGreen
            )
            Text(
                text = sub,
                style = MaterialTheme.typography.labelSmall,
                color = EmeraldPulse
            )
        }
    }
}

@Composable
private fun DailyOrderVolumeCard(volumes: List<DailyOrderVolume>) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Daily Order Volume",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = ForestGreen
            )
            if (volumes.isEmpty()) {
                Text(
                    text = "Daily order counts will chart automatically once farmers submit spray bookings.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val maxCount = volumes.maxOf { it.count }.coerceAtLeast(1)
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                ) {
                    val slotW = size.width / volumes.size.coerceAtLeast(1)
                    val barW = (slotW * 0.45f).coerceAtMost(48f)
                    volumes.forEachIndexed { idx, vol ->
                        val ratio = vol.count.toFloat() / maxCount.toFloat()
                        val barH = (size.height - 16f) * ratio.coerceAtLeast(0.12f)
                        val left = idx * slotW + (slotW - barW) / 2f
                        drawRoundRect(
                            color = EmeraldPulse,
                            topLeft = Offset(left, size.height - barH),
                            size = Size(barW, barH),
                            cornerRadius = CornerRadius(12f, 12f)
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    volumes.forEach { vol ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${vol.count}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = ForestGreen
                            )
                            Text(
                                text = vol.dayLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyAdminNotice(text: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(18.dp)
        )
    }
}
