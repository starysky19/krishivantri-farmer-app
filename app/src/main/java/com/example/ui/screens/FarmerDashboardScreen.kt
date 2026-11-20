package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Agriculture
import androidx.compose.material.icons.rounded.Assignment
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudQueue
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.FlightTakeoff
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.ManageAccounts
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.model.DroneType
import com.example.data.model.FarmBoundaryEntity
import com.example.data.model.FarmerProfileEntity
import com.example.data.model.LatLngPoint
import com.example.data.model.MapStyleType
import com.example.data.model.OperatorLocationMarker
import com.example.data.model.OrderStatus
import com.example.data.model.OrderWithDetails
import com.example.data.model.PaymentMethod
import com.example.data.model.UserEntity
import com.example.data.repository.MapRepository
import com.example.domain.GeoPolygonCalculator
import com.example.domain.LocationMatch
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FarmerDashboardScreen(
    user: UserEntity,
    profile: FarmerProfileEntity?,
    mapRepository: MapRepository,
    mapState: MapBookingState,
    savedFarms: List<FarmBoundaryEntity>,
    bookingOrdersForMap: List<OrderWithDetails>,
    realtimeOperators: List<OperatorLocationMarker>,
    orders: List<OrderWithDetails>,
    onLocationPermissionResult: (Boolean) -> Unit,
    onResumeGpsUpdates: (Boolean) -> Unit,
    onPauseGpsUpdates: () -> Unit,
    onRefreshCurrentLocation: () -> Unit,
    onSelectMapStyle: (MapStyleType) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onCenterMap: () -> Unit,
    onSelectLocationSuggestion: (LocationMatch) -> Unit,
    onMapTapPoint: (LatLngPoint) -> Unit,
    onUpdateBoundaryPoint: (Int, LatLngPoint) -> Unit,
    onMapCameraMoved: (LatLngPoint) -> Unit,
    onMapLoadingChanged: (Boolean) -> Unit,
    onMapError: (String?) -> Unit,
    onRetryMapLoad: () -> Unit,
    onUndoPolygonPoint: () -> Unit,
    onClearPolygon: () -> Unit,
    onZoomMap: (Boolean) -> Unit,
    onResetCompass: () -> Unit,
    onFarmNameChange: (String) -> Unit,
    onSaveFarmToSupabase: () -> Unit,
    onFocusSavedFarm: (FarmBoundaryEntity) -> Unit,
    onCacheCurrentRegionOffline: () -> Unit,
    onSelectPaymentMethod: (PaymentMethod) -> Unit,
    onConfirmOrder: () -> Unit,
    onConsumeBookingSuccess: () -> Unit,
    onConfirmJobDone: (String) -> Unit,
    onRatePilot: (String, Int) -> Unit,
    onUpdateCredentials: (
        name: String,
        phone: String,
        farmerIdNumber: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
    onConfirmOrderWithMethod: (PaymentMethod) -> Unit = { method ->
        onSelectPaymentMethod(method)
        onConfirmOrder()
    }
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
    var showEditCredentialsDialog by remember { mutableStateOf(false) }
    var ratingOrderTarget by remember { mutableStateOf<OrderWithDetails?>(null) }
    var showNonCancellablePolicyDialog by remember { mutableStateOf(false) }

    val activeOrders = orders.filter {
        it.statusEnum != OrderStatus.COMPLETED && it.statusEnum != OrderStatus.CANCELLED
    }
    val completedOrders = orders.filter { it.statusEnum == OrderStatus.COMPLETED }
    val awaitingConfirmationCount = orders.count { it.statusEnum == OrderStatus.AWAITING_CONFIRMATION }

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
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = user.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = SoftSage
                            ) {
                                Text(
                                    text = profile?.farmerIdNumber ?: "FRM-ID",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = ForestGreen,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Krishivantri Farmer Portal",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    // Harvest Coins Pill
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = HarvestGoldBg,
                        border = BorderStroke(1.dp, HarvestAmber.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { selectedTab = 3 }
                            .testTag("farmer_coins_chip")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Eco,
                                contentDescription = "Harvest Coins",
                                tint = HarvestAmber,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "${profile?.coins ?: 0} Coins",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = HarvestAmber
                            )
                        }
                    }

                    // Edit Credentials Quick Icon
                    IconButton(
                        onClick = { showEditCredentialsDialog = true },
                        modifier = Modifier.testTag("farmer_edit_credentials_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ManageAccounts,
                            contentDescription = "Change Credentials",
                            tint = ForestGreen
                        )
                    }

                    // Log Out
                    IconButton(
                        onClick = onSignOut,
                        modifier = Modifier.testTag("farmer_signout_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Logout,
                            contentDescription = "Log Out",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Rounded.Map, contentDescription = "Book Spray") },
                    label = { Text("Book Spray") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ForestGreen,
                        selectedTextColor = ForestGreen,
                        indicatorColor = SoftSage
                    ),
                    modifier = Modifier.testTag("farmer_nav_book")
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = {
                        Box {
                            Icon(Icons.Rounded.Assignment, contentDescription = "My Orders")
                            if (awaitingConfirmationCount > 0) {
                                Surface(
                                    shape = CircleShape,
                                    color = AlertRust,
                                    modifier = Modifier
                                        .size(10.dp)
                                        .align(Alignment.TopEnd)
                                ) {}
                            }
                        }
                    },
                    label = { Text("My Orders") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ForestGreen,
                        selectedTextColor = ForestGreen,
                        indicatorColor = SoftSage
                    ),
                    modifier = Modifier.testTag("farmer_nav_orders")
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Rounded.Agriculture, contentDescription = "My Farms") },
                    label = { Text("My Farms") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ForestGreen,
                        selectedTextColor = ForestGreen,
                        indicatorColor = SoftSage
                    ),
                    modifier = Modifier.testTag("farmer_nav_farms")
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Rounded.Person, contentDescription = "Profile") },
                    label = { Text("Profile") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ForestGreen,
                        selectedTextColor = ForestGreen,
                        indicatorColor = SoftSage
                    ),
                    modifier = Modifier.testTag("farmer_nav_profile")
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
                0 -> FarmerMapBookingTab(
                    mapRepository = mapRepository,
                    mapState = mapState,
                    savedFarms = savedFarms,
                    bookingOrders = bookingOrdersForMap,
                    realtimeOperators = realtimeOperators,
                    onSelectMapStyle = onSelectMapStyle,
                    onSearchQueryChange = onSearchQueryChange,
                    onCenterMap = onCenterMap,
                    onSelectLocationSuggestion = onSelectLocationSuggestion,
                    onRefreshCurrentLocation = onRefreshCurrentLocation,
                    onMapTapPoint = onMapTapPoint,
                    onUpdateBoundaryPoint = onUpdateBoundaryPoint,
                    onMapCameraMoved = onMapCameraMoved,
                    onMapLoadingChanged = onMapLoadingChanged,
                    onMapError = onMapError,
                    onRetryMapLoad = onRetryMapLoad,
                    onUndoPolygonPoint = onUndoPolygonPoint,
                    onClearPolygon = onClearPolygon,
                    onZoomMap = onZoomMap,
                    onResetCompass = onResetCompass,
                    onFarmNameChange = onFarmNameChange,
                    onSaveFarmToSupabase = onSaveFarmToSupabase,
                    onCacheCurrentRegionOffline = onCacheCurrentRegionOffline,
                    onSelectPaymentMethod = onSelectPaymentMethod,
                    onConfirmOrderWithMethod = onConfirmOrderWithMethod
                )

                1 -> FarmerOrdersTab(
                    activeOrders = activeOrders,
                    completedOrders = completedOrders,
                    onConfirmJobDone = { orderId ->
                        onConfirmJobDone(orderId)
                        val order = orders.firstOrNull { it.order.id == orderId }
                        if (order != null) {
                            ratingOrderTarget = order
                        }
                    },
                    onOpenRateDialog = { target -> ratingOrderTarget = target },
                    onShowNonCancellableNotice = { showNonCancellablePolicyDialog = true }
                )

                2 -> FarmerSavedFarmsTab(
                    savedFarms = savedFarms,
                    onFocusFarm = { farm ->
                        onFocusSavedFarm(farm)
                        selectedTab = 0
                    }
                )

                3 -> FarmerAccountTab(
                    user = user,
                    profile = profile,
                    orders = orders,
                    onOpenEditCredentials = { showEditCredentialsDialog = true },
                    onSignOut = onSignOut
                )
            }
        }
    }

    // Edit Farmer Credentials Dialog
    if (showEditCredentialsDialog) {
        EditFarmerCredentialsDialog(
            currentUser = user,
            currentProfile = profile,
            onSave = onUpdateCredentials,
            onDismiss = { showEditCredentialsDialog = false }
        )
    }

    // Rate Pilot Modal
    ratingOrderTarget?.let { orderTarget ->
        RatePilotDialog(
            order = orderTarget,
            onSubmitRating = { stars ->
                onRatePilot(orderTarget.order.id, stars)
                ratingOrderTarget = null
            },
            onDismiss = { ratingOrderTarget = null }
        )
    }

    // Booking Success Dialog
    mapState.bookingSuccessOrderId?.let { orderId ->
        AlertDialog(
            onDismissRequest = onConsumeBookingSuccess,
            icon = {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(SoftSage),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = ForestGreen
                    )
                }
            },
            title = {
                Text(
                    text = "Spray Mission Dispatched!",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Your precision spray booking is confirmed. Licensed drone pilots in your area are receiving priority telemetry.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = HarvestGoldBg,
                        border = BorderStroke(1.dp, HarvestAmber.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Lock,
                                contentDescription = null,
                                tint = HarvestAmber,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Non-cancellable policy: Service bookings cannot be cancelled after booking & payment. Payment is held safely in escrow until you click 'Job Done'.",
                                style = MaterialTheme.typography.labelSmall,
                                color = ForestGreen
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onConsumeBookingSuccess()
                        selectedTab = 1
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreen)
                ) {
                    Text("View Active Mission")
                }
            },
            dismissButton = {
                TextButton(onClick = onConsumeBookingSuccess) {
                    Text("Close")
                }
            }
        )
    }

    // Non-Cancellable Policy Info Dialog
    if (showNonCancellablePolicyDialog) {
        AlertDialog(
            onDismissRequest = { showNonCancellablePolicyDialog = false },
            icon = {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(HarvestGoldBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = HarvestAmber
                    )
                }
            },
            title = {
                Text(
                    text = "Non-Cancellable Booking Policy",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Once you book and pay for a drone spray service, the booking cannot be cancelled.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Why? Licensed DGCA drone pilots reserve their flight slots, prepare battery cycles, and load authorized spray payload based on your field geometry.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = SoftSage,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Your money is 100% escrow protected. The pilot receives NO wallet payout until spraying is complete and you personally click 'Job Done'.",
                            style = MaterialTheme.typography.labelSmall,
                            color = ForestGreen,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showNonCancellablePolicyDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreen)
                ) {
                    Text("Understood")
                }
            }
        )
    }
}

// -------------------------------------------------------------
// TAB 0: MAP & BOOKING TAB
// -------------------------------------------------------------
@Composable
private fun FarmerMapBookingTab(
    mapRepository: MapRepository,
    mapState: MapBookingState,
    savedFarms: List<FarmBoundaryEntity>,
    bookingOrders: List<OrderWithDetails>,
    realtimeOperators: List<OperatorLocationMarker>,
    onSelectMapStyle: (MapStyleType) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onCenterMap: () -> Unit,
    onSelectLocationSuggestion: (LocationMatch) -> Unit,
    onRefreshCurrentLocation: () -> Unit,
    onMapTapPoint: (LatLngPoint) -> Unit,
    onUpdateBoundaryPoint: (Int, LatLngPoint) -> Unit,
    onMapCameraMoved: (LatLngPoint) -> Unit,
    onMapLoadingChanged: (Boolean) -> Unit,
    onMapError: (String?) -> Unit,
    onRetryMapLoad: () -> Unit,
    onUndoPolygonPoint: () -> Unit,
    onClearPolygon: () -> Unit,
    onZoomMap: (Boolean) -> Unit,
    onResetCompass: () -> Unit,
    onFarmNameChange: (String) -> Unit,
    onSaveFarmToSupabase: () -> Unit,
    onCacheCurrentRegionOffline: () -> Unit,
    onSelectPaymentMethod: (PaymentMethod) -> Unit,
    onConfirmOrderWithMethod: (PaymentMethod) -> Unit,
    modifier: Modifier = Modifier
) {
    var showSuggestions by remember { mutableStateOf(false) }
    var showPaymentChoiceDialog by remember { mutableStateOf(false) }

    val mapPane: @Composable (Modifier) -> Unit = { paneModifier ->
        Box(modifier = paneModifier) {
            MapLibreFarmMap(
                mapRepository = mapRepository,
                styleType = mapState.mapStyleType,
                cameraTarget = mapState.mapCenter,
                cameraFlyNonce = mapState.cameraFlyNonce,
                zoomInNonce = mapState.zoomInNonce,
                zoomOutNonce = mapState.zoomOutNonce,
                resetCompassNonce = mapState.resetCompassNonce,
                styleRetryNonce = mapState.styleRetryNonce,
                hasLocationPermission = mapState.hasLocationPermission,
                drawingPoints = mapState.polygonPoints,
                computedAcres = mapState.computedAcres,
                computedHectares = mapState.computedHectares,
                savedFarms = emptyList(),
                bookingOrders = emptyList(),
                operators = realtimeOperators,
                onMapTapPoint = onMapTapPoint,
                onUpdateBoundaryPoint = onUpdateBoundaryPoint,
                onMapLoadingChanged = onMapLoadingChanged,
                onMapError = onMapError,
                onCameraMoved = onMapCameraMoved,
                modifier = Modifier.fillMaxSize()
            )

            // Compact Top Search & Style Bar
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                    tonalElevation = 4.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .padding(start = 10.dp, end = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = "Search",
                            tint = ForestGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (mapState.searchQuery.isEmpty()) {
                                Text(
                                    text = "Search taluka, village, or town…",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            BasicTextField(
                                value = mapState.searchQuery,
                                onValueChange = {
                                    onSearchQueryChange(it)
                                    showSuggestions = true
                                },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                cursorBrush = SolidColor(ForestGreen),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("map_search_input")
                            )
                        }
                        IconButton(
                            onClick = {
                                showSuggestions = false
                                onCenterMap()
                            },
                            modifier = Modifier.testTag("map_search_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = "Search and Center",
                                tint = ForestGreen
                            )
                        }
                        IconButton(
                            onClick = onRefreshCurrentLocation,
                            modifier = Modifier.testTag("map_gps_center_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MyLocation,
                                contentDescription = "My GPS Location",
                                tint = EmeraldPulse
                            )
                        }
                    }
                }

                // Location suggestions dropdown
                if (showSuggestions && mapState.locationSuggestions.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 6.dp,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            mapState.locationSuggestions.take(4).forEach { suggestion ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = suggestion.displayName,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    onClick = {
                                        onSelectLocationSuggestion(suggestion)
                                        showSuggestions = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Map Style & Offline Cache Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MapStyleType.entries.forEach { style ->
                        val isSelected = mapState.mapStyleType == style
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) {
                                ForestGreen
                            } else {
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
                            },
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) ForestGreen else MaterialTheme.colorScheme.outline
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onSelectMapStyle(style) }
                        ) {
                            Text(
                                text = style.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(onClick = onCacheCurrentRegionOffline)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Download,
                                contentDescription = "Save Offline Map",
                                tint = ForestGreen,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Offline",
                                style = MaterialTheme.typography.labelSmall,
                                color = ForestGreen
                            )
                        }
                    }
                }

                mapState.offlineDownloadProgress?.let { progress ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = "Caching offline map tiles: $progress%",
                                style = MaterialTheme.typography.labelSmall
                            )
                            LinearProgressIndicator(
                                progress = { progress / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp),
                                color = EmeraldPulse
                            )
                        }
                    }
                }
            }

            // Bottom-Start Point Selection Status + Quick Undo/Delete Pills
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 10.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    tonalElevation = 4.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Text(
                        text = when {
                            mapState.polygonPoints.isEmpty() -> "Tap map to mark corners"
                            mapState.polygonPoints.size < 3 ->
                                "${mapState.polygonPoints.size} pts • Tap ${3 - mapState.polygonPoints.size}+ more"
                            else -> "${mapState.polygonPoints.size} pts • Tap to add more"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (mapState.polygonPoints.size >= 3) ForestGreen else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                    )
                }

                if (mapState.polygonPoints.isNotEmpty()) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                        tonalElevation = 4.dp,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onUndoPolygonPoint)
                            .testTag("map_undo_point_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Undo,
                                contentDescription = "Undo Last Point",
                                tint = ForestGreen,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                        tonalElevation = 4.dp,
                        border = BorderStroke(1.dp, AlertRust.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onClearPolygon)
                            .testTag("map_delete_area_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = "Delete Selected Area",
                                tint = AlertRust,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Bottom-End Zoom Controls
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 10.dp, bottom = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    tonalElevation = 4.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .clickable { onZoomMap(true) }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Add, contentDescription = "Zoom In", tint = ForestGreen)
                    }
                }
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    tonalElevation = 4.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .clickable { onZoomMap(false) }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Remove, contentDescription = "Zoom Out", tint = ForestGreen)
                    }
                }
            }
        }
    }

    val bookingPane: @Composable (Modifier) -> Unit = { paneModifier ->
        Surface(
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = paneModifier
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Pinned Header: Selected Area, Amount Value & Delete/Reselect Controls
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (mapState.polygonPoints.size >= 3) {
                            SoftSage
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        },
                        border = BorderStroke(
                            1.dp,
                            if (mapState.polygonPoints.size >= 3) {
                                ForestGreen.copy(alpha = 0.35f)
                            } else {
                                MaterialTheme.colorScheme.outline
                            }
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = when {
                                        mapState.polygonPoints.isEmpty() -> "No Area Selected"
                                        mapState.polygonPoints.size < 3 ->
                                            "${mapState.polygonPoints.size} Point${if (mapState.polygonPoints.size == 1) "" else "s"} Placed"
                                        else -> "${mapState.polygonPoints.size} Points • Selected Area"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (mapState.polygonPoints.size >= 3) EmeraldPulse else ForestGreen,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (mapState.polygonPoints.size >= 3) {
                                        "${mapState.computedAcres} Acres • ${mapState.computedHectares} Ha"
                                    } else {
                                        "Tap 3+ points on map in any polygon shape"
                                    },
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (mapState.polygonPoints.size >= 3) {
                                    Text(
                                        text = "Tap map to add more points or drag dots to adjust",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Amount (₹450/ac)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = formatInr(mapState.computedAmount),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = ForestGreen,
                                    modifier = Modifier.testTag("selected_area_amount_text")
                                )
                            }
                        }
                    }

                    if (mapState.polygonPoints.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onUndoPolygonPoint,
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .testTag("panel_undo_point_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Undo,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "Undo Point",
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1
                                )
                            }
                            OutlinedButton(
                                onClick = onClearPolygon,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = AlertRust),
                                border = BorderStroke(1.dp, AlertRust.copy(alpha = 0.65f)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .testTag("panel_delete_area_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteOutline,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "Delete & Reselect",
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

                // Scrollable Middle: Save Farm, Payment Method & Policy Notice
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (mapState.polygonPoints.size >= 3) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = mapState.farmNameInput,
                                onValueChange = onFarmNameChange,
                                placeholder = { Text("Farm name (e.g. North Plot)") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedButton(
                                onClick = onSaveFarmToSupabase,
                                enabled = !mapState.isSavingFarm,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.height(48.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.Save,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("Save")
                            }
                        }
                    }

                    // Non-Cancellable Policy Notice Card
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = HarvestGoldBg,
                        border = BorderStroke(1.dp, HarvestAmber.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Lock,
                                contentDescription = null,
                                tint = HarvestAmber,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Non-cancellable once booked & paid. Choose Online or Cash when you click Book. Payout to pilot is held safely in escrow until you click 'Job Done'.",
                                style = MaterialTheme.typography.labelSmall,
                                color = ForestGreen
                            )
                        }
                    }
                }

                // Pinned Bottom CTA Booking Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Button(
                        onClick = { showPaymentChoiceDialog = true },
                        enabled = mapState.polygonPoints.size >= 3 && mapState.computedAcres > 0.0,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("farmer_confirm_booking_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FlightTakeoff,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (mapState.polygonPoints.size < 3) {
                                "Outline Farmland on Map"
                            } else {
                                "Book Spray Service • ${formatInr(mapState.computedAmount)}"
                            },
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        if (maxWidth >= 720.dp) {
            Row(modifier = Modifier.fillMaxSize()) {
                mapPane(Modifier.weight(0.55f).fillMaxHeight())
                bookingPane(Modifier.weight(0.45f).fillMaxHeight())
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                mapPane(Modifier.fillMaxWidth().weight(0.55f))
                bookingPane(Modifier.fillMaxWidth().weight(0.45f))
            }
        }
    }

    if (showPaymentChoiceDialog) {
        BookPaymentMethodDialog(
            acres = mapState.computedAcres,
            hectares = mapState.computedHectares,
            totalAmount = mapState.computedAmount,
            initialSelection = mapState.selectedPaymentMethod,
            onConfirmPaymentChoice = { chosenMethod ->
                showPaymentChoiceDialog = false
                onSelectPaymentMethod(chosenMethod)
                onConfirmOrderWithMethod(chosenMethod)
            },
            onDismiss = { showPaymentChoiceDialog = false }
        )
    }
}

@Composable
private fun BookPaymentMethodDialog(
    acres: Double,
    hectares: Double,
    totalAmount: Double,
    initialSelection: PaymentMethod,
    onConfirmPaymentChoice: (PaymentMethod) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedMethod by remember { mutableStateOf(initialSelection) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Select Payment Method",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SoftSage,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Selected Farmland",
                                style = MaterialTheme.typography.labelSmall,
                                color = ForestGreen
                            )
                            Text(
                                text = "$acres Acres • $hectares Ha",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = ForestGreen
                            )
                        }
                        Text(
                            text = formatInr(totalAmount),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = ForestGreen
                        )
                    }
                }

                PaymentMethod.entries.forEach { method ->
                    val isSelected = selectedMethod == method
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) SoftSage else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) ForestGreen else MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { selectedMethod = method }
                            .testTag("payment_option_${method.apiValue}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 56.dp)
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = if (method == PaymentMethod.ONLINE) {
                                    Icons.Rounded.Payments
                                } else {
                                    Icons.Rounded.Agriculture
                                },
                                contentDescription = method.label,
                                tint = if (isSelected) ForestGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = method.label,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) ForestGreen else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (method == PaymentMethod.ONLINE) {
                                        "Pay via Razorpay (UPI, Card, NetBanking). Escrow protected until 'Job Done'."
                                    } else {
                                        "Pay cash directly to your pilot after spraying is completed."
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = "Selected",
                                    tint = ForestGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmPaymentChoice(selectedMethod) },
                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("payment_dialog_confirm_button")
            ) {
                Text(
                    if (selectedMethod == PaymentMethod.ONLINE) {
                        "Pay Online • ${formatInr(totalAmount)}"
                    } else {
                        "Book with Cash • ${formatInr(totalAmount)}"
                    }
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text("Cancel")
            }
        }
    )
}

// -------------------------------------------------------------
// TAB 1: MY ORDERS & MISSIONS TAB
// -------------------------------------------------------------
@Composable
private fun FarmerOrdersTab(
    activeOrders: List<OrderWithDetails>,
    completedOrders: List<OrderWithDetails>,
    onConfirmJobDone: (String) -> Unit,
    onOpenRateDialog: (OrderWithDetails) -> Unit,
    onShowNonCancellableNotice: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Active Spray Missions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        if (activeOrders.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Eco,
                            contentDescription = null,
                            tint = EmeraldPulse,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "No Active Spray Missions",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Use the 'Book Spray' tab to outline your farmland and dispatch priority drone pilots.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(activeOrders, key = { it.order.id }) { item ->
                ActiveOrderCard(
                    item = item,
                    onConfirmJobDone = { onConfirmJobDone(item.order.id) },
                    onShowNonCancellableNotice = onShowNonCancellableNotice
                )
            }
        }

        if (completedOrders.isNotEmpty()) {
            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Completed Missions History",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            items(completedOrders, key = { it.order.id }) { item ->
                CompletedOrderCard(
                    item = item,
                    onOpenRateDialog = { onOpenRateDialog(item) }
                )
            }
        }
    }
}

@Composable
private fun ActiveOrderCard(
    item: OrderWithDetails,
    onConfirmJobDone: () -> Unit,
    onShowNonCancellableNotice: () -> Unit
) {
    val status = item.statusEnum
    val isAwaitingConfirmation = status == OrderStatus.AWAITING_CONFIRMATION
    val isSpraying = status == OrderStatus.IN_PROGRESS
    val isEnRoute = status == OrderStatus.ACCEPTED

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.5.dp,
            if (isAwaitingConfirmation) EmeraldPulse else MaterialTheme.colorScheme.outline
        ),
        tonalElevation = if (isAwaitingConfirmation) 4.dp else 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row: Polygon Preview + Status Badge + Non-cancellable lock
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MiniPolygonPreview(points = item.polygonPoints)
                    Column {
                        Text(
                            text = "${item.order.acresToSpray} Acres",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = item.order.locationAddress.ifBlank { "Farmland Plot" },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Amount: ${formatInr(item.order.totalAmount)} (${item.order.paymentMethod.uppercase()})",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = ForestGreen
                        )
                    }
                }

                // Non-cancellable Policy Pill
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = HarvestGoldBg,
                    border = BorderStroke(1.dp, HarvestAmber.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onShowNonCancellableNotice)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = "Non-cancellable",
                            tint = HarvestAmber,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "Non-Cancellable",
                            style = MaterialTheme.typography.labelSmall,
                            color = ForestGreen
                        )
                    }
                }
            }

            // Status Timeline Strip
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = when (status) {
                    OrderStatus.AWAITING_CONFIRMATION -> VerifiedTealBg
                    OrderStatus.IN_PROGRESS -> SoftSage
                    OrderStatus.ACCEPTED -> SoftSage.copy(alpha = 0.6f)
                    else -> HarvestGoldBg
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = when (status) {
                            OrderStatus.AWAITING_CONFIRMATION -> Icons.Rounded.TaskAlt
                            OrderStatus.IN_PROGRESS -> Icons.Rounded.Agriculture
                            OrderStatus.ACCEPTED -> Icons.Rounded.FlightTakeoff
                            else -> Icons.Rounded.WarningAmber
                        },
                        contentDescription = null,
                        tint = when (status) {
                            OrderStatus.AWAITING_CONFIRMATION -> VerifiedTeal
                            OrderStatus.IN_PROGRESS -> ForestGreen
                            OrderStatus.ACCEPTED -> ForestGreen
                            else -> HarvestAmber
                        },
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = when (status) {
                            OrderStatus.PENDING -> "Finding nearby priority drone pilot..."
                            OrderStatus.ACCEPTED -> "Pilot ${item.pilotName ?: ""} en route • ETA: ${item.order.pilotEtaMinutes}m (${item.liveDistanceKm?.let { GeoPolygonCalculator.formatDistanceLabel(it) } ?: "Nearby"})"
                            OrderStatus.IN_PROGRESS -> "Pilot ${item.pilotName ?: ""} spraying farmland now..."
                            OrderStatus.AWAITING_CONFIRMATION -> "Spraying Done by Pilot! Verify and click 'Job Done'."
                            else -> status.displayLabel
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = ForestGreen
                    )
                }
            }

            // Assigned Pilot details if available
            item.pilotName?.let { pilotName ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Pilot: $pilotName (${item.pilotPhone ?: ""})",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                        val drone = DroneType.fromApi(item.pilotDroneType ?: "quadcopter").label
                        Text(
                            text = "$drone • ★ ${item.pilotRatingAverage ?: 5.0}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "Pilot Payout: ${formatInr(item.order.pilotEarnings)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = ForestGreen
                    )
                }
            }

            // *** CRITICAL REQUIREMENT ***
            // When pilot completes spraying -> Farmer clicks "Job Done" -> amount transferred into pilot's wallet!
            if (isAwaitingConfirmation || isSpraying) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = SoftSage,
                    border = BorderStroke(1.dp, ForestGreen.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (isAwaitingConfirmation) {
                                "The pilot has finished spraying your farmland. Confirm 'Job Done' to release ${formatInr(item.order.pilotEarnings)} directly into ${item.pilotName ?: "the pilot"}'s wallet and earn +1 Harvest Coin."
                            } else {
                                "Pilot is currently spraying. Once finished, click 'Job Done' to release ${formatInr(item.order.pilotEarnings)} into their wallet."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = ForestGreen
                        )
                        Button(
                            onClick = onConfirmJobDone,
                            colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("farmer_job_done_button_${item.order.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Job Done • Release ${formatInr(item.order.pilotEarnings)}",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompletedOrderCard(
    item: OrderWithDetails,
    onOpenRateDialog: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
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
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MiniPolygonPreview(points = item.polygonPoints)
                Column {
                    Text(
                        text = "${item.order.acresToSpray} Acres Completed",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${formatInr(item.order.totalAmount)} • Pilot: ${item.pilotName ?: "Assigned Pilot"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Payout settled to pilot wallet (+1 Coin earned)",
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldPulse
                    )
                }
            }

            if (item.order.pilotRating == null) {
                OutlinedButton(
                    onClick = onOpenRateDialog,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Star,
                        contentDescription = null,
                        tint = HarvestAmber,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Rate")
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Star,
                        contentDescription = null,
                        tint = HarvestAmber,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "${item.order.pilotRating}★",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 2: SAVED FARMS TAB
// -------------------------------------------------------------
@Composable
private fun FarmerSavedFarmsTab(
    savedFarms: List<FarmBoundaryEntity>,
    onFocusFarm: (FarmBoundaryEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "My Farmland Boundaries",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Saved GPS polygons for precision drone dispatch and recurring spray missions.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (savedFarms.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Agriculture,
                            contentDescription = null,
                            tint = EmeraldPulse,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "No Farmland Boundaries Saved",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Draw farmland polygons on the map and tap 'Save Farm' to reuse them anytime.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(savedFarms, key = { it.id }) { farm ->
                val points = GeoPolygonCalculator.decodePolygon(farm.polygonCoordinatesJson)
                Surface(
                    shape = RoundedCornerShape(18.dp),
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
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MiniPolygonPreview(points = points)
                            Column {
                                Text(
                                    text = farm.farmName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${farm.areaAcres} Acres • ${farm.areaHectares} Ha",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (farm.isSyncedToSupabase) {
                                            Icons.Rounded.CloudDone
                                        } else {
                                            Icons.Rounded.CloudQueue
                                        },
                                        contentDescription = null,
                                        tint = if (farm.isSyncedToSupabase) EmeraldPulse else HarvestAmber,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = if (farm.isSyncedToSupabase) "Synced to Supabase" else "Local Storage",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = { onFocusFarm(farm) },
                            colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Book Spray")
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 3: ACCOUNT & CREDENTIALS TAB
// -------------------------------------------------------------
@Composable
private fun FarmerAccountTab(
    user: UserEntity,
    profile: FarmerProfileEntity?,
    orders: List<OrderWithDetails>,
    onOpenEditCredentials: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Farmer Profile Card
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = SoftSage,
                            modifier = Modifier.size(54.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Person,
                                    contentDescription = null,
                                    tint = ForestGreen,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = user.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Registered Farmer",
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldPulse
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onOpenEditCredentials,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("account_change_credentials_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ManageAccounts,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Edit")
                    }
                }

                // Credentials Details List
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CredentialRow(
                        icon = Icons.Rounded.Person,
                        label = "Full Name",
                        value = user.name
                    )
                    CredentialRow(
                        icon = Icons.Rounded.Email,
                        label = "Email Address",
                        value = user.email.ifBlank { "Not linked" }
                    )
                    CredentialRow(
                        icon = Icons.Rounded.Phone,
                        label = "Mobile Number",
                        value = user.phone
                    )
                    CredentialRow(
                        icon = Icons.Rounded.Badge,
                        label = "Farmer ID Number",
                        value = profile?.farmerIdNumber ?: "Not Assigned"
                    )
                }

                Text(
                    text = "Tap 'Edit' above to change your name, mobile number, or Farmer ID. Updates are synchronized with the cloud backend.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Harvest Coins & Gamification Card
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = HarvestGoldBg,
            border = BorderStroke(1.dp, HarvestAmber.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Harvest Reward Coins",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = ForestGreen
                    )
                    Text(
                        text = "Earn +1 Harvest Coin for every spray mission verified via 'Job Done'.",
                        style = MaterialTheme.typography.bodySmall,
                        color = ForestGreen.copy(alpha = 0.85f)
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = HarvestAmber,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "${profile?.coins ?: 0}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // Summary Stats Card
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${orders.size}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = ForestGreen
                    )
                    Text(
                        text = "Total Bookings",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val completed = orders.count { it.statusEnum == OrderStatus.COMPLETED }
                    Text(
                        text = "$completed",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPulse
                    )
                    Text(
                        text = "Sprayed Acres",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val totalAcreage = orders.filter { it.statusEnum == OrderStatus.COMPLETED }
                        .sumOf { it.order.acresToSpray }
                    Text(
                        text = String.format(java.util.Locale.US, "%.1f", totalAcreage),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = ForestGreen
                    )
                    Text(
                        text = "Acres Protected",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Log Out Button
        Button(
            onClick = onSignOut,
            colors = ButtonDefaults.buttonColors(containerColor = AlertRust),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("farmer_logout_button")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.Logout,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text("Log Out")
        }
    }
}

@Composable
private fun CredentialRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = ForestGreen,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

// -------------------------------------------------------------
// CHANGE FARMER CREDENTIALS DIALOG
// -------------------------------------------------------------
@Composable
private fun EditFarmerCredentialsDialog(
    currentUser: UserEntity,
    currentProfile: FarmerProfileEntity?,
    onSave: (
        name: String,
        phone: String,
        farmerIdNumber: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) -> Unit,
    onDismiss: () -> Unit
) {
    var editName by rememberSaveable(currentUser.id) { mutableStateOf(currentUser.name) }
    var editPhone by rememberSaveable(currentUser.id) { mutableStateOf(currentUser.phone) }
    var editFarmerId by rememberSaveable(currentUser.id) {
        mutableStateOf(currentProfile?.farmerIdNumber.orEmpty())
    }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(SoftSage),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.ManageAccounts,
                    contentDescription = null,
                    tint = ForestGreen
                )
            }
        },
        title = {
            Text(
                text = "Change Farmer Credentials",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Update your full name, 10-digit mobile number, or official Farmer ID number.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = editName,
                    onValueChange = { editName = it },
                    label = { Text("Farmer Full Name") },
                    leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_credentials_name_input")
                )

                OutlinedTextField(
                    value = editPhone,
                    onValueChange = { if (it.length <= 15) editPhone = it },
                    label = { Text("Mobile Number (10 digits)") },
                    leadingIcon = { Icon(Icons.Rounded.Phone, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_credentials_phone_input")
                )

                OutlinedTextField(
                    value = editFarmerId,
                    onValueChange = { editFarmerId = it },
                    label = { Text("Farmer ID / Registration No.") },
                    leadingIcon = { Icon(Icons.Rounded.Badge, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_credentials_id_input")
                )

                val err = errorMessage
                if (!err.isNullOrBlank()) {
                    Text(
                        text = err,
                        style = MaterialTheme.typography.bodySmall,
                        color = AlertRust
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (editName.isBlank()) {
                        errorMessage = "Full name cannot be blank."
                        return@Button
                    }
                    if (editPhone.trim().length < 10) {
                        errorMessage = "Enter a valid 10-digit mobile number."
                        return@Button
                    }
                    if (editFarmerId.isBlank()) {
                        errorMessage = "Farmer ID number cannot be blank."
                        return@Button
                    }
                    isSaving = true
                    errorMessage = null
                    onSave(
                        editName,
                        editPhone,
                        editFarmerId,
                        {
                            isSaving = false
                            onDismiss()
                        },
                        { msg ->
                            isSaving = false
                            errorMessage = msg
                        }
                    )
                },
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                modifier = Modifier.testTag("edit_credentials_save_button")
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Save Credentials")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// -------------------------------------------------------------
// PILOT RATING DIALOG
// -------------------------------------------------------------
@Composable
private fun RatePilotDialog(
    order: OrderWithDetails,
    onSubmitRating: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedStars by remember { mutableIntStateOf(5) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Rate Drone Pilot Service",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "How was the spraying precision by ${order.pilotName ?: "the drone pilot"}?",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    (1..5).forEach { star ->
                        IconButton(onClick = { selectedStars = star }) {
                            Icon(
                                imageVector = Icons.Rounded.Star,
                                contentDescription = "$star Stars",
                                tint = if (star <= selectedStars) HarvestAmber else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
                Text(
                    text = when (selectedStars) {
                        5 -> "Excellent • Perfect Coverage & Timely Arrival"
                        4 -> "Very Good • Satisfied Spraying Quality"
                        3 -> "Average • Standard Service"
                        2 -> "Below Average • Needed Improvement"
                        else -> "Poor Service"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmitRating(selectedStars) },
                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen)
            ) {
                Text("Submit Rating ($selectedStars★)")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Skip")
            }
        }
    )
}
