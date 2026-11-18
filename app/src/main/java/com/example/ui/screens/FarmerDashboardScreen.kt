package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.FlightTakeoff
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.ManageAccounts
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Stars
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
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
import kotlinx.coroutines.delay
import java.util.Locale

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
    onUpdateCredentials: (name: String, phone: String, farmerIdNumber: String, onSuccess: () -> Unit, onError: (String) -> Unit) -> Unit,
    onConfirmJobDone: (String) -> Unit,
    onRatePilot: (String, Int) -> Unit,
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
    var showEditCredentialsDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(mapState.bookingSuccessOrderId) {
        if (mapState.bookingSuccessOrderId != null) {
            selectedTab = 1
            onConsumeBookingSuccess()
        }
    }

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
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showEditCredentialsDialog = true }
                            .padding(vertical = 2.dp, horizontal = 4.dp)
                    ) {
                        Text(
                            text = user.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "ID: ${profile?.farmerIdNumber ?: "-"} • ${user.phone}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                actions = {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = HarvestGoldBg,
                        modifier = Modifier
                            .padding(end = 2.dp)
                            .testTag("farmer_coins_badge")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Stars,
                                contentDescription = "Harvest Coins virtual rewards",
                                tint = HarvestAmber,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "${profile?.coins ?: 0}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = HarvestAmber
                            )
                        }
                    }
                    IconButton(
                        onClick = { showEditCredentialsDialog = true },
                        modifier = Modifier.testTag("farmer_edit_credentials_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ManageAccounts,
                            contentDescription = "Edit Farmer Credentials",
                            tint = ForestGreen
                        )
                    }
                    IconButton(
                        onClick = onSignOut,
                        modifier = Modifier.testTag("farmer_sign_out_button")
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
                    icon = { Icon(Icons.Rounded.LocationOn, contentDescription = "Book Spray") },
                    label = { Text("Book Spray") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ForestGreen,
                        selectedTextColor = ForestGreen,
                        indicatorColor = SoftSage
                    ),
                    modifier = Modifier.testTag("farmer_tab_book")
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Rounded.ReceiptLong, contentDescription = "My Orders") },
                    label = { Text("Orders (${orders.size})") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ForestGreen,
                        selectedTextColor = ForestGreen,
                        indicatorColor = SoftSage
                    ),
                    modifier = Modifier.testTag("farmer_tab_orders")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (selectedTab == 0) {
                FarmerMapBookingContent(
                    mapRepository = mapRepository,
                    mapState = mapState,
                    savedFarms = savedFarms,
                    bookingOrdersForMap = bookingOrdersForMap,
                    realtimeOperators = realtimeOperators,
                    onRequestLocationPermission = {
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    },
                    onRefreshCurrentLocation = onRefreshCurrentLocation,
                    onSelectMapStyle = onSelectMapStyle,
                    onSearchQueryChange = onSearchQueryChange,
                    onCenterMap = onCenterMap,
                    onSelectLocationSuggestion = onSelectLocationSuggestion,
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
                    onFocusSavedFarm = onFocusSavedFarm,
                    onCacheCurrentRegionOffline = onCacheCurrentRegionOffline,
                    onSelectPaymentMethod = onSelectPaymentMethod,
                    onConfirmOrder = onConfirmOrder
                )
            } else {
                FarmerOrdersContent(
                    user = user,
                    profile = profile,
                    orders = orders,
                    onEditCredentials = { showEditCredentialsDialog = true },
                    onConfirmJobDone = onConfirmJobDone,
                    onRatePilot = onRatePilot
                )
            }
        }
    }

    if (showEditCredentialsDialog) {
        EditFarmerCredentialsDialog(
            initialName = user.name,
            initialPhone = user.phone,
            initialFarmerId = profile?.farmerIdNumber.orEmpty(),
            onSave = { newName, newPhone, newFarmerId, onError ->
                onUpdateCredentials(
                    newName,
                    newPhone,
                    newFarmerId,
                    { showEditCredentialsDialog = false },
                    onError
                )
            },
            onDismiss = { showEditCredentialsDialog = false }
        )
    }
}

@Composable
private fun FarmerMapBookingContent(
    mapRepository: MapRepository,
    mapState: MapBookingState,
    savedFarms: List<FarmBoundaryEntity>,
    bookingOrdersForMap: List<OrderWithDetails>,
    realtimeOperators: List<OperatorLocationMarker>,
    onRequestLocationPermission: () -> Unit,
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
    onConfirmOrder: () -> Unit
) {
    val focusManager = LocalFocusManager.current

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideLayout = maxWidth >= 720.dp

        val mapSection: @Composable (Modifier) -> Unit = { mapModifier ->
            Box(modifier = mapModifier) {
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
                    savedFarms = savedFarms,
                    bookingOrders = bookingOrdersForMap,
                    operators = realtimeOperators,
                    onMapTapPoint = onMapTapPoint,
                    onUpdateBoundaryPoint = onUpdateBoundaryPoint,
                    onMapLoadingChanged = onMapLoadingChanged,
                    onMapError = onMapError,
                    onCameraMoved = onMapCameraMoved,
                    modifier = Modifier.fillMaxSize()
                )

                // Compact top floating Search + Style selector + GPS banner
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .widthIn(max = 600.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                        shadowElevation = 4.dp,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = mapState.searchQuery,
                                onValueChange = onSearchQueryChange,
                                placeholder = {
                                    Text(
                                        text = "Search village, taluka, or survey area",
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(
                                    onSearch = {
                                        focusManager.clearFocus()
                                        onCenterMap()
                                    }
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("map_search_input")
                            )
                            FilledTonalIconButton(
                                onClick = {
                                    focusManager.clearFocus()
                                    onCenterMap()
                                },
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = ForestGreen,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("map_search_button")
                            ) {
                                if (mapState.isSearchingLocation) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = Color.White
                                    )
                                } else {
                                    Icon(Icons.Rounded.Search, contentDescription = "Search location")
                                }
                            }
                        }
                    }

                    // Search suggestions
                    AnimatedVisibility(visible = mapState.locationSuggestions.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            shadowElevation = 4.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                mapState.locationSuggestions.take(4).forEach { match ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                focusManager.clearFocus()
                                                onSelectLocationSuggestion(match)
                                            }
                                            .padding(horizontal = 12.dp, vertical = 9.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.LocationOn,
                                            contentDescription = null,
                                            tint = EmeraldPulse,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = match.displayName,
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Map style pills + saved farm chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
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
                                shadowElevation = 2.dp,
                                modifier = Modifier
                                    .height(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onSelectMapStyle(style) }
                                    .testTag("map_style_${style.name.lowercase(Locale.US)}")
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

                        items(savedFarms, key = { "chip_${it.id}" }) { farm ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = SoftSage.copy(alpha = 0.95f),
                                border = BorderStroke(1.dp, EmeraldPulse),
                                modifier = Modifier
                                    .height(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onFocusSavedFarm(farm) }
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(horizontal = 10.dp)
                                ) {
                                    Text(
                                        text = "${farm.farmName} (${farm.areaAcres} ac)",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = ForestGreen
                                    )
                                }
                            }
                        }
                    }

                    // Graceful permission denied notice
                    if (mapState.locationPermissionDenied) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = HarvestGoldBg,
                            border = BorderStroke(1.dp, HarvestAmber),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "GPS permission needed to center on your farm",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = HarvestAmber,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(onClick = onRequestLocationPermission) {
                                    Text("Enable GPS", color = ForestGreen, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Offline tile download progress
                    val dlProgress = mapState.offlineDownloadProgress
                    if (dlProgress != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = "Caching offline map tiles: $dlProgress%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ForestGreen
                                )
                                LinearProgressIndicator(
                                    progress = { dlProgress / 100f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp),
                                    color = ForestGreen
                                )
                            }
                        }
                    }
                }

                // Compact 2x2 floating controls at bottom-end so map center stays clear
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        MapFloatingControlButton(
                            icon = Icons.Rounded.Add,
                            contentDescription = "Zoom in map",
                            onClick = { onZoomMap(true) },
                            testTag = "map_zoom_in_button"
                        )
                        MapFloatingControlButton(
                            icon = Icons.Rounded.MyLocation,
                            contentDescription = "Center on my GPS location",
                            onClick = onRefreshCurrentLocation,
                            testTag = "map_current_location_button"
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        MapFloatingControlButton(
                            icon = Icons.Rounded.Remove,
                            contentDescription = "Zoom out map",
                            onClick = { onZoomMap(false) },
                            testTag = "map_zoom_out_button"
                        )
                        MapFloatingControlButton(
                            icon = Icons.Rounded.CloudDownload,
                            contentDescription = "Download region for offline map use",
                            onClick = onCacheCurrentRegionOffline,
                            testTag = "map_offline_cache_button"
                        )
                    }
                }

                // OpenStreetMap attribution + live operators pill at bottom-start
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (realtimeOperators.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = ForestGreen.copy(alpha = 0.90f)
                        ) {
                            Text(
                                text = "${realtimeOperators.count { it.isAvailable }} Drone Pilots Nearby",
                                style = MaterialTheme.typography.labelSmall,
                                color = SoftSage,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                    ) {
                        Text(
                            text = "© OpenStreetMap contributors",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                // Loading indicator
                if (mapState.isMapLoading) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                        shadowElevation = 4.dp,
                        modifier = Modifier.align(Alignment.Center)
                    ) {
                        CircularProgressIndicator(
                            color = ForestGreen,
                            modifier = Modifier
                                .padding(12.dp)
                                .size(28.dp)
                        )
                    }
                }

                // Map tile load error + retry
                val mapError = mapState.mapErrorMessage
                if (!mapError.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = AlertRustBg,
                        border = BorderStroke(1.dp, AlertRust),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = AlertRust)
                            Text(
                                text = mapError,
                                style = MaterialTheme.typography.bodySmall,
                                color = AlertRust
                            )
                            Button(
                                onClick = onRetryMapLoad,
                                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                                modifier = Modifier.testTag("map_retry_button")
                            ) {
                                Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Retry Map")
                            }
                        }
                    }
                }
            }
        }

        val paymentPanelSection: @Composable (Modifier, Boolean) -> Unit = { panelModifier, isSidePanel ->
            Surface(
                shape = if (isSidePanel) {
                    RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp)
                } else {
                    RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)
                },
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = panelModifier
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Header row: Polygon instruction + Undo/Clear
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (mapState.polygonPoints.size < 3) {
                                    "Tap Field Corners on Map (${mapState.polygonPoints.size}/3+)"
                                } else {
                                    "Selected Land Area (${mapState.polygonPoints.size} pts)"
                                },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = ForestGreen,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Tap points to outline; drag pins to adjust.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            IconButton(
                                onClick = onUndoPolygonPoint,
                                enabled = mapState.polygonPoints.isNotEmpty(),
                                modifier = Modifier.testTag("undo_polygon_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.Undo,
                                    contentDescription = "Undo last point",
                                    tint = ForestGreen
                                )
                            }
                            IconButton(
                                onClick = onClearPolygon,
                                enabled = mapState.polygonPoints.isNotEmpty(),
                                modifier = Modifier.testTag("clear_polygon_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteSweep,
                                    contentDescription = "Clear boundary",
                                    tint = AlertRust
                                )
                            }
                        }
                    }

                    // Computed Acres, Hectares, and Price Strip
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = SoftSage.copy(alpha = 0.65f),
                        border = BorderStroke(1.dp, EmeraldPulse.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Selected Area",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${String.format(Locale.US, "%.2f", mapState.computedAcres)} acres",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = ForestGreen,
                                    modifier = Modifier.testTag("computed_acres_text")
                                )
                                Text(
                                    text = "${String.format(Locale.US, "%.2f", mapState.computedHectares)} ha",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EmeraldPulse,
                                    modifier = Modifier.testTag("computed_hectares_text")
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Payable (₹${GeoPolygonCalculator.TARIFF_PER_ACRE_INR.toInt()}/acre)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = formatInr(mapState.computedAmount),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = ForestGreen,
                                    modifier = Modifier.testTag("computed_price_text")
                                )
                            }
                        }
                    }

                    // Cash vs Online Payment Method Selection (always visible in bottom half)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PaymentMethod.entries.forEach { method ->
                            val active = mapState.selectedPaymentMethod == method
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onSelectPaymentMethod(method) }
                                    .border(
                                        width = if (active) 1.5.dp else 1.dp,
                                        color = if (active) ForestGreen else MaterialTheme.colorScheme.outline,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .testTag("payment_${method.apiValue}_chip"),
                                color = if (active) SoftSage else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Payments,
                                        contentDescription = null,
                                        tint = if (active) ForestGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = method.label,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                        color = if (active) ForestGreen else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    // Non-cancellable booking notice
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("booking_non_cancellable_notice"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = null,
                            tint = HarvestAmber,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Bookings cannot be cancelled after booking & payment. Pilot payout releases only when you tap Job Done.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Book Spray CTA (always visible without scrolling on standard phones)
                    Button(
                        onClick = onConfirmOrder,
                        enabled = mapState.polygonPoints.size >= 3 && mapState.computedAcres > 0.0,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("confirm_booking_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FlightTakeoff,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (mapState.computedAmount > 0.0) {
                                "Book Drone Spray • ${formatInr(mapState.computedAmount)}"
                            } else {
                                "Outline Field to Book Spray"
                            },
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    // Save Farm to Supabase row below CTA so selecting >= 3 points never hides payment controls or shrinks map
                    AnimatedVisibility(visible = mapState.polygonPoints.size >= 3) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = mapState.farmNameInput,
                                onValueChange = onFarmNameChange,
                                placeholder = { Text("Save farm name (optional)") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("farm_name_input")
                            )
                            OutlinedButton(
                                onClick = onSaveFarmToSupabase,
                                enabled = !mapState.isSavingFarm,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .height(50.dp)
                                    .testTag("save_farm_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.BookmarkAdd,
                                    contentDescription = "Save farm boundary",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(if (mapState.isSavingFarm) "Saving..." else "Save")
                            }
                        }
                    }
                }
            }
        }

        if (isWideLayout) {
            Row(modifier = Modifier.fillMaxSize()) {
                mapSection(
                    Modifier
                        .weight(0.58f)
                        .fillMaxHeight()
                )
                paymentPanelSection(
                    Modifier
                        .weight(0.42f)
                        .fillMaxHeight(),
                    true
                )
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                // Guaranteed >= 55% height for the interactive Map & selected boundary
                mapSection(
                    Modifier
                        .weight(0.55f)
                        .fillMaxWidth()
                )
                // Remaining 45% height for calculated acres, amount, Cash/Online & booking
                paymentPanelSection(
                    Modifier
                        .weight(0.45f)
                        .fillMaxWidth(),
                    false
                )
            }
        }
    }
}

@Composable
private fun MapFloatingControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shadowElevation = 4.dp,
        modifier = Modifier
            .size(48.dp)
            .testTag(testTag)
    ) {
        IconButton(onClick = onClick) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = ForestGreen
            )
        }
    }
}

@Composable
private fun FarmerOrdersContent(
    user: UserEntity,
    profile: FarmerProfileEntity?,
    orders: List<OrderWithDetails>,
    onEditCredentials: () -> Unit,
    onConfirmJobDone: (String) -> Unit,
    onRatePilot: (String, Int) -> Unit
) {
    // 10-second live countdown ticker for Blinkit-style ETA display
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(orders) {
        while (orders.any { it.statusEnum == OrderStatus.ACCEPTED }) {
            delay(10_000L)
            nowMs = System.currentTimeMillis()
        }
    }

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
            // Farmer Credentials Quick Card
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = user.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = ForestGreen
                            )
                            Text(
                                text = "${user.phone} • Farmer ID: ${profile?.farmerIdNumber ?: "-"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(
                            onClick = onEditCredentials,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("orders_edit_credentials_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ManageAccounts,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Edit")
                        }
                    }
                }
            }

            // Gamified Harvest Coins Summary Banner
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = HarvestGoldBg,
                    border = BorderStroke(1.dp, HarvestAmber.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Stars,
                            contentDescription = "Harvest Coins",
                            tint = HarvestAmber,
                            modifier = Modifier.size(32.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Harvest Coins: ${profile?.coins ?: 0}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = HarvestAmber
                            )
                            Text(
                                text = "Earn +1 Harvest Coin after tapping Job Done on a completed spray booking. Virtual reward points for gamification only.",
                                style = MaterialTheme.typography.bodySmall,
                                color = ForestGreen
                            )
                        }
                    }
                }
            }

            if (orders.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "No spray bookings yet. Switch to Book Spray to map your farmland and request a drone pilot.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(20.dp)
                        )
                    }
                }
            } else {
                items(orders, key = { it.order.id }) { item ->
                    FarmerOrderCard(
                        item = item,
                        nowMs = nowMs,
                        onConfirmJobDone = onConfirmJobDone,
                        onRatePilot = onRatePilot
                    )
                }
            }
        }
    }
}

@Composable
private fun FarmerOrderCard(
    item: OrderWithDetails,
    nowMs: Long,
    onConfirmJobDone: (String) -> Unit,
    onRatePilot: (String, Int) -> Unit
) {
    val status = item.statusEnum
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (status == OrderStatus.ACCEPTED || status == OrderStatus.IN_PROGRESS || status == OrderStatus.PILOT_COMPLETED) 1.5.dp else 1.dp,
            color = when (status) {
                OrderStatus.PILOT_COMPLETED -> HarvestAmber
                OrderStatus.ACCEPTED, OrderStatus.IN_PROGRESS -> ForestGreen
                else -> MaterialTheme.colorScheme.outline
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("farmer_order_card_${item.order.id}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = when (status) {
                        OrderStatus.ACCEPTED -> VerifiedTealBg
                        OrderStatus.IN_PROGRESS -> SoftSage
                        OrderStatus.PILOT_COMPLETED -> HarvestGoldBg
                        OrderStatus.COMPLETED -> SoftSage
                        OrderStatus.PENDING -> HarvestGoldBg
                        OrderStatus.CANCELLED -> AlertRustBg
                    }
                ) {
                    Text(
                        text = status.displayLabel,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = when (status) {
                            OrderStatus.ACCEPTED -> VerifiedTeal
                            OrderStatus.IN_PROGRESS -> ForestGreen
                            OrderStatus.PILOT_COMPLETED -> HarvestAmber
                            OrderStatus.COMPLETED -> ForestGreen
                            OrderStatus.PENDING -> HarvestAmber
                            OrderStatus.CANCELLED -> AlertRust
                        },
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
                Text(
                    text = formatInr(item.order.totalAmount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ForestGreen
                )
            }

            // Blinkit-style Live Pilot Arrival Countdown Card
            if (status == OrderStatus.ACCEPTED) {
                val acceptedAt = item.order.acceptedAt
                val elapsedMinutes = if (acceptedAt != null) {
                    ((nowMs - acceptedAt) / 60_000L).toInt().coerceAtLeast(0)
                } else {
                    0
                }
                val liveDist = item.liveDistanceKm ?: item.order.distanceKm
                val droneEnum = item.pilotDroneType?.let { DroneType.fromApi(it) } ?: DroneType.QUADCOPTER
                val remainingMins = if (liveDist != null) {
                    GeoPolygonCalculator.estimateArrivalMinutes(liveDist, droneEnum)
                } else {
                    (item.order.pilotEtaMinutes - elapsedMinutes).coerceAtLeast(1)
                }
                val initialEta = item.order.initialEtaMinutes.coerceAtLeast(1)
                val progressFraction = (1f - (remainingMins.toFloat() / initialEta.toFloat())).coerceIn(0.12f, 0.96f)

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = VerifiedTealBg.copy(alpha = 0.7f),
                    border = BorderStroke(1.dp, VerifiedTeal.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pilot_live_eta_banner_${item.order.id}")
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                    imageVector = Icons.Rounded.Schedule,
                                    contentDescription = "Live arrival countdown",
                                    tint = VerifiedTeal,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Pilot arriving in $remainingMins mins",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = ForestGreen,
                                    modifier = Modifier.testTag("eta_minutes_text_${item.order.id}")
                                )
                            }
                            if (liveDist != null) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.White.copy(alpha = 0.8f)
                                ) {
                                    Text(
                                        text = GeoPolygonCalculator.formatDistanceLabel(liveDist),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = VerifiedTeal,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        LinearProgressIndicator(
                            progress = { progressFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = VerifiedTeal,
                            trackColor = Color.White.copy(alpha = 0.6f)
                        )

                        val pName = item.pilotName ?: "Assigned Pilot"
                        val pPhone = item.pilotPhone?.let { " • $it" }.orEmpty()
                        val pLat = item.pilotLat
                        val pLng = item.pilotLng
                        val gpsTag = if (pLat != null && pLng != null) " • Live GPS Active" else ""
                        Text(
                            text = "$pName (${droneEnum.label})$pPhone$gpsTag",
                            style = MaterialTheme.typography.labelSmall,
                            color = ForestGreen
                        )
                    }
                }
            }

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
                        text = "${String.format(Locale.US, "%.2f", item.order.acresToSpray)} acres • ${item.paymentMethodEnum.label}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val assignedPilotName = item.pilotName
                    if (assignedPilotName != null && status != OrderStatus.ACCEPTED) {
                        Text(
                            text = "Pilot: $assignedPilotName",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldPulse
                        )
                    }
                }
            }

            // Non-cancellable & escrow status row for active bookings (no cancel option after booking/payment)
            if (status == OrderStatus.PENDING || status == OrderStatus.ACCEPTED || status == OrderStatus.IN_PROGRESS) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("order_non_cancellable_badge_${item.order.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = null,
                            tint = ForestGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        val escrowText = if (item.paymentMethodEnum == PaymentMethod.ONLINE) {
                            "Booking locked (non-cancellable) • ${formatInr(item.order.pilotEarnings)} held until you tap Job Done."
                        } else {
                            "Booking locked (non-cancellable after confirmation)."
                        }
                        Text(
                            text = escrowText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Pilot finished spraying: Farmer must click "Job Done" to release payment into pilot's wallet
            if (status == OrderStatus.PILOT_COMPLETED) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = HarvestGoldBg,
                    border = BorderStroke(1.dp, HarvestAmber.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pilot_completed_confirmation_card_${item.order.id}")
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.TaskAlt,
                                contentDescription = null,
                                tint = HarvestAmber,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Pilot Completed Spraying",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = ForestGreen
                            )
                        }
                        Text(
                            text = if (item.paymentMethodEnum == PaymentMethod.ONLINE) {
                                "Confirm spray completion on your field. Clicking Job Done transfers ${formatInr(item.order.pilotEarnings)} into ${item.pilotName ?: "the pilot"}'s wallet."
                            } else {
                                "Confirm spray completion on your field and click Job Done to close this booking."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = ForestGreen
                        )
                        Button(
                            onClick = { onConfirmJobDone(item.order.id) },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
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
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (item.paymentMethodEnum == PaymentMethod.ONLINE) {
                                    "Job Done • Release ${formatInr(item.order.pilotEarnings)}"
                                } else {
                                    "Job Done"
                                },
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }

            // Completed Order: Wallet release + +1 Coin confirmation + Pilot Rating
            if (status == OrderStatus.COMPLETED) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = SoftSage.copy(alpha = 0.55f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = EmeraldPulse,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = if (item.paymentMethodEnum == PaymentMethod.ONLINE) {
                                    "Job Done • ${formatInr(item.order.pilotEarnings)} transferred to pilot wallet • +1 Coin"
                                } else {
                                    "Job Done • Spray completed • +1 Harvest Coin earned"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = ForestGreen
                            )
                        }

                        val currentRating = item.order.pilotRating
                        if (currentRating != null) {
                            Text(
                                text = "You rated this pilot ${currentRating}★",
                                style = MaterialTheme.typography.labelSmall,
                                color = HarvestAmber
                            )
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Rate Pilot:",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ForestGreen
                                )
                                (1..5).forEach { star ->
                                    IconButton(
                                        onClick = { onRatePilot(item.order.id, star) },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .testTag("rate_pilot_${item.order.id}_$star")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Star,
                                            contentDescription = "Rate $star stars",
                                            tint = HarvestAmber
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
private fun EditFarmerCredentialsDialog(
    initialName: String,
    initialPhone: String,
    initialFarmerId: String,
    onSave: (name: String, phone: String, farmerId: String, onError: (String) -> Unit) -> Unit,
    onDismiss: () -> Unit
) {
    var name by rememberSaveable(initialName) { mutableStateOf(initialName) }
    var phone by rememberSaveable(initialPhone) { mutableStateOf(initialPhone) }
    var farmerId by rememberSaveable(initialFarmerId) { mutableStateOf(initialFarmerId) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.ManageAccounts,
                    contentDescription = null,
                    tint = ForestGreen
                )
                Text(
                    text = "Update Farmer Credentials",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        errorMessage = null
                    },
                    label = { Text("Farmer Full Name") },
                    leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_farmer_name_input")
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = {
                        if (it.length <= 15) {
                            phone = it
                            errorMessage = null
                        }
                    },
                    label = { Text("Mobile Number") },
                    leadingIcon = { Icon(Icons.Rounded.Phone, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_farmer_phone_input")
                )
                OutlinedTextField(
                    value = farmerId,
                    onValueChange = {
                        farmerId = it
                        errorMessage = null
                    },
                    label = { Text("Farmer ID Number") },
                    leadingIcon = { Icon(Icons.Rounded.Badge, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_farmer_id_input")
                )
                val err = errorMessage
                if (!err.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = AlertRustBg,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = err,
                            style = MaterialTheme.typography.bodySmall,
                            color = AlertRust,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, phone, farmerId) { err -> errorMessage = err } },
                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("save_farmer_credentials_button")
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.height(48.dp)
            ) {
                Text("Cancel")
            }
        }
    )
}
