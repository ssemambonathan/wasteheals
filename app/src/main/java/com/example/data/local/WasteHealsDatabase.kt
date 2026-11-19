package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
  entities = [OfflineCollectionEntity::class, ScaleCalibrationEntity::class],
  version = 1,
  exportSchema = false
)
abstract class WasteHealsDatabase : RoomDatabase() {
  abstract fun offlineCollectionDao(): OfflineCollectionDao

  companion object {
    @Volatile
    private var INSTANCE: WasteHealsDatabase? = null

    fun getInstance(context: Context): WasteHealsDatabase {
      return INSTANCE ?: synchronized(this) {
        INSTANCE ?: Room.databaseBuilder(
          context.applicationContext,
          WasteHealsDatabase::class.java,
          "wasteheals_offline.db"
        ).build().also { INSTANCE = it }
      }
    }
  }
}
