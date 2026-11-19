package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.OfflineCollectionEntity
import com.example.data.local.WasteHealsDatabase
import com.example.domain.model.OriginType
import com.example.domain.model.SyncStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `app metadata and firebase database config present`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    val dbId = context.getString(R.string.firestore_database_id)
    assertEquals("WasteHeals", appName)
    assertTrue(dbId.isNotBlank())
  }

  @Test
  fun `room offline collection queue enforces idempotent deduplication`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = Room.inMemoryDatabaseBuilder(context, WasteHealsDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    try {
      val dao = db.offlineCollectionDao()
      val entity = OfflineCollectionEntity(
        idempotencyKey = "idem_unique_12345678",
        originType = OriginType.PICKER_HUB.name,
        hubId = "hub_nakawa",
        pickerUid = "picker_01",
        pickerName = "Amina Namukasa",
        householdId = null,
        agentUid = "agent_01",
        materialId = "mat_pet",
        materialName = "Clear PET Bottles",
        grade = "A",
        grossKgMilli = 20_000L,
        tareKgMilli = 1_500L,
        netKgMilli = 18_500L,
        deductionKgMilli = 500L,
        acceptedKgMilli = 18_000L,
        rateIdSnapshot = "rate_mat_pet_v1",
        unitPriceUgxMinor = 900L,
        totalEarningsUgxMinor = 16_200L,
        receiptRef = "WH-PROV-001",
        evidenceUri = null,
        originalEventEpochMs = 1_700_000_000_000L,
        deviceId = "ANDROID_FIELD_DEVICE",
        syncStatus = SyncStatus.PENDING_SYNC.name
      )
      val firstRowId = dao.insertIfNew(entity)
      val duplicateRowId = dao.insertIfNew(entity)

      assertTrue(firstRowId > 0L)
      assertEquals(-1L, duplicateRowId)
      assertEquals(1, dao.getPendingForSync().size)
    } finally {
      db.close()
    }
  }
}
