package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.CommissionLogEntity
import com.example.data.model.FarmBoundaryEntity
import com.example.data.model.FarmerProfileEntity
import com.example.data.model.OrderEntity
import com.example.data.model.PilotProfileEntity
import com.example.data.model.RejectedOrderEntity
import com.example.data.model.UserEntity

@Database(
    entities = [
        UserEntity::class,
        PilotProfileEntity::class,
        FarmerProfileEntity::class,
        OrderEntity::class,
        CommissionLogEntity::class,
        RejectedOrderEntity::class,
        FarmBoundaryEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class KrishivantriDatabase : RoomDatabase() {
    abstract fun dao(): KrishivantriDao

    companion object {
        @Volatile
        private var INSTANCE: KrishivantriDatabase? = null

        fun getInstance(context: Context): KrishivantriDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    KrishivantriDatabase::class.java,
                    "krishivantri_db"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
