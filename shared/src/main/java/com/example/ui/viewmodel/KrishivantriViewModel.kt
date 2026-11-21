package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.KrishivantriDatabase
import com.example.data.local.PersistentAuthSessionStore
import com.example.data.model.AgriStoreCartItem
import com.example.data.model.AgriStoreOrderReceipt
import com.example.data.model.AgriStoreProduct
import com.example.data.model.AppLanguage
import com.example.data.model.CommissionLogEntity
import com.example.data.model.DailyOrderVolume
import com.example.data.model.DroneType
import com.example.data.model.FarmBoundaryEntity
import com.example.data.model.FarmerCatalogDefaults
import com.example.data.model.FarmerProfileEntity
import com.example.data.model.FarmerWithUser
import com.example.data.model.LatLngPoint
import com.example.data.model.LocationSnapshot
import com.example.data.model.MapStyleType
import com.example.data.model.OfflineMapRegionInfo
import com.example.data.model.OperatorLocationMarker
import com.example.data.model.OperatorRoleType
import com.example.data.model.OrderStatus
import com.example.data.model.OrderWithDetails
import com.example.data.model.PaymentMethod
import com.example.data.model.PilotPerformanceStat
import com.example.data.model.PilotProfileEntity
import com.example.data.model.UserEntity
import com.example.data.model.UserRole
import com.example.data.remote.SupabaseConnectionStatus
import com.example.data.repository.AcceptOrderResult
import com.example.data.repository.AuthResult
import com.example.data.repository.EmailAuthResult
import com.example.data.repository.EmailOtpAuthRepository
import com.example.data.repository.FarmRepository
import com.example.data.repository.JobDoneResult
import com.example.data.repository.KrishivantriRepository
import com.example.data.repository.LocationRepository
import com.example.data.repository.MapRepository
import com.example.data.repository.OtpRequestResult
import com.example.domain.GeoPolygonCalculator
import com.example.domain.LocationMatch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RazorpaySheetState(
    val title: String,
    val subtitle: String,
    val amount: Double,
    val purpose: RazorpayPurpose
)

sealed class RazorpayPurpose {
    data class FarmerOrderCheckout(
        val locationAddress: String,
        val polygonPoints: List<LatLngPoint>,
        val farmName: String = ""
    ) : RazorpayPurpose()

    data class PilotWalletTopUp(val pilotId: String) : RazorpayPurpose()

    data class AgriStoreCheckout(
        val itemCount: Int,
        val summaryLabel: String
    ) : RazorpayPurpose()
}

data class InsufficientWalletAlert(
    val requiredCommission: Double,
    val currentBalance: Double
)

data class AuthUiState(
    val selectedRole: UserRole = UserRole.FARMER,
    val isSignUpMode: Boolean = false,
    val isRestoringSession: Boolean = true,
    val otpRequestedEmail: String? = null,
    val otpInfoMessage: String? = null,
    val otpResendCooldownSeconds: Int = 0,
    val otpExpiresInSeconds: Int = 0,
    val otpFailedAttempts: Int = 0,
    val maxOtpAttempts: Int = EmailOtpAuthRepository.MAX_OTP_ATTEMPTS,
    val errorMessage: String? = null,
    val isLoading: Boolean = false
)

data class DatabaseConnectionUiState(
    val localDbName: String = "krishivantri_db (Room SQLite)",
    val cloudStatus: SupabaseConnectionStatus = SupabaseConnectionStatus.Checking,
    val lastCheckedAt: Long? = null
)

data class MapBookingState(
    val searchQuery: String = "Nashik, Maharashtra",
    val resolvedLocationLabel: String = "Nashik, Maharashtra",
    val locationSuggestions: List<LocationMatch> = emptyList(),
    val mapCenter: LatLngPoint = LatLngPoint(20.0059, 73.7897),
    val mapStyleType: MapStyleType = MapStyleType.HYBRID,
    val cameraFlyNonce: Int = 0,
    val zoomInNonce: Int = 0,
    val zoomOutNonce: Int = 0,
    val resetCompassNonce: Int = 0,
    val styleRetryNonce: Int = 0,
    val isMapLoading: Boolean = true,
    val mapErrorMessage: String? = null,
    val hasLocationPermission: Boolean = false,
    val locationPermissionDenied: Boolean = false,
    val currentGpsLocation: LocationSnapshot? = null,
    val polygonPoints: List<LatLngPoint> = emptyList(),
    val computedAcres: Double = 0.0,
    val computedHectares: Double = 0.0,
    val computedAmount: Double = 0.0,
    val farmNameInput: String = "",
    val isSavingFarm: Boolean = false,
    val offlineRegions: List<OfflineMapRegionInfo> = emptyList(),
    val offlineDownloadProgress: Int? = null,
    val selectedPaymentMethod: PaymentMethod = PaymentMethod.ONLINE,
    val selectedCrop: String = FarmerCatalogDefaults.cropTypes.first(),
    val selectedFormulaId: String = FarmerCatalogDefaults.sprayObjectives.first().id,
    val isSearchingLocation: Boolean = false,
    val bookingSuccessOrderId: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class KrishivantriViewModel(application: Application) : AndroidViewModel(application) {

    private val database = KrishivantriDatabase.getInstance(application)
    private val repository = KrishivantriRepository(database)
    private val sessionStore = PersistentAuthSessionStore(application)
    val emailAuthRepository = EmailOtpAuthRepository(database, sessionStore)
    val locationRepository = LocationRepository(application)
    val farmRepository = FarmRepository(database.dao())
    val mapRepository = MapRepository(application, database.dao())

    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    private val _authState = MutableStateFlow(AuthUiState())
    val authState: StateFlow<AuthUiState> = _authState.asStateFlow()

    private val _mapBookingState = MutableStateFlow(
        MapBookingState(hasLocationPermission = locationRepository.hasFineOrCoarsePermission())
    )
    val mapBookingState: StateFlow<MapBookingState> = _mapBookingState.asStateFlow()

    private val _razorpaySheet = MutableStateFlow<RazorpaySheetState?>(null)
    val razorpaySheet: StateFlow<RazorpaySheetState?> = _razorpaySheet.asStateFlow()

    private val _insufficientWalletAlert = MutableStateFlow<InsufficientWalletAlert?>(null)
    val insufficientWalletAlert: StateFlow<InsufficientWalletAlert?> = _insufficientWalletAlert.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _databaseStatus = MutableStateFlow(DatabaseConnectionUiState())
    val databaseStatus: StateFlow<DatabaseConnectionUiState> = _databaseStatus.asStateFlow()

    private val _appLanguage = MutableStateFlow(AppLanguage.EN)
    val appLanguage: StateFlow<AppLanguage> = _appLanguage.asStateFlow()

    private val _agriStoreCart = MutableStateFlow<List<AgriStoreCartItem>>(emptyList())
    val agriStoreCart: StateFlow<List<AgriStoreCartItem>> = _agriStoreCart.asStateFlow()

    private val _agriStoreReceipts = MutableStateFlow<List<AgriStoreOrderReceipt>>(emptyList())
    val agriStoreReceipts: StateFlow<List<AgriStoreOrderReceipt>> = _agriStoreReceipts.asStateFlow()

    private var gpsObserverJob: Job? = null
    private var pilotLiveLocationJob: Job? = null
    private var farmerSupabaseSyncJob: Job? = null
    private var marketSupabaseSyncJob: Job? = null
    private var otpCountdownJob: Job? = null
    private val otpRequestHistoryMillis = mutableListOf<Long>()

    init {
        viewModelScope.launch {
            runCatching { repository.ensureAdminSeeded() }
            refreshOfflineRegions()
            restorePersistentSessionOnLaunch()
        }
        checkDatabaseConnection()
    }

    private suspend fun restorePersistentSessionOnLaunch() {
        val restored = runCatching { emailAuthRepository.restorePersistentSession() }.getOrNull()
        if (restored is EmailAuthResult.Success) {
            _currentUser.value = restored.user
            _mapBookingState.value = MapBookingState(
                hasLocationPermission = locationRepository.hasFineOrCoarsePermission(),
                offlineRegions = _mapBookingState.value.offlineRegions
            )
            if (restored.user.role == UserRole.FARMER.apiValue) {
                startFarmerSupabaseSync(restored.user.id)
            } else if (restored.user.role == UserRole.ADMIN.apiValue) {
                startMarketSupabaseSync()
            }
        }
        _authState.update { it.copy(isRestoringSession = false) }
    }

    fun checkDatabaseConnection(announceResult: Boolean = false) {
        viewModelScope.launch {
            _databaseStatus.update { it.copy(cloudStatus = SupabaseConnectionStatus.Checking) }
            val result = repository.checkCloudDatabaseConnection()
            _databaseStatus.update {
                it.copy(
                    cloudStatus = result,
                    lastCheckedAt = System.currentTimeMillis()
                )
            }
            if (announceResult) {
                _snackbarMessage.value = when (result) {
                    is SupabaseConnectionStatus.Connected ->
                        "Cloud sync is active and up to date."
                    is SupabaseConnectionStatus.MissingConfig,
                    is SupabaseConnectionStatus.Error ->
                        "Unable to reach cloud sync right now. Please try again."
                    is SupabaseConnectionStatus.Checking -> null
                }
            }
        }
    }

    // Farmer reactive streams
    val farmerProfile: StateFlow<FarmerProfileEntity?> = _currentUser
        .flatMapLatest { user ->
            if (user?.role == UserRole.FARMER.apiValue) {
                repository.observeFarmerProfile(user.id)
            } else {
                flowOf(null)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val farmerOrders: StateFlow<List<OrderWithDetails>> = _currentUser
        .flatMapLatest { user ->
            if (user?.role == UserRole.FARMER.apiValue) {
                repository.observeOrdersForFarmer(user.id)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savedFarms: StateFlow<List<FarmBoundaryEntity>> = _currentUser
        .flatMapLatest { user ->
            if (user != null) {
                farmRepository.observeFarmerBoundaries(user.id)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allBookingOrdersForMap: StateFlow<List<OrderWithDetails>> =
        repository.observeAllOrdersWithDetails()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val realtimeOperators: StateFlow<List<OperatorLocationMarker>> =
        mapRepository.observeRealtimeOperators(_mapBookingState.value.mapCenter)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Pilot reactive streams
    val pilotProfile: StateFlow<PilotProfileEntity?> = _currentUser
        .flatMapLatest { user ->
            if (user?.role == UserRole.PILOT.apiValue) {
                repository.observePilotProfile(user.id)
            } else {
                flowOf(null)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val pilotQueueOrders: StateFlow<List<OrderWithDetails>> = _currentUser
        .flatMapLatest { user ->
            if (user?.role == UserRole.PILOT.apiValue) {
                repository.observePilotQueue(user.id)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pilotAssignedOrders: StateFlow<List<OrderWithDetails>> = _currentUser
        .flatMapLatest { user ->
            if (user?.role == UserRole.PILOT.apiValue) {
                repository.observePilotAssignedOrders(user.id)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Admin-only reactive streams
    data class AdminOverviewState(
        val totalPilots: Int = 0,
        val verifiedPilots: Int = 0,
        val totalFarmers: Int = 0,
        val totalOrders: Int = 0,
        val totalGmv: Double = 0.0,
        val totalPlatformCommission: Double = 0.0,
        val dailyVolumes: List<DailyOrderVolume> = emptyList(),
        val pilotStats: List<PilotPerformanceStat> = emptyList(),
        val farmers: List<FarmerWithUser> = emptyList(),
        val commissionLogs: List<CommissionLogEntity> = emptyList()
    )

    val adminOverview: StateFlow<AdminOverviewState> = _currentUser
        .flatMapLatest { user ->
            if (user?.role == UserRole.ADMIN.apiValue) {
                combine(
                    repository.observePilotPerformanceStats(),
                    repository.observeAllFarmersWithUsers(),
                    repository.observeAllOrdersWithDetails(),
                    repository.observeDailyOrderVolumes(),
                    repository.observeCommissionLogs()
                ) { pilotStats, farmers, orders, daily, logs ->
                    AdminOverviewState(
                        totalPilots = pilotStats.size,
                        verifiedPilots = pilotStats.count { it.isLicenseVerified },
                        totalFarmers = farmers.size,
                        totalOrders = orders.size,
                        totalGmv = orders.sumOf { it.order.totalAmount },
                        totalPlatformCommission = logs.sumOf { it.amount },
                        dailyVolumes = daily,
                        pilotStats = pilotStats,
                        farmers = farmers,
                        commissionLogs = logs
                    )
                }
            } else {
                flowOf(AdminOverviewState())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AdminOverviewState())

    // --- Auth Actions ---

    fun selectAuthRole(role: UserRole) {
        if (role == UserRole.ADMIN) return
        _authState.update { it.copy(selectedRole = role, errorMessage = null) }
    }

    fun toggleAuthMode(isSignUp: Boolean) {
        otpCountdownJob?.cancel()
        otpCountdownJob = null
        _authState.update {
            it.copy(
                isSignUpMode = isSignUp,
                otpRequestedEmail = null,
                otpInfoMessage = null,
                otpResendCooldownSeconds = 0,
                otpExpiresInSeconds = 0,
                otpFailedAttempts = 0,
                errorMessage = null
            )
        }
    }

    fun registerFarmerWithEmail(
        name: String,
        email: String,
        phone: String,
        farmerId: String
    ) {
        viewModelScope.launch {
            _authState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = emailAuthRepository.registerFarmerWithEmail(name, email, phone, farmerId)) {
                is EmailAuthResult.Success -> {
                    _currentUser.value = result.user
                    _authState.update {
                        it.copy(
                            isLoading = false,
                            otpRequestedEmail = null,
                            otpInfoMessage = null,
                            errorMessage = null
                        )
                    }
                    _mapBookingState.value = MapBookingState(
                        hasLocationPermission = locationRepository.hasFineOrCoarsePermission(),
                        offlineRegions = _mapBookingState.value.offlineRegions
                    )
                    if (result.user.role == UserRole.FARMER.apiValue) {
                        startFarmerSupabaseSync(result.user.id)
                    }
                }
                is EmailAuthResult.Error -> {
                    _authState.update { it.copy(isLoading = false, errorMessage = result.message) }
                }
            }
        }
    }

    fun requestLoginOtp(email: String) {
        val cleanEmail = EmailOtpAuthRepository.normalizeEmail(email)
        val currentState = _authState.value
        if (currentState.otpRequestedEmail == cleanEmail && currentState.otpResendCooldownSeconds > 0) {
            _authState.update {
                it.copy(
                    errorMessage = "Please wait ${currentState.otpResendCooldownSeconds}s before requesting a new OTP."
                )
            }
            return
        }

        val now = System.currentTimeMillis()
        otpRequestHistoryMillis.removeAll { now - it > 15 * 60 * 1000L }
        if (otpRequestHistoryMillis.size >= 5) {
            _authState.update {
                it.copy(errorMessage = "Too many OTP requests. Please wait a few minutes and try again.")
            }
            return
        }

        viewModelScope.launch {
            _authState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = emailAuthRepository.requestLoginOtp(cleanEmail)) {
                is OtpRequestResult.Sent -> {
                    otpRequestHistoryMillis.add(System.currentTimeMillis())
                    _authState.update {
                        it.copy(
                            isLoading = false,
                            otpRequestedEmail = cleanEmail,
                            otpInfoMessage = result.message,
                            otpResendCooldownSeconds = result.cooldownSeconds,
                            otpExpiresInSeconds = result.expiresInSeconds,
                            otpFailedAttempts = 0,
                            errorMessage = null
                        )
                    }
                    startOtpCountdowns()
                }
                is OtpRequestResult.RateLimited -> {
                    _authState.update {
                        it.copy(
                            isLoading = false,
                            otpResendCooldownSeconds = result.cooldownSeconds,
                            errorMessage = result.message
                        )
                    }
                    startOtpCountdowns()
                }
                is OtpRequestResult.Error -> {
                    _authState.update { it.copy(isLoading = false, errorMessage = result.message) }
                }
            }
        }
    }

    fun verifyLoginOtp(otp: String) {
        val state = _authState.value
        val targetEmail = state.otpRequestedEmail
        if (targetEmail.isNullOrBlank()) {
            _authState.update { it.copy(errorMessage = "Request an OTP first.") }
            return
        }
        if (state.otpExpiresInSeconds <= 0) {
            _authState.update {
                it.copy(errorMessage = "This OTP has expired after 5 minutes. Please request a new OTP.")
            }
            return
        }
        if (state.otpFailedAttempts >= state.maxOtpAttempts) {
            _authState.update {
                it.copy(errorMessage = "Maximum verification attempts reached. Please request a new OTP.")
            }
            return
        }

        viewModelScope.launch {
            _authState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = emailAuthRepository.verifyLoginOtp(targetEmail, otp)) {
                is EmailAuthResult.Success -> {
                    otpCountdownJob?.cancel()
                    otpCountdownJob = null
                    _currentUser.value = result.user
                    _authState.update {
                        it.copy(
                            isLoading = false,
                            otpRequestedEmail = null,
                            otpInfoMessage = null,
                            otpResendCooldownSeconds = 0,
                            otpExpiresInSeconds = 0,
                            otpFailedAttempts = 0,
                            errorMessage = null
                        )
                    }
                    _mapBookingState.value = MapBookingState(
                        hasLocationPermission = locationRepository.hasFineOrCoarsePermission(),
                        offlineRegions = _mapBookingState.value.offlineRegions
                    )
                    if (result.user.role == UserRole.FARMER.apiValue) {
                        startFarmerSupabaseSync(result.user.id)
                    }
                }
                is EmailAuthResult.Error -> {
                    val nextFailed = _authState.value.otpFailedAttempts + 1
                    val lockedMsg = if (nextFailed >= _authState.value.maxOtpAttempts) {
                        "Maximum verification attempts reached. Please request a new OTP."
                    } else {
                        result.message
                    }
                    _authState.update {
                        it.copy(
                            isLoading = false,
                            otpFailedAttempts = nextFailed,
                            errorMessage = lockedMsg
                        )
                    }
                }
            }
        }
    }

    fun editLoginEmail() {
        otpCountdownJob?.cancel()
        otpCountdownJob = null
        _authState.update {
            it.copy(
                otpRequestedEmail = null,
                otpInfoMessage = null,
                otpResendCooldownSeconds = 0,
                otpExpiresInSeconds = 0,
                otpFailedAttempts = 0,
                errorMessage = null
            )
        }
    }

    private fun startOtpCountdowns() {
        otpCountdownJob?.cancel()
        otpCountdownJob = viewModelScope.launch {
            while (true) {
                val cur = _authState.value
                if (cur.otpResendCooldownSeconds <= 0 && cur.otpExpiresInSeconds <= 0) {
                    break
                }
                kotlinx.coroutines.delay(1000L)
                _authState.update { st ->
                    st.copy(
                        otpResendCooldownSeconds = (st.otpResendCooldownSeconds - 1).coerceAtLeast(0),
                        otpExpiresInSeconds = (st.otpExpiresInSeconds - 1).coerceAtLeast(0)
                    )
                }
            }
        }
    }

    fun submitPilotAuth(
        name: String,
        phone: String,
        droneType: DroneType,
        tankCapacityText: String,
        licenseUri: String,
        bankUpiId: String
    ) {
        val isSignUp = _authState.value.isSignUpMode
        if (phone.trim().length < 10) {
            _authState.update { it.copy(errorMessage = "Enter a valid 10-digit contact number.") }
            return
        }
        if (isSignUp) {
            val tank = tankCapacityText.toDoubleOrNull()
            if (name.isBlank() || tank == null || tank <= 0.0 || bankUpiId.isBlank()) {
                _authState.update {
                    it.copy(errorMessage = "Complete name, tank capacity, and UPI/Bank details.")
                }
                return
            }
            if (licenseUri.isBlank()) {
                _authState.update {
                    it.copy(errorMessage = "Please upload your Drone Pilot License for verification.")
                }
                return
            }
        }

        viewModelScope.launch {
            _authState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = if (isSignUp) {
                repository.registerPilot(
                    name = name,
                    phone = phone,
                    droneType = droneType,
                    tankCapacity = tankCapacityText.toDouble(),
                    licenseDocumentUri = licenseUri,
                    bankUpiId = bankUpiId
                )
            } else {
                repository.loginByRole(phone, UserRole.PILOT)
            }
            when (result) {
                is AuthResult.Success -> {
                    _currentUser.value = result.user
                    _authState.update { it.copy(isLoading = false) }
                    syncPilotLiveLocationTracking(result.user)
                    startMarketSupabaseSync()
                }
                is AuthResult.Error -> {
                    _authState.update { it.copy(isLoading = false, errorMessage = result.message) }
                }
            }
        }
    }

    fun submitAdminLogin(phone: String, passcode: String, onFailed: (String) -> Unit) {
        viewModelScope.launch {
            when (val result = repository.loginAdmin(phone, passcode)) {
                is AuthResult.Success -> {
                    _currentUser.value = result.user
                    startMarketSupabaseSync()
                }
                is AuthResult.Error -> onFailed(result.message)
            }
        }
    }

    fun updateFarmerCredentials(
        name: String,
        phone: String,
        farmerIdNumber: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val user = _currentUser.value ?: return
        if (name.isBlank()) {
            onError("Enter your full name.")
            return
        }
        if (phone.trim().length < 10) {
            onError("Enter a valid 10-digit mobile number.")
            return
        }
        if (farmerIdNumber.isBlank()) {
            onError("Enter your Farmer ID number.")
            return
        }
        viewModelScope.launch {
            when (val result = repository.updateFarmerCredentials(user.id, name, phone, farmerIdNumber)) {
                is AuthResult.Success -> {
                    _currentUser.value = result.user
                    _snackbarMessage.value = "Farmer credentials updated successfully."
                    onSuccess()
                }
                is AuthResult.Error -> onError(result.message)
            }
        }
    }

    fun signOut() {
        stopBatteryEfficientGpsUpdates()
        pilotLiveLocationJob?.cancel()
        pilotLiveLocationJob = null
        farmerSupabaseSyncJob?.cancel()
        farmerSupabaseSyncJob = null
        marketSupabaseSyncJob?.cancel()
        marketSupabaseSyncJob = null
        otpCountdownJob?.cancel()
        otpCountdownJob = null
        val previousToken = sessionStore.loadSession()?.accessToken
        sessionStore.clearSession()
        viewModelScope.launch {
            emailAuthRepository.revokeAccessToken(previousToken)
        }
        _currentUser.value = null
        _authState.value = AuthUiState(
            selectedRole = UserRole.FARMER,
            isSignUpMode = false,
            isRestoringSession = false
        )
        _mapBookingState.value = MapBookingState(
            hasLocationPermission = locationRepository.hasFineOrCoarsePermission()
        )
        _agriStoreCart.value = emptyList()
        _agriStoreReceipts.value = emptyList()
        _razorpaySheet.value = null
        _insufficientWalletAlert.value = null
    }

    // --- GPS & Location Permission Actions ---

    fun onLocationPermissionResult(granted: Boolean) {
        _mapBookingState.update {
            it.copy(
                hasLocationPermission = granted,
                locationPermissionDenied = !granted
            )
        }
        if (granted) {
            val user = _currentUser.value
            if (user?.role == UserRole.PILOT.apiValue) {
                syncPilotLiveLocationTracking(user)
            } else {
                startBatteryEfficientGpsUpdates(drawingActive = true)
                refreshCurrentLocation()
            }
        }
    }

    private fun syncPilotLiveLocationTracking(pilotUser: UserEntity?) {
        pilotLiveLocationJob?.cancel()
        if (pilotUser == null || pilotUser.role != UserRole.PILOT.apiValue) return
        if (!locationRepository.hasFineOrCoarsePermission()) return

        pilotLiveLocationJob = viewModelScope.launch {
            // Seed baseline GPS coordinate immediately
            locationRepository.getCurrentLocationOnce()?.let { snap ->
                _mapBookingState.update { it.copy(currentGpsLocation = snap) }
                repository.updatePilotLiveLocation(pilotUser.id, snap.lat, snap.lng)
            }

            // Stream continuous GPS updates while Pilot is Online or En Route on an Accepted job
            locationRepository.observeLocationUpdates(highPrecisionForDrawing = true)
                .collect { snap ->
                    _mapBookingState.update { it.copy(currentGpsLocation = snap) }
                    val prof = pilotProfile.value
                    val hasEnRouteMission = pilotAssignedOrders.value.any {
                        it.statusEnum == OrderStatus.ACCEPTED
                    }
                    if (prof?.isOnline == true || hasEnRouteMission) {
                        repository.updatePilotLiveLocation(pilotUser.id, snap.lat, snap.lng)
                        mapRepository.publishOperatorLocation(
                            operatorId = pilotUser.id,
                            name = pilotUser.name,
                            roleType = if (prof?.droneType == DroneType.HEXACOPTER.apiValue) {
                                OperatorRoleType.DRONE_OPERATOR
                            } else {
                                OperatorRoleType.DRONE_MITRA
                            },
                            lat = snap.lat,
                            lng = snap.lng,
                            droneType = prof?.droneType ?: DroneType.QUADCOPTER.apiValue,
                            isAvailable = prof?.isOnline ?: true,
                            headingDegrees = snap.bearing
                        )
                    }
                }
        }
    }

    fun startBatteryEfficientGpsUpdates(drawingActive: Boolean) {
        if (!locationRepository.hasFineOrCoarsePermission()) return
        gpsObserverJob?.cancel()
        gpsObserverJob = viewModelScope.launch {
            locationRepository.observeLocationUpdates(highPrecisionForDrawing = drawingActive)
                .collect { snapshot ->
                    _mapBookingState.update { it.copy(currentGpsLocation = snapshot) }
                }
        }
    }

    fun stopBatteryEfficientGpsUpdates() {
        gpsObserverJob?.cancel()
        gpsObserverJob = null
    }

    fun refreshCurrentLocation() {
        if (!locationRepository.hasFineOrCoarsePermission()) {
            _mapBookingState.update { it.copy(locationPermissionDenied = true) }
            return
        }
        viewModelScope.launch {
            val loc = locationRepository.getCurrentLocationOnce()
            if (loc != null) {
                _mapBookingState.update {
                    it.copy(
                        currentGpsLocation = loc,
                        mapCenter = LatLngPoint(loc.lat, loc.lng),
                        cameraFlyNonce = it.cameraFlyNonce + 1
                    )
                }
            } else {
                _snackbarMessage.value = "Waiting for GPS lock. Ensure device Location is enabled."
            }
        }
    }

    // --- MapLibre Styles, Search, Boundary Drawing & Offline Caching ---

    fun selectMapStyle(styleType: MapStyleType) {
        _mapBookingState.update {
            it.copy(
                mapStyleType = styleType,
                isMapLoading = true,
                mapErrorMessage = null
            )
        }
    }

    fun onMapLoadingChanged(loading: Boolean) {
        _mapBookingState.update { it.copy(isMapLoading = loading) }
    }

    fun onMapError(error: String?) {
        _mapBookingState.update { it.copy(mapErrorMessage = error) }
    }

    fun retryMapLoad() {
        _mapBookingState.update {
            it.copy(
                styleRetryNonce = it.styleRetryNonce + 1,
                isMapLoading = true,
                mapErrorMessage = null
            )
        }
    }

    fun updateSearchQuery(query: String) {
        _mapBookingState.update { it.copy(searchQuery = query) }
    }

    fun searchAndCenterMap(context: Context) {
        val query = _mapBookingState.value.searchQuery.trim()
        if (query.isEmpty()) return
        viewModelScope.launch {
            _mapBookingState.update { it.copy(isSearchingLocation = true) }
            val matches = locationRepository.searchLocations(query)
            val best = matches.firstOrNull()
            if (best != null) {
                _mapBookingState.update {
                    it.copy(
                        mapCenter = best.point,
                        resolvedLocationLabel = best.displayName,
                        locationSuggestions = matches,
                        cameraFlyNonce = it.cameraFlyNonce + 1,
                        isSearchingLocation = false
                    )
                }
            } else {
                _mapBookingState.update { it.copy(isSearchingLocation = false) }
            }
        }
    }

    fun selectLocationSuggestion(match: LocationMatch) {
        _mapBookingState.update {
            it.copy(
                searchQuery = match.displayName,
                resolvedLocationLabel = match.displayName,
                mapCenter = match.point,
                locationSuggestions = emptyList(),
                cameraFlyNonce = it.cameraFlyNonce + 1
            )
        }
    }

    fun onMapCameraMoved(center: LatLngPoint) {
        _mapBookingState.update { it.copy(mapCenter = center) }
    }

    fun addMapBoundaryPoint(point: LatLngPoint) {
        val updated = _mapBookingState.value.polygonPoints + point
        recalculatePolygon(updated)
    }

    fun updateMapBoundaryPoint(index: Int, point: LatLngPoint) {
        val current = _mapBookingState.value.polygonPoints
        if (index !in current.indices) return
        val updated = current.toMutableList().apply { set(index, point) }
        recalculatePolygon(updated)
    }

    fun undoLastPolygonPoint() {
        val updated = _mapBookingState.value.polygonPoints.dropLast(1)
        recalculatePolygon(updated)
    }

    fun clearPolygon() {
        recalculatePolygon(emptyList())
        _mapBookingState.update { it.copy(farmNameInput = "") }
    }

    fun zoomMap(zoomIn: Boolean) {
        _mapBookingState.update { state ->
            if (zoomIn) state.copy(zoomInNonce = state.zoomInNonce + 1)
            else state.copy(zoomOutNonce = state.zoomOutNonce + 1)
        }
    }

    fun resetCompass() {
        _mapBookingState.update { it.copy(resetCompassNonce = it.resetCompassNonce + 1) }
    }

    private fun recalculatePolygon(points: List<LatLngPoint>) {
        val acres = GeoPolygonCalculator.calculatePolygonAreaAcres(points)
        val hectares = GeoPolygonCalculator.calculatePolygonAreaHectares(points)
        val tariff = GeoPolygonCalculator.calculateTariff(acres)
        _mapBookingState.update {
            it.copy(
                polygonPoints = points,
                computedAcres = tariff.acres,
                computedHectares = hectares,
                computedAmount = tariff.totalAmount
            )
        }
    }

    fun updateFarmName(name: String) {
        _mapBookingState.update { it.copy(farmNameInput = name) }
    }

    fun saveFarmBoundaryToSupabase() {
        val user = _currentUser.value ?: return
        val state = _mapBookingState.value
        if (state.polygonPoints.size < 3 || state.computedAcres <= 0.0) {
            _snackbarMessage.value = "Draw at least 3 points on the map before saving a farm."
            return
        }
        viewModelScope.launch {
            _mapBookingState.update { it.copy(isSavingFarm = true) }
            val name = state.farmNameInput.ifBlank {
                "${state.resolvedLocationLabel.substringBefore(",")} Plot"
            }
            val saved = farmRepository.saveFarmBoundary(
                farmerId = user.id,
                farmName = name,
                polygonPoints = state.polygonPoints
            )
            _mapBookingState.update { it.copy(isSavingFarm = false, farmNameInput = "") }
            _snackbarMessage.value = if (saved.isSyncedToSupabase) {
                "Saved '${saved.farmName}' (${saved.areaAcres} ac / ${saved.areaHectares} ha) to My Farms."
            } else {
                "Saved '${saved.farmName}' locally (${saved.areaAcres} ac / ${saved.areaHectares} ha). Will sync automatically when online."
            }
        }
    }

    fun focusSavedFarm(farm: FarmBoundaryEntity) {
        val points = GeoPolygonCalculator.decodePolygon(farm.polygonCoordinatesJson)
        val center = LatLngPoint(farm.centerLat, farm.centerLng)
        recalculatePolygon(points)
        _mapBookingState.update {
            it.copy(
                mapCenter = center,
                farmNameInput = farm.farmName,
                cameraFlyNonce = it.cameraFlyNonce + 1
            )
        }
    }

    fun cacheCurrentRegionOffline() {
        val center = _mapBookingState.value.mapCenter
        val label = _mapBookingState.value.resolvedLocationLabel.ifBlank { "Farm Area" }
        _mapBookingState.update { it.copy(offlineDownloadProgress = 0) }
        mapRepository.cacheRegionOffline(
            regionName = label,
            center = center,
            onProgress = { pct ->
                _mapBookingState.update { it.copy(offlineDownloadProgress = pct) }
            },
            onComplete = { region ->
                _mapBookingState.update { it.copy(offlineDownloadProgress = null) }
                refreshOfflineRegions()
                _snackbarMessage.value = "Offline map cached for ${region.name}."
            },
            onError = { err ->
                _mapBookingState.update { it.copy(offlineDownloadProgress = null) }
                _snackbarMessage.value = "Offline cache failed: $err"
            }
        )
    }

    private fun refreshOfflineRegions() {
        viewModelScope.launch {
            val regions = runCatching { mapRepository.listOfflineRegions() }.getOrDefault(emptyList())
            _mapBookingState.update { it.copy(offlineRegions = regions) }
        }
    }

    fun toggleLanguage(language: AppLanguage) {
        _appLanguage.value = language
    }

    fun selectSprayCrop(crop: String) {
        _mapBookingState.update { it.copy(selectedCrop = crop) }
    }

    fun selectSprayFormula(formulaId: String) {
        _mapBookingState.update { it.copy(selectedFormulaId = formulaId) }
    }

    fun rebookOrderPolygon(order: OrderWithDetails) {
        val points = order.polygonPoints
        if (points.size < 3) return
        val centerLat = if (order.order.fieldCenterLat != 0.0) {
            order.order.fieldCenterLat
        } else {
            points.map { it.lat }.average()
        }
        val centerLng = if (order.order.fieldCenterLng != 0.0) {
            order.order.fieldCenterLng
        } else {
            points.map { it.lng }.average()
        }
        val cleanLocation = order.order.locationAddress
            .substringBefore(" • ")
            .ifBlank { "Saved Plot" }
        recalculatePolygon(points)
        _mapBookingState.update {
            it.copy(
                searchQuery = cleanLocation,
                resolvedLocationLabel = cleanLocation,
                mapCenter = LatLngPoint(centerLat, centerLng),
                farmNameInput = cleanLocation,
                cameraFlyNonce = it.cameraFlyNonce + 1
            )
        }
        _snackbarMessage.value = "Plot loaded onto Book Spray map (${order.order.acresToSpray} acres)."
    }

    fun addToAgriCart(product: AgriStoreProduct) {
        _agriStoreCart.update { current ->
            val existing = current.firstOrNull { it.product.id == product.id }
            if (existing != null) {
                current.map {
                    if (it.product.id == product.id) it.copy(quantity = it.quantity + 1) else it
                }
            } else {
                current + AgriStoreCartItem(product = product, quantity = 1)
            }
        }
    }

    fun updateAgriCartQty(productId: String, delta: Int) {
        _agriStoreCart.update { current ->
            current.mapNotNull { item ->
                if (item.product.id != productId) {
                    item
                } else {
                    val next = item.quantity + delta
                    if (next <= 0) null else item.copy(quantity = next)
                }
            }
        }
    }

    fun clearAgriCart() {
        _agriStoreCart.value = emptyList()
    }

    fun initiateAgriStoreCheckout(paymentMethod: PaymentMethod) {
        val cart = _agriStoreCart.value
        if (cart.isEmpty()) {
            _snackbarMessage.value = "Add bio-inputs to your cart before checkout."
            return
        }
        val totalItems = cart.sumOf { it.quantity }
        val totalAmount = cart.sumOf { it.lineTotal }
        val summary = cart.joinToString(", ") { "${it.quantity}× ${it.product.title}" }
        if (paymentMethod == PaymentMethod.ONLINE) {
            _razorpaySheet.value = RazorpaySheetState(
                title = "Krishivantri Agri Store",
                subtitle = "$totalItems items • $summary",
                amount = totalAmount,
                purpose = RazorpayPurpose.AgriStoreCheckout(
                    itemCount = totalItems,
                    summaryLabel = summary
                )
            )
        } else {
            val receipt = AgriStoreOrderReceipt(
                orderId = "KV-STR-${(System.currentTimeMillis() % 100000).toString().padStart(5, '0')}",
                itemCount = totalItems,
                totalAmount = totalAmount,
                paymentMethod = PaymentMethod.CASH
            )
            _agriStoreReceipts.update { listOf(receipt) + it }
            _agriStoreCart.value = emptyList()
            _snackbarMessage.value = "Agri Store order ${receipt.orderId} placed (Cash on Delivery)."
        }
    }

    fun selectPaymentMethod(method: PaymentMethod) {
        _mapBookingState.update { it.copy(selectedPaymentMethod = method) }
    }

    fun initiateFarmerOrderPlacement(
        paymentMethod: PaymentMethod = _mapBookingState.value.selectedPaymentMethod
    ) {
        val farmer = _currentUser.value ?: return
        val booking = _mapBookingState.value
        if (booking.polygonPoints.size < 3 || booking.computedAcres <= 0.0) {
            _snackbarMessage.value = "Tap at least 3 boundary points on the map to outline your farmland."
            return
        }
        _mapBookingState.update { it.copy(selectedPaymentMethod = paymentMethod) }
        val baseLocation = booking.searchQuery.ifBlank { booking.resolvedLocationLabel }
            .substringBefore(" • ")
            .ifBlank { "Farmland Plot" }
        val formulaLabel = FarmerCatalogDefaults.sprayObjectives
            .firstOrNull { it.id == booking.selectedFormulaId }?.title ?: "Organic Pest Control"
        val locationLabel = "$baseLocation • ${booking.selectedCrop} ($formulaLabel)"

        if (paymentMethod == PaymentMethod.ONLINE) {
            _razorpaySheet.value = RazorpaySheetState(
                title = "Krishivantri Spray Booking",
                subtitle = "${booking.computedAcres} acres • $baseLocation • ${booking.selectedCrop}",
                amount = booking.computedAmount,
                purpose = RazorpayPurpose.FarmerOrderCheckout(
                    locationAddress = locationLabel,
                    polygonPoints = booking.polygonPoints,
                    farmName = booking.farmNameInput
                )
            )
        } else {
            viewModelScope.launch {
                val order = repository.placeOrder(
                    farmerId = farmer.id,
                    locationAddress = locationLabel,
                    polygonPoints = booking.polygonPoints,
                    paymentMethod = PaymentMethod.CASH
                )
                autoSaveBookedBoundaryIfNew(
                    farmerId = farmer.id,
                    locationLabel = locationLabel,
                    preferredName = booking.farmNameInput,
                    polygonPoints = booking.polygonPoints
                )
                _mapBookingState.update {
                    it.copy(
                        polygonPoints = emptyList(),
                        computedAcres = 0.0,
                        computedHectares = 0.0,
                        computedAmount = 0.0,
                        farmNameInput = "",
                        bookingSuccessOrderId = order.id
                    )
                }
                _snackbarMessage.value = "Cash spray order confirmed for ${order.acresToSpray} acres."
            }
        }
    }

    fun confirmRazorpayPayment(razorpayPaymentId: String? = null) {
        val sheet = _razorpaySheet.value ?: return
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            when (val purpose = sheet.purpose) {
                is RazorpayPurpose.FarmerOrderCheckout -> {
                    val order = repository.placeOrder(
                        farmerId = user.id,
                        locationAddress = purpose.locationAddress,
                        polygonPoints = purpose.polygonPoints,
                        paymentMethod = PaymentMethod.ONLINE,
                        razorpayPaymentId = razorpayPaymentId
                    )
                    autoSaveBookedBoundaryIfNew(
                        farmerId = user.id,
                        locationLabel = purpose.locationAddress,
                        preferredName = purpose.farmName,
                        polygonPoints = purpose.polygonPoints
                    )
                    _mapBookingState.update {
                        it.copy(
                            polygonPoints = emptyList(),
                            computedAcres = 0.0,
                            computedHectares = 0.0,
                            computedAmount = 0.0,
                            farmNameInput = "",
                            bookingSuccessOrderId = order.id
                        )
                    }
                    val refSuffix = razorpayPaymentId?.let { " ($it)" }.orEmpty()
                    _snackbarMessage.value = "Razorpay payment verified$refSuffix. Order placed for ${order.acresToSpray} acres."
                }
                is RazorpayPurpose.PilotWalletTopUp -> {
                    repository.topUpPilotWallet(purpose.pilotId, sheet.amount)
                    _snackbarMessage.value = "₹${sheet.amount.toInt()} added to your pilot wallet."
                }
                is RazorpayPurpose.AgriStoreCheckout -> {
                    val receipt = AgriStoreOrderReceipt(
                        orderId = "KV-STR-${(System.currentTimeMillis() % 100000).toString().padStart(5, '0')}",
                        itemCount = purpose.itemCount,
                        totalAmount = sheet.amount,
                        paymentMethod = PaymentMethod.ONLINE,
                        razorpayPaymentId = razorpayPaymentId
                    )
                    _agriStoreReceipts.update { listOf(receipt) + it }
                    _agriStoreCart.value = emptyList()
                    val refSuffix = razorpayPaymentId?.let { " ($it)" }.orEmpty()
                    _snackbarMessage.value = "Agri Store payment verified$refSuffix. Order ${receipt.orderId} confirmed."
                }
            }
            _razorpaySheet.value = null
        }
    }

    private suspend fun autoSaveBookedBoundaryIfNew(
        farmerId: String,
        locationLabel: String,
        preferredName: String,
        polygonPoints: List<LatLngPoint>
    ) {
        if (polygonPoints.size < 3) return
        val alreadySaved = savedFarms.value.any { existing ->
            val existingPoints = GeoPolygonCalculator.decodePolygon(existing.polygonCoordinatesJson)
            existingPoints.size == polygonPoints.size &&
                existingPoints.zip(polygonPoints).all { (a, b) ->
                    kotlin.math.abs(a.lat - b.lat) < 0.00005 &&
                        kotlin.math.abs(a.lng - b.lng) < 0.00005
                }
        }
        if (!alreadySaved) {
            val acres = GeoPolygonCalculator.calculatePolygonAreaAcres(polygonPoints)
            val fallbackLabel = locationLabel.substringBefore(",").trim().ifBlank { "My Farm" }
            val resolvedName = preferredName.trim().ifBlank { "$fallbackLabel ($acres ac)" }
            runCatching {
                farmRepository.saveFarmBoundary(
                    farmerId = farmerId,
                    farmName = resolvedName,
                    polygonPoints = polygonPoints
                )
            }
        }
    }

    fun onRazorpayPaymentError(code: Int, description: String?) {
        _razorpaySheet.value = null
        val detail = description?.takeIf { it.isNotBlank() } ?: "Payment cancelled or declined"
        _snackbarMessage.value = "Razorpay checkout incomplete ($code): $detail"
    }

    fun onRazorpayKeyMissing() {
        _razorpaySheet.value = null
        _snackbarMessage.value = "Online payment is temporarily unavailable. Please try again shortly or choose Cash on Spraying."
    }

    private fun startFarmerSupabaseSync(farmerId: String) {
        farmerSupabaseSyncJob?.cancel()
        farmerSupabaseSyncJob = viewModelScope.launch {
            while (true) {
                runCatching {
                    farmRepository.syncWithSupabase(farmerId)
                    repository.syncFarmerStateFromSupabase(farmerId)
                }
                kotlinx.coroutines.delay(15_000L)
            }
        }
    }

    private fun startMarketSupabaseSync() {
        marketSupabaseSyncJob?.cancel()
        marketSupabaseSyncJob = viewModelScope.launch {
            while (true) {
                runCatching {
                    repository.syncPilotAndMarketStateFromSupabase()
                }
                kotlinx.coroutines.delay(15_000L)
            }
        }
    }

    fun dismissRazorpaySheet() {
        _razorpaySheet.value = null
    }

    fun consumeBookingSuccess() {
        _mapBookingState.update { it.copy(bookingSuccessOrderId = null) }
    }

    fun ratePilot(orderId: String, stars: Int) {
        viewModelScope.launch {
            repository.ratePilotForOrder(orderId, stars)
            _snackbarMessage.value = "Thank you for rating your pilot ($stars★)!"
        }
    }

    fun confirmFarmerJobDone(orderId: String) {
        viewModelScope.launch {
            when (val result = repository.confirmJobDoneByFarmer(orderId)) {
                is JobDoneResult.Success -> {
                    val recipient = result.pilotName ?: "pilot"
                    _snackbarMessage.value = if (result.amountTransferredToPilot > 0.0) {
                        "Job done confirmed! ₹${result.amountTransferredToPilot.toInt()} transferred to $recipient's wallet (+1 Harvest Coin)."
                    } else {
                        "Job done confirmed! Spraying settled (+1 Harvest Coin)."
                    }
                }
                is JobDoneResult.Error -> {
                    _snackbarMessage.value = result.message
                }
            }
        }
    }

    // --- Pilot Actions ---

    fun togglePilotOnline(isOnline: Boolean) {
        val user = _currentUser.value ?: return
        val profile = pilotProfile.value
        viewModelScope.launch {
            repository.togglePilotOnline(user.id, isOnline)
            val loc = locationRepository.getCurrentLocationOnce() ?: _mapBookingState.value.currentGpsLocation
            if (loc != null) {
                repository.updatePilotLiveLocation(user.id, loc.lat, loc.lng)
            }
            val lat = loc?.lat ?: profile?.currentLat ?: _mapBookingState.value.mapCenter.lat
            val lng = loc?.lng ?: profile?.currentLng ?: _mapBookingState.value.mapCenter.lng
            mapRepository.publishOperatorLocation(
                operatorId = user.id,
                name = user.name,
                roleType = if (profile?.droneType == "hexacopter") {
                    OperatorRoleType.DRONE_OPERATOR
                } else {
                    OperatorRoleType.DRONE_MITRA
                },
                lat = lat,
                lng = lng,
                droneType = profile?.droneType ?: "quadcopter",
                isAvailable = isOnline,
                headingDegrees = loc?.bearing ?: 0f
            )
            if (isOnline) {
                syncPilotLiveLocationTracking(user)
            }
        }
    }

    fun acceptOrder(orderId: String) {
        val user = _currentUser.value ?: return
        val profile = pilotProfile.value
        viewModelScope.launch {
            val loc = locationRepository.getCurrentLocationOnce() ?: _mapBookingState.value.currentGpsLocation
            val pilotPoint = loc?.let { LatLngPoint(it.lat, it.lng) }
            when (val result = repository.acceptOrder(user.id, orderId, pilotPoint)) {
                is AcceptOrderResult.Success -> {
                    if (pilotPoint != null) {
                        mapRepository.publishOperatorLocation(
                            operatorId = user.id,
                            name = user.name,
                            roleType = if (profile?.droneType == DroneType.HEXACOPTER.apiValue) {
                                OperatorRoleType.DRONE_OPERATOR
                            } else {
                                OperatorRoleType.DRONE_MITRA
                            },
                            lat = pilotPoint.lat,
                            lng = pilotPoint.lng,
                            droneType = profile?.droneType ?: DroneType.QUADCOPTER.apiValue,
                            isAvailable = profile?.isOnline ?: true,
                            headingDegrees = loc.bearing
                        )
                    }
                    _snackbarMessage.value = if (pilotPoint != null) {
                        "Mission accepted! Live distance & ETA shared with farmer."
                    } else {
                        "Mission accepted! Head to the farmer's field."
                    }
                }
                is AcceptOrderResult.InsufficientWallet -> {
                    _insufficientWalletAlert.value = InsufficientWalletAlert(
                        requiredCommission = result.requiredCommission,
                        currentBalance = result.currentBalance
                    )
                }
                is AcceptOrderResult.Error -> {
                    _snackbarMessage.value = result.message
                }
            }
        }
    }

    fun rejectOrder(orderId: String) {
        val pilotId = _currentUser.value?.id ?: return
        viewModelScope.launch {
            repository.rejectOrder(pilotId, orderId)
            _snackbarMessage.value = "Order declined and moved out of your queue."
        }
    }

    fun startSpraying(orderId: String) {
        viewModelScope.launch {
            repository.startSpraying(orderId)
            _snackbarMessage.value = "Spraying mission started."
        }
    }

    fun completeOrder(orderId: String) {
        viewModelScope.launch {
            repository.markPilotSprayingCompleted(orderId)
            _snackbarMessage.value = "Spraying marked finished. Waiting for Farmer 'Job Done' confirmation to credit wallet."
        }
    }

    fun openWalletTopUp(amount: Double = 500.0) {
        val pilotId = _currentUser.value?.id ?: return
        _insufficientWalletAlert.value = null
        _razorpaySheet.value = RazorpaySheetState(
            title = "Pilot Wallet Top-Up",
            subtitle = "Advance reserve for Cash on Field 10% commission",
            amount = amount,
            purpose = RazorpayPurpose.PilotWalletTopUp(pilotId)
        )
    }

    fun dismissInsufficientWalletAlert() {
        _insufficientWalletAlert.value = null
    }

    // --- Admin Actions ---

    fun verifyPilotLicense(pilotId: String, verified: Boolean) {
        viewModelScope.launch {
            repository.setPilotLicenseVerified(pilotId, verified)
            _snackbarMessage.value = if (verified) {
                "Pilot license verified for Priority Dispatch."
            } else {
                "Pilot license verification revoked."
            }
        }
    }

    fun consumeSnackbar() {
        _snackbarMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        stopBatteryEfficientGpsUpdates()
        pilotLiveLocationJob?.cancel()
        farmerSupabaseSyncJob?.cancel()
        marketSupabaseSyncJob?.cancel()
    }
}
