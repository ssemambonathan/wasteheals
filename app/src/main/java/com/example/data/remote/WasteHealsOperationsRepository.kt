package com.example.data.remote

import android.content.Context
import com.example.R
import com.example.data.local.OfflineCollectionEntity
import com.example.data.local.WasteHealsDatabase
import com.example.domain.engine.DoubleEntryAccountingEngine
import com.example.domain.engine.MoneyAndWeight
import com.example.domain.engine.PricingAndWeighingEngine
import com.example.domain.model.CompostBatch
import com.example.domain.model.CompostStatus
import com.example.domain.model.InventoryBatch
import com.example.domain.model.OriginType
import com.example.domain.model.PayoutStatus
import com.example.domain.model.PurchaseRate
import com.example.domain.model.SaleStatus
import com.example.domain.model.SettlementMethod
import com.example.domain.model.SyncStatus
import com.example.domain.model.VerificationStatus
import com.example.domain.model.WasteDelivery
import com.example.domain.model.WasteSale
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class WasteHealsOperationsRepository(
  private val db: FirebaseFirestore,
  private val localDb: WasteHealsDatabase,
  private val auth: FirebaseAuth = Firebase.auth
) {
  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(context.applicationContext.getString(R.string.firestore_database_id)),
    WasteHealsDatabase.getInstance(context.applicationContext)
  )

  private fun requireUserId(): String =
    auth.currentUser?.uid ?: throw IllegalStateException("Google Sign-In required.")

  private fun isBootstrapAdmin(): Boolean {
    val user = auth.currentUser ?: return false
    return user.email == WasteHealsCatalogRepository.BOOTSTRAP_ADMIN_EMAIL && user.isEmailVerified
  }

  private fun currentPeriodId(): String =
    SimpleDateFormat("yyyy-MM", Locale.US).format(Date())

  fun observeOfflineQueue(): Flow<List<OfflineCollectionEntity>> =
    localDb.offlineCollectionDao().observeAll()

  fun observeDeliveries(uid: String): Flow<List<WasteDelivery>> =
    (if (isBootstrapAdmin()) db.collection("waste_deliveries")
    else db.collection("waste_deliveries").whereArrayContains("viewerUids", uid))
      .snapshots()
      .map { it.toObjects(WasteDelivery::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
      .catch { e ->
        rethrowIfCancellation(e)
        handleFirestoreError(e, OperationType.LIST, "waste_deliveries")
        throw e
      }

  fun observeInventoryBatches(uid: String): Flow<List<InventoryBatch>> =
    (if (isBootstrapAdmin()) db.collection("inventory_batches")
    else db.collection("inventory_batches").whereArrayContains("viewerUids", uid))
      .snapshots()
      .map { it.toObjects(InventoryBatch::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
      .catch { e ->
        rethrowIfCancellation(e)
        handleFirestoreError(e, OperationType.LIST, "inventory_batches")
        throw e
      }

  fun observeCompostBatches(uid: String): Flow<List<CompostBatch>> =
    (if (isBootstrapAdmin()) db.collection("compost_batches")
    else db.collection("compost_batches").whereArrayContains("viewerUids", uid))
      .snapshots()
      .map { it.toObjects(CompostBatch::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
      .catch { e ->
        rethrowIfCancellation(e)
        handleFirestoreError(e, OperationType.LIST, "compost_batches")
        throw e
      }

  fun observeWasteSales(uid: String): Flow<List<WasteSale>> =
    (if (isBootstrapAdmin()) db.collection("waste_sales")
    else db.collection("waste_sales").whereArrayContains("viewerUids", uid))
      .snapshots()
      .map { it.toObjects(WasteSale::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
      .catch { e ->
        rethrowIfCancellation(e)
        handleFirestoreError(e, OperationType.LIST, "waste_sales")
        throw e
      }

  /**
   * Captures a field collection locally in Room with a provisional receipt and deterministic idempotency key.
   * Does NOT create final financial liabilities until synced and accepted.
   */
  suspend fun enqueueOfflineCollection(
    originType: OriginType,
    hubId: String,
    pickerUid: String,
    pickerName: String,
    householdId: String?,
    rate: PurchaseRate,
    grossKgMilli: Long,
    tareKgMilli: Long,
    deductionKgMilli: Long,
    evidenceUri: String?
  ): Result<OfflineCollectionEntity> {
    return try {
      val agentUid = requireUserId()
      val calc = PricingAndWeighingEngine.calculate(
        grossKgMilli = grossKgMilli,
        tareKgMilli = tareKgMilli,
        deductionKgMilli = deductionKgMilli,
        unitPriceUgxMinor = rate.purchasePriceUgxMinor
      ).getOrThrow()

      val idemKey = UUID.randomUUID().toString().replace("-", "")
      val receipt = "WH-PROV-${idemKey.take(6).uppercase()}"
      val entity = OfflineCollectionEntity(
        idempotencyKey = idemKey,
        originType = originType.name,
        hubId = hubId,
        pickerUid = pickerUid.ifBlank { agentUid },
        pickerName = pickerName.trim().ifBlank { "Walk-in Picker" }.take(80),
        householdId = householdId?.takeIf { it.isNotBlank() },
        agentUid = agentUid,
        materialId = rate.materialId,
        materialName = rate.materialName,
        grade = rate.grade,
        grossKgMilli = calc.grossKgMilli,
        tareKgMilli = calc.tareKgMilli,
        netKgMilli = calc.netKgMilli,
        deductionKgMilli = calc.deductionKgMilli,
        acceptedKgMilli = calc.acceptedKgMilli,
        rateIdSnapshot = rate.id.ifBlank { "rate_${rate.materialId}_v${rate.version}" },
        unitPriceUgxMinor = calc.unitPriceUgxMinor,
        totalEarningsUgxMinor = calc.totalEarningsUgxMinor,
        receiptRef = receipt,
        evidenceUri = evidenceUri,
        originalEventEpochMs = System.currentTimeMillis(),
        deviceId = "ANDROID_FIELD_DEVICE",
        syncStatus = SyncStatus.PENDING_SYNC.name
      )
      val insertedId = localDb.offlineCollectionDao().insertIfNew(entity)
      Result.success(entity.copy(localId = insertedId))
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  /**
   * Idempotently syncs Room queue items to Firestore using deterministic document ID `del_${idempotencyKey}`.
   * Prevents duplicate financial liabilities even across repeated mobile retries.
   */
  suspend fun syncPendingOfflineCollections(): Result<Int> {
    return try {
      val uid = requireUserId()
      val pending = localDb.offlineCollectionDao().getPendingForSync()
      var syncedCount = 0
      for (item in pending) {
        localDb.offlineCollectionDao().updateSyncStatus(item.localId, SyncStatus.SYNCING.name, null)
        val docId = "del_${item.idempotencyKey.take(40)}"
        val docRef = db.collection("waste_deliveries").document(docId)
        try {
          val existingDoc = docRef.get().await()
          if (existingDoc.exists()) {
            localDb.offlineCollectionDao().updateSyncStatus(item.localId, SyncStatus.CONFLICT_DUPLICATE.name, "Already synced on server")
            continue
          }
          val viewers = listOf(uid, item.pickerUid, item.agentUid).distinct()
          val payload = mapOf(
            "idempotencyKey" to item.idempotencyKey,
            "originType" to item.originType,
            "hubId" to item.hubId,
            "pickerUid" to item.pickerUid,
            "pickerName" to item.pickerName,
            "householdId" to item.householdId,
            "agentUid" to item.agentUid,
            "materialId" to item.materialId,
            "materialName" to item.materialName,
            "grade" to item.grade,
            "grossKgMilli" to item.grossKgMilli,
            "tareKgMilli" to item.tareKgMilli,
            "netKgMilli" to item.netKgMilli,
            "deductionKgMilli" to item.deductionKgMilli,
            "acceptedKgMilli" to item.acceptedKgMilli,
            "rateIdSnapshot" to item.rateIdSnapshot,
            "unitPriceUgxMinor" to item.unitPriceUgxMinor,
            "totalEarningsUgxMinor" to item.totalEarningsUgxMinor,
            "currency" to "UGX",
            "receiptRef" to item.receiptRef.replace("WH-PROV-", "WH-REC-"),
            "evidenceUri" to item.evidenceUri,
            "verificationStatus" to VerificationStatus.PENDING_VERIFICATION.name,
            "originalEventEpochMs" to item.originalEventEpochMs,
            "deviceId" to item.deviceId,
            "viewerUids" to viewers,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
          ).filterValues { it != null }
          docRef.set(payload).await()
          localDb.offlineCollectionDao().updateSyncStatus(item.localId, SyncStatus.SYNCED.name, null)
          syncedCount++
        } catch (e: CancellationException) {
          throw e
        } catch (e: Exception) {
          handleFirestoreError(e, OperationType.CREATE, "waste_deliveries/$docId")
          localDb.offlineCollectionDao().updateSyncStatus(item.localId, SyncStatus.SYNC_FAILED.name, e.localizedMessage)
        }
      }
      Result.success(syncedCount)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  /**
   * Atomically verifies a delivery, creates an InventoryBatch, creates a guaranteed PickerEarning,
   * and posts a balanced Double-Entry Journal (DR Inventory / CR Picker Earnings Payable).
   */
  suspend fun acceptDeliveryAtomically(delivery: WasteDelivery): Result<Unit> {
    val uid = requireUserId()
    val deliveryPath = "waste_deliveries/${delivery.id}"
    return try {
      require(delivery.id.isNotBlank()) { "Delivery ID missing." }
      require(delivery.verificationStatus != VerificationStatus.ACCEPTED.name) {
        "Delivery already accepted — duplicate processing blocked."
      }
      val batchId = "bat_${delivery.id}"
      val earningId = "earn_${delivery.id}"
      val entryId = "je_del_${delivery.id}"
      val viewers = (delivery.viewerUids + uid).distinct().take(15)

      val journal = DoubleEntryAccountingEngine.forDeliveryAcceptance(
        entryRef = "JE-${delivery.receiptRef.takeLast(6)}",
        periodId = currentPeriodId(),
        deliveryId = delivery.id,
        hubId = delivery.hubId,
        earningsUgx = delivery.totalEarningsUgxMinor,
        materialName = delivery.materialName,
        acceptedKgMilli = delivery.acceptedKgMilli
      ).getOrThrow()

      val batch = db.batch()
      batch.update(
        db.collection("waste_deliveries").document(delivery.id),
        mapOf(
          "verificationStatus" to VerificationStatus.ACCEPTED.name,
          "viewerUids" to viewers,
          "updatedAt" to FieldValue.serverTimestamp()
        )
      )
      batch.set(
        db.collection("inventory_batches").document(batchId),
        mapOf(
          "batchCode" to "BATCH-${delivery.receiptRef.takeLast(6)}",
          "hubId" to delivery.hubId,
          "materialId" to delivery.materialId,
          "materialName" to delivery.materialName,
          "grade" to delivery.grade,
          "sourceDeliveryId" to delivery.id,
          "receivedKgMilli" to delivery.acceptedKgMilli,
          "availableKgMilli" to delivery.acceptedKgMilli,
          "allocatedKgMilli" to 0L,
          "soldKgMilli" to 0L,
          "rejectedKgMilli" to 0L,
          "disposedKgMilli" to 0L,
          "unitCostUgxMinor" to delivery.unitPriceUgxMinor,
          "status" to "RECEIVED",
          "ownerUid" to uid,
          "viewerUids" to viewers,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp()
        )
      )
      batch.set(
        db.collection("picker_earnings").document(earningId),
        mapOf(
          "deliveryId" to delivery.id,
          "pickerUid" to delivery.pickerUid,
          "pickerName" to delivery.pickerName,
          "hubId" to delivery.hubId,
          "acceptedKgMilli" to delivery.acceptedKgMilli,
          "unitRateUgxMinor" to delivery.unitPriceUgxMinor,
          "amountUgxMinor" to delivery.totalEarningsUgxMinor,
          "currency" to "UGX",
          "settlementMethod" to SettlementMethod.DIRECT_MOMO.name,
          "payoutStatus" to PayoutStatus.UNSETTLED.name,
          "ownerUid" to uid,
          "viewerUids" to viewers,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp()
        )
      )
      batch.set(
        db.collection("journal_entries").document(entryId),
        mapOf(
          "entryRef" to journal.entryRef,
          "periodId" to journal.periodId,
          "eventType" to journal.eventType,
          "sourceId" to journal.sourceId,
          "idempotencyKey" to journal.idempotencyKey,
          "description" to journal.description,
          "totalDebitsUgxMinor" to journal.totalDebitsUgxMinor,
          "totalCreditsUgxMinor" to journal.totalCreditsUgxMinor,
          "currency" to journal.currency,
          "postedByUid" to uid,
          "status" to "POSTED",
          "viewerUids" to viewers,
          "createdAt" to FieldValue.serverTimestamp()
        )
      )
      journal.lines.forEachIndexed { idx, line ->
        batch.set(
          db.collection("journal_lines").document("${entryId}_$idx"),
          mapOf(
            "entryId" to entryId,
            "accountCode" to line.accountCode,
            "accountName" to line.accountName,
            "accountCategory" to line.accountCategory.name,
            "debitUgxMinor" to line.debitUgxMinor,
            "creditUgxMinor" to line.creditUgxMinor,
            "currency" to "UGX",
            "hubId" to line.hubId,
            "postedByUid" to uid,
            "viewerUids" to viewers,
            "createdAt" to FieldValue.serverTimestamp()
          )
        )
      }
      batch.set(
        db.collection("audit_logs").document("aud_${entryId}"),
        mapOf(
          "actorUid" to uid,
          "action" to "DELIVERY_ACCEPTED",
          "entityType" to "WASTE_DELIVERY",
          "entityId" to delivery.id,
          "summary" to "Accepted ${MoneyAndWeight.formatKg(delivery.acceptedKgMilli)} ${delivery.materialName}; guaranteed ${MoneyAndWeight.formatUgx(delivery.totalEarningsUgxMinor)}",
          "viewerUids" to viewers,
          "createdAt" to FieldValue.serverTimestamp()
        )
      )
      batch.commit().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, deliveryPath)
      Result.failure(e)
    }
  }

  /**
   * Records a Commercial Buyer Sale from available InventoryBatch, confirms weighbridge weight,
   * updates inventory (non-negative guard), computes UGX 30/kg Healthcare Obligation Accrued,
   * and posts balanced Double-Entry Journal (AR, Revenue, COGS, Inventory, Healthcare Accrued).
   */
  suspend fun createAndConfirmBuyerSale(
    batchItem: InventoryBatch,
    buyerName: String,
    dispatchedKgMilli: Long,
    confirmedSoldKgMilli: Long,
    rejectedKgMilli: Long,
    rejectionReason: String?,
    unitSellPriceUgxMinor: Long,
    healthcareUgxPerKg: Long
  ): Result<Unit> {
    val uid = requireUserId()
    val saleId = "sale_${UUID.randomUUID().toString().replace("-", "").take(16)}"
    return try {
      require(dispatchedKgMilli > 0L) { "Dispatched weight must be positive." }
      require(confirmedSoldKgMilli + rejectedKgMilli <= dispatchedKgMilli) {
        "Confirmed + rejected kg cannot exceed dispatched kg."
      }
      require(dispatchedKgMilli <= batchItem.availableKgMilli) {
        "Insufficient stock in ${batchItem.batchCode}: available ${MoneyAndWeight.formatKg(batchItem.availableKgMilli)}."
      }
      val viewers = (batchItem.viewerUids + uid).distinct().take(15)
      val remainingAvailable = batchItem.availableKgMilli - dispatchedKgMilli
      val newSold = batchItem.soldKgMilli + confirmedSoldKgMilli
      val newRejected = batchItem.rejectedKgMilli + rejectedKgMilli
      val newStatus = if (remainingAvailable == 0L) "SOLD_OUT" else "ALLOCATED"

      val invoiceUgx = MoneyAndWeight.calculateAmountUgx(confirmedSoldKgMilli, unitSellPriceUgxMinor)
      val cogsUgx = MoneyAndWeight.calculateAmountUgx(confirmedSoldKgMilli, batchItem.unitCostUgxMinor)
      val hcAccruedUgx = MoneyAndWeight.calculateAmountUgx(confirmedSoldKgMilli, healthcareUgxPerKg)

      val entryId = "je_$saleId"
      val journal = DoubleEntryAccountingEngine.forConfirmedSale(
        entryRef = "JE-${saleId.takeLast(6).uppercase()}",
        periodId = currentPeriodId(),
        saleId = saleId,
        hubId = batchItem.hubId,
        invoiceTotalUgx = invoiceUgx,
        inventoryCostUgx = cogsUgx,
        confirmedSoldKgMilli = confirmedSoldKgMilli,
        healthcareUgxPerKg = healthcareUgxPerKg,
        buyerName = buyerName.trim()
      ).getOrThrow()

      val batch = db.batch()
      batch.update(
        db.collection("inventory_batches").document(batchItem.id),
        mapOf(
          "availableKgMilli" to remainingAvailable,
          "soldKgMilli" to newSold,
          "rejectedKgMilli" to newRejected,
          "status" to newStatus,
          "updatedAt" to FieldValue.serverTimestamp()
        )
      )
      val saleStatus = if (confirmedSoldKgMilli > 0L) SaleStatus.CONFIRMED.name else SaleStatus.REJECTED.name
      batch.set(
        db.collection("waste_sales").document(saleId),
        mapOf(
          "saleRef" to "SL-${saleId.takeLast(6).uppercase()}",
          "buyerName" to buyerName.trim().take(100),
          "buyerOwnerUid" to uid,
          "hubId" to batchItem.hubId,
          "batchId" to batchItem.id,
          "materialId" to batchItem.materialId,
          "materialName" to batchItem.materialName,
          "grade" to batchItem.grade,
          "dispatchedKgMilli" to dispatchedKgMilli,
          "confirmedSoldKgMilli" to confirmedSoldKgMilli,
          "rejectedKgMilli" to rejectedKgMilli,
          "rejectionReason" to rejectionReason?.takeIf { it.isNotBlank() }?.take(300),
          "unitSellPriceUgxMinor" to unitSellPriceUgxMinor,
          "totalInvoiceUgxMinor" to invoiceUgx,
          "paidUgxMinor" to 0L,
          "currency" to "UGX",
          "healthcareRateUgxPerKg" to healthcareUgxPerKg,
          "healthcareAccruedUgxMinor" to hcAccruedUgx,
          "healthcareFundedUgxMinor" to 0L,
          "status" to saleStatus,
          "ownerUid" to uid,
          "viewerUids" to viewers,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null }
      )
      batch.set(
        db.collection("journal_entries").document(entryId),
        mapOf(
          "entryRef" to journal.entryRef,
          "periodId" to journal.periodId,
          "eventType" to journal.eventType,
          "sourceId" to journal.sourceId,
          "idempotencyKey" to journal.idempotencyKey,
          "description" to journal.description,
          "totalDebitsUgxMinor" to journal.totalDebitsUgxMinor,
          "totalCreditsUgxMinor" to journal.totalCreditsUgxMinor,
          "currency" to "UGX",
          "postedByUid" to uid,
          "status" to "POSTED",
          "viewerUids" to viewers,
          "createdAt" to FieldValue.serverTimestamp()
        )
      )
      journal.lines.forEachIndexed { idx, line ->
        batch.set(
          db.collection("journal_lines").document("${entryId}_$idx"),
          mapOf(
            "entryId" to entryId,
            "accountCode" to line.accountCode,
            "accountName" to line.accountName,
            "accountCategory" to line.accountCategory.name,
            "debitUgxMinor" to line.debitUgxMinor,
            "creditUgxMinor" to line.creditUgxMinor,
            "currency" to "UGX",
            "hubId" to line.hubId,
            "postedByUid" to uid,
            "viewerUids" to viewers,
            "createdAt" to FieldValue.serverTimestamp()
          )
        )
      }
      batch.commit().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, "waste_sales/$saleId")
      Result.failure(e)
    }
  }

  /**
   * Records Buyer Cash Receipt (partial or full) and transfers proportional UGX 30/kg share
   * from Operating Cash into Restricted Healthcare Cash Fund.
   */
  suspend fun recordBuyerCashReceipt(sale: WasteSale, cashAmountUgx: Long): Result<Unit> {
    val uid = requireUserId()
    val path = "waste_sales/${sale.id}"
    return try {
      require(cashAmountUgx > 0L) { "Receipt amount must be positive." }
      val outstanding = sale.totalInvoiceUgxMinor - sale.paidUgxMinor
      require(cashAmountUgx <= outstanding) {
        "Payment exceeds outstanding receivable (${MoneyAndWeight.formatUgx(outstanding)})."
      }
      val newPaid = sale.paidUgxMinor + cashAmountUgx
      // Proportional healthcare cash funding capped at total accrued
      val targetFunded = if (sale.totalInvoiceUgxMinor > 0L) {
        (sale.healthcareAccruedUgxMinor * newPaid) / sale.totalInvoiceUgxMinor
      } else 0L
      val fundedDelta = (targetFunded - sale.healthcareFundedUgxMinor).coerceAtLeast(0L)
      val newFunded = sale.healthcareFundedUgxMinor + fundedDelta
      val newStatus = if (newPaid >= sale.totalInvoiceUgxMinor) SaleStatus.PAID.name else SaleStatus.PARTIALLY_PAID.name

      val seq = UUID.randomUUID().toString().replace("-", "").take(6)
      val entryId = "je_rcpt_${sale.id}_$seq"
      val viewers = (sale.viewerUids + uid).distinct().take(15)
      val journal = DoubleEntryAccountingEngine.forBuyerCashReceipt(
        entryRef = "JE-RC-${seq.uppercase()}",
        periodId = currentPeriodId(),
        saleId = sale.id,
        receiptSeq = seq,
        hubId = sale.hubId,
        cashReceivedUgx = cashAmountUgx,
        healthcareFundedDeltaUgx = fundedDelta,
        buyerName = sale.buyerName
      ).getOrThrow()

      val batch = db.batch()
      batch.update(
        db.collection("waste_sales").document(sale.id),
        mapOf(
          "paidUgxMinor" to newPaid,
          "healthcareFundedUgxMinor" to newFunded,
          "status" to newStatus,
          "updatedAt" to FieldValue.serverTimestamp()
        )
      )
      batch.set(
        db.collection("journal_entries").document(entryId),
        mapOf(
          "entryRef" to journal.entryRef,
          "periodId" to journal.periodId,
          "eventType" to journal.eventType,
          "sourceId" to journal.sourceId,
          "idempotencyKey" to journal.idempotencyKey,
          "description" to journal.description,
          "totalDebitsUgxMinor" to journal.totalDebitsUgxMinor,
          "totalCreditsUgxMinor" to journal.totalCreditsUgxMinor,
          "currency" to "UGX",
          "postedByUid" to uid,
          "status" to "POSTED",
          "viewerUids" to viewers,
          "createdAt" to FieldValue.serverTimestamp()
        )
      )
      journal.lines.forEachIndexed { idx, line ->
        batch.set(
          db.collection("journal_lines").document("${entryId}_$idx"),
          mapOf(
            "entryId" to entryId,
            "accountCode" to line.accountCode,
            "accountName" to line.accountName,
            "accountCategory" to line.accountCategory.name,
            "debitUgxMinor" to line.debitUgxMinor,
            "creditUgxMinor" to line.creditUgxMinor,
            "currency" to "UGX",
            "hubId" to line.hubId,
            "postedByUid" to uid,
            "viewerUids" to viewers,
            "createdAt" to FieldValue.serverTimestamp()
          )
        )
      }
      batch.commit().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, path)
      Result.failure(e)
    }
  }

  suspend fun createCompostBatch(
    hubId: String,
    inputKgMilli: Long,
    composition: String,
    processingCostUgx: Long
  ): Result<Unit> {
    val uid = requireUserId()
    val batchId = "cmp_${UUID.randomUUID().toString().replace("-", "").take(12)}"
    return try {
      require(inputKgMilli > 0L) { "Organic input weight must be positive." }
      db.collection("compost_batches").document(batchId).set(
        mapOf(
          "batchCode" to "ORG-${batchId.takeLast(6).uppercase()}",
          "hubId" to hubId,
          "inputKgMilli" to inputKgMilli,
          "materialComposition" to composition.trim().take(200),
          "moisturePctMilli" to 55000L,
          "temperatureCelsiusMilli" to 58000L,
          "turningCount" to 1L,
          "rejectedKgMilli" to 0L,
          "lossKgMilli" to 0L,
          "outputCompostKgMilli" to 0L,
          "soldCompostKgMilli" to 0L,
          "processingCostUgxMinor" to processingCostUgx,
          "status" to CompostStatus.ACTIVE.name,
          "operatorUid" to uid,
          "viewerUids" to listOf(uid),
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, "compost_batches/$batchId")
      Result.failure(e)
    }
  }

  suspend fun logCompostTurnAndOutput(
    batchItem: CompostBatch,
    moisturePctMilli: Long,
    tempCelsiusMilli: Long,
    outputCompostKgMilli: Long,
    rejectedKgMilli: Long,
    approveFinished: Boolean
  ): Result<Unit> {
    val uid = requireUserId()
    val path = "compost_batches/${batchItem.id}"
    return try {
      require(outputCompostKgMilli + rejectedKgMilli <= batchItem.inputKgMilli) {
        "Output + rejected kg cannot exceed input kg."
      }
      val lossKg = (batchItem.inputKgMilli - outputCompostKgMilli - rejectedKgMilli).coerceAtLeast(0L)
      val nextStatus = if (approveFinished && outputCompostKgMilli > 0L) {
        CompostStatus.APPROVED.name
      } else {
        CompostStatus.CURING.name
      }
      db.collection("compost_batches").document(batchItem.id).update(
        mapOf(
          "moisturePctMilli" to moisturePctMilli,
          "temperatureCelsiusMilli" to tempCelsiusMilli,
          "turningCount" to batchItem.turningCount + 1L,
          "rejectedKgMilli" to rejectedKgMilli,
          "lossKgMilli" to lossKg,
          "outputCompostKgMilli" to outputCompostKgMilli,
          "status" to nextStatus,
          "approvedByUid" to (if (approveFinished) uid else batchItem.approvedByUid),
          "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null }
      ).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, path)
      Result.failure(e)
    }
  }

  private fun rethrowIfCancellation(error: Throwable) {
    val firestoreErr = generateSequence(error) { it.cause }
      .filterIsInstance<FirebaseFirestoreException>()
      .firstOrNull()
    if (firestoreErr == null && error is CancellationException) throw error
  }
}
