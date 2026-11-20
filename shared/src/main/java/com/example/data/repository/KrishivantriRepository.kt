package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.KrishivantriDatabase
import com.example.data.model.CommissionLogEntity
import com.example.data.model.DailyOrderVolume
import com.example.data.model.DroneType
import com.example.data.model.FarmerProfileEntity
import com.example.data.model.FarmerWithUser
import com.example.data.model.LatLngPoint
import com.example.data.model.OrderEntity
import com.example.data.model.OrderStatus
import com.example.data.model.OrderWithDetails
import com.example.data.model.PaymentMethod
import com.example.data.model.PaymentStatus
import com.example.data.model.PilotPerformanceStat
import com.example.data.model.PilotProfileEntity
import com.example.data.model.PilotWithUser
import com.example.data.model.RejectedOrderEntity
import com.example.data.model.UserEntity
import com.example.data.model.UserRole
import com.example.data.remote.SupabaseClient
import com.example.data.remote.SupabaseConnectionStatus
import com.example.data.remote.SupabaseCoordDto
import com.example.data.remote.SupabaseFarmerCoinsPatchDto
import com.example.data.remote.SupabaseFarmerIdPatchDto
import com.example.data.remote.SupabaseFarmerProfileDto
import com.example.data.remote.SupabaseOrderAcceptedPatchDto
import com.example.data.remote.SupabaseOrderDistancePatchDto
import com.example.data.remote.SupabaseOrderDto
import com.example.data.remote.SupabaseOrderRatingPatchDto
import com.example.data.remote.SupabaseOrderStatusPatchDto
import com.example.data.remote.SupabasePilotAccountPatchDto
import com.example.data.remote.SupabasePilotLicensePatchDto
import com.example.data.remote.SupabasePilotLocationPatchDto
import com.example.data.remote.SupabasePilotOnlinePatchDto
import com.example.data.remote.SupabasePilotProfileDto
import com.example.data.remote.SupabasePilotRatingPatchDto
import com.example.data.remote.SupabasePilotWalletPatchDto
import com.example.data.remote.SupabaseUserDto
import com.example.data.remote.SupabaseUserNamePatchDto
import com.example.data.remote.SupabaseUserPatchDto
import com.example.domain.GeoPolygonCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToInt

sealed class AuthResult {
    data class Success(val user: UserEntity) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

sealed class AcceptOrderResult {
    data object Success : AcceptOrderResult()
    data class InsufficientWallet(
        val requiredCommission: Double,
        val currentBalance: Double
    ) : AcceptOrderResult()
    data class Error(val message: String) : AcceptOrderResult()
}

sealed class CancelOrderResult {
    data object Success : CancelOrderResult()
    data class NotAllowed(val message: String) : CancelOrderResult()
}

sealed class JobDoneResult {
    data class Success(
        val amountTransferredToPilot: Double,
        val pilotName: String?
    ) : JobDoneResult()
    data class Error(val message: String) : JobDoneResult()
}

class KrishivantriRepository(private val database: KrishivantriDatabase) {

    private val dao = database.dao()

    companion object {
        const val ADMIN_PHONE = "9999000000"
        const val ADMIN_PASSCODE = "KRISHI-ADMIN-2026"
    }

    suspend fun checkCloudDatabaseConnection(): SupabaseConnectionStatus =
        SupabaseClient.checkConnection()

    suspend fun ensureAdminSeeded() {
        val existing = dao.getUserByPhone(ADMIN_PHONE)
        if (existing == null) {
            dao.insertUser(
                UserEntity(
                    id = "admin-root-001",
                    phone = ADMIN_PHONE,
                    name = "Krishivantri Operations Admin",
                    role = UserRole.ADMIN.apiValue
                )
            )
        }
    }

    suspend fun registerFarmer(
        name: String,
        phone: String,
        farmerIdNumber: String
    ): AuthResult {
        val cleanPhone = phone.trim()
        val existing = dao.getUserByPhone(cleanPhone)
        if (existing != null) {
            return AuthResult.Error("Mobile number already registered. Please sign in.")
        }
        val remoteExisting = withContext(Dispatchers.IO) {
            runCatching {
                SupabaseClient.api?.getUsersByPhone("eq.$cleanPhone")?.body()?.firstOrNull()
            }.getOrNull()
        }
        if (remoteExisting != null) {
            return AuthResult.Error("Mobile number already registered. Please sign in.")
        }
        val user = UserEntity(
            id = UUID.randomUUID().toString(),
            phone = cleanPhone,
            name = name.trim(),
            role = UserRole.FARMER.apiValue
        )
        val profile = FarmerProfileEntity(
            userId = user.id,
            farmerIdNumber = farmerIdNumber.trim().uppercase(Locale.getDefault()),
            coins = 0
        )
        database.withTransaction {
            dao.insertUser(user)
            dao.insertFarmerProfile(profile)
        }
        withContext(Dispatchers.IO) {
            runCatching {
                SupabaseClient.api?.let { api ->
                    api.upsertUser(
                        SupabaseUserDto(
                            id = user.id,
                            phone = user.phone,
                            name = user.name,
                            role = user.role,
                            created_at = user.createdAt
                        )
                    )
                    api.upsertFarmerProfile(
                        SupabaseFarmerProfileDto(
                            user_id = profile.userId,
                            farmer_id_number = profile.farmerIdNumber,
                            coins = profile.coins
                        )
                    )
                }
            }
        }
        return AuthResult.Success(user)
    }

    suspend fun registerPilot(
        name: String,
        phone: String,
        droneType: DroneType,
        tankCapacity: Double,
        licenseDocumentUri: String,
        bankUpiId: String
    ): AuthResult {
        val cleanPhone = phone.trim()
        val existing = dao.getUserByPhone(cleanPhone)
        if (existing != null) {
            return AuthResult.Error("Contact number already registered. Please sign in.")
        }
        val user = UserEntity(
            id = UUID.randomUUID().toString(),
            phone = cleanPhone,
            name = name.trim(),
            role = UserRole.PILOT.apiValue
        )
        val profile = PilotProfileEntity(
            userId = user.id,
            droneType = droneType.apiValue,
            tankCapacity = tankCapacity,
            licenseDocumentUri = licenseDocumentUri,
            isLicenseVerified = false,
            bankUpiId = bankUpiId.trim(),
            walletBalance = 0.0,
            isOnline = false,
            ratingAverage = 5.0,
            ratingCount = 0
        )
        database.withTransaction {
            dao.insertUser(user)
            dao.insertPilotProfile(profile)
        }
        withContext(Dispatchers.IO) {
            runCatching {
                SupabaseClient.api?.let { api ->
                    api.upsertUser(
                        SupabaseUserDto(
                            id = user.id,
                            phone = user.phone,
                            name = user.name,
                            role = user.role,
                            created_at = user.createdAt
                        )
                    )
                    api.upsertPilotProfile(
                        SupabasePilotProfileDto(
                            user_id = profile.userId,
                            drone_type = profile.droneType,
                            tank_capacity = profile.tankCapacity,
                            license_document_uri = profile.licenseDocumentUri,
                            is_license_verified = profile.isLicenseVerified,
                            bank_upi_id = profile.bankUpiId,
                            wallet_balance = profile.walletBalance,
                            is_online = profile.isOnline,
                            rating_average = profile.ratingAverage,
                            rating_count = profile.ratingCount,
                            current_lat = profile.currentLat,
                            current_lng = profile.currentLng,
                            location_updated_at = profile.locationUpdatedAt
                        )
                    )
                }
            }
        }
        return AuthResult.Success(user)
    }

    suspend fun loginByRole(phone: String, expectedRole: UserRole): AuthResult {
        val cleanPhone = phone.trim()
        var user = dao.getUserByPhone(cleanPhone)
        if (user == null) {
            // Hydrate from Supabase if account exists on cloud DB
            user = withContext(Dispatchers.IO) {
                runCatching {
                    val api = SupabaseClient.api ?: return@runCatching null
                    val resp = api.getUsersByPhone("eq.$cleanPhone")
                    val remoteUser = resp.body()?.firstOrNull() ?: return@runCatching null
                    val entity = UserEntity(
                        id = remoteUser.id,
                        phone = remoteUser.phone,
                        name = remoteUser.name,
                        role = remoteUser.role,
                        createdAt = remoteUser.created_at
                    )
                    dao.insertUser(entity)
                    if (entity.role.equals(UserRole.FARMER.apiValue, ignoreCase = true)) {
                        val profResp = api.getFarmerProfile("eq.${entity.id}")
                        profResp.body()?.firstOrNull()?.let { fp ->
                            dao.insertFarmerProfile(
                                FarmerProfileEntity(
                                    userId = fp.user_id,
                                    farmerIdNumber = fp.farmer_id_number,
                                    coins = fp.coins
                                )
                            )
                        }
                    } else if (entity.role.equals(UserRole.PILOT.apiValue, ignoreCase = true)) {
                        val profResp = api.getPilotProfile("eq.${entity.id}")
                        profResp.body()?.firstOrNull()?.let { pp ->
                            dao.insertPilotProfile(
                                PilotProfileEntity(
                                    userId = pp.user_id,
                                    droneType = pp.drone_type,
                                    tankCapacity = pp.tank_capacity,
                                    licenseDocumentUri = pp.license_document_uri,
                                    isLicenseVerified = pp.is_license_verified,
                                    bankUpiId = pp.bank_upi_id,
                                    walletBalance = pp.wallet_balance,
                                    isOnline = pp.is_online,
                                    ratingAverage = pp.rating_average,
                                    ratingCount = pp.rating_count,
                                    currentLat = pp.current_lat,
                                    currentLng = pp.current_lng,
                                    locationUpdatedAt = pp.location_updated_at
                                )
                            )
                        }
                    }
                    entity
                }.getOrNull()
            }
        }
        if (user == null) {
            return AuthResult.Error("No ${expectedRole.apiValue} account found for this number. Please sign up.")
        }
        if (!user.role.equals(expectedRole.apiValue, ignoreCase = true)) {
            return AuthResult.Error("This number is registered under a different role.")
        }
        return AuthResult.Success(user)
    }

    suspend fun loginAdmin(phone: String, passcode: String): AuthResult {
        ensureAdminSeeded()
        val cleanPhone = phone.trim()
        val user = dao.getUserByPhone(cleanPhone)
        if (user == null || user.role != UserRole.ADMIN.apiValue || passcode.trim() != ADMIN_PASSCODE) {
            return AuthResult.Error("Invalid administrator credentials.")
        }
        return AuthResult.Success(user)
    }

    suspend fun updateFarmerCredentials(
        userId: String,
        name: String,
        phone: String,
        farmerIdNumber: String
    ): AuthResult {
        val cleanName = name.trim()
        val cleanPhone = phone.trim()
        val cleanFarmerId = farmerIdNumber.trim().uppercase(Locale.getDefault())

        if (cleanName.isBlank()) {
            return AuthResult.Error("Farmer name cannot be empty.")
        }
        if (cleanPhone.length < 10) {
            return AuthResult.Error("Enter a valid 10-digit mobile number.")
        }
        if (cleanFarmerId.isBlank()) {
            return AuthResult.Error("Farmer ID number cannot be empty.")
        }

        val existingLocal = dao.getUserByPhone(cleanPhone)
        if (existingLocal != null && existingLocal.id != userId) {
            return AuthResult.Error("This mobile number is already registered to another account.")
        }

        val remoteConflict = withContext(Dispatchers.IO) {
            runCatching {
                val api = SupabaseClient.api ?: return@runCatching false
                val matches = api.getUsersByPhone("eq.$cleanPhone").body().orEmpty()
                matches.any { it.id != userId }
            }.getOrDefault(false)
        }
        if (remoteConflict) {
            return AuthResult.Error("This mobile number is already registered on another account.")
        }

        val (updatedUser, updatedProfile) = database.withTransaction {
            val currentUser = dao.getUserById(userId)
                ?: throw IllegalStateException("Farmer account not found.")
            val currentProfile = dao.getFarmerProfile(userId)
            val nextUser = currentUser.copy(
                name = cleanName,
                phone = cleanPhone
            )
            val nextProfile = FarmerProfileEntity(
                userId = userId,
                farmerIdNumber = cleanFarmerId,
                coins = currentProfile?.coins ?: 0
            )
            dao.insertUser(nextUser)
            dao.insertFarmerProfile(nextProfile)
            nextUser to nextProfile
        }

        withContext(Dispatchers.IO) {
            runCatching {
                SupabaseClient.api?.let { api ->
                    val patchUserResp = api.patchUserCredentials(
                        idFilter = "eq.$userId",
                        body = SupabaseUserPatchDto(name = updatedUser.name, phone = updatedUser.phone)
                    )
                    if (!patchUserResp.isSuccessful || patchUserResp.body().isNullOrEmpty()) {
                        api.upsertUser(
                            SupabaseUserDto(
                                id = updatedUser.id,
                                phone = updatedUser.phone,
                                name = updatedUser.name,
                                role = updatedUser.role,
                                created_at = updatedUser.createdAt
                            )
                        )
                    }
                    val patchProfResp = api.patchFarmerIdNumber(
                        userIdFilter = "eq.$userId",
                        body = SupabaseFarmerIdPatchDto(farmer_id_number = updatedProfile.farmerIdNumber)
                    )
                    if (!patchProfResp.isSuccessful || patchProfResp.body().isNullOrEmpty()) {
                        api.upsertFarmerProfile(
                            SupabaseFarmerProfileDto(
                                user_id = updatedProfile.userId,
                                farmer_id_number = updatedProfile.farmerIdNumber,
                                coins = updatedProfile.coins
                            )
                        )
                    }
                }
            }
        }
        return AuthResult.Success(updatedUser)
    }

    fun observeFarmerProfile(farmerId: String): Flow<FarmerProfileEntity?> =
        dao.observeFarmerProfile(farmerId)

    fun observePilotProfile(pilotId: String): Flow<PilotProfileEntity?> =
        dao.observePilotProfile(pilotId)

    private fun resolveFieldCenter(order: OrderEntity, points: List<LatLngPoint>): LatLngPoint {
        if (order.fieldCenterLat != 0.0 || order.fieldCenterLng != 0.0) {
            return LatLngPoint(order.fieldCenterLat, order.fieldCenterLng)
        }
        return GeoPolygonCalculator.calculatePolygonCenter(points)
            ?: points.firstOrNull()
            ?: LatLngPoint(20.0059, 73.7897)
    }

    private fun mapOrderToDetails(
        order: OrderEntity,
        userMap: Map<String, UserEntity>,
        pilotMap: Map<String, PilotProfileEntity>
    ): OrderWithDetails {
        val farmer = userMap[order.farmerId]
        val pilotUser = order.assignedPilotId?.let { userMap[it] }
        val pilotProfile = order.assignedPilotId?.let { pilotMap[it] }
        val points = GeoPolygonCalculator.decodePolygon(order.fieldPolygonCoordinatesJson)
        val computedDistance = if (
            pilotProfile?.currentLat != null &&
            pilotProfile.currentLng != null &&
            (order.fieldCenterLat != 0.0 || order.fieldCenterLng != 0.0 || points.isNotEmpty())
        ) {
            val fieldCenter = resolveFieldCenter(order, points)
            GeoPolygonCalculator.calculateDistanceKm(
                LatLngPoint(pilotProfile.currentLat, pilotProfile.currentLng),
                fieldCenter
            )
        } else {
            order.distanceKm
        }
        return OrderWithDetails(
            order = order,
            farmerName = farmer?.name ?: "Farmer",
            farmerPhone = farmer?.phone ?: "",
            pilotName = pilotUser?.name,
            pilotPhone = pilotUser?.phone,
            pilotDroneType = pilotProfile?.droneType,
            pilotRatingAverage = pilotProfile?.ratingAverage,
            polygonPoints = points,
            pilotLat = pilotProfile?.currentLat,
            pilotLng = pilotProfile?.currentLng,
            liveDistanceKm = computedDistance ?: order.distanceKm
        )
    }

    fun observeAllOrdersWithDetails(): Flow<List<OrderWithDetails>> =
        combine(
            dao.observeAllOrders(),
            dao.observeAllUsers(),
            dao.observeAllPilots()
        ) { orders, users, pilots ->
            val userMap = users.associateBy { it.id }
            val pilotMap = pilots.associateBy { it.userId }
            orders.map { order -> mapOrderToDetails(order, userMap, pilotMap) }
        }

    fun observeOrdersForFarmer(farmerId: String): Flow<List<OrderWithDetails>> =
        combine(
            dao.observeOrdersByFarmer(farmerId),
            dao.observeAllUsers(),
            dao.observeAllPilots()
        ) { orders, users, pilots ->
            val userMap = users.associateBy { it.id }
            val pilotMap = pilots.associateBy { it.userId }
            orders.map { order -> mapOrderToDetails(order, userMap, pilotMap) }
        }

    /**
     * Priority Dispatch Queue for a Pilot:
     * Only visible when Pilot is Online. Orders are prioritized by pilot tier & order recency,
     * excluding orders this pilot rejected.
     */
    fun observePilotQueue(pilotId: String): Flow<List<OrderWithDetails>> =
        combine(
            observeAllOrdersWithDetails(),
            dao.observeRejectedOrderIds(pilotId),
            dao.observePilotProfile(pilotId)
        ) { allOrders, rejectedIds, pilotProfile ->
            if (pilotProfile == null || !pilotProfile.isOnline) {
                emptyList()
            } else {
                val rejectedSet = rejectedIds.toSet()
                val priorityScore = GeoPolygonCalculator.calculateDispatchPriorityScore(pilotProfile)
                val pilotLoc = if (pilotProfile.currentLat != null && pilotProfile.currentLng != null) {
                    LatLngPoint(pilotProfile.currentLat, pilotProfile.currentLng)
                } else {
                    null
                }
                allOrders
                    .filter { it.order.orderStatus == OrderStatus.PENDING.apiValue && it.order.id !in rejectedSet }
                    .map { item ->
                        val dist = if (pilotLoc != null) {
                            val center = resolveFieldCenter(item.order, item.polygonPoints)
                            GeoPolygonCalculator.calculateDistanceKm(pilotLoc, center)
                        } else {
                            item.liveDistanceKm
                        }
                        item.copy(
                            pilotLat = pilotProfile.currentLat,
                            pilotLng = pilotProfile.currentLng,
                            liveDistanceKm = dist
                        )
                    }
                    .sortedWith(
                        compareByDescending<OrderWithDetails> {
                            // High-acreage orders prioritized for high-priority pilots (Hexacopter + high rating)
                            if (priorityScore >= 115.0) it.order.acresToSpray else 0.0
                        }.thenByDescending { it.order.createdAt }
                    )
            }
        }

    fun observePilotAssignedOrders(pilotId: String): Flow<List<OrderWithDetails>> =
        combine(
            dao.observeOrdersByPilot(pilotId),
            dao.observeAllUsers(),
            dao.observeAllPilots()
        ) { orders, users, pilots ->
            val userMap = users.associateBy { it.id }
            val pilotMap = pilots.associateBy { it.userId }
            orders.map { order -> mapOrderToDetails(order, userMap, pilotMap) }
        }

    suspend fun placeOrder(
        farmerId: String,
        locationAddress: String,
        polygonPoints: List<LatLngPoint>,
        paymentMethod: PaymentMethod,
        razorpayPaymentId: String? = null
    ): OrderEntity {
        val computedAcres = GeoPolygonCalculator.calculatePolygonAreaAcres(polygonPoints)
        val tariff = GeoPolygonCalculator.calculateTariff(computedAcres)
        val center = GeoPolygonCalculator.calculatePolygonCenter(polygonPoints)
            ?: polygonPoints.firstOrNull()
            ?: LatLngPoint(20.0059, 73.7897)
        val order = OrderEntity(
            id = UUID.randomUUID().toString(),
            farmerId = farmerId,
            assignedPilotId = null,
            fieldPolygonCoordinatesJson = GeoPolygonCalculator.encodePolygon(polygonPoints),
            acresToSpray = tariff.acres,
            locationAddress = locationAddress.trim(),
            totalAmount = tariff.totalAmount,
            adminCommission = tariff.adminCommission,
            pilotEarnings = tariff.pilotEarnings,
            paymentMethod = paymentMethod.apiValue,
            paymentStatus = if (paymentMethod == PaymentMethod.ONLINE) {
                PaymentStatus.PAID.apiValue
            } else {
                PaymentStatus.PENDING.apiValue
            },
            orderStatus = OrderStatus.PENDING.apiValue,
            pilotEtaMinutes = 15,
            initialEtaMinutes = 15,
            distanceKm = null,
            fieldCenterLat = center.lat,
            fieldCenterLng = center.lng,
            acceptedAt = null,
            razorpayPaymentId = razorpayPaymentId
        )
        dao.insertOrder(order)
        withContext(Dispatchers.IO) {
            runCatching {
                SupabaseClient.api?.upsertOrder(
                    SupabaseOrderDto(
                        id = order.id,
                        farmer_id = order.farmerId,
                        assigned_pilot_id = order.assignedPilotId,
                        field_polygon_coordinates = polygonPoints.map { SupabaseCoordDto(it.lat, it.lng) },
                        acres_to_spray = order.acresToSpray,
                        location_address = order.locationAddress,
                        total_amount = order.totalAmount,
                        admin_commission = order.adminCommission,
                        pilot_earnings = order.pilotEarnings,
                        payment_method = order.paymentMethod,
                        payment_status = order.paymentStatus,
                        order_status = order.orderStatus,
                        pilot_eta_minutes = order.pilotEtaMinutes,
                        initial_eta_minutes = order.initialEtaMinutes,
                        distance_km = order.distanceKm,
                        field_center_lat = order.fieldCenterLat,
                        field_center_lng = order.fieldCenterLng,
                        accepted_at = order.acceptedAt,
                        pilot_rating = order.pilotRating,
                        razorpay_payment_id = order.razorpayPaymentId,
                        created_at = order.createdAt
                    )
                )
            }
        }
        return order
    }

    suspend fun syncFarmerStateFromSupabase(farmerId: String) = withContext(Dispatchers.IO) {
        val api = SupabaseClient.api ?: return@withContext
        runCatching {
            val remoteUser = api.getUsersById("eq.$farmerId").body()?.firstOrNull()
            if (remoteUser == null) {
                dao.getUserById(farmerId)?.let { localUser ->
                    api.upsertUser(
                        SupabaseUserDto(
                            id = localUser.id,
                            phone = localUser.phone,
                            name = localUser.name,
                            role = localUser.role,
                            created_at = localUser.createdAt
                        )
                    )
                }
            }
            val remoteProfile = api.getFarmerProfile("eq.$farmerId").body()?.firstOrNull()
            if (remoteProfile != null) {
                dao.insertFarmerProfile(
                    FarmerProfileEntity(
                        userId = remoteProfile.user_id,
                        farmerIdNumber = remoteProfile.farmer_id_number,
                        coins = remoteProfile.coins
                    )
                )
            } else {
                dao.getFarmerProfile(farmerId)?.let { localProf ->
                    api.upsertFarmerProfile(
                        SupabaseFarmerProfileDto(
                            user_id = localProf.userId,
                            farmer_id_number = localProf.farmerIdNumber,
                            coins = localProf.coins
                        )
                    )
                }
            }
        }
        runCatching {
            val orders = api.getOrdersForFarmer("eq.$farmerId").body().orEmpty()
            orders.forEach { dto ->
                val localExisting = dao.getOrderById(dto.id)
                // Protect locally completed + wallet-credited orders or local awaiting_confirmation state from stale cloud status
                if (localExisting != null && localExisting.orderStatus == OrderStatus.COMPLETED.apiValue) {
                    return@forEach
                }
                val resolvedStatus = if (
                    localExisting?.orderStatus == OrderStatus.AWAITING_CONFIRMATION.apiValue &&
                    dto.order_status == OrderStatus.IN_PROGRESS.apiValue
                ) {
                    OrderStatus.AWAITING_CONFIRMATION.apiValue
                } else {
                    dto.order_status
                }
                val poly = dto.field_polygon_coordinates.map { LatLngPoint(it.lat, it.lng) }
                dao.insertOrder(
                    OrderEntity(
                        id = dto.id,
                        farmerId = dto.farmer_id,
                        assignedPilotId = dto.assigned_pilot_id,
                        fieldPolygonCoordinatesJson = GeoPolygonCalculator.encodePolygon(poly),
                        acresToSpray = dto.acres_to_spray,
                        locationAddress = dto.location_address,
                        totalAmount = dto.total_amount,
                        adminCommission = dto.admin_commission,
                        pilotEarnings = dto.pilot_earnings,
                        paymentMethod = dto.payment_method,
                        paymentStatus = dto.payment_status,
                        orderStatus = resolvedStatus,
                        pilotEtaMinutes = dto.pilot_eta_minutes,
                        initialEtaMinutes = dto.initial_eta_minutes,
                        distanceKm = dto.distance_km,
                        fieldCenterLat = dto.field_center_lat,
                        fieldCenterLng = dto.field_center_lng,
                        acceptedAt = dto.accepted_at,
                        pilotRating = dto.pilot_rating,
                        razorpayPaymentId = dto.razorpay_payment_id,
                        pilotWalletCredited = localExisting?.pilotWalletCredited
                            ?: (resolvedStatus == OrderStatus.COMPLETED.apiValue),
                        createdAt = dto.created_at
                    )
                )
                val pilotId = dto.assigned_pilot_id
                if (!pilotId.isNullOrBlank()) {
                    api.getUsersById("eq.$pilotId").body()?.firstOrNull()?.let { pu ->
                        dao.insertUser(
                            UserEntity(
                                id = pu.id,
                                phone = pu.phone,
                                name = pu.name,
                                role = pu.role,
                                createdAt = pu.created_at
                            )
                        )
                    }
                    api.getPilotProfile("eq.$pilotId").body()?.firstOrNull()?.let { pp ->
                        dao.insertPilotProfile(
                            PilotProfileEntity(
                                userId = pp.user_id,
                                droneType = pp.drone_type,
                                tankCapacity = pp.tank_capacity,
                                licenseDocumentUri = pp.license_document_uri,
                                isLicenseVerified = pp.is_license_verified,
                                bankUpiId = pp.bank_upi_id,
                                walletBalance = pp.wallet_balance,
                                isOnline = pp.is_online,
                                ratingAverage = pp.rating_average,
                                ratingCount = pp.rating_count,
                                currentLat = pp.current_lat,
                                currentLng = pp.current_lng,
                                locationUpdatedAt = pp.location_updated_at
                            )
                        )
                    }
                }
            }
        }
    }

    suspend fun togglePilotOnline(pilotId: String, isOnline: Boolean) {
        val profile = dao.getPilotProfile(pilotId) ?: return
        dao.updatePilotProfile(profile.copy(isOnline = isOnline))
        withContext(Dispatchers.IO) {
            runCatching {
                SupabaseClient.api?.patchPilotOnline(
                    userIdFilter = "eq.$pilotId",
                    body = SupabasePilotOnlinePatchDto(is_online = isOnline)
                )
            }
        }
    }

    suspend fun updatePilotLiveLocation(pilotId: String, lat: Double, lng: Double) {
        val now = System.currentTimeMillis()
        val orderUpdates = mutableListOf<Pair<String, SupabaseOrderDistancePatchDto>>()
        val updatedPilot = database.withTransaction {
            val pilot = dao.getPilotProfile(pilotId) ?: return@withTransaction false
            dao.updatePilotProfile(
                pilot.copy(
                    currentLat = lat,
                    currentLng = lng,
                    locationUpdatedAt = now
                )
            )
            val droneType = DroneType.fromApi(pilot.droneType)
            val activeOrders = dao.getActiveOrdersByPilot(pilotId)
            activeOrders.forEach { order ->
                val points = GeoPolygonCalculator.decodePolygon(order.fieldPolygonCoordinatesJson)
                val fieldCenter = resolveFieldCenter(order, points)
                val distKm = GeoPolygonCalculator.calculateDistanceKm(LatLngPoint(lat, lng), fieldCenter)
                val updatedEta = if (order.orderStatus == OrderStatus.ACCEPTED.apiValue) {
                    GeoPolygonCalculator.estimateArrivalMinutes(distKm, droneType)
                } else {
                    0
                }
                dao.updateOrder(
                    order.copy(
                        distanceKm = distKm,
                        pilotEtaMinutes = updatedEta,
                        fieldCenterLat = fieldCenter.lat,
                        fieldCenterLng = fieldCenter.lng
                    )
                )
                orderUpdates.add(
                    order.id to SupabaseOrderDistancePatchDto(
                        distance_km = distKm,
                        pilot_eta_minutes = updatedEta,
                        field_center_lat = fieldCenter.lat,
                        field_center_lng = fieldCenter.lng
                    )
                )
            }
            true
        }
        if (updatedPilot) {
            withContext(Dispatchers.IO) {
                runCatching {
                    val api = SupabaseClient.api ?: return@runCatching
                    api.patchPilotLocation(
                        userIdFilter = "eq.$pilotId",
                        body = SupabasePilotLocationPatchDto(
                            current_lat = lat,
                            current_lng = lng,
                            location_updated_at = now
                        )
                    )
                    orderUpdates.forEach { (orderId, patch) ->
                        api.patchOrderDistance(
                            orderIdFilter = "eq.$orderId",
                            body = patch
                        )
                    }
                }
            }
        }
    }

    suspend fun topUpPilotWallet(pilotId: String, amount: Double) {
        if (amount <= 0.0) return
        val newBalance = database.withTransaction {
            val profile = dao.getPilotProfile(pilotId) ?: return@withTransaction null
            val updatedBalance = ((profile.walletBalance + amount) * 100.0).roundToInt() / 100.0
            dao.updatePilotProfile(profile.copy(walletBalance = updatedBalance))
            updatedBalance
        }
        if (newBalance != null) {
            withContext(Dispatchers.IO) {
                runCatching {
                    SupabaseClient.api?.patchPilotWalletBalance(
                        userIdFilter = "eq.$pilotId",
                        body = SupabasePilotWalletPatchDto(wallet_balance = newBalance)
                    )
                }
            }
        }
    }

    /**
     * Enforces the Cash Order Wallet Rule:
     * If payment_method == 'cash', pilot can only accept if wallet_balance >= 10% admin_commission.
     * Accepting immediately deducts that 10% commission.
     * Also calculates distance from the pilot's live GPS location to the farmer's field center
     * and sets initial real-time ETA.
     */
    suspend fun acceptOrder(
        pilotId: String,
        orderId: String,
        pilotLocation: LatLngPoint? = null
    ): AcceptOrderResult {
        var acceptedPatchToSync: SupabaseOrderAcceptedPatchDto? = null
        var updatedWalletToSync: Double? = null
        var acceptedAtTimestamp = 0L

        val result = database.withTransaction {
            val order = dao.getOrderById(orderId)
                ?: return@withTransaction AcceptOrderResult.Error("Order not found.")
            if (order.orderStatus != OrderStatus.PENDING.apiValue) {
                return@withTransaction AcceptOrderResult.Error("Order is no longer available.")
            }
            val existingPilot = dao.getPilotProfile(pilotId)
                ?: return@withTransaction AcceptOrderResult.Error("Pilot profile not found.")

            val now = System.currentTimeMillis()
            val pilotWithLocation = if (pilotLocation != null) {
                existingPilot.copy(
                    currentLat = pilotLocation.lat,
                    currentLng = pilotLocation.lng,
                    locationUpdatedAt = now
                )
            } else {
                existingPilot
            }

            val isCashOrder = order.paymentMethod == PaymentMethod.CASH.apiValue
            if (isCashOrder) {
                if (pilotWithLocation.walletBalance < order.adminCommission) {
                    return@withTransaction AcceptOrderResult.InsufficientWallet(
                        requiredCommission = order.adminCommission,
                        currentBalance = pilotWithLocation.walletBalance
                    )
                }
                val newWallet = ((pilotWithLocation.walletBalance - order.adminCommission) * 100.0).roundToInt() / 100.0
                dao.updatePilotProfile(pilotWithLocation.copy(walletBalance = newWallet))
                updatedWalletToSync = newWallet
                dao.insertCommissionLog(
                    CommissionLogEntity(
                        orderId = order.id,
                        pilotId = pilotId,
                        amount = order.adminCommission,
                        paymentMethod = order.paymentMethod,
                        note = "10% Cash Order Commission deducted on acceptance"
                    )
                )
            } else if (pilotLocation != null) {
                dao.updatePilotProfile(pilotWithLocation)
            }

            val effectiveLoc = pilotLocation ?: if (
                pilotWithLocation.currentLat != null && pilotWithLocation.currentLng != null
            ) {
                LatLngPoint(pilotWithLocation.currentLat, pilotWithLocation.currentLng)
            } else {
                null
            }
            val fieldCenter = resolveFieldCenter(
                order,
                GeoPolygonCalculator.decodePolygon(order.fieldPolygonCoordinatesJson)
            )
            val distKm = effectiveLoc?.let { GeoPolygonCalculator.calculateDistanceKm(it, fieldCenter) }
            val droneType = DroneType.fromApi(pilotWithLocation.droneType)
            val eta = if (distKm != null) {
                GeoPolygonCalculator.estimateArrivalMinutes(distKm, droneType)
            } else if (droneType == DroneType.HEXACOPTER) {
                12
            } else {
                16
            }

            dao.updateOrder(
                order.copy(
                    assignedPilotId = pilotId,
                    orderStatus = OrderStatus.ACCEPTED.apiValue,
                    pilotEtaMinutes = eta,
                    initialEtaMinutes = eta,
                    distanceKm = distKm,
                    fieldCenterLat = fieldCenter.lat,
                    fieldCenterLng = fieldCenter.lng,
                    acceptedAt = now
                )
            )
            acceptedAtTimestamp = now
            acceptedPatchToSync = SupabaseOrderAcceptedPatchDto(
                assigned_pilot_id = pilotId,
                order_status = OrderStatus.ACCEPTED.apiValue,
                pilot_eta_minutes = eta,
                initial_eta_minutes = eta,
                distance_km = distKm,
                field_center_lat = fieldCenter.lat,
                field_center_lng = fieldCenter.lng,
                accepted_at = now
            )
            AcceptOrderResult.Success
        }

        if (result is AcceptOrderResult.Success) {
            withContext(Dispatchers.IO) {
                runCatching {
                    val api = SupabaseClient.api ?: return@runCatching
                    acceptedPatchToSync?.let { patch ->
                        api.patchOrderAccepted(
                            orderIdFilter = "eq.$orderId",
                            body = patch
                        )
                    }
                    updatedWalletToSync?.let { wallet ->
                        api.patchPilotWalletBalance(
                            userIdFilter = "eq.$pilotId",
                            body = SupabasePilotWalletPatchDto(wallet_balance = wallet)
                        )
                    }
                    if (pilotLocation != null) {
                        api.patchPilotLocation(
                            userIdFilter = "eq.$pilotId",
                            body = SupabasePilotLocationPatchDto(
                                current_lat = pilotLocation.lat,
                                current_lng = pilotLocation.lng,
                                location_updated_at = acceptedAtTimestamp
                            )
                        )
                    }
                }
            }
        }
        return result
    }

    suspend fun rejectOrder(pilotId: String, orderId: String) {
        dao.insertRejectedOrder(RejectedOrderEntity(orderId = orderId, pilotId = pilotId))
    }

    suspend fun startSpraying(orderId: String) {
        val order = dao.getOrderById(orderId) ?: return
        if (order.orderStatus == OrderStatus.ACCEPTED.apiValue) {
            dao.updateOrder(
                order.copy(
                    orderStatus = OrderStatus.IN_PROGRESS.apiValue,
                    pilotEtaMinutes = 0
                )
            )
            withContext(Dispatchers.IO) {
                runCatching {
                    SupabaseClient.api?.patchOrderStatus(
                        orderIdFilter = "eq.$orderId",
                        body = SupabaseOrderStatusPatchDto(
                            order_status = OrderStatus.IN_PROGRESS.apiValue,
                            pilot_eta_minutes = 0
                        )
                    )
                }
            }
        }
    }

    /**
     * Bookings are non-cancellable once placed and paid.
     */
    suspend fun cancelOrderByFarmer(orderId: String): CancelOrderResult {
        val order = dao.getOrderById(orderId)
            ?: return CancelOrderResult.NotAllowed("Order not found.")
        return CancelOrderResult.NotAllowed(
            if (order.paymentStatus == PaymentStatus.PAID.apiValue) {
                "Service bookings cannot be cancelled after booking and payment."
            } else {
                "Active spray bookings cannot be cancelled once placed."
            }
        )
    }

    /**
     * Pilot finishes spraying -> sets order status to 'awaiting_confirmation'.
     * Does NOT transfer funds to the pilot's wallet yet; funds stay held until the Farmer clicks "Job Done".
     */
    suspend fun markPilotSprayingCompleted(orderId: String) {
        val updated = database.withTransaction {
            val order = dao.getOrderById(orderId) ?: return@withTransaction false
            if (order.orderStatus == OrderStatus.COMPLETED.apiValue ||
                order.orderStatus == OrderStatus.AWAITING_CONFIRMATION.apiValue
            ) {
                return@withTransaction false
            }
            dao.updateOrder(
                order.copy(
                    orderStatus = OrderStatus.AWAITING_CONFIRMATION.apiValue,
                    pilotEtaMinutes = 0
                )
            )
            true
        }
        if (updated) {
            withContext(Dispatchers.IO) {
                runCatching {
                    SupabaseClient.api?.patchOrderStatus(
                        orderIdFilter = "eq.$orderId",
                        body = SupabaseOrderStatusPatchDto(
                            order_status = OrderStatus.AWAITING_CONFIRMATION.apiValue,
                            pilot_eta_minutes = 0
                        )
                    )
                }
            }
        }
    }

    /**
     * Pilot-side completion hook: marks spraying finished and waits for Farmer "Job Done"
     * confirmation before releasing payout into pilot's wallet.
     */
    suspend fun completeOrder(orderId: String) {
        markPilotSprayingCompleted(orderId)
    }

    /**
     * Farmer clicks "Job Done":
     * - Marks order_status = 'completed', payment_status = 'paid', pilotWalletCredited = true
     * - Transfers pilot's net payout (order.pilotEarnings) to pilot's walletBalance for online-paid orders
     * - Awards +1 Harvest Coin to the Farmer
     * - Logs 10% platform commission for Online orders
     */
    suspend fun confirmJobDoneByFarmer(orderId: String): JobDoneResult {
        val preOrder = dao.getOrderById(orderId)
            ?: return JobDoneResult.Error("Order not found.")
        val prePilotId = preOrder.assignedPilotId
            ?: return JobDoneResult.Error("No pilot is assigned to this booking yet.")

        // Pull latest pilot profile from Supabase if configured so wallet balance starts up to date
        withContext(Dispatchers.IO) {
            runCatching {
                SupabaseClient.api?.getPilotProfile("eq.$prePilotId")?.body()?.firstOrNull()?.let { pp ->
                    val existingLocal = dao.getPilotProfile(prePilotId)
                    if (existingLocal == null) {
                        dao.insertPilotProfile(
                            PilotProfileEntity(
                                userId = pp.user_id,
                                droneType = pp.drone_type,
                                tankCapacity = pp.tank_capacity,
                                licenseDocumentUri = pp.license_document_uri,
                                isLicenseVerified = pp.is_license_verified,
                                bankUpiId = pp.bank_upi_id,
                                walletBalance = pp.wallet_balance,
                                isOnline = pp.is_online,
                                ratingAverage = pp.rating_average,
                                ratingCount = pp.rating_count,
                                currentLat = pp.current_lat,
                                currentLng = pp.current_lng,
                                locationUpdatedAt = pp.location_updated_at
                            )
                        )
                    } else if (pp.wallet_balance > existingLocal.walletBalance) {
                        dao.updatePilotProfile(existingLocal.copy(walletBalance = pp.wallet_balance))
                    }
                }
            }
        }

        var farmerIdToSync: String? = null
        var updatedFarmerCoins: Int? = null
        var pilotIdToSync: String? = null
        var updatedPilotWallet: Double? = null
        var transferredAmount = 0.0
        var pilotDisplayName: String? = null

        val result = database.withTransaction {
            val order = dao.getOrderById(orderId)
                ?: return@withTransaction JobDoneResult.Error("Order not found.")
            if (order.orderStatus == OrderStatus.COMPLETED.apiValue && order.pilotWalletCredited) {
                return@withTransaction JobDoneResult.Error("Job already confirmed and payout transferred.")
            }
            val pilotId = order.assignedPilotId
                ?: return@withTransaction JobDoneResult.Error("No pilot is assigned to this booking yet.")
            if (order.orderStatus !in setOf(
                    OrderStatus.AWAITING_CONFIRMATION.apiValue,
                    OrderStatus.IN_PROGRESS.apiValue
                )
            ) {
                return@withTransaction JobDoneResult.Error(
                    "Pilot must complete spraying before you can click Job Done."
                )
            }

            val isOnlineEscrow = order.paymentMethod == PaymentMethod.ONLINE.apiValue
            val amountToCredit = if (isOnlineEscrow && !order.pilotWalletCredited) {
                order.pilotEarnings
            } else {
                0.0
            }

            dao.updateOrder(
                order.copy(
                    orderStatus = OrderStatus.COMPLETED.apiValue,
                    paymentStatus = PaymentStatus.PAID.apiValue,
                    pilotEtaMinutes = 0,
                    pilotWalletCredited = true
                )
            )

            pilotDisplayName = dao.getUserById(pilotId)?.name
            val pilot = dao.getPilotProfile(pilotId)
            if (pilot != null && amountToCredit > 0.0) {
                val newWallet = ((pilot.walletBalance + amountToCredit) * 100.0).roundToInt() / 100.0
                dao.updatePilotProfile(pilot.copy(walletBalance = newWallet))
                pilotIdToSync = pilotId
                updatedPilotWallet = newWallet
            }
            transferredAmount = amountToCredit

            val farmer = dao.getFarmerProfile(order.farmerId)
            if (farmer != null) {
                val nextCoins = farmer.coins + 1
                dao.updateFarmerProfile(farmer.copy(coins = nextCoins))
                farmerIdToSync = order.farmerId
                updatedFarmerCoins = nextCoins
            }

            if (isOnlineEscrow) {
                dao.insertCommissionLog(
                    CommissionLogEntity(
                        orderId = order.id,
                        pilotId = pilotId,
                        amount = order.adminCommission,
                        paymentMethod = order.paymentMethod,
                        note = "10% Online Commission settled & ${order.pilotEarnings} released to pilot wallet on Farmer Job Done"
                    )
                )
            }

            JobDoneResult.Success(
                amountTransferredToPilot = transferredAmount,
                pilotName = pilotDisplayName
            )
        }

        if (result is JobDoneResult.Success) {
            withContext(Dispatchers.IO) {
                runCatching {
                    val api = SupabaseClient.api ?: return@runCatching
                    api.patchOrderStatus(
                        orderIdFilter = "eq.$orderId",
                        body = SupabaseOrderStatusPatchDto(
                            order_status = OrderStatus.COMPLETED.apiValue,
                            payment_status = PaymentStatus.PAID.apiValue,
                            pilot_eta_minutes = 0
                        )
                    )
                    val pId = pilotIdToSync
                    val pWallet = updatedPilotWallet
                    if (pId != null && pWallet != null) {
                        api.patchPilotWalletBalance(
                            userIdFilter = "eq.$pId",
                            body = SupabasePilotWalletPatchDto(wallet_balance = pWallet)
                        )
                    }
                    val fId = farmerIdToSync
                    val fCoins = updatedFarmerCoins
                    if (fId != null && fCoins != null) {
                        api.patchFarmerCoins(
                            userIdFilter = "eq.$fId",
                            body = SupabaseFarmerCoinsPatchDto(coins = fCoins)
                        )
                    }
                }
            }
        }

        return result
    }

    suspend fun ratePilotForOrder(orderId: String, rating: Int) {
        val clamped = rating.coerceIn(1, 5)
        var ratingSyncTarget: Triple<String, Double, Int>? = null
        database.withTransaction {
            val order = dao.getOrderById(orderId) ?: return@withTransaction
            val pilotId = order.assignedPilotId ?: return@withTransaction
            if (order.pilotRating != null) return@withTransaction

            dao.updateOrder(order.copy(pilotRating = clamped))
            val pilot = dao.getPilotProfile(pilotId) ?: return@withTransaction
            val newCount = pilot.ratingCount + 1
            val currentTotal = if (pilot.ratingCount == 0) 0.0 else pilot.ratingAverage * pilot.ratingCount
            val newAverage = (((currentTotal + clamped) / newCount) * 100.0).roundToInt() / 100.0
            dao.updatePilotProfile(
                pilot.copy(
                    ratingAverage = newAverage,
                    ratingCount = newCount
                )
            )
            ratingSyncTarget = Triple(pilotId, newAverage, newCount)
        }
        withContext(Dispatchers.IO) {
            runCatching {
                val api = SupabaseClient.api ?: return@runCatching
                api.patchOrderRating(
                    orderIdFilter = "eq.$orderId",
                    body = SupabaseOrderRatingPatchDto(pilot_rating = clamped)
                )
                ratingSyncTarget?.let { (pilotId, avg, count) ->
                    api.patchPilotRating(
                        userIdFilter = "eq.$pilotId",
                        body = SupabasePilotRatingPatchDto(
                            rating_average = avg,
                            rating_count = count
                        )
                    )
                }
            }
        }
    }

    suspend fun syncPilotAndMarketStateFromSupabase() = pullMarketStateFromSupabase()

    suspend fun syncAdminStateFromSupabase() = pullMarketStateFromSupabase()

    private suspend fun pullMarketStateFromSupabase() = withContext(Dispatchers.IO) {
        val api = SupabaseClient.api ?: return@withContext
        runCatching {
            api.getAllUsers().body().orEmpty().forEach { u ->
                dao.insertUser(
                    UserEntity(
                        id = u.id,
                        phone = u.phone,
                        name = u.name,
                        role = u.role,
                        createdAt = u.created_at
                    )
                )
            }
        }
        runCatching {
            api.getAllPilots().body().orEmpty().forEach { pp ->
                dao.insertPilotProfile(
                    PilotProfileEntity(
                        userId = pp.user_id,
                        droneType = pp.drone_type,
                        tankCapacity = pp.tank_capacity,
                        licenseDocumentUri = pp.license_document_uri,
                        isLicenseVerified = pp.is_license_verified,
                        bankUpiId = pp.bank_upi_id,
                        walletBalance = pp.wallet_balance,
                        isOnline = pp.is_online,
                        ratingAverage = pp.rating_average,
                        ratingCount = pp.rating_count,
                        currentLat = pp.current_lat,
                        currentLng = pp.current_lng,
                        locationUpdatedAt = pp.location_updated_at
                    )
                )
            }
        }
        runCatching {
            api.getAllOrders().body().orEmpty().forEach { dto ->
                val localExisting = dao.getOrderById(dto.id)
                if (localExisting != null && localExisting.orderStatus == OrderStatus.COMPLETED.apiValue) {
                    return@forEach
                }
                val resolvedStatus = if (
                    localExisting?.orderStatus == OrderStatus.AWAITING_CONFIRMATION.apiValue &&
                    dto.order_status == OrderStatus.IN_PROGRESS.apiValue
                ) {
                    OrderStatus.AWAITING_CONFIRMATION.apiValue
                } else {
                    dto.order_status
                }
                val poly = dto.field_polygon_coordinates.map { LatLngPoint(it.lat, it.lng) }
                dao.insertOrder(
                    OrderEntity(
                        id = dto.id,
                        farmerId = dto.farmer_id,
                        assignedPilotId = dto.assigned_pilot_id,
                        fieldPolygonCoordinatesJson = GeoPolygonCalculator.encodePolygon(poly),
                        acresToSpray = dto.acres_to_spray,
                        locationAddress = dto.location_address,
                        totalAmount = dto.total_amount,
                        adminCommission = dto.admin_commission,
                        pilotEarnings = dto.pilot_earnings,
                        paymentMethod = dto.payment_method,
                        paymentStatus = dto.payment_status,
                        orderStatus = resolvedStatus,
                        pilotEtaMinutes = dto.pilot_eta_minutes,
                        initialEtaMinutes = dto.initial_eta_minutes,
                        distanceKm = dto.distance_km,
                        fieldCenterLat = dto.field_center_lat,
                        fieldCenterLng = dto.field_center_lng,
                        acceptedAt = dto.accepted_at,
                        pilotRating = dto.pilot_rating,
                        razorpayPaymentId = dto.razorpay_payment_id,
                        pilotWalletCredited = localExisting?.pilotWalletCredited
                            ?: (resolvedStatus == OrderStatus.COMPLETED.apiValue),
                        createdAt = dto.created_at
                    )
                )
            }
        }
    }

    // --- Admin Flows ---

    fun observeAllPilotsWithUsers(): Flow<List<PilotWithUser>> =
        combine(dao.observeAllUsers(), dao.observeAllPilots()) { users, pilots ->
            val userMap = users.associateBy { it.id }
            pilots.mapNotNull { profile ->
                userMap[profile.userId]?.let { user -> PilotWithUser(user, profile) }
            }
        }

    fun observeAllFarmersWithUsers(): Flow<List<FarmerWithUser>> =
        combine(dao.observeAllUsers(), dao.observeAllFarmers()) { users, farmers ->
            val userMap = users.associateBy { it.id }
            farmers.mapNotNull { profile ->
                userMap[profile.userId]?.let { user -> FarmerWithUser(user, profile) }
            }
        }

    fun observePilotPerformanceStats(): Flow<List<PilotPerformanceStat>> =
        combine(observeAllPilotsWithUsers(), dao.observeAllOrders()) { pilots, orders ->
            pilots.map { (user, profile) ->
                val completedOrders = orders.filter {
                    it.assignedPilotId == user.id && it.orderStatus == OrderStatus.COMPLETED.apiValue
                }
                PilotPerformanceStat(
                    pilotId = user.id,
                    pilotName = user.name,
                    phone = user.phone,
                    droneType = DroneType.fromApi(profile.droneType),
                    tankCapacity = profile.tankCapacity,
                    isLicenseVerified = profile.isLicenseVerified,
                    licenseDocumentUri = profile.licenseDocumentUri,
                    ratingAverage = profile.ratingAverage,
                    walletBalance = profile.walletBalance,
                    completedOrdersCount = completedOrders.size,
                    totalEarnings = completedOrders.sumOf { it.pilotEarnings }
                )
            }.sortedByDescending { it.completedOrdersCount }
        }

    fun observeDailyOrderVolumes(): Flow<List<DailyOrderVolume>> =
        combine(dao.observeAllOrders()) { (orders) ->
            val fmt = SimpleDateFormat("MMM dd", Locale.getDefault())
            orders.groupBy { fmt.format(Date(it.createdAt)) }
                .map { (day, dayOrders) ->
                    DailyOrderVolume(
                        dayLabel = day,
                        count = dayOrders.size,
                        revenue = dayOrders.sumOf { it.totalAmount }
                    )
                }
                .take(7)
        }

    fun observeCommissionLogs(): Flow<List<CommissionLogEntity>> =
        dao.observeCommissionLogs()

    suspend fun setPilotLicenseVerified(pilotId: String, verified: Boolean) {
        val profile = dao.getPilotProfile(pilotId) ?: return
        dao.updatePilotProfile(profile.copy(isLicenseVerified = verified))
        withContext(Dispatchers.IO) {
            runCatching {
                SupabaseClient.api?.patchPilotLicense(
                    userIdFilter = "eq.$pilotId",
                    body = SupabasePilotLicensePatchDto(is_license_verified = verified)
                )
            }
        }
    }

    suspend fun updatePilotAccountDetails(
        pilotId: String,
        name: String,
        droneType: DroneType,
        tankCapacity: Double,
        bankUpiId: String,
        licenseDocumentUri: String? = null
    ): AuthResult {
        val cleanName = name.trim()
        val cleanUpi = bankUpiId.trim()
        if (cleanName.isBlank()) {
            return AuthResult.Error("Pilot name cannot be empty.")
        }
        if (cleanUpi.isBlank()) {
            return AuthResult.Error("UPI ID cannot be empty.")
        }
        if (tankCapacity <= 0.0) {
            return AuthResult.Error("Tank capacity must be positive.")
        }

        val (updatedUser, updatedProfile) = database.withTransaction {
            val user = dao.getUserById(pilotId)
                ?: throw IllegalStateException("Pilot user not found.")
            val profile = dao.getPilotProfile(pilotId)
                ?: throw IllegalStateException("Pilot profile not found.")
            val nextUser = user.copy(name = cleanName)
            val nextProfile = profile.copy(
                droneType = droneType.apiValue,
                tankCapacity = tankCapacity,
                bankUpiId = cleanUpi,
                licenseDocumentUri = licenseDocumentUri ?: profile.licenseDocumentUri
            )
            dao.insertUser(nextUser)
            dao.updatePilotProfile(nextProfile)
            nextUser to nextProfile
        }
        withContext(Dispatchers.IO) {
            runCatching {
                val api = SupabaseClient.api ?: return@runCatching
                api.patchUserName(
                    idFilter = "eq.$pilotId",
                    body = SupabaseUserNamePatchDto(name = updatedUser.name)
                )
                api.patchPilotAccount(
                    userIdFilter = "eq.$pilotId",
                    body = SupabasePilotAccountPatchDto(
                        drone_type = updatedProfile.droneType,
                        tank_capacity = updatedProfile.tankCapacity,
                        bank_upi_id = updatedProfile.bankUpiId,
                        license_document_uri = updatedProfile.licenseDocumentUri
                    )
                )
            }
        }
        return AuthResult.Success(updatedUser)
    }
}
