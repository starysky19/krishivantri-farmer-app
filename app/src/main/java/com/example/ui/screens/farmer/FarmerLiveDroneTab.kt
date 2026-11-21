package com.example.ui.screens.farmer

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.FlightTakeoff
import androidx.compose.material.icons.rounded.Height
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Opacity
import androidx.compose.material.icons.rounded.Radar
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.DroneType
import com.example.data.model.LatLngPoint
import com.example.data.model.MapStyleType
import com.example.data.model.OperatorLocationMarker
import com.example.data.model.OrderStatus
import com.example.data.model.OrderWithDetails
import com.example.data.remote.SupabaseConnectionStatus
import com.example.data.repository.MapRepository
import com.example.domain.GeoPolygonCalculator
import com.example.ui.components.MapLibreFarmMap
import com.example.ui.components.formatInr
import com.example.ui.theme.CharcoalLeaf
import com.example.ui.theme.EmeraldPulse
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.HarvestAmber
import com.example.ui.theme.HarvestGoldBg
import com.example.ui.theme.MistSage
import com.example.ui.theme.MutedPine
import com.example.ui.theme.PureSurface
import com.example.ui.theme.SageOutline
import com.example.ui.theme.SoftSage
import com.example.ui.theme.VerifiedTeal
import com.example.ui.theme.VerifiedTealBg
import com.example.ui.viewmodel.DatabaseConnectionUiState

@Composable
fun FarmerLiveDroneTab(
    activeOrders: List<OrderWithDetails>,
    mapRepository: MapRepository,
    realtimeOperators: List<OperatorLocationMarker>,
    databaseStatus: DatabaseConnectionUiState,
    hasLocationPermission: Boolean,
    onConfirmJobDone: (String) -> Unit,
    onShowNonCancellableNotice: () -> Unit,
    onBookNewSpray: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedOrderId by remember(activeOrders) {
        mutableStateOf(activeOrders.firstOrNull()?.order?.id)
    }
    val activeOrder = activeOrders.firstOrNull { it.order.id == selectedOrderId } ?: activeOrders.firstOrNull()

    if (activeOrder == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = PureSurface,
                border = BorderStroke(1.dp, SageOutline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(SoftSage),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Radar,
                            contentDescription = null,
                            tint = ForestGreen,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Text(
                        text = "No Active Drone Mission",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = CharcoalLeaf
                    )
                    Text(
                        text = "Book a spray mission from the map to see real-time pilot GPS telemetry, nozzle flow coverage, and confirm Job Done payout.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedPine,
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = onBookNewSpray,
                        colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Icon(Icons.Rounded.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Book Drone Spraying")
                    }
                }
            }
        }
        return
    }

    val status = activeOrder.statusEnum
    val statusBadgeLabel = when (status) {
        OrderStatus.PENDING -> "FINDING DGCA PILOT"
        OrderStatus.ACCEPTED -> "PILOT EN ROUTE"
        OrderStatus.IN_PROGRESS -> "SPRAYING IN PROGRESS"
        OrderStatus.AWAITING_CONFIRMATION -> "SPRAY COMPLETED • CONFIRM"
        else -> status.displayLabel.uppercase()
    }
    val etaLabel = when (status) {
        OrderStatus.PENDING -> "Assigning (< 15m)"
        OrderStatus.ACCEPTED -> "${activeOrder.order.pilotEtaMinutes}m ETA"
        OrderStatus.IN_PROGRESS -> "${(activeOrder.order.acresToSpray * 4.5).toInt().coerceAtLeast(6)}m remaining"
        OrderStatus.AWAITING_CONFIRMATION -> "Ready for Job Done"
        else -> "Active"
    }
    val coverageFraction = when (status) {
        OrderStatus.PENDING -> 0.05f
        OrderStatus.ACCEPTED -> 0.20f
        OrderStatus.IN_PROGRESS -> 0.68f
        OrderStatus.AWAITING_CONFIRMATION -> 1.0f
        else -> 1.0f
    }
    val coveredAcres = activeOrder.order.acresToSpray * coverageFraction
    val plotCenter = if (activeOrder.order.fieldCenterLat != 0.0) {
        LatLngPoint(activeOrder.order.fieldCenterLat, activeOrder.order.fieldCenterLng)
    } else if (activeOrder.polygonPoints.isNotEmpty()) {
        LatLngPoint(
            activeOrder.polygonPoints.map { it.lat }.average(),
            activeOrder.polygonPoints.map { it.lng }.average()
        )
    } else {
        LatLngPoint(20.0059, 73.7897)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Multi-order switcher if >1 active mission
        if (activeOrders.size > 1) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(activeOrders, key = { it.order.id }) { candidate ->
                        val selected = candidate.order.id == activeOrder.order.id
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = if (selected) ForestGreen else MistSage,
                            border = BorderStroke(1.dp, if (selected) ForestGreen else SageOutline),
                            modifier = Modifier
                                .heightIn(min = 40.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .clickable { selectedOrderId = candidate.order.id }
                        ) {
                            Text(
                                text = "${candidate.order.acresToSpray} ac • ${candidate.order.locationAddress.substringBefore(" • ")}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (selected) PureSurface else CharcoalLeaf,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        // Top Mission Status + ETA Countdown Strip (Reference Image 4)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = SoftSage,
                    border = BorderStroke(1.dp, ForestGreen.copy(alpha = 0.25f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(EmeraldPulse)
                        )
                        Text(
                            text = statusBadgeLabel,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = ForestGreen,
                            maxLines = 2
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MistSage,
                    border = BorderStroke(1.dp, SageOutline)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Schedule,
                            contentDescription = null,
                            tint = ForestGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = etaLabel,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = CharcoalLeaf
                        )
                    }
                }
            }
        }

        // Live Tracking Map Card + Field Coverage Bar
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = PureSurface,
                border = BorderStroke(1.dp, SageOutline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(235.dp)
                    ) {
                        MapLibreFarmMap(
                            mapRepository = mapRepository,
                            styleType = MapStyleType.HYBRID,
                            cameraTarget = plotCenter,
                            cameraFlyNonce = activeOrder.order.id.hashCode(),
                            zoomInNonce = 0,
                            zoomOutNonce = 0,
                            resetCompassNonce = 0,
                            styleRetryNonce = 0,
                            hasLocationPermission = hasLocationPermission,
                            drawingPoints = activeOrder.polygonPoints,
                            computedAcres = activeOrder.order.acresToSpray,
                            computedHectares = GeoPolygonCalculator.calculatePolygonAreaHectares(activeOrder.polygonPoints),
                            savedFarms = emptyList(),
                            bookingOrders = listOf(activeOrder),
                            operators = realtimeOperators,
                            onMapTapPoint = {},
                            onUpdateBoundaryPoint = { _, _ -> },
                            onMapLoadingChanged = {},
                            onMapError = {},
                            onCameraMoved = {},
                            modifier = Modifier.fillMaxSize()
                        )

                        Column(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = PureSurface.copy(alpha = 0.94f),
                                border = BorderStroke(1.dp, SageOutline)
                            ) {
                                Text(
                                    text = "${activeOrder.order.acresToSpray} Acres • ${activeOrder.order.locationAddress.substringBefore(" • ")}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = CharcoalLeaf,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = SoftSage.copy(alpha = 0.95f),
                                border = BorderStroke(1.dp, ForestGreen.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = activeOrder.liveDistanceKm?.let {
                                        "Pilot ${GeoPolygonCalculator.formatDistanceLabel(it)} away"
                                    } ?: status.displayLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ForestGreen,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    // Field Coverage Footer
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(modifier = Modifier.width(68.dp)) {
                            Text(
                                text = "Field Coverage",
                                style = MaterialTheme.typography.labelSmall,
                                color = MutedPine
                            )
                        }
                        Text(
                            text = "${(coverageFraction * 100).toInt()}%",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = ForestGreen
                        )
                        LinearProgressIndicator(
                            progress = { coverageFraction },
                            color = EmeraldPulse,
                            trackColor = SoftSage,
                            modifier = Modifier
                                .weight(1f)
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                        Text(
                            text = "${String.format(java.util.Locale.US, "%.1f", coveredAcres)} / ${activeOrder.order.acresToSpray} ac",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = CharcoalLeaf
                        )
                    }
                }
            }
        }

        // Telemetry HUD Card (Reference Image 4)
        item {
            val isCloudConnected = databaseStatus.cloudStatus is SupabaseConnectionStatus.Connected
            val signalText = when (databaseStatus.cloudStatus) {
                is SupabaseConnectionStatus.Connected -> "Signal: Live Cloud Synced"
                is SupabaseConnectionStatus.Checking -> "Signal: Syncing..."
                else -> "Signal: Telemetry Standby"
            }

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = PureSurface,
                border = BorderStroke(1.dp, SageOutline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Sensors,
                                contentDescription = null,
                                tint = ForestGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "TELEMETRY HUD",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = CharcoalLeaf
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isCloudConnected) VerifiedTealBg else HarvestGoldBg
                        ) {
                            Text(
                                text = signalText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isCloudConnected) VerifiedTeal else HarvestAmber,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TelemetryCell(
                            icon = Icons.Rounded.Speed,
                            value = if (status == OrderStatus.IN_PROGRESS) "18" else if (status == OrderStatus.ACCEPTED) "24" else "0",
                            unit = "KM/H",
                            label = "SPEED",
                            modifier = Modifier.weight(1f)
                        )
                        TelemetryCell(
                            icon = Icons.Rounded.Height,
                            value = if (status == OrderStatus.IN_PROGRESS) "2.5m" else if (status == OrderStatus.ACCEPTED) "12m" else "--",
                            unit = "AGL",
                            label = "ALTITUDE",
                            modifier = Modifier.weight(1f)
                        )
                        TelemetryCell(
                            icon = Icons.Rounded.Opacity,
                            value = if (status == OrderStatus.AWAITING_CONFIRMATION) "10%" else if (status == OrderStatus.IN_PROGRESS) "65%" else "100%",
                            unit = "TANK",
                            label = "PAYLOAD",
                            modifier = Modifier.weight(1f)
                        )
                        TelemetryCell(
                            icon = Icons.Rounded.BatteryChargingFull,
                            value = if (status == OrderStatus.IN_PROGRESS) "78%" else "96%",
                            unit = "PACK",
                            label = "BATTERY",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Assigned Pilot + Drone Hardware Card (Reference Image 4)
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = PureSurface,
                border = BorderStroke(1.dp, SageOutline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (activeOrder.pilotName != null) {
                        val droneLabel = DroneType.fromApi(activeOrder.pilotDroneType ?: "quadcopter").label
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(contentAlignment = Alignment.BottomEnd) {
                                    Surface(
                                        shape = CircleShape,
                                        color = SoftSage,
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = activeOrder.pilotName!!.take(2).uppercase(),
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = ForestGreen
                                            )
                                        }
                                    }
                                    Surface(
                                        shape = CircleShape,
                                        color = PureSurface,
                                        modifier = Modifier.size(18.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Verified,
                                            contentDescription = "Verified",
                                            tint = ForestGreen,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = activeOrder.pilotName!!,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = CharcoalLeaf
                                    )
                                    Text(
                                        text = "DGCA Certified Drone Pilot",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MutedPine
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Star,
                                            contentDescription = null,
                                            tint = HarvestAmber,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "${activeOrder.pilotRatingAverage ?: 4.9} • Priority Verified",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = CharcoalLeaf
                                        )
                                    }
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (!activeOrder.pilotPhone.isNullOrBlank()) {
                                    Surface(
                                        shape = CircleShape,
                                        color = SoftSage,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .clickable {
                                                val dial = Intent(
                                                    Intent.ACTION_DIAL,
                                                    Uri.parse("tel:${activeOrder.pilotPhone}")
                                                )
                                                runCatching { context.startActivity(dial) }
                                            }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Rounded.Call,
                                                contentDescription = "Call Pilot",
                                                tint = ForestGreen
                                            )
                                        }
                                    }
                                }
                                Surface(
                                    shape = CircleShape,
                                    color = MistSage,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .clickable(onClick = onShowNonCancellableNotice)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Rounded.Info,
                                            contentDescription = "Escrow & Policy Info",
                                            tint = ForestGreen
                                        )
                                    }
                                }
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MistSage,
                            border = BorderStroke(1.dp, SageOutline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Flight,
                                        contentDescription = null,
                                        tint = ForestGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "KrishiVantri $droneLabel Platform",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = CharcoalLeaf
                                        )
                                        Text(
                                            text = "Ultrasonic Dual Flow Nozzles • Calibrated",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MutedPine
                                        )
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = VerifiedTealBg
                                ) {
                                    Text(
                                        text = "READY",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = VerifiedTeal,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = HarvestGoldBg,
                                modifier = Modifier.size(46.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Rounded.FlightTakeoff, contentDescription = null, tint = HarvestAmber)
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Assigning Priority DGCA Pilot",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = CharcoalLeaf
                                )
                                Text(
                                    text = "Nearby verified drone operators have received your ${activeOrder.order.acresToSpray} acre polygon & payment mode (${activeOrder.paymentMethodEnum.label}).",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MutedPine
                                )
                            }
                        }
                    }
                }
            }
        }

        // Done Spraying (Confirm Completion) CTA or Escrow Notice
        item {
            if (status == OrderStatus.IN_PROGRESS || status == OrderStatus.AWAITING_CONFIRMATION) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = { onConfirmJobDone(activeOrder.order.id) },
                        colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("farmer_job_done_button_${activeOrder.order.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Done Spraying (Confirm Completion)",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = null,
                            tint = MutedPine,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Click only after the pilot finishes full ${activeOrder.order.acresToSpray} acres. This will disburse ${formatInr(activeOrder.order.pilotEarnings)} escrow payout to the pilot (+1 Harvest Coin).",
                            style = MaterialTheme.typography.labelSmall,
                            color = MutedPine
                        )
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = HarvestGoldBg,
                    border = BorderStroke(1.dp, HarvestAmber.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(onClick = onShowNonCancellableNotice)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Rounded.Lock, contentDescription = null, tint = HarvestAmber, modifier = Modifier.size(18.dp))
                        Text(
                            text = "Booking ${formatInr(activeOrder.order.totalAmount)} is escrow protected & non-cancellable. You can confirm 'Job Done' once spraying begins.",
                            style = MaterialTheme.typography.labelSmall,
                            color = ForestGreen,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TelemetryCell(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    unit: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MistSage,
        border = BorderStroke(1.dp, SageOutline),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = ForestGreen,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = CharcoalLeaf
            )
            Text(
                text = unit,
                style = MaterialTheme.typography.labelSmall,
                color = MutedPine
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MutedPine
            )
        }
    }
}
