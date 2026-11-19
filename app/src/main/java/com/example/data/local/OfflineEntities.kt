package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.model.SyncStatus

@Entity(
  tableName = "offline_collections",
  indices = [Index(value = ["idempotencyKey"], unique = true)]
)
data class OfflineCollectionEntity(
  @PrimaryKey(autoGenerate = true) val localId: Long = 0L,
  val idempotencyKey: String,
  val originType: String,
  val hubId: String,
  val pickerUid: String,
  val pickerName: String,
  val householdId: String?,
  val agentUid: String,
  val materialId: String,
  val materialName: String,
  val grade: String,
  val grossKgMilli: Long,
  val tareKgMilli: Long,
  val netKgMilli: Long,
  val deductionKgMilli: Long,
  val acceptedKgMilli: Long,
  val rateIdSnapshot: String,
  val unitPriceUgxMinor: Long,
  val totalEarningsUgxMinor: Long,
  val receiptRef: String,
  val evidenceUri: String?,
  val originalEventEpochMs: Long,
  val deviceId: String,
  val syncStatus: String = SyncStatus.PENDING_SYNC.name,
  val syncError: String? = null
)

@Entity(tableName = "scale_calibrations")
data class ScaleCalibrationEntity(
  @PrimaryKey val scaleId: String,
  val hubId: String,
  val lastCalibratedEpochMs: Long,
  val maxCapacityKgMilli: Long,
  val status: String
)
