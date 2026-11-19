package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FlightTakeoff
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.ManageAccounts
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.model.DroneType
import com.example.data.model.LatLngPoint
import com.example.data.model.MapStyleType
import com.example.data.model.OperatorLocationMarker
import com.example.data.model.OrderStatus
import com.example.data.model.OrderWithDetails
import com.example.data.model.PaymentMethod
import com.example.data.model.PilotProfileEntity
import com.example.data.model.UserEntity
import com.example.data.repository.MapRepository
import com.example.domain.GeoPolygonCalculator
import com.example.ui.components.MapLibreFarmMap
import com.example.ui.components.MiniPolygonPreview
import com.example.ui.components.formatInr
import com.example.ui.theme.AlertRust
import com.example.ui.theme.AlertRustBg
import com.example.ui.theme.EmeraldPulse
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.HarvestAmber
import com.example.ui.theme.HarvestGoldBg
import com.example.ui.theme.SoftSage
import com.example.ui.theme.VerifiedTeal
import com.example.ui.theme.VerifiedTealBg
import com.example.ui.viewmodel.MapBookingState
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PilotDashboardScreen(
    user: UserEntity,
    profile: PilotProfileEntity?,
    mapRepository: MapRepository,
    mapState: MapBookingState,
    queueOrders: List<OrderWithDetails>,
    assignedOrders: List<OrderWithDetails>,
    realtimeOperators: List<OperatorLocationMarker>,
    onLocationPermissionResult: (Boolean) -> Unit,
    onResumeGpsUpdates: (Boolean) -> Unit,
    onPauseGpsUpdates: () -> Unit,
    onRefreshCurrentLocation: () -> Unit,
    onRefreshPilotSync: () -> Unit,
    onSelectMapStyle: (MapStyleType) -> Unit,
    onMapCameraMoved: (LatLngPoint) -> Unit,
    onMapLoadingChanged: (Boolean) -> Unit,
    onMapError: (String?) -> Unit,
    onRetryMapLoad: () -> Unit,
    onZoomMap: (Boolean) -> Unit,
    onToggleOnline: (Boolean) -> Unit,
    onAcceptOrder: (String) -> Unit,
    onRejectOrder: (String) -> Unit,
    onStartSpraying: (String) -> Unit,
    onCompleteOrder: (String) -> Unit,
    onOpenWalletTopUp: (Double) -> Unit,
    onUpdateAccountDetails: (String, DroneType, String, String, String?) -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        onLocationPermissionResult(granted)
    }

    LaunchedEffect(Unit) {
        val hasFine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (hasFine || hasCoarse) {
            onLocationPermissionResult(true)
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> onResumeGpsUpdates(true)
                Lifecycle.Event.ON_PAUSE -> onPauseGpsUpdates()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            onPauseGpsUpdates()
        }
    }

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val activeMissions = assignedOrders.filter {
        it.statusEnum == OrderStatus.ACCEPTED || it.statusEnum == OrderStatus.IN_PROGRESS
    }
    val completedMissions = assignedOrders.filter { it.statusEnum == OrderStatus.COMPLETED }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                title = {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = user.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (profile?.isLicenseVerified == true) {
                                Icon(
                                    imageVector = Icons.Rounded.Verified,
                                    contentDescription = "Verified Pilot License",
                                    tint = VerifiedTeal,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        val droneLabel = DroneType.fromApi(profile?.droneType ?: "quadcopter").label
                        Text(
                            text = "$droneLabel • ${profile?.tankCapacity?.toInt() ?: 10}L Tank",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    // Wallet pill -> click opens Razorpay Top-Up
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SoftSage,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onOpenWalletTopUp(500.0) }
                            .testTag("pilot_header_wallet_chip")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AccountBalanceWallet,
                                contentDescription = "Wallet reserve",
                                tint = ForestGreen,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = formatInr(profile?.walletBalance ?: 0.0),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = ForestGreen
                            )
                        }
                    }

                    IconButton(
                        onClick = onRefreshPilotSync,
                        modifier = Modifier.testTag("pilot_refresh_sync_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Sync orders from cloud",
                            tint = ForestGreen
                        )
                    }

                    IconButton(
                        onClick = onSignOut,
                        modifier = Modifier.testTag("pilot_sign_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Logout,
                            contentDescription = "Sign Out",
                            tint = ForestGreen
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Rounded.FlightTakeoff, contentDescription = "Dispatch Queue") },
                    label = { Text("Queue (${queueOrders.size})") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ForestGreen,
                        selectedTextColor = ForestGreen,
                        indicatorColor = SoftSage
                    ),
                    modifier = Modifier.testTag("pilot_tab_queue")
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Rounded.Map, contentDescription = "Live Mission Map") },
                    label = { Text("Live Map (${activeMissions.size})") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ForestGreen,
                        selectedTextColor = ForestGreen,
                        indicatorColor = SoftSage
                    ),
                    modifier = Modifier.testTag("pilot_tab_map")
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Rounded.Payments, contentDescription = "Earnings & Wallet") },
                    label = { Text("Earnings") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ForestGreen,
                        selectedTextColor = ForestGreen,
                        indicatorColor = SoftSage
                    ),
                    modifier = Modifier.testTag("pilot_tab_earnings")
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Rounded.ManageAccounts, contentDescription = "Account Details") },
                    label = { Text("Account") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ForestGreen,
                        selectedTextColor = ForestGreen,
                        indicatorColor = SoftSage
                    ),
                    modifier = Modifier.testTag("pilot_tab_account")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> PilotDispatchQueueTab(
                    profile = profile,
                    mapState = mapState,
                    queueOrders = queueOrders,
                    activeMissions = activeMissions,
                    onRequestLocationPermission = {
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    },
                    onToggleOnline = onToggleOnline,
                    onAcceptOrder = { orderId ->
                        onAcceptOrder(orderId)
                        selectedTab = 1
                    },
                    onRejectOrder = onRejectOrder,
                    onOpenWalletTopUp = onOpenWalletTopUp,
                    onOpenLiveMap = { selectedTab = 1 }
                )

                1 -> PilotLiveMapTab(
                    mapRepository = mapRepository,
                    mapState = mapState,
                    activeMissions = activeMissions,
                    queueOrders = queueOrders,
                    realtimeOperators = realtimeOperators,
                    onRefreshCurrentLocation = onRefreshCurrentLocation,
                    onSelectMapStyle = onSelectMapStyle,
                    onMapCameraMoved = onMapCameraMoved,
                    onMapLoadingChanged = onMapLoadingChanged,
                    onMapError = onMapError,
                    onRetryMapLoad = onRetryMapLoad,
                    onZoomMap = onZoomMap,
                    onStartSpraying = onStartSpraying,
                    onCompleteOrder = onCompleteOrder
                )

                2 -> PilotEarningsWalletTab(
                    profile = profile,
                    completedMissions = completedMissions,
                    onOpenWalletTopUp = onOpenWalletTopUp,
                    onEditAccountUpi = { selectedTab = 3 }
                )

                else -> PilotAccountDetailsTab(
                    user = user,
                    profile = profile,
                    onSaveAccountDetails = onUpdateAccountDetails,
                    onSignOut = onSignOut
                )
            }
        }
    }
}

@Composable
private fun PilotDispatchQueueTab(
    profile: PilotProfileEntity?,
    mapState: MapBookingState,
    queueOrders: List<OrderWithDetails>,
    activeMissions: List<OrderWithDetails>,
    onRequestLocationPermission: () -> Unit,
    onToggleOnline: (Boolean) -> Unit,
    onAcceptOrder: (String) -> Unit,
    onRejectOrder: (String) -> Unit,
    onOpenWalletTopUp: (Double) -> Unit,
    onOpenLiveMap: () -> Unit
) {
    val isOnline = profile?.isOnline == true

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 620.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Online / Offline Duty Toggle Card
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isOnline) SoftSage else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, if (isOnline) ForestGreen else MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isOnline) "You are ONLINE" else "You are OFFLINE",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isOnline) ForestGreen else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isOnline) {
                                    "Live GPS telemetry active. Incoming farmer spray bookings appear below."
                                } else {
                                    "Switch Online to receive nearby farm spray jobs and broadcast location."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isOnline,
                            onCheckedChange = onToggleOnline,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = ForestGreen
                            ),
                            modifier = Modifier.testTag("pilot_online_switch")
                        )
                    }
                }
            }

            // GPS Permission notice if denied
            if (mapState.locationPermissionDenied) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = HarvestGoldBg,
                        border = BorderStroke(1.dp, HarvestAmber),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "GPS permission needed to calculate field distance & live arrival ETA.",
                                style = MaterialTheme.typography.bodySmall,
                                color = HarvestAmber,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = onRequestLocationPermission) {
                                Text("Enable GPS", color = ForestGreen, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Active mission quick jump banner
            if (activeMissions.isNotEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = VerifiedTealBg,
                        border = BorderStroke(1.dp, VerifiedTeal.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .clickable(onClick = onOpenLiveMap)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${activeMissions.size} Active Spray Mission(s)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = VerifiedTeal
                                )
                                Text(
                                    text = "Tap to open Live Map, Start Spraying, or Complete Job.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ForestGreen
                                )
                            }
                            Icon(
                                imageVector = Icons.Rounded.Map,
                                contentDescription = "Open live map",
                                tint = VerifiedTeal
                            )
                        }
                    }
                }
            }

            // Razorpay Wallet Top-Up Card (needed for Cash on Field 10% commission)
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Commission Wallet Reserve",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = formatInr(profile?.walletBalance ?: 0.0),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = ForestGreen,
                                    modifier = Modifier.testTag("pilot_wallet_balance_text")
                                )
                            }
                            Button(
                                onClick = { onOpenWalletTopUp(500.0) },
                                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("pilot_topup_button")
                            ) {
                                Text("Add ₹500")
                            }
                        }
                        Text(
                            text = "Cash on Field orders deduct 10% platform commission on acceptance via Razorpay wallet reserve.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (!isOnline) {
                item {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Go Online using the toggle above to view pending farmer spray bookings.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(20.dp)
                        )
                    }
                }
            } else if (queueOrders.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "No open spray requests right now. New bookings from farmers sync automatically every 10 seconds.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(20.dp)
                        )
                    }
                }
            } else {
                items(queueOrders, key = { it.order.id }) { item ->
                    PilotQueueOrderCard(
                        item = item,
                        droneType = DroneType.fromApi(profile?.droneType ?: "quadcopter"),
                        onAccept = { onAcceptOrder(item.order.id) },
                        onDecline = { onRejectOrder(item.order.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PilotQueueOrderCard(
    item: OrderWithDetails,
    droneType: DroneType,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    val isCash = item.paymentMethodEnum == PaymentMethod.CASH
    val distLabel = item.liveDistanceKm?.let { GeoPolygonCalculator.formatDistanceLabel(it) } ?: "Distance syncing"
    val etaMins = item.liveDistanceKm?.let { GeoPolygonCalculator.estimateArrivalMinutes(it, droneType) }
        ?: item.order.pilotEtaMinutes

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("pilot_queue_card_${item.order.id}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MiniPolygonPreview(points = item.polygonPoints)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.order.locationAddress,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${item.farmerName} • ${String.format(Locale.US, "%.2f", item.order.acresToSpray)} acres",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Schedule,
                            contentDescription = null,
                            tint = VerifiedTeal,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "$distLabel • ~$etaMins min ETA",
                            style = MaterialTheme.typography.labelSmall,
                            color = VerifiedTeal,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatInr(item.order.pilotEarnings),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ForestGreen
                    )
                    Text(
                        text = "Your 90% Share",
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldPulse
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isCash) HarvestGoldBg else SoftSage,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isCash) {
                        "Cash on Field (${formatInr(item.order.totalAmount)} total) • Deducts ${formatInr(item.order.adminCommission)} commission"
                    } else {
                        "Paid Online via Razorpay (${formatInr(item.order.totalAmount)} total) • Instant wallet credit disabled needed"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isCash) HarvestAmber else ForestGreen,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDecline,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("pilot_decline_button_${item.order.id}")
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Decline", color = AlertRust)
                }
                Button(
                    onClick = onAccept,
                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1.4f)
                        .height(48.dp)
                        .testTag("pilot_accept_button_${item.order.id}")
                ) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Accept Mission")
                }
            }
        }
    }
}

@Composable
private fun PilotLiveMapTab(
    mapRepository: MapRepository,
    mapState: MapBookingState,
    activeMissions: List<OrderWithDetails>,
    queueOrders: List<OrderWithDetails>,
    realtimeOperators: List<OperatorLocationMarker>,
    onRefreshCurrentLocation: () -> Unit,
    onSelectMapStyle: (MapStyleType) -> Unit,
    onMapCameraMoved: (LatLngPoint) -> Unit,
    onMapLoadingChanged: (Boolean) -> Unit,
    onMapError: (String?) -> Unit,
    onRetryMapLoad: () -> Unit,
    onZoomMap: (Boolean) -> Unit,
    onStartSpraying: (String) -> Unit,
    onCompleteOrder: (String) -> Unit
) {
    val allOrdersForMap = activeMissions + queueOrders

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWide = maxWidth >= 720.dp

        val mapContent: @Composable (Modifier) -> Unit = { mapMod ->
            Box(modifier = mapMod) {
                MapLibreFarmMap(
                    mapRepository = mapRepository,
                    styleType = mapState.mapStyleType,
                    cameraTarget = activeMissions.firstOrNull()?.polygonPoints?.firstOrNull() ?: mapState.mapCenter,
                    cameraFlyNonce = mapState.cameraFlyNonce,
                    zoomInNonce = mapState.zoomInNonce,
                    zoomOutNonce = mapState.zoomOutNonce,
                    resetCompassNonce = mapState.resetCompassNonce,
                    styleRetryNonce = mapState.styleRetryNonce,
                    hasLocationPermission = mapState.hasLocationPermission,
                    drawingPoints = activeMissions.firstOrNull()?.polygonPoints.orEmpty(),
                    computedAcres = activeMissions.firstOrNull()?.order?.acresToSpray ?: 0.0,
                    computedHectares = (activeMissions.firstOrNull()?.order?.acresToSpray ?: 0.0) * 0.404686,
                    savedFarms = emptyList(),
                    bookingOrders = allOrdersForMap,
                    operators = realtimeOperators,
                    onMapTapPoint = {},
                    onUpdateBoundaryPoint = { _, _ -> },
                    onMapLoadingChanged = onMapLoadingChanged,
                    onMapError = onMapError,
                    onCameraMoved = onMapCameraMoved,
                    modifier = Modifier.fillMaxSize()
                )

                // Style selector strip at top
                LazyRow(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(MapStyleType.entries) { style ->
                        val active = mapState.mapStyleType == style
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (active) ForestGreen else MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                            border = BorderStroke(
                                1.dp,
                                if (active) ForestGreen else MaterialTheme.colorScheme.outline
                            ),
                            modifier = Modifier
                                .height(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onSelectMapStyle(style) }
                                .testTag("pilot_map_style_${style.name.lowercase(Locale.US)}")
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            ) {
                                Text(
                                    text = style.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (active) Color.White else ForestGreen
                                )
                            }
                        }
                    }
                }

                // Compact zoom + GPS buttons
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PilotMapControlButton(
                        icon = Icons.Rounded.MyLocation,
                        contentDescription = "Center on my GPS location",
                        onClick = onRefreshCurrentLocation
                    )
                    PilotMapControlButton(
                        icon = Icons.Rounded.Add,
                        contentDescription = "Zoom in map",
                        onClick = { onZoomMap(true) }
                    )
                    PilotMapControlButton(
                        icon = Icons.Rounded.Remove,
                        contentDescription = "Zoom out map",
                        onClick = { onZoomMap(false) }
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp)
                ) {
                    Text(
                        text = "© OpenStreetMap contributors",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                val mapError = mapState.mapErrorMessage
                if (!mapError.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = AlertRustBg,
                        border = BorderStroke(1.dp, AlertRust),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(20.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(text = mapError, style = MaterialTheme.typography.bodySmall, color = AlertRust)
                            Button(
                                onClick = onRetryMapLoad,
                                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen)
                            ) {
                                Text("Retry Map")
                            }
                        }
                    }
                }
            }
        }

        val missionPanel: @Composable (Modifier) -> Unit = { panelMod ->
            Surface(
                shape = if (isWide) RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp)
                else RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = panelMod
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Active Spray Missions (${activeMissions.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = ForestGreen
                    )
                    if (activeMissions.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Accept a spray job from Queue to view field boundaries, navigate by GPS, and start spraying.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    } else {
                        activeMissions.forEach { mission ->
                            PilotActiveMissionCard(
                                item = mission,
                                onStartSpraying = { onStartSpraying(mission.order.id) },
                                onCompleteOrder = { onCompleteOrder(mission.order.id) }
                            )
                        }
                    }
                }
            }
        }

        if (isWide) {
            Row(modifier = Modifier.fillMaxSize()) {
                mapContent(Modifier.weight(0.58f).fillMaxHeight())
                missionPanel(Modifier.weight(0.42f).fillMaxHeight())
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                mapContent(Modifier.weight(0.55f).fillMaxWidth())
                missionPanel(Modifier.weight(0.45f).fillMaxWidth())
            }
        }
    }
}

@Composable
private fun PilotActiveMissionCard(
    item: OrderWithDetails,
    onStartSpraying: () -> Unit,
    onCompleteOrder: () -> Unit
) {
    val status = item.statusEnum
    val distLabel = item.liveDistanceKm?.let { GeoPolygonCalculator.formatDistanceLabel(it) } ?: "Calculating distance"

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SoftSage.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, ForestGreen.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${item.farmerName} • ${item.farmerPhone}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = ForestGreen,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${item.order.locationAddress} • ${String.format(Locale.US, "%.2f", item.order.acresToSpray)} ac",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = formatInr(item.order.pilotEarnings),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ForestGreen
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Rounded.LocationOn, contentDescription = null, tint = VerifiedTeal, modifier = Modifier.size(16.dp))
                Text(
                    text = if (status == OrderStatus.ACCEPTED) {
                        "$distLabel • Live ETA ${item.order.pilotEtaMinutes} mins shared with farmer"
                    } else {
                        "Spraying in progress on farm polygon"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = VerifiedTeal,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (status == OrderStatus.ACCEPTED) {
                Button(
                    onClick = onStartSpraying,
                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("pilot_start_spray_${item.order.id}")
                ) {
                    Icon(Icons.Rounded.FlightTakeoff, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Arrived — Start Spraying")
                }
            } else if (status == OrderStatus.IN_PROGRESS) {
                Button(
                    onClick = onCompleteOrder,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPulse),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("pilot_complete_job_${item.order.id}")
                ) {
                    Icon(Icons.Rounded.TaskAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Complete Job (+${formatInr(item.order.pilotEarnings)})")
                }
            }
        }
    }
}

@Composable
private fun PilotMapControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shadowElevation = 4.dp,
        modifier = Modifier.size(48.dp)
    ) {
        IconButton(onClick = onClick) {
            Icon(imageVector = icon, contentDescription = contentDescription, tint = ForestGreen)
        }
    }
}

@Composable
private fun PilotEarningsWalletTab(
    profile: PilotProfileEntity?,
    completedMissions: List<OrderWithDetails>,
    onOpenWalletTopUp: (Double) -> Unit,
    onEditAccountUpi: () -> Unit
) {
    val totalNetEarnings = completedMissions.sumOf { it.order.pilotEarnings }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 620.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = SoftSage,
                    border = BorderStroke(1.dp, ForestGreen.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Total Net Pilot Earnings (90%)",
                            style = MaterialTheme.typography.labelMedium,
                            color = ForestGreen
                        )
                        Text(
                            text = formatInr(totalNetEarnings),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = ForestGreen
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Completed Jobs: ${completedMissions.size}",
                                style = MaterialTheme.typography.labelMedium,
                                color = ForestGreen
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Star,
                                    contentDescription = null,
                                    tint = HarvestAmber,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "${String.format(Locale.US, "%.1f", profile?.ratingAverage ?: 5.0)} (${profile?.ratingCount ?: 0} reviews)",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = HarvestAmber
                                )
                            }
                        }
                    }
                }
            }

            item {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Razorpay Commission Wallet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ForestGreen
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Balance: ${formatInr(profile?.walletBalance ?: 0.0)} • UPI: ${profile?.bankUpiId ?: "-"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(
                                onClick = onEditAccountUpi,
                                modifier = Modifier.testTag("pilot_earnings_edit_upi_button")
                            ) {
                                Text("Edit UPI", color = ForestGreen, fontWeight = FontWeight.Bold)
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(250.0, 500.0, 1000.0).forEach { amt ->
                                OutlinedButton(
                                    onClick = { onOpenWalletTopUp(amt) },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("pilot_topup_preset_${amt.toInt()}")
                                ) {
                                    Text("+${formatInr(amt)}", color = ForestGreen, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            if (completedMissions.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "No completed spray jobs yet. Accept and finish farm jobs to see payouts and farmer ratings here.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(20.dp)
                        )
                    }
                }
            } else {
                items(completedMissions, key = { it.order.id }) { mission ->
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = mission.order.locationAddress,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${mission.farmerName} • ${String.format(Locale.US, "%.2f", mission.order.acresToSpray)} ac • ${mission.paymentMethodEnum.label}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "+${formatInr(mission.order.pilotEarnings)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldPulse
                                )
                                val rating = mission.order.pilotRating
                                if (rating != null) {
                                    Text(
                                        text = "${rating}★ rated",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = HarvestAmber
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
