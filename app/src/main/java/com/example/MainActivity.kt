package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.UserRole
import com.example.data.remote.KrishivantriRuntimeConfig
import com.example.ui.components.RazorpayCheckoutSheet
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.FarmerDashboardScreen
import com.example.ui.theme.KrishivantriTheme
import com.example.ui.viewmodel.KrishivantriViewModel
import com.example.ui.viewmodel.RazorpaySheetState
import com.razorpay.Checkout
import com.razorpay.PaymentData
import com.razorpay.PaymentResultWithDataListener
import org.json.JSONObject
import org.maplibre.android.MapLibre
import kotlin.math.roundToInt

class MainActivity : ComponentActivity(), PaymentResultWithDataListener {

    private val viewModel: KrishivantriViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        KrishivantriRuntimeConfig.configure(
            apiBaseUrl = BuildConfig.API_BASE_URL,
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseAnonKey = BuildConfig.SUPABASE_ANON_KEY,
            razorpayKeyId = BuildConfig.RAZORPAY_KEY_ID,
            satelliteTileUrl = BuildConfig.SATELLITE_TILE_URL,
            satelliteApiKey = BuildConfig.SATELLITE_API_KEY
        )
        runCatching { MapLibre.getInstance(applicationContext) }
        enableEdgeToEdge()
        setContent {
            KrishivantriTheme {
                KrishivantriApp(
                    viewModel = viewModel,
                    onLaunchRazorpay = ::launchRazorpayCheckout
                )
            }
        }
    }

    private fun launchRazorpayCheckout(sheet: RazorpaySheetState) {
        if (!KrishivantriRuntimeConfig.isRazorpayConfigured()) {
            viewModel.onRazorpayKeyMissing()
            return
        }
        runCatching {
            val checkout = Checkout()
            checkout.setKeyID(KrishivantriRuntimeConfig.razorpayKeyId.trim())

            val options = JSONObject().apply {
                put("name", "Krishivantri")
                put("description", sheet.subtitle)
                put("currency", "INR")
                put("amount", (sheet.amount * 100.0).roundToInt().coerceAtLeast(100))
                put("theme", JSONObject().put("color", "#1B4332"))

                val retryObj = JSONObject().apply {
                    put("enabled", true)
                    put("max_count", 3)
                }
                put("retry", retryObj)

                val prefill = JSONObject()
                viewModel.currentUser.value?.phone?.takeIf { it.isNotBlank() }?.let {
                    prefill.put("contact", it)
                }
                put("prefill", prefill)
            }
            checkout.open(this, options)
        }.onFailure { err ->
            viewModel.onRazorpayPaymentError(-1, err.localizedMessage)
        }
    }

    override fun onPaymentSuccess(razorpayPaymentId: String?, paymentData: PaymentData?) {
        viewModel.confirmRazorpayPayment(razorpayPaymentId ?: paymentData?.paymentId)
    }

    override fun onPaymentError(code: Int, response: String?, paymentData: PaymentData?) {
        viewModel.onRazorpayPaymentError(code, response)
    }
}

@Composable
fun KrishivantriApp(
    viewModel: KrishivantriViewModel,
    onLaunchRazorpay: (RazorpaySheetState) -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val mapState by viewModel.mapBookingState.collectAsStateWithLifecycle()
    val farmerProfile by viewModel.farmerProfile.collectAsStateWithLifecycle()
    val farmerOrders by viewModel.farmerOrders.collectAsStateWithLifecycle()
    val savedFarms by viewModel.savedFarms.collectAsStateWithLifecycle()
    val bookingOrdersForMap by viewModel.allBookingOrdersForMap.collectAsStateWithLifecycle()
    val realtimeOperators by viewModel.realtimeOperators.collectAsStateWithLifecycle()
    val adminOverview by viewModel.adminOverview.collectAsStateWithLifecycle()
    val razorpaySheet by viewModel.razorpaySheet.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.selectAuthRole(UserRole.FARMER)
    }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.consumeSnackbar()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val user = currentUser
            if (user == null) {
                AuthScreen(
                    state = authState,
                    onToggleSignUp = viewModel::toggleAuthMode,
                    onSubmitFarmer = viewModel::submitFarmerAuth,
                    onSubmitAdmin = viewModel::submitAdminLogin
                )
            } else {
                when (UserRole.fromApi(user.role)) {
                    UserRole.ADMIN -> AdminDashboardScreen(
                        overview = adminOverview,
                        onVerifyPilotLicense = viewModel::verifyPilotLicense,
                        onSignOut = viewModel::signOut
                    )

                    UserRole.FARMER, UserRole.PILOT -> FarmerDashboardScreen(
                        user = user,
                        profile = farmerProfile,
                        mapRepository = viewModel.mapRepository,
                        mapState = mapState,
                        savedFarms = savedFarms,
                        bookingOrdersForMap = bookingOrdersForMap,
                        realtimeOperators = realtimeOperators,
                        orders = farmerOrders,
                        onLocationPermissionResult = viewModel::onLocationPermissionResult,
                        onResumeGpsUpdates = viewModel::startBatteryEfficientGpsUpdates,
                        onPauseGpsUpdates = viewModel::stopBatteryEfficientGpsUpdates,
                        onRefreshCurrentLocation = viewModel::refreshCurrentLocation,
                        onSelectMapStyle = viewModel::selectMapStyle,
                        onSearchQueryChange = viewModel::updateSearchQuery,
                        onCenterMap = { viewModel.searchAndCenterMap(context) },
                        onSelectLocationSuggestion = viewModel::selectLocationSuggestion,
                        onMapTapPoint = viewModel::addMapBoundaryPoint,
                        onUpdateBoundaryPoint = viewModel::updateMapBoundaryPoint,
                        onMapCameraMoved = viewModel::onMapCameraMoved,
                        onMapLoadingChanged = viewModel::onMapLoadingChanged,
                        onMapError = viewModel::onMapError,
                        onRetryMapLoad = viewModel::retryMapLoad,
                        onUndoPolygonPoint = viewModel::undoLastPolygonPoint,
                        onClearPolygon = viewModel::clearPolygon,
                        onZoomMap = viewModel::zoomMap,
                        onResetCompass = viewModel::resetCompass,
                        onFarmNameChange = viewModel::updateFarmName,
                        onSaveFarmToSupabase = viewModel::saveFarmBoundaryToSupabase,
                        onFocusSavedFarm = viewModel::focusSavedFarm,
                        onCacheCurrentRegionOffline = viewModel::cacheCurrentRegionOffline,
                        onSelectPaymentMethod = viewModel::selectPaymentMethod,
                        onConfirmOrder = viewModel::initiateFarmerOrderPlacement,
                        onConsumeBookingSuccess = viewModel::consumeBookingSuccess,
                        onRatePilot = viewModel::ratePilot,
                        onSignOut = viewModel::signOut
                    )
                }
            }
        }
    }

    razorpaySheet?.let { sheet ->
        RazorpayCheckoutSheet(
            state = sheet,
            onConfirmPayment = { onLaunchRazorpay(sheet) },
            onDismiss = viewModel::dismissRazorpaySheet
        )
    }
}
