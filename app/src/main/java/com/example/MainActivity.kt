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
import com.example.ui.components.RazorpayHostedCheckoutDialog
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
        val resolvedSupabaseUrl = listOf(
            BuildConfig.NEXT_PUBLIC_SUPABASE_URL,
            BuildConfig.SUPABASE_URL
        ).firstOrNull { it.isNotBlank() && !it.contains("your-project") }
            ?: BuildConfig.SUPABASE_URL

        val resolvedSupabaseAnonKey = listOf(
            BuildConfig.NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY,
            BuildConfig.SUPABASE_ANON_KEY
        ).firstOrNull { it.isNotBlank() && !it.startsWith("MY_SUPABASE") }
            ?: BuildConfig.SUPABASE_ANON_KEY

        KrishivantriRuntimeConfig.configure(
            apiBaseUrl = BuildConfig.API_BASE_URL,
            supabaseUrl = resolvedSupabaseUrl,
            supabaseAnonKey = resolvedSupabaseAnonKey,
            razorpayKeyId = BuildConfig.RAZORPAY_KEY_ID,
            satelliteTileUrl = BuildConfig.SATELLITE_TILE_URL,
            satelliteApiKey = BuildConfig.SATELLITE_API_KEY
        )
        if (KrishivantriRuntimeConfig.isRazorpayConfigured()) {
            runCatching { Checkout.preload(applicationContext) }
        }
        runCatching { MapLibre.getInstance(applicationContext) }
        enableEdgeToEdge()
        setContent {
            KrishivantriTheme {
                KrishivantriApp(
                    viewModel = viewModel,
                    onLaunchRazorpaySdk = ::launchRazorpayCheckout
                )
            }
        }
    }

    private fun launchRazorpayCheckout(sheet: RazorpaySheetState) {
        if (!KrishivantriRuntimeConfig.isRazorpayConfigured()) {
            viewModel.onRazorpayPaymentError(-1, "RAZORPAY_KEY_ID is not configured in AI Studio Secrets.")
            return
        }
        runCatching {
            val checkout = Checkout()
            checkout.setKeyID(KrishivantriRuntimeConfig.razorpayKeyId.trim())
            checkout.setImage(R.mipmap.ic_launcher)

            val options = JSONObject().apply {
                put("name", "Krishivantri")
                put("description", sheet.subtitle.take(200))
                put("currency", "INR")
                put("amount", (sheet.amount * 100.0).roundToInt().coerceAtLeast(100))
                put("theme", JSONObject().put("color", "#1B4332"))

                val retryObj = JSONObject().apply {
                    put("enabled", true)
                    put("max_count", 3)
                }
                put("retry", retryObj)

                val prefill = JSONObject()
                val rawPhone = viewModel.currentUser.value?.phone.orEmpty()
                val digits = rawPhone.filter { it.isDigit() }
                if (digits.length >= 10) {
                    val contact = if (digits.length == 10) "+91$digits" else "+$digits"
                    prefill.put("contact", contact)
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
        val readable = parseRazorpayErrorDescription(response)
        viewModel.onRazorpayPaymentError(code, readable)
    }

    private fun parseRazorpayErrorDescription(raw: String?): String? {
        val text = raw?.trim().orEmpty()
        if (text.isEmpty()) return null
        return runCatching {
            val obj = JSONObject(text)
            obj.optJSONObject("error")?.optString("description")?.takeIf { it.isNotBlank() }
                ?: obj.optString("description").takeIf { it.isNotBlank() }
                ?: text
        }.getOrDefault(text)
    }
}

@Composable
fun KrishivantriApp(
    viewModel: KrishivantriViewModel,
    onLaunchRazorpaySdk: (RazorpaySheetState) -> Unit
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
    val hostedCheckoutSession by viewModel.hostedCheckoutSession.collectAsStateWithLifecycle()
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
                        onUpdateCredentials = viewModel::updateFarmerCredentials,
                        onConfirmJobDone = viewModel::confirmJobDone,
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
            onConfirmPayment = { viewModel.startRazorpayCheckout(onFallbackToSdk = onLaunchRazorpaySdk) },
            onDismiss = viewModel::dismissRazorpaySheet
        )
    }

    hostedCheckoutSession?.let { session ->
        RazorpayHostedCheckoutDialog(
            session = session,
            onPaymentCallback = viewModel::verifyAndCompleteHostedCheckout,
            onDismiss = { viewModel.dismissHostedCheckout(cancelledByUser = true) }
        )
    }
}
