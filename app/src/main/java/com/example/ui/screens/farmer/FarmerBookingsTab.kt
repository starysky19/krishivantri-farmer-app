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
import androidx.compose.material.icons.rounded.Agriculture
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudQueue
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.HeadsetMic
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.DroneType
import com.example.data.model.FarmBoundaryEntity
import com.example.data.model.FarmerProfileEntity
import com.example.data.model.OrderStatus
import com.example.data.model.OrderWithDetails
import com.example.data.model.PaymentMethod
import com.example.domain.GeoPolygonCalculator
import com.example.ui.components.MiniPolygonPreview
import com.example.ui.components.formatInr
import com.example.ui.theme.CharcoalLeaf
import com.example.ui.theme.DeepMoss
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FarmerBookingsTab(
    profile: FarmerProfileEntity?,
    orders: List<OrderWithDetails>,
    savedFarms: List<FarmBoundaryEntity>,
    onViewLiveDrone: () -> Unit,
    onConfirmJobDone: (String) -> Unit,
    onOpenRateDialog: (OrderWithDetails) -> Unit,
    onRebookOrderPolygon: (OrderWithDetails) -> Unit,
    onBookSavedFarm: (FarmBoundaryEntity) -> Unit,
    onGoToBookSpray: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedFilter by remember { mutableIntStateOf(0) } // 0=All, 1=Upcoming, 2=Completed, 3=My Farms
    var reportTarget by remember { mutableStateOf<OrderWithDetails?>(null) }

    val activeOrders = orders.filter {
        it.statusEnum != OrderStatus.COMPLETED && it.statusEnum != OrderStatus.CANCELLED
    }
    val completedOrders = orders.filter { it.statusEnum == OrderStatus.COMPLETED }
    val totalAcresSprayed = completedOrders.sumOf { it.order.acresToSpray }
    val ecoWaterSavedLitres = (totalAcresSprayed * 36.0).toInt()
    val ecoMoneySaved = totalAcresSprayed * 180.0

    val displayedOrders = when (selectedFilter) {
        1 -> activeOrders
        2 -> completedOrders
        else -> orders
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Flight Log & Field Impact Hero Card (Reference Image 3)
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = Color.Transparent,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(Brush.linearGradient(listOf(ForestGreen, DeepMoss)))
                        .padding(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                                    imageVector = Icons.Rounded.Verified,
                                    contentDescription = null,
                                    tint = SoftSage,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "FLIGHT LOG & FIELD IMPACT",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = SoftSage
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = PureSurface.copy(alpha = 0.16f)
                            ) {
                                Text(
                                    text = "Kisan ID: ${profile?.farmerIdNumber ?: "#KV-FARM"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = PureSurface,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Column {
                            Text(
                                text = "${String.format(Locale.US, "%.1f", totalAcresSprayed)} Acres Sprayed",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = PureSurface
                            )
                            Text(
                                text = "Precision drone coverage & saved plots synced to cloud",
                                style = MaterialTheme.typography.bodySmall,
                                color = SoftSage.copy(alpha = 0.9f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ImpactInnerTile(
                                icon = Icons.Rounded.Flight,
                                label = "Missions",
                                value = "${completedOrders.size} Completed",
                                sub = "${activeOrders.size} active",
                                modifier = Modifier.weight(1f)
                            )
                            ImpactInnerTile(
                                icon = Icons.Rounded.Eco,
                                label = "Eco Savings",
                                value = formatInr(ecoMoneySaved),
                                sub = "${ecoWaterSavedLitres}L water saved",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // 2. Segmented Filter Strip (All Bookings | Upcoming | Completed | My Farms)
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val tabs = listOf(
                    0 to "All Bookings (${orders.size})",
                    1 to "Upcoming (${activeOrders.size})",
                    2 to "Completed (${completedOrders.size})",
                    3 to "My Farms (${savedFarms.size})"
                )
                items(tabs) { (idx, label) ->
                    val selected = selectedFilter == idx
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = if (selected) PureSurface else MistSage,
                        border = BorderStroke(
                            1.dp,
                            if (selected) ForestGreen.copy(alpha = 0.45f) else SageOutline
                        ),
                        tonalElevation = if (selected) 2.dp else 0.dp,
                        modifier = Modifier
                            .heightIn(min = 44.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { selectedFilter = idx }
                            .testTag("bookings_filter_$idx")
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = if (selected) ForestGreen else MutedPine
                            )
                        }
                    }
                }
            }
        }

        // 3. Content List: Saved Farms or Bookings
        if (selectedFilter == 3) {
            if (savedFarms.isEmpty()) {
                item {
                    EmptyBookingsCard(
                        title = "No Saved Farm Boundaries",
                        subtitle = "Outline a farmland boundary on the Book Spray map and save it so you can rebook in one tap.",
                        ctaLabel = "Outline Farmland",
                        onAction = onGoToBookSpray
                    )
                }
            } else {
                items(savedFarms, key = { it.id }) { farm ->
                    SavedFarmHistoryCard(
                        farm = farm,
                        onBookFarm = { onBookSavedFarm(farm) }
                    )
                }
            }
        } else {
            if (displayedOrders.isEmpty()) {
                item {
                    EmptyBookingsCard(
                        title = "No Spray Bookings Yet",
                        subtitle = "Select your crop boundary on the Book Spray map and schedule a DGCA-certified drone pilot.",
                        ctaLabel = "Book Drone Spraying",
                        onAction = onGoToBookSpray
                    )
                }
            } else {
                items(displayedOrders, key = { it.order.id }) { item ->
                    BookingHistoryCard(
                        item = item,
                        onViewLiveStatus = onViewLiveDrone,
                        onConfirmJobDone = { onConfirmJobDone(item.order.id) },
                        onOpenReport = { reportTarget = item },
                        onBookAgain = { onRebookOrderPolygon(item) },
                        onRatePilot = { onOpenRateDialog(item) }
                    )
                }
            }
        }

        // 4. Toll-Free Kisan Helpline Card (Reference Image 3)
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MistSage,
                border = BorderStroke(1.dp, SageOutline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = ForestGreen,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.HeadsetMic,
                                    contentDescription = null,
                                    tint = PureSurface,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Need Help With A Booking?",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = CharcoalLeaf
                            )
                            Text(
                                text = "Toll-free Kisan Helpline • 24/7 Support",
                                style = MaterialTheme.typography.labelSmall,
                                color = MutedPine
                            )
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = PureSurface,
                        border = BorderStroke(1.dp, SageOutline),
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .clickable {
                                runCatching {
                                    context.startActivity(
                                        Intent(Intent.ACTION_DIAL, Uri.parse("tel:18001801551"))
                                    )
                                }
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Call, contentDescription = "Call Kisan Helpline", tint = ForestGreen)
                        }
                    }
                }
            }
        }
    }

    reportTarget?.let { order ->
        SpraySummaryReportDialog(
            item = order,
            onDismiss = { reportTarget = null }
        )
    }
}

@Composable
private fun ImpactInnerTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    sub: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = PureSurface.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, PureSurface.copy(alpha = 0.18f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(icon, contentDescription = null, tint = SoftSage, modifier = Modifier.size(14.dp))
                Text(label, style = MaterialTheme.typography.labelSmall, color = SoftSage)
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = PureSurface
            )
            Text(
                text = sub,
                style = MaterialTheme.typography.labelSmall,
                color = SoftSage.copy(alpha = 0.85f)
            )
        }
    }
}

@Composable
private fun BookingHistoryCard(
    item: OrderWithDetails,
    onViewLiveStatus: () -> Unit,
    onConfirmJobDone: () -> Unit,
    onOpenReport: () -> Unit,
    onBookAgain: () -> Unit,
    onRatePilot: () -> Unit
) {
    val status = item.statusEnum
    val isActive = status != OrderStatus.COMPLETED && status != OrderStatus.CANCELLED
    val dateLabel = remember(item.order.createdAt) {
        SimpleDateFormat("dd MMM yyyy • hh:mm a", Locale.US).format(Date(item.order.createdAt))
    }
    val locationParts = item.order.locationAddress.split(" • ")
    val plotName = locationParts.firstOrNull().orEmpty().ifBlank { "Farmland Plot" }
    val cropSubtitle = locationParts.getOrNull(1) ?: "Precision Crop Spray"

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = PureSurface,
        border = BorderStroke(1.dp, if (isActive) ForestGreen.copy(alpha = 0.3f) else SageOutline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top Date + Status Chip
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
                        imageVector = if (isActive) Icons.Rounded.Schedule else Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = if (isActive) HarvestAmber else EmeraldPulse,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = dateLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = CharcoalLeaf
                    )
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isActive) HarvestGoldBg else SoftSage
                ) {
                    Text(
                        text = status.displayLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isActive) HarvestAmber else ForestGreen,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Plot Title, Polygon Thumbnail, Amount & Payment Method
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    MiniPolygonPreview(points = item.polygonPoints, modifier = Modifier.size(52.dp))
                    Column {
                        Text(
                            text = plotName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = CharcoalLeaf,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "$cropSubtitle • ${item.order.acresToSpray} Acres",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedPine,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatInr(item.order.totalAmount),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = CharcoalLeaf
                    )
                    Text(
                        text = if (item.paymentMethodEnum == PaymentMethod.ONLINE) "Paid Online" else "Cash on Field",
                        style = MaterialTheme.typography.labelSmall,
                        color = ForestGreen
                    )
                }
            }

            // Pilot & Drone Details Strip
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MistSage,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (item.pilotName != null) "Pilot & Rating" else "Pilot Dispatch",
                            style = MaterialTheme.typography.labelSmall,
                            color = MutedPine
                        )
                        Text(
                            text = item.pilotName?.let { "$it ★ ${item.pilotRatingAverage ?: 5.0}" }
                                ?: "Matching nearby DGCA pilot...",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = CharcoalLeaf
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Drone Platform",
                            style = MaterialTheme.typography.labelSmall,
                            color = MutedPine
                        )
                        Text(
                            text = DroneType.fromApi(item.pilotDroneType ?: "quadcopter").label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = ForestGreen
                        )
                    }
                }
            }

            // Card Actions
            if (isActive) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onViewLiveStatus,
                        colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                    ) {
                        Icon(Icons.Rounded.Sensors, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("View Live Status", style = MaterialTheme.typography.labelMedium)
                    }
                    if (status == OrderStatus.IN_PROGRESS || status == OrderStatus.AWAITING_CONFIRMATION) {
                        OutlinedButton(
                            onClick = onConfirmJobDone,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Text("Job Done", style = MaterialTheme.typography.labelMedium, color = ForestGreen)
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onOpenReport,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                    ) {
                        Icon(Icons.Rounded.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Spray Summary", style = MaterialTheme.typography.labelMedium)
                    }
                    Button(
                        onClick = onBookAgain,
                        colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                    ) {
                        Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Book Again", style = MaterialTheme.typography.labelMedium)
                    }
                }
                if (item.order.pilotRating == null) {
                    TextButton(
                        onClick = onRatePilot,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Icon(Icons.Rounded.Star, contentDescription = null, tint = HarvestAmber, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Rate Pilot Service", color = ForestGreen, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun SavedFarmHistoryCard(
    farm: FarmBoundaryEntity,
    onBookFarm: () -> Unit
) {
    val points = remember(farm.polygonCoordinatesJson) {
        GeoPolygonCalculator.decodePolygon(farm.polygonCoordinatesJson)
    }
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = PureSurface,
        border = BorderStroke(1.dp, SageOutline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                MiniPolygonPreview(points = points)
                Column {
                    Text(
                        text = farm.farmName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = CharcoalLeaf
                    )
                    Text(
                        text = "${farm.areaAcres} Acres • ${farm.areaHectares} Ha",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedPine
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (farm.isSyncedToSupabase) Icons.Rounded.CloudDone else Icons.Rounded.CloudQueue,
                            contentDescription = null,
                            tint = if (farm.isSyncedToSupabase) EmeraldPulse else HarvestAmber,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = if (farm.isSyncedToSupabase) "Synced to Cloud" else "Pending Sync",
                            style = MaterialTheme.typography.labelSmall,
                            color = MutedPine
                        )
                    }
                }
            }

            Button(
                onClick = onBookFarm,
                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text("Book Spray")
            }
        }
    }
}

@Composable
private fun EmptyBookingsCard(
    title: String,
    subtitle: String,
    ctaLabel: String,
    onAction: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = PureSurface,
        border = BorderStroke(1.dp, SageOutline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Agriculture,
                contentDescription = null,
                tint = EmeraldPulse,
                modifier = Modifier.size(38.dp)
            )
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = CharcoalLeaf)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MutedPine,
                textAlign = TextAlign.Center
            )
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Icon(Icons.Rounded.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(ctaLabel)
            }
        }
    }
}

@Composable
private fun SpraySummaryReportDialog(
    item: OrderWithDetails,
    onDismiss: () -> Unit
) {
    val dateFormatted = remember(item.order.createdAt) {
        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date(item.order.createdAt))
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Mission Spray Summary", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SummaryLine("Order Reference", item.order.id.take(12).uppercase())
                SummaryLine("Date & Time", dateFormatted)
                SummaryLine("Farmland Plot", item.order.locationAddress)
                SummaryLine("Sprayed Area", "${item.order.acresToSpray} Acres")
                SummaryLine("Assigned Pilot", item.pilotName ?: "Verified Pilot")
                SummaryLine("Water Conserved", "~${(item.order.acresToSpray * 36).toInt()} Litres (60% less)")
                SummaryLine("Payment Method", item.paymentMethodEnum.label)
                item.order.razorpayPaymentId?.let { SummaryLine("Razorpay Ref", it) }
                SummaryLine("Total Settled", formatInr(item.order.totalAmount))
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen)
            ) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun SummaryLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MutedPine)
        Spacer(Modifier.width(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = CharcoalLeaf,
            textAlign = TextAlign.End
        )
    }
}
