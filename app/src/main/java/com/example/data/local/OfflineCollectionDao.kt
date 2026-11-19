package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface OfflineCollectionDao {
  @Query("SELECT * FROM offline_collections ORDER BY originalEventEpochMs DESC")
  fun observeAll(): Flow<List<OfflineCollectionEntity>>

  @Query("SELECT * FROM offline_collections WHERE syncStatus IN ('PENDING_SYNC', 'SYNC_FAILED') ORDER BY originalEventEpochMs ASC")
  suspend fun getPendingForSync(): List<OfflineCollectionEntity>

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  suspend fun insertIfNew(entity: OfflineCollectionEntity): Long

  @Query("UPDATE offline_collections SET syncStatus = :status, syncError = :error WHERE localId = :localId")
  suspend fun updateSyncStatus(localId: Long, status: String, error: String?)

  @Query("SELECT * FROM scale_calibrations ORDER BY lastCalibratedEpochMs DESC")
  fun observeScaleCalibrations(): Flow<List<ScaleCalibrationEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertScaleCalibration(scale: ScaleCalibrationEntity)
}
