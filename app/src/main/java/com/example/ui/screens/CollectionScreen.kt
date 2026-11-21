package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
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
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ErrorContainerLight
import com.example.ui.theme.ErrorCrimson
import com.example.ui.theme.IceBlueBorder
import com.example.ui.theme.IceBlueSurface
import com.example.ui.theme.IndustrialCyanText
import com.example.ui.theme.IndustrialSlateCard
import com.example.ui.theme.IndustrialSlateDark
import com.example.ui.theme.ScaleDigitalDisplayStyle
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TabularCompactStyle
import com.example.ui.theme.TabularMoneyStyle
import com.example.ui.theme.TelemetryAmber
import com.example.ui.theme.TelemetryObsidian
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
  val scale = snap.scales.firstOrNull()
  val activeHub = snap.hubs.firstOrNull { it.id == selectedHubId } ?: snap.hubs.firstOrNull()

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
        subtitle = "Offline-first Room engine with immutable rate snapshots & idempotent sync"
      )
    }

    // 1. Dark Obsidian Local Telemetry & Scale Status Bar (Screenshot 1 Top Bar)
    item {
      Surface(
        color = TelemetryObsidian,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier.padding(12.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(7.dp),
              modifier = Modifier.weight(1f)
            ) {
              Box(
                modifier = Modifier
                  .size(8.dp)
                  .clip(CircleShape)
                  .background(IndustrialCyanText)
              )
              Text(
                text = "ROOM LOCAL ENGINE ACTIVE",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
            Surface(
              color = Color(0xFF4D3812),
              shape = RoundedCornerShape(8.dp)
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
              ) {
                Icon(
                  imageVector = Icons.Filled.Sync,
                  contentDescription = null,
                  tint = TelemetryAmber,
                  modifier = Modifier.size(13.dp)
                )
                Text(
                  text = "$pendingQueueCount Buffered",
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                  color = TelemetryAmber
                )
              }
            }
          }

          Surface(
            color = Color(0xFF1D1507),
            shape = RoundedCornerShape(9.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 7.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f)
              ) {
                Icon(
                  imageVector = Icons.Filled.Scale,
                  contentDescription = "Scale Calibration",
                  tint = TelemetryAmber,
                  modifier = Modifier.size(15.dp)
                )
                Text(
                  text = scale?.let { "${it.scaleId} • ${it.status}" } ?: "SCALE-NAK-01 • CALIBRATED",
                  style = TabularCompactStyle,
                  color = TelemetryAmber,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
              StatusPill(
                text = "MAX ${MoneyAndWeight.formatKg(scale?.maxCapacityKgMilli ?: 500_000L)}",
                containerColor = Color(0xFF0E3924),
                contentColor = Color(0xFF7ADBA5),
                showDot = true
              )
            }
          }
        }
      }
    }

    // 2. Collector & Collection Origin Card (Screenshot 1 Collector Block)
    item {
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
      ) {
        Column(
          modifier = Modifier.padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("Verified Collector & Origin", style = MaterialTheme.typography.titleMedium)
            StatusPill(
              text = "PROVISIONAL",
              containerColor = IceBlueSurface,
              contentColor = MaterialTheme.colorScheme.primary
            )
          }

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
        }
      }
    }

    // 3. Industrial Scale Console Card (Screenshot 1 Digital LCD + 2-Col Material Grid + Snapshot Rate Lock)
    item {
      val calcSuccess = liveCalcResult?.getOrNull()
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Icon(Icons.Filled.Scale, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
              Text("Industrial Scale Console", style = MaterialTheme.typography.titleLarge)
            }
            StatusPill(
              text = "LIVE CALIBRATED",
              containerColor = IceBlueSurface,
              contentColor = SuccessGreen,
              showDot = true
            )
          }

          // Dark Midnight Slate LCD Scale Display
          Surface(
            color = IndustrialSlateDark,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(
              modifier = Modifier.padding(14.dp),
              verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "CHANNEL: ${scale?.scaleId ?: "CH-01"} (LOADCELL)",
                  style = MaterialTheme.typography.labelSmall,
                  color = Color.White.copy(alpha = 0.7f)
                )
                Text(
                  text = if (calcSuccess != null) "STABLE (±0.02KG)" else "CHECK INPUT",
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                  color = if (calcSuccess != null) IndustrialCyanText else TelemetryAmber
                )
              }

              Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
              ) {
                Text(
                  text = "CALCULATED NET PAY-WEIGHT",
                  style = MaterialTheme.typography.labelSmall,
                  color = Color.White.copy(alpha = 0.72f)
                )
                Row(
                  verticalAlignment = Alignment.Bottom,
                  horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  Text(
                    text = calcSuccess?.let {
                      MoneyAndWeight.milliKgToBigDecimal(it.acceptedKgMilli).setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()
                    } ?: "--.--",
                    style = ScaleDigitalDisplayStyle,
                    color = Color.White
                  )
                  Text(
                    text = "KG",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = IndustrialCyanText,
                    modifier = Modifier.padding(bottom = 5.dp)
                  )
                }
              }

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Surface(
                  color = IndustrialSlateCard,
                  shape = RoundedCornerShape(10.dp),
                  modifier = Modifier.weight(1f)
                ) {
                  Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                      text = "GROSS MEASURED",
                      style = MaterialTheme.typography.labelSmall,
                      color = Color.White.copy(alpha = 0.68f)
                    )
                    Text(
                      text = if (grossMilli >= 0L) MoneyAndWeight.formatKg(grossMilli) else "Invalid",
                      style = TabularMoneyStyle,
                      color = Color.White
                    )
                  }
                }
                Surface(
                  color = IndustrialSlateCard,
                  shape = RoundedCornerShape(10.dp),
                  modifier = Modifier.weight(1f)
                ) {
                  Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Text(
                        text = "TARE + DEDUCT",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.68f)
                      )
                      Surface(color = Color(0xFF4D3812), shape = RoundedCornerShape(4.dp)) {
                        Text(
                          text = "BIN",
                          style = MaterialTheme.typography.labelSmall,
                          color = TelemetryAmber,
                          modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                      }
                    }
                    Text(
                      text = if (tareMilli >= 0L && deductionMilli >= 0L) {
                        MoneyAndWeight.formatKg(tareMilli + deductionMilli)
                      } else "Invalid",
                      style = TabularMoneyStyle,
                      color = TelemetryAmber
                    )
                  }
                }
              }
            }
          }

          // Manual Weight Entry Inputs (keeps exact testTags intact)
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

          // Quick Moisture / Contamination Presets
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("Moisture / Contamination Preset", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              listOf(0L to "Clean 0%", 5L to "Sand 5%", 15L to "Wet 15%").forEach { (pct, label) ->
                val netBase = (grossMilli - tareMilli).coerceAtLeast(0L)
                val targetDeductMilli = (netBase * pct) / 100L
                val isSelected = kotlin.math.abs((deductionMilli.coerceAtLeast(0L)) - targetDeductMilli) <= 10L
                Surface(
                  color = if (isSelected) EmeraldPrimary else IceBlueSurface,
                  shape = RoundedCornerShape(8.dp),
                  modifier = Modifier.clickable {
                    deductionText = MoneyAndWeight.milliKgToBigDecimal(targetDeductMilli).toPlainString()
                  }
                ) {
                  Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                  )
                }
              }
            }
          }

          // 2-Column Material Classification Grid (Screenshot 1)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "CLASSIFICATION CATEGORY",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
              text = "${activeHub?.name ?: "Kampala"} Pricebook",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.primary
            )
          }

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
            val rateRows = snap.rates.chunked(2)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              rateRows.forEach { pair ->
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  pair.forEach { rate ->
                    val selected = activeRate?.id == rate.id
                    Surface(
                      color = if (selected) EmeraldPrimary else MaterialTheme.colorScheme.surface,
                      shape = RoundedCornerShape(12.dp),
                      border = BorderStroke(
                        width = if (selected) 1.5.dp else 1.dp,
                        color = if (selected) EmeraldPrimary else MaterialTheme.colorScheme.outline
                      ),
                      modifier = Modifier
                        .weight(1f)
                        .clickable { selectedRateId = rate.id }
                        .testTag("rate_chip_${rate.materialId}")
                    ) {
                      Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                      ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                          Text(
                            text = "${rate.materialName} (${rate.grade})",
                            style = MaterialTheme.typography.titleSmall,
                            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                          )
                          Text(
                            text = "${MoneyAndWeight.formatUgx(rate.purchasePriceUgxMinor)} / kg",
                            style = TabularCompactStyle,
                            color = if (selected) Color.White.copy(alpha = 0.88f) else MaterialTheme.colorScheme.onSurfaceVariant
                          )
                        }
                        Icon(
                          imageVector = if (selected) Icons.Filled.RadioButtonChecked else Icons.Filled.RadioButtonUnchecked,
                          contentDescription = if (selected) "Selected" else "Select",
                          tint = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                          modifier = Modifier.size(18.dp)
                        )
                      }
                    }
                  }
                  if (pair.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                  }
                }
              }
            }
          }

          // Snapshot Contract Rate Lock Box
          if (activeRate != null) {
            Surface(
              color = IceBlueSurface,
              shape = RoundedCornerShape(12.dp),
              border = BorderStroke(1.dp, IceBlueBorder),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(10.dp),
                  modifier = Modifier.weight(1f)
                ) {
                  Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                  )
                  Column {
                    Text(
                      text = "SNAPSHOT CONTRACT RATE (v${activeRate.version})",
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                      text = "${activeRate.materialName} (Grade ${activeRate.grade})",
                      style = MaterialTheme.typography.titleSmall
                    )
                  }
                }
                Text(
                  text = "${MoneyAndWeight.formatUgx(activeRate.purchasePriceUgxMinor)} / kg",
                  style = TabularMoneyStyle,
                  color = MaterialTheme.colorScheme.primary
                )
              }
            }
          }

          liveCalcResult?.onFailure { err ->
            Text(
              text = err.localizedMessage ?: "Invalid weights",
              style = MaterialTheme.typography.bodySmall,
              color = ErrorCrimson
            )
          }
        }
      }
    }

    // 4. Instant Acceptance Ledger Breakdown Card (Screenshot 1)
    liveCalcResult?.getOrNull()?.let { calc ->
      if (activeRate != null) {
        val hcAccrualUgx = MoneyAndWeight.calculateAmountUgx(calc.acceptedKgMilli, snap.policy.healthcareUgxPerKg)
        item {
          Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
          ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text("Instant Acceptance Ledger Breakdown", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                StatusPill(
                  text = "DUAL LEDGER",
                  containerColor = IceBlueSurface,
                  contentColor = MaterialTheme.colorScheme.primary
                )
              }

              Surface(
                color = IceBlueSurface,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier.padding(12.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Filled.Payments, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column {
                      Text("Picker Guaranteed Cash", style = MaterialTheme.typography.titleSmall)
                      Text(
                        "${MoneyAndWeight.formatKg(calc.acceptedKgMilli)} × ${MoneyAndWeight.formatUgx(activeRate.purchasePriceUgxMinor)}",
                        style = TabularCompactStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                      )
                    }
                  }
                  Text(
                    text = MoneyAndWeight.formatUgx(calc.totalEarningsUgxMinor),
                    style = TabularMoneyStyle,
                    color = MaterialTheme.colorScheme.primary
                  )
                }
              }

              Surface(
                color = IceBlueSurface,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier.padding(12.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Filled.LocalHospital, contentDescription = null, tint = SuccessGreen)
                    Column {
                      Text("Healthcare Reserve Accrual", style = MaterialTheme.typography.titleSmall)
                      Text(
                        "${MoneyAndWeight.formatKg(calc.acceptedKgMilli)} × UGX ${snap.policy.healthcareUgxPerKg}/kg",
                        style = TabularCompactStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                      )
                    }
                  }
                  Text(
                    text = "+${MoneyAndWeight.formatUgx(hcAccrualUgx)}",
                    style = TabularMoneyStyle,
                    color = SuccessGreen
                  )
                }
              }

              Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.Star, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(16.dp))
                    Text("Monthly Bonus Pool Eligibility", style = MaterialTheme.typography.bodySmall)
                  }
                  Text(
                    text = "${snap.policy.pickerRewardPct}% Surplus Tier",
                    style = TabularCompactStyle.copy(fontWeight = FontWeight.Bold),
                    color = WarningAmber
                  )
                }
              }
            }
          }
        }
      }
    }

    // 5. Settlement Dispatch Pipeline & Primary Actions (Screenshot 1)
    item {
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = "SETTLEMENT DISPATCH PIPELINE",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.5.dp, EmeraldPrimary),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Icon(Icons.Filled.RadioButtonChecked, contentDescription = null, tint = EmeraldPrimary)
              Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  Text("Direct MTN / Airtel Mobile Money", style = MaterialTheme.typography.titleSmall)
                  StatusPill("DEC-001", EmeraldPrimary, Color.White)
                }
                Text(
                  text = "Authorized via Maker-Checker direct MoMo sandbox disbursement",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
              Icon(Icons.Filled.Bolt, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }

          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Icon(
                Icons.Filled.RadioButtonUnchecked,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
              )
              Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  Text(
                    text = "WasteHeals Stored-Value Wallet",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                  StatusPill("DEC-002", ErrorContainerLight, ErrorCrimson)
                }
                Text(
                  text = "Pending Bank of Uganda (BoU) Sandbox Clearance (walletEnabled=${snap.policy.walletEnabled})",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
              Icon(Icons.Filled.AccountBalanceWallet, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
            }
          }
        }
      }
    }

    // 6. Primary Forest Emerald Action Button + Secondary Photo/Tare/Sync Controls
    item {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
          colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.White),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .testTag("save_offline_collection_button")
        ) {
          Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Accept & Save Provisional Receipt",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedButton(
            onClick = {
              tareText = "0.000"
              deductionText = "0.000"
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f)
          ) {
            Icon(Icons.Filled.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Re-Tare / Zero")
          }
          OutlinedButton(
            onClick = {
              photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
              )
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .weight(1f)
              .testTag("attach_evidence_button")
          ) {
            Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(if (selectedEvidenceUri != null) "Photo Attached" else "Attach Photo")
          }
        }
      }
    }

    // 7. Local Hub Journal & Room Offline Queue (Screenshot 1 bottom list)
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
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
              modifier = Modifier.weight(1f)
            ) {
              Box(
                modifier = Modifier
                  .size(8.dp)
                  .clip(CircleShape)
                  .background(if (synced) SuccessGreen else WarningAmber)
              )
              Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("${q.receiptRef} • ${q.materialName} (${q.grade})", style = MaterialTheme.typography.titleSmall)
                Text(
                  text = "${q.pickerName} • Accepted: ${MoneyAndWeight.formatKg(q.acceptedKgMilli)}",
                  style = TabularCompactStyle,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!q.syncError.isNullOrBlank()) {
                  Text(q.syncError, style = MaterialTheme.typography.labelSmall, color = ErrorCrimson)
                }
              }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text(
                text = MoneyAndWeight.formatUgx(q.totalEarningsUgxMinor),
                style = TabularCompactStyle.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
              )
              StatusPill(
                text = q.syncStatus,
                containerColor = if (synced) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = if (synced) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onTertiaryContainer
              )
            }
          }
        }
      }
    }

    // 8. Cloud Deliveries & Official Receipts
    item {
      SectionHeader(
        title = "Local Hub Journal & Synced Deliveries (${snap.deliveries.size})",
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
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
              modifier = Modifier.weight(1f)
            ) {
              Icon(
                imageVector = if (accepted) Icons.Filled.CheckCircle else Icons.Filled.History,
                contentDescription = null,
                tint = if (accepted) SuccessGreen else WarningAmber,
                modifier = Modifier.size(18.dp)
              )
              Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
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
