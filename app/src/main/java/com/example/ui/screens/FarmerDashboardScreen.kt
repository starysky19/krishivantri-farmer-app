package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.ManageAccounts
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.model.AgriStoreCartItem
import com.example.data.model.AgriStoreOrderReceipt
import com.example.data.model.AgriStoreProduct
import com.example.data.model.AppLanguage
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
import com.example.domain.LocationMatch
import com.example.ui.screens.farmer.FarmerAccountTab
import com.example.ui.screens.farmer.FarmerAgriStoreTab
import com.example.ui.screens.farmer.FarmerBookSprayTab
import com.example.ui.screens.farmer.FarmerBookingsTab
import com.example.ui.screens.farmer.FarmerBottomNavBar
import com.example.ui.screens.farmer.FarmerLiveDroneTab
import com.example.ui.screens.farmer.FarmerTabIndex
import com.example.ui.screens.farmer.FarmerTopHeader
import com.example.ui.theme.AlertRust
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.HarvestAmber
import com.example.ui.theme.HarvestGoldBg
import com.example.ui.theme.SoftSage
import com.example.ui.viewmodel.DatabaseConnectionUiState
import com.example.ui.viewmodel.MapBookingState

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
    databaseStatus: DatabaseConnectionUiState = DatabaseConnectionUiState(),
    appLanguage: AppLanguage = AppLanguage.EN,
    agriStoreCart: List<AgriStoreCartItem> = emptyList(),
    agriStoreReceipts: List<AgriStoreOrderReceipt> = emptyList(),
    onToggleLanguage: (AppLanguage) -> Unit = {},
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
    onRetryMapLoad: () -> Unit = {},
    onUndoPolygonPoint: () -> Unit,
    onClearPolygon: () -> Unit,
    onZoomMap: (Boolean) -> Unit,
    onResetCompass: () -> Unit = {},
    onFarmNameChange: (String) -> Unit,
    onSaveFarmToSupabase: () -> Unit,
    onFocusSavedFarm: (FarmBoundaryEntity) -> Unit,
    onCacheCurrentRegionOffline: () -> Unit,
    onSelectSprayCrop: (String) -> Unit = {},
    onSelectSprayFormula: (String) -> Unit = {},
    onSelectPaymentMethod: (PaymentMethod) -> Unit,
    onConfirmOrder: () -> Unit = {},
    onConfirmOrderWithMethod: (PaymentMethod) -> Unit = { method ->
        onSelectPaymentMethod(method)
        onConfirmOrder()
    },
    onRebookOrderPolygon: (OrderWithDetails) -> Unit = {},
    onAddToAgriCart: (AgriStoreProduct) -> Unit = {},
    onUpdateAgriCartQty: (String, Int) -> Unit = { _, _ -> },
    onClearAgriCart: () -> Unit = {},
    onCheckoutAgriStore: (PaymentMethod) -> Unit = {},
    onRefreshDatabaseConnection: () -> Unit = {},
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

    var selectedTab by rememberSaveable { mutableIntStateOf(FarmerTabIndex.BOOK_SPRAY) }
    var showEditCredentialsDialog by remember { mutableStateOf(false) }
    var ratingOrderTarget by remember { mutableStateOf<OrderWithDetails?>(null) }
    var showNonCancellablePolicyDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = selectedTab != FarmerTabIndex.BOOK_SPRAY) {
        selectedTab = FarmerTabIndex.BOOK_SPRAY
    }

    val activeOrders = orders.filter {
        it.statusEnum != OrderStatus.COMPLETED && it.statusEnum != OrderStatus.CANCELLED
    }
    val cartItemCount = agriStoreCart.sumOf { it.quantity }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            FarmerTopHeader(
                selectedTab = selectedTab,
                userName = user.name,
                appLanguage = appLanguage,
                onToggleLanguage = onToggleLanguage,
                onOpenAccount = { selectedTab = FarmerTabIndex.ACCOUNT }
            )
        },
        bottomBar = {
            FarmerBottomNavBar(
                selectedTab = selectedTab,
                onSelectTab = { selectedTab = it },
                hasActiveMission = activeOrders.isNotEmpty(),
                cartItemCount = cartItemCount,
                appLanguage = appLanguage
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                FarmerTabIndex.BOOK_SPRAY -> FarmerBookSprayTab(
                    mapRepository = mapRepository,
                    mapState = mapState,
                    savedFarms = savedFarms,
                    realtimeOperators = realtimeOperators,
                    appLanguage = appLanguage,
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
                    onUndoPolygonPoint = onUndoPolygonPoint,
                    onClearPolygon = onClearPolygon,
                    onZoomMap = onZoomMap,
                    onFarmNameChange = onFarmNameChange,
                    onSaveFarmToSupabase = onSaveFarmToSupabase,
                    onFocusSavedFarm = onFocusSavedFarm,
                    onCacheCurrentRegionOffline = onCacheCurrentRegionOffline,
                    onSelectSprayCrop = onSelectSprayCrop,
                    onSelectSprayFormula = onSelectSprayFormula,
                    onSelectPaymentMethod = onSelectPaymentMethod,
                    onConfirmOrderWithMethod = onConfirmOrderWithMethod
                )

                FarmerTabIndex.LIVE_DRONE -> FarmerLiveDroneTab(
                    activeOrders = activeOrders,
                    mapRepository = mapRepository,
                    realtimeOperators = realtimeOperators,
                    databaseStatus = databaseStatus,
                    hasLocationPermission = mapState.hasLocationPermission,
                    onConfirmJobDone = { orderId ->
                        onConfirmJobDone(orderId)
                        orders.firstOrNull { it.order.id == orderId }?.let { ratingOrderTarget = it }
                    },
                    onShowNonCancellableNotice = { showNonCancellablePolicyDialog = true },
                    onBookNewSpray = { selectedTab = FarmerTabIndex.BOOK_SPRAY }
                )

                FarmerTabIndex.BOOKINGS -> FarmerBookingsTab(
                    profile = profile,
                    orders = orders,
                    savedFarms = savedFarms,
                    onViewLiveDrone = { selectedTab = FarmerTabIndex.LIVE_DRONE },
                    onConfirmJobDone = { orderId ->
                        onConfirmJobDone(orderId)
                        orders.firstOrNull { it.order.id == orderId }?.let { ratingOrderTarget = it }
                    },
                    onOpenRateDialog = { ratingOrderTarget = it },
                    onRebookOrderPolygon = { order ->
                        onRebookOrderPolygon(order)
                        selectedTab = FarmerTabIndex.BOOK_SPRAY
                    },
                    onBookSavedFarm = { farm ->
                        onFocusSavedFarm(farm)
                        selectedTab = FarmerTabIndex.BOOK_SPRAY
                    },
                    onGoToBookSpray = { selectedTab = FarmerTabIndex.BOOK_SPRAY }
                )

                FarmerTabIndex.AGRI_STORE -> FarmerAgriStoreTab(
                    cart = agriStoreCart,
                    receipts = agriStoreReceipts,
                    onAddToCart = onAddToAgriCart,
                    onUpdateCartQty = onUpdateAgriCartQty,
                    onClearCart = onClearAgriCart,
                    onCheckoutAgriStore = onCheckoutAgriStore
                )

                else -> FarmerAccountTab(
                    user = user,
                    profile = profile,
                    orders = orders,
                    onOpenEditCredentials = { showEditCredentialsDialog = true },
                    onSignOut = onSignOut
                )
            }
        }
    }

    if (showEditCredentialsDialog) {
        EditFarmerCredentialsDialog(
            currentUser = user,
            currentProfile = profile,
            onSave = onUpdateCredentials,
            onDismiss = { showEditCredentialsDialog = false }
        )
    }

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

    mapState.bookingSuccessOrderId?.let {
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
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = ForestGreen)
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
                        text = "Your precision spray booking is confirmed. Licensed DGCA drone pilots nearby are receiving priority telemetry.",
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
                            Icon(Icons.Rounded.Lock, contentDescription = null, tint = HarvestAmber, modifier = Modifier.size(18.dp))
                            Text(
                                text = "Non-cancellable policy: Payment is held safely in escrow until you click 'Job Done'.",
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
                        selectedTab = FarmerTabIndex.LIVE_DRONE
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreen)
                ) {
                    Text("Track Live Drone")
                }
            },
            dismissButton = {
                TextButton(onClick = onConsumeBookingSuccess) {
                    Text("Close")
                }
            }
        )
    }

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
                    Icon(Icons.Rounded.Lock, contentDescription = null, tint = HarvestAmber)
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
                        text = "Licensed DGCA drone pilots reserve flight slots, prepare battery cycles, and load authorized spray payload based on your field geometry.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = SoftSage,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Your money is 100% escrow protected. The pilot receives payout only after spraying completes and you click 'Job Done'.",
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
                Icon(Icons.Rounded.ManageAccounts, contentDescription = null, tint = ForestGreen)
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

                errorMessage?.takeIf { it.isNotBlank() }?.let { err ->
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
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text("Save Credentials")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

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
            TextButton(onClick = onDismiss) { Text("Skip") }
        }
    )
}
