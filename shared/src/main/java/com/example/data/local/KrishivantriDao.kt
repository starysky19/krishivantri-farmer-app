package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.CommissionLogEntity
import com.example.data.model.FarmBoundaryEntity
import com.example.data.model.FarmerProfileEntity
import com.example.data.model.OrderEntity
import com.example.data.model.PilotProfileEntity
import com.example.data.model.RejectedOrderEntity
import com.example.data.model.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface KrishivantriDao {

    @Query("SELECT * FROM users")
    fun observeAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE phone = :phone LIMIT 1")
    suspend fun getUserByPhone(phone: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Query("SELECT * FROM pilot_profiles")
    fun observeAllPilots(): Flow<List<PilotProfileEntity>>

    @Query("SELECT * FROM pilot_profiles WHERE userId = :userId LIMIT 1")
    fun observePilotProfile(userId: String): Flow<PilotProfileEntity?>

    @Query("SELECT * FROM pilot_profiles WHERE userId = :userId LIMIT 1")
    suspend fun getPilotProfile(userId: String): PilotProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPilotProfile(profile: PilotProfileEntity)

    @Update
    suspend fun updatePilotProfile(profile: PilotProfileEntity)

    @Query("SELECT * FROM farmer_profiles")
    fun observeAllFarmers(): Flow<List<FarmerProfileEntity>>

    @Query("SELECT * FROM farmer_profiles WHERE userId = :userId LIMIT 1")
    fun observeFarmerProfile(userId: String): Flow<FarmerProfileEntity?>

    @Query("SELECT * FROM farmer_profiles WHERE userId = :userId LIMIT 1")
    suspend fun getFarmerProfile(userId: String): FarmerProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFarmerProfile(profile: FarmerProfileEntity)

    @Update
    suspend fun updateFarmerProfile(profile: FarmerProfileEntity)

    @Query("SELECT * FROM orders ORDER BY createdAt DESC")
    fun observeAllOrders(): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE farmerId = :farmerId ORDER BY createdAt DESC")
    fun observeOrdersByFarmer(farmerId: String): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE assignedPilotId = :pilotId ORDER BY createdAt DESC")
    fun observeOrdersByPilot(pilotId: String): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE assignedPilotId = :pilotId AND orderStatus IN ('accepted', 'in_progress')")
    suspend fun getActiveOrdersByPilot(pilotId: String): List<OrderEntity>

    @Query("SELECT * FROM orders WHERE id = :orderId LIMIT 1")
    suspend fun getOrderById(orderId: String): OrderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderEntity)

    @Update
    suspend fun updateOrder(order: OrderEntity)

    @Query("SELECT * FROM commission_logs ORDER BY createdAt DESC")
    fun observeCommissionLogs(): Flow<List<CommissionLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCommissionLog(log: CommissionLogEntity)

    @Query("SELECT orderId FROM rejected_orders WHERE pilotId = :pilotId")
    fun observeRejectedOrderIds(pilotId: String): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRejectedOrder(rejected: RejectedOrderEntity)

    @Query("SELECT * FROM farm_boundaries WHERE farmerId = :farmerId ORDER BY createdAt DESC")
    fun observeFarmBoundariesForFarmer(farmerId: String): Flow<List<FarmBoundaryEntity>>

    @Query("SELECT * FROM farm_boundaries ORDER BY createdAt DESC")
    fun observeAllFarmBoundaries(): Flow<List<FarmBoundaryEntity>>

    @Query("SELECT * FROM farm_boundaries WHERE farmerId = :farmerId AND isSyncedToSupabase = 0")
    suspend fun getUnsyncedFarmBoundaries(farmerId: String): List<FarmBoundaryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFarmBoundary(boundary: FarmBoundaryEntity)

    @Update
    suspend fun updateFarmBoundary(boundary: FarmBoundaryEntity)

    @Query("DELETE FROM farm_boundaries WHERE id = :boundaryId")
    suspend fun deleteFarmBoundary(boundaryId: String)
}
