package com.petanalyzer.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.petanalyzer.data.dao.BehaviorLogDao
import com.petanalyzer.data.entity.BehaviorLogEntity

@Database(
    entities = [BehaviorLogEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class PetDatabase : RoomDatabase() {

    abstract fun behaviorLogDao(): BehaviorLogDao

    companion object {
        @Volatile
        private var INSTANCE: PetDatabase? = null

        fun getInstance(context: Context): PetDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    PetDatabase::class.java,
                    "pet_behavior.db",
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
