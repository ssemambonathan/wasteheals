package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.domain.engine.MoneyAndWeight
import com.example.domain.engine.PricingAndWeighingEngine
import com.example.domain.model.OriginType
import com.example.domain.model.SyncStatus
import com.example.domain.model.VerificationStatus
import com.example.domain.model.WasteDelivery
import com.example.ui.components.DigitalReceiptDialog
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.state.WasteHealsSnapshot
import com.example.ui.theme.ErrorCrimson
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TabularCompactStyle
import com.example.ui.theme.TabularMoneyStyle
import com.example.ui.theme.WarningAmber

@Composable
fun CollectionScreen(
  snap: WasteHealsSnapshot,
  isBusy: Boolean,
  onSaveOfflineCollection: (
    originType: OriginType,
    hubId: String,
    pickerName: String,
    householdId: String?,
    rate: com.example.domain.model.PurchaseRate,
    grossKgMilli: Long,
    tareKgMilli: Long,
    deductionKgMilli: Long,
    evidenceUri: String?
  ) -> Unit,
  onSyncOfflineQueue: () -> Unit
) {
  var selectedOrigin by remember { mutableStateOf(OriginType.PICKER_HUB) }
  var selectedHubId by remember(snap.hubs) { mutableStateOf(snap.hubs.firstOrNull()?.id ?: "hub_nakawa") }
  var selectedRateId by remember(snap.rates) { mutableStateOf(snap.rates.firstOrNull()?.id ?: "") }
  var pickerName by remember { mutableStateOf(snap.profile?.displayName ?: "Kampala Picker") }
  var householdRef by remember { mutableStateOf("") }
  var grossText by remember { mutableStateOf("25.000") }
  var tareText by remember { mutableStateOf("1.500") }
  var deductionText by remember { mutableStateOf("0.500") }
  var selectedEvidenceUri by remember { mutableStateOf<Uri?>(null) }
  var receiptDelivery by remember { mutableStateOf<WasteDelivery?>(null) }

  // Zero-permission Android Photo Picker (Play Policy compliant)
  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri ->
    if (uri != null) selectedEvidenceUri = uri
  }

  val activeRate = remember(snap.rates, selectedRateId) {
    snap.rates.firstOrNull { it.id == selectedRateId } ?: snap.rates.firstOrNull()
  }

  val grossMilli = remember(grossText) { MoneyAndWeight.parseKgToMilli(grossText) ?: -1L }
  val tareMilli = remember(tareText) { MoneyAndWeight.parseKgToMilli(tareText) ?: -1L }
  val deductionMilli = remember(deductionText) { MoneyAndWeight.parseKgToMilli(deductionText) ?: -1L }

  val liveCalcResult = remember(grossMilli, tareMilli, deductionMilli, activeRate) {
    if (activeRate == null) null
    else PricingAndWeighingEngine.calculate(
      grossKgMilli = grossMilli,
      tareKgMilli = tareMilli,
      deductionKgMilli = deductionMilli,
      unitPriceUgxMinor = activeRate.purchasePriceUgxMinor
    )
  }

  val pendingQueueCount = snap.offlineQueue.count {
    it.syncStatus == SyncStatus.PENDING_SYNC.name || it.syncStatus == SyncStatus.SYNC_FAILED.name
  }

  receiptDelivery?.let { del ->
    DigitalReceiptDialog(delivery = del, onDismiss = { receiptDelivery = null })
  }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    item {
      SectionHeader(
        title = "Field Collection & Weighing",
        subtitle = "Offline-first Room queue with immutable rate snapshots & idempotent sync"
      )
    }

    // Scale Calibration Banner
    item {
      val scale = snap.scales.firstOrNull()
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
          ) {
            Icon(Icons.Filled.Scale, contentDescription = "Scale Calibration", tint = MaterialTheme.colorScheme.primary)
            Column {
              Text(
                text = scale?.let { "${it.scaleId} • ${it.status}" } ?: "SCALE-NAK-01 • CALIBRATED",
                style = MaterialTheme.typography.titleSmall
              )
              Text(
                text = "Max Cap: ${MoneyAndWeight.formatKg(scale?.maxCapacityKgMilli ?: 500_000L)} • Hub verified before ledger posting",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
          StatusPill(
            text = "PROVISIONAL",
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
          )
        }
      }
    }

    // Collection Entry Form
    item {
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Text("1. Collection Origin", style = MaterialTheme.typography.titleSmall)
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            OriginType.entries.forEach { origin ->
              FilterChip(
                selected = selectedOrigin == origin,
                onClick = { selectedOrigin = origin },
                label = { Text(origin.label) },
                modifier = Modifier.testTag("origin_chip_${origin.name}")
              )
            }
          }

          if (snap.hubs.isNotEmpty()) {
            Text("2. Collection Hub", style = MaterialTheme.typography.titleSmall)
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              snap.hubs.forEach { hub ->
                FilterChip(
                  selected = selectedHubId == hub.id,
                  onClick = { selectedHubId = hub.id },
                  label = { Text(hub.name) }
                )
              }
            }
          }

          Text("3. Material & Approved Purchase Rate", style = MaterialTheme.typography.titleSmall)
          if (snap.rates.isEmpty()) {
            Surface(
              color = MaterialTheme.colorScheme.errorContainer,
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Text(
                text = "No approved material purchase rates published yet. Sign in as admin to initialize Kampala hub rates.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(12.dp)
              )
            }
          } else {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              snap.rates.forEach { rate ->
                FilterChip(
                  selected = activeRate?.id == rate.id,
                  onClick = { selectedRateId = rate.id },
                  label = {
                    Text("${rate.materialName} (${rate.grade}) • ${MoneyAndWeight.formatUgx(rate.purchasePriceUgxMinor)}/kg")
                  },
                  modifier = Modifier.testTag("rate_chip_${rate.materialId}")
                )
              }
            }
          }

          OutlinedTextField(
            value = pickerName,
            onValueChange = { pickerName = it },
            label = { Text("Picker / Collector Name") },
            singleLine = true,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("collection_picker_name_input")
          )

          if (selectedOrigin == OriginType.HOUSEHOLD_AGENT) {
            OutlinedTextField(
              value = householdRef,
              onValueChange = { householdRef = it },
              label = { Text("Household Reference (optional)") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth()
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            OutlinedTextField(
              value = grossText,
              onValueChange = { grossText = it },
              label = { Text("Gross (kg)") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
              singleLine = true,
              modifier = Modifier
                .weight(1f)
                .testTag("gross_kg_input")
            )
            OutlinedTextField(
              value = tareText,
              onValueChange = { tareText = it },
              label = { Text("Tare (kg)") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
              singleLine = true,
              modifier = Modifier
                .weight(1f)
                .testTag("tare_kg_input")
            )
            OutlinedTextField(
              value = deductionText,
              onValueChange = { deductionText = it },
              label = { Text("Deduct (kg)") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
              singleLine = true,
              modifier = Modifier
                .weight(1f)
                .testTag("deduction_kg_input")
            )
          }

          // Live BigDecimal Calculation Preview
          liveCalcResult?.fold(
            onSuccess = { calc ->
              Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                  Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Net Weight", style = MaterialTheme.typography.bodySmall)
                    Text(MoneyAndWeight.formatKg(calc.netKgMilli), style = TabularCompactStyle)
                  }
                  Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Accepted Weight (after moisture/contamination)", style = MaterialTheme.typography.bodySmall)
                    Text(MoneyAndWeight.formatKg(calc.acceptedKgMilli), style = TabularCompactStyle)
                  }
                  HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                  Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Guaranteed Picker Earning", style = MaterialTheme.typography.titleSmall)
                    Text(
                      text = MoneyAndWeight.formatUgx(calc.totalEarningsUgxMinor),
                      style = TabularMoneyStyle,
                      color = MaterialTheme.colorScheme.primary
                    )
                  }
                }
              }
            },
            onFailure = { err ->
              Text(
                text = err.localizedMessage ?: "Invalid weights",
                style = MaterialTheme.typography.bodySmall,
                color = ErrorCrimson
              )
            }
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            OutlinedButton(
              onClick = {
                photoPickerLauncher.launch(
                  PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
              },
              modifier = Modifier
                .weight(1f)
                .testTag("attach_evidence_button")
            ) {
              Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(if (selectedEvidenceUri != null) "Photo Attached" else "Attach Photo")
            }

            Button(
              onClick = {
                val r = activeRate ?: return@Button
                onSaveOfflineCollection(
                  selectedOrigin,
                  selectedHubId,
                  pickerName,
                  householdRef.takeIf { it.isNotBlank() },
                  r,
                  grossMilli,
                  tareMilli,
                  deductionMilli,
                  selectedEvidenceUri?.toString()
                )
              },
              enabled = !isBusy && activeRate != null && liveCalcResult?.isSuccess == true,
              modifier = Modifier
                .weight(1.3f)
                .testTag("save_offline_collection_button")
            ) {
              Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Save Provisional")
            }
          }
        }
      }
    }

    // Room Offline Queue
    item {
      SectionHeader(
        title = "Offline Room Queue (${snap.offlineQueue.size})",
        subtitle = "$pendingQueueCount pending sync • Safe against duplicate retries",
        actionLabel = if (pendingQueueCount > 0) "Sync Now" else null,
        onAction = if (pendingQueueCount > 0 && !isBusy) onSyncOfflineQueue else null
      )
    }

    if (snap.offlineQueue.isEmpty()) {
      item {
        EmptyStateCard(
          icon = Icons.Filled.CloudQueue,
          title = "Offline Queue Clear",
          description = "Field collections saved offline appear here with provisional receipts until synced."
        )
      }
    } else {
      item {
        if (pendingQueueCount > 0) {
          Button(
            onClick = onSyncOfflineQueue,
            enabled = !isBusy,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("sync_offline_queue_button")
          ) {
            Icon(Icons.Filled.Sync, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Idempotently Sync $pendingQueueCount Event(s) to Firestore")
          }
        }
      }
      items(snap.offlineQueue, key = { it.localId }) { q ->
        val synced = q.syncStatus == SyncStatus.SYNCED.name || q.syncStatus == SyncStatus.CONFLICT_DUPLICATE.name
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surface,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
              Text("${q.receiptRef} • ${q.materialName} (${q.grade})", style = MaterialTheme.typography.titleSmall)
              Text(
                text = "${q.pickerName} • Accepted: ${MoneyAndWeight.formatKg(q.acceptedKgMilli)} • ${MoneyAndWeight.formatUgx(q.totalEarningsUgxMinor)}",
                style = TabularCompactStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              if (!q.syncError.isNullOrBlank()) {
                Text(q.syncError, style = MaterialTheme.typography.labelSmall, color = ErrorCrimson)
              }
            }
            StatusPill(
              text = q.syncStatus,
              containerColor = if (synced) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.tertiaryContainer,
              contentColor = if (synced) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onTertiaryContainer
            )
          }
        }
      }
    }

    // Cloud Deliveries & Official Receipts
    item {
      SectionHeader(
        title = "Synced Field Deliveries (${snap.deliveries.size})",
        subtitle = "Official receipts & verification state"
      )
    }

    if (snap.deliveries.isEmpty()) {
      item {
        EmptyStateCard(
          icon = Icons.Filled.CloudDone,
          title = "No Synced Deliveries Yet",
          description = "Save a provisional collection and tap Sync to transmit it to hub verification."
        )
      }
    } else {
      items(snap.deliveries, key = { it.id }) { del ->
        val accepted = del.verificationStatus == VerificationStatus.ACCEPTED.name
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surface,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(del.receiptRef, style = MaterialTheme.typography.titleSmall)
                StatusPill(
                  text = del.verificationStatus,
                  containerColor = if (accepted) SuccessGreen.copy(alpha = 0.15f) else WarningAmber.copy(alpha = 0.18f),
                  contentColor = if (accepted) SuccessGreen else WarningAmber
                )
              }
              Text(
                text = "${del.materialName} • ${MoneyAndWeight.formatKg(del.acceptedKgMilli)} • ${MoneyAndWeight.formatUgx(del.totalEarningsUgxMinor)}",
                style = TabularCompactStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            OutlinedButton(
              onClick = { receiptDelivery = del },
              modifier = Modifier.testTag("view_receipt_${del.id}")
            ) {
              Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Receipt")
            }
          }
        }
      }
    }
  }
}
