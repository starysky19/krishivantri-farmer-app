package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass
import java.util.UUID

enum class UserRole(val apiValue: String) {
    ADMIN("admin"),
    PILOT("pilot"),
    FARMER("farmer");

    companion object {
        fun fromApi(value: String): UserRole =
            entries.firstOrNull { it.apiValue.equals(value, ignoreCase = true) } ?: FARMER
    }
}

enum class DroneType(val apiValue: String, val label: String, val priorityBoost: Double) {
    QUADCOPTER("quadcopter", "Quadcopter", 10.0),
    HEXACOPTER("hexacopter", "Hexacopter", 25.0);

    companion object {
        fun fromApi(value: String): DroneType =
            entries.firstOrNull { it.apiValue.equals(value, ignoreCase = true) } ?: QUADCOPTER
    }
}

enum class PaymentMethod(val apiValue: String, val label: String) {
    ONLINE("online", "Razorpay UPI (Online)"),
    CASH("cash", "Cash on Field");

    companion object {
        fun fromApi(value: String): PaymentMethod =
            entries.firstOrNull { it.apiValue.equals(value, ignoreCase = true) } ?: ONLINE
    }
}

enum class PaymentStatus(val apiValue: String) {
    PENDING("pending"),
    PAID("paid")
}

enum class OrderStatus(val apiValue: String, val displayLabel: String) {
    PENDING("pending", "Finding Priority Pilot"),
    ACCEPTED("accepted", "Pilot En Route"),
    IN_PROGRESS("in_progress", "Spraying In Progress"),
    COMPLETED("completed", "Spray Completed"),
    CANCELLED("cancelled", "Cancelled");

    companion object {
        fun fromApi(value: String): OrderStatus =
            entries.firstOrNull { it.apiValue.equals(value, ignoreCase = true) } ?: PENDING
    }
}

@JsonClass(generateAdapter = true)
data class LatLngPoint(
    val lat: Double,
    val lng: Double
)

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val phone: String,
    val name: String,
    val role: String, // 'admin', 'pilot', 'farmer'
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "pilot_profiles")
data class PilotProfileEntity(
    @PrimaryKey val userId: String,
    val droneType: String, // 'quadcopter' or 'hexacopter'
    val tankCapacity: Double, // in liters
    val licenseDocumentUri: String,
    val isLicenseVerified: Boolean = false,
    val bankUpiId: String,
    val walletBalance: Double = 0.0,
    val isOnline: Boolean = false,
    val ratingAverage: Double = 5.0,
    val ratingCount: Int = 0,
    val currentLat: Double? = null,
    val currentLng: Double? = null,
    val locationUpdatedAt: Long = 0L
)

@Entity(tableName = "farmer_profiles")
data class FarmerProfileEntity(
    @PrimaryKey val userId: String,
    val farmerIdNumber: String,
    val coins: Int = 0
)

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val farmerId: String,
    val assignedPilotId: String? = null,
    val fieldPolygonCoordinatesJson: String,
    val acresToSpray: Double,
    val locationAddress: String,
    val totalAmount: Double,
    val adminCommission: Double,
    val pilotEarnings: Double,
    val paymentMethod: String, // 'online' or 'cash'
    val paymentStatus: String, // 'pending' or 'paid'
    val orderStatus: String, // 'pending', 'accepted', 'in_progress', 'completed', 'cancelled'
    val pilotEtaMinutes: Int = 15,
    val initialEtaMinutes: Int = 15,
    val distanceKm: Double? = null,
    val fieldCenterLat: Double = 0.0,
    val fieldCenterLng: Double = 0.0,
    val acceptedAt: Long? = null,
    val pilotRating: Int? = null,
    val razorpayPaymentId: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "commission_logs")
data class CommissionLogEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val orderId: String,
    val pilotId: String,
    val amount: Double,
    val paymentMethod: String,
    val note: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "rejected_orders")
data class RejectedOrderEntity(
    val orderId: String,
    val pilotId: String,
    @PrimaryKey val compositeKey: String = "${orderId}_$pilotId"
)

data class PilotWithUser(
    val user: UserEntity,
    val profile: PilotProfileEntity
) {
    val droneTypeEnum: DroneType get() = DroneType.fromApi(profile.droneType)
}

data class FarmerWithUser(
    val user: UserEntity,
    val profile: FarmerProfileEntity
)

data class OrderWithDetails(
    val order: OrderEntity,
    val farmerName: String,
    val farmerPhone: String,
    val pilotName: String?,
    val pilotPhone: String?,
    val pilotDroneType: String?,
    val pilotRatingAverage: Double?,
    val polygonPoints: List<LatLngPoint>,
    val pilotLat: Double? = null,
    val pilotLng: Double? = null,
    val liveDistanceKm: Double? = null
) {
    val statusEnum: OrderStatus get() = OrderStatus.fromApi(order.orderStatus)
    val paymentMethodEnum: PaymentMethod get() = PaymentMethod.fromApi(order.paymentMethod)
}

data class DailyOrderVolume(
    val dayLabel: String,
    val count: Int,
    val revenue: Double
)

data class PilotPerformanceStat(
    val pilotId: String,
    val pilotName: String,
    val phone: String,
    val droneType: DroneType,
    val tankCapacity: Double,
    val isLicenseVerified: Boolean,
    val licenseDocumentUri: String,
    val ratingAverage: Double,
    val walletBalance: Double,
    val completedOrdersCount: Int,
    val totalEarnings: Double
)

enum class MapStyleType(val label: String) {
    STANDARD("Standard"),
    SATELLITE("Satellite"),
    HYBRID("Hybrid")
}

enum class OperatorRoleType(val apiValue: String, val label: String) {
    DRONE_OPERATOR("drone_operator", "Drone Operator"),
    DRONE_MITRA("drone_mitra", "Drone Mitra");

    companion object {
        fun fromApi(value: String): OperatorRoleType =
            entries.firstOrNull { it.apiValue.equals(value, ignoreCase = true) } ?: DRONE_OPERATOR
    }
}

@Entity(tableName = "farm_boundaries")
data class FarmBoundaryEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val farmerId: String,
    val farmName: String,
    val polygonCoordinatesJson: String,
    val areaAcres: Double,
    val areaHectares: Double,
    val centerLat: Double,
    val centerLng: Double,
    val isSyncedToSupabase: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class OperatorLocationMarker(
    val id: String,
    val name: String,
    val roleType: OperatorRoleType,
    val lat: Double,
    val lng: Double,
    val droneType: String,
    val isAvailable: Boolean,
    val headingDegrees: Float = 0f,
    val updatedAt: Long = System.currentTimeMillis()
)

data class OfflineMapRegionInfo(
    val id: Long,
    val name: String,
    val completedBytes: Long,
    val completedTiles: Long,
    val requiredTiles: Long,
    val isComplete: Boolean
)

data class LocationSnapshot(
    val lat: Double,
    val lng: Double,
    val accuracyMeters: Float,
    val bearing: Float = 0f,
    val provider: String = "gps",
    val timestamp: Long = System.currentTimeMillis()
)

