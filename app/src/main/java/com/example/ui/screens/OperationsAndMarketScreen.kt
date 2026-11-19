package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Compost
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.domain.engine.MoneyAndWeight
import com.example.domain.model.CompostBatch
import com.example.domain.model.CompostStatus
import com.example.domain.model.InventoryBatch
import com.example.domain.model.SaleStatus
import com.example.domain.model.VerificationStatus
import com.example.domain.model.WasteDelivery
import com.example.domain.model.WasteSale
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.state.WasteHealsSnapshot
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TabularCompactStyle
import com.example.ui.theme.TabularMoneyStyle
import com.example.ui.theme.WarningAmber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OperationsAndMarketScreen(
  snap: WasteHealsSnapshot,
  isBusy: Boolean,
  onAcceptDelivery: (WasteDelivery) -> Unit,
  onCreateAndConfirmSale: (
    batchItem: InventoryBatch,
    buyerName: String,
    dispatchedKgMilli: Long,
    confirmedSoldKgMilli: Long,
    rejectedKgMilli: Long,
    rejectionReason: String?,
    unitSellPriceUgx: Long,
    healthcareUgxPerKg: Long
  ) -> Unit,
  onRecordBuyerReceipt: (WasteSale, Long) -> Unit,
  onCreateCompostBatch: (hubId: String, inputKgMilli: Long, composition: String, costUgx: Long) -> Unit,
  onUpdateCompostBatch: (
    batch: CompostBatch,
    moisturePctMilli: Long,
    tempMilli: Long,
    outputKgMilli: Long,
    rejectedKgMilli: Long,
    approveFinished: Boolean
  ) -> Unit
) {
  var subTab by remember { mutableIntStateOf(0) }
  val tabs = listOf("Verify Queue", "Inventory", "Composting", "Buyer Sales")

  var saleTargetBatch by remember { mutableStateOf<InventoryBatch?>(null) }
  var paymentTargetSale by remember { mutableStateOf<WasteSale?>(null) }
  var compostEditBatch by remember { mutableStateOf<CompostBatch?>(null) }

  saleTargetBatch?.let { batchItem ->
    CreateBuyerSaleDialog(
      batchItem = batchItem,
      snap = snap,
      onDismiss = { saleTargetBatch = null },
      onConfirm = { buyer, disp, sold, rej, reason, sellPrice ->
        onCreateAndConfirmSale(
          batchItem, buyer, disp, sold, rej, reason, sellPrice, snap.policy.healthcareUgxPerKg
        )
        saleTargetBatch = null
      }
    )
  }

  paymentTargetSale?.let { sale ->
    RecordBuyerCashDialog(
      sale = sale,
      onDismiss = { paymentTargetSale = null },
      onConfirm = { cashUgx ->
        onRecordBuyerReceipt(sale, cashUgx)
        paymentTargetSale = null
      }
    )
  }

  compostEditBatch?.let { cmp ->
    CompostInspectionDialog(
      batch = cmp,
      onDismiss = { compostEditBatch = null },
      onSave = { moist, temp, outKg, rejKg, approve ->
        onUpdateCompostBatch(cmp, moist, temp, outKg, rejKg, approve)
        compostEditBatch = null
      }
    )
  }

  Column(modifier = Modifier.fillMaxSize()) {
    PrimaryScrollableTabRow(selectedTabIndex = subTab, edgePadding = 12.dp) {
      tabs.forEachIndexed { idx, title ->
        Tab(
          selected = subTab == idx,
          onClick = { subTab = idx },
          text = { Text(title) },
          modifier = Modifier.testTag("ops_subtab_$idx")
        )
      }
    }

    when (subTab) {
      0 -> HubVerificationTab(snap, isBusy, onAcceptDelivery)
      1 -> InventoryTab(snap) { saleTargetBatch = it }
      2 -> CompostingTab(snap, isBusy, onCreateCompostBatch) { compostEditBatch = it }
      3 -> BuyerSalesTab(snap) { paymentTargetSale = it }
    }
  }
}

@Composable
private fun HubVerificationTab(
  snap: WasteHealsSnapshot,
  isBusy: Boolean,
  onAcceptDelivery: (WasteDelivery) -> Unit
) {
  val pending = snap.deliveries.filter { it.verificationStatus == VerificationStatus.PENDING_VERIFICATION.name }
  val accepted = snap.deliveries.filter { it.verificationStatus == VerificationStatus.ACCEPTED.name }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    item {
      SectionHeader(
        title = "Hub Verification Queue (${pending.size})",
        subtitle = "Accepting atomically creates Inventory Batch, Guaranteed Picker Earning & Balanced Journal"
      )
    }
    if (pending.isEmpty()) {
      item {
        EmptyStateCard(
          icon = Icons.Filled.Verified,
          title = "All Synced Deliveries Verified",
          description = "New collections synced from field devices appear here for hub weighbridge acceptance."
        )
      }
    } else {
      items(pending, key = { it.id }) { del ->
        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
          Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text("${del.receiptRef} • ${del.materialName} (${del.grade})", style = MaterialTheme.typography.titleSmall)
              StatusPill("PENDING", WarningAmber.copy(alpha = 0.18f), WarningAmber)
            }
            Text(
              text = "Picker: ${del.pickerName} • Accepted: ${MoneyAndWeight.formatKg(del.acceptedKgMilli)} @ ${MoneyAndWeight.formatUgx(del.unitPriceUgxMinor)}/kg",
              style = TabularCompactStyle,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
              text = "Guaranteed Payable: ${MoneyAndWeight.formatUgx(del.totalEarningsUgxMinor)}",
              style = TabularMoneyStyle,
              color = MaterialTheme.colorScheme.primary
            )
            Button(
              onClick = { onAcceptDelivery(del) },
              enabled = !isBusy,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("accept_delivery_${del.id}")
            ) {
              Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Accept Delivery & Post Double-Entry Ledger")
            }
          }
        }
      }
    }

    item {
      SectionHeader(
        title = "Verified Deliveries (${accepted.size})",
        subtitle = "Immutable post-acceptance operational records"
      )
    }
    items(accepted, key = { it.id }) { del ->
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
          Column(modifier = Modifier.weight(1f)) {
            Text("${del.receiptRef} • ${del.materialName}", style = MaterialTheme.typography.titleSmall)
            Text(
              "${MoneyAndWeight.formatKg(del.acceptedKgMilli)} • ${MoneyAndWeight.formatUgx(del.totalEarningsUgxMinor)}",
              style = TabularCompactStyle,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          StatusPill("ACCEPTED", SuccessGreen.copy(alpha = 0.15f), SuccessGreen)
        }
      }
    }
  }
}

@Composable
private fun InventoryTab(
  snap: WasteHealsSnapshot,
  onDispatchForSale: (InventoryBatch) -> Unit
) {
  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    item {
      SectionHeader(
        title = "Hub Inventory Batches (${snap.inventory.size})",
        subtitle = "Strict mass balance: Received = Available + Sold + Rejected + Disposed"
      )
    }
    if (snap.inventory.isEmpty()) {
      item {
        EmptyStateCard(
          icon = Icons.Filled.Inventory2,
          title = "No Inventory Batches",
          description = "Accept a delivery in the Verify Queue to generate traceable hub inventory."
        )
      }
    } else {
      items(snap.inventory, key = { it.id }) { bat ->
        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
          Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text("${bat.batchCode} • ${bat.materialName} (${bat.grade})", style = MaterialTheme.typography.titleSmall)
              StatusPill(bat.status, MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Text(
              text = "Available: ${MoneyAndWeight.formatKg(bat.availableKgMilli)} / Received: ${MoneyAndWeight.formatKg(bat.receivedKgMilli)}",
              style = TabularCompactStyle
            )
            Text(
              text = "Sold: ${MoneyAndWeight.formatKg(bat.soldKgMilli)} • Rejected: ${MoneyAndWeight.formatKg(bat.rejectedKgMilli)} • Unit Cost: ${MoneyAndWeight.formatUgx(bat.unitCostUgxMinor)}/kg",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (bat.availableKgMilli > 0L) {
              Button(
                onClick = { onDispatchForSale(bat) },
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("sell_batch_${bat.id}")
              ) {
                Icon(Icons.Filled.LocalShipping, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Dispatch & Confirm Buyer Sale")
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun CompostingTab(
  snap: WasteHealsSnapshot,
  isBusy: Boolean,
  onCreateCompostBatch: (hubId: String, inputKgMilli: Long, composition: String, costUgx: Long) -> Unit,
  onInspectBatch: (CompostBatch) -> Unit
) {
  var inputKgText by remember { mutableStateOf("120.000") }
  var composition by remember { mutableStateOf("Market organics 70% + Dry sawdust 30%") }
  var costText by remember { mutableStateOf("18000") }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    item {
      SectionHeader(
        title = "Organic Waste Recovery & Composting",
        subtitle = "Track input biomass, moisture/temperature turns, shrinkage loss, and cured compost yield"
      )
    }
    item {
      Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text("Start New Composting Windrow Batch", style = MaterialTheme.typography.titleSmall)
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = inputKgText,
              onValueChange = { inputKgText = it },
              label = { Text("Input Organic (kg)") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
              singleLine = true,
              modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
              value = costText,
              onValueChange = { costText = it },
              label = { Text("Ops Cost (UGX)") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              singleLine = true,
              modifier = Modifier.weight(1f)
            )
          }
          OutlinedTextField(
            value = composition,
            onValueChange = { composition = it },
            label = { Text("Feedstock Composition") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
          Button(
            onClick = {
              val kg = MoneyAndWeight.parseKgToMilli(inputKgText) ?: 0L
              val cost = MoneyAndWeight.parseUgxToLong(costText) ?: 0L
              onCreateCompostBatch(snap.hubs.firstOrNull()?.id ?: "hub_nakawa", kg, composition, cost)
            },
            enabled = !isBusy && (MoneyAndWeight.parseKgToMilli(inputKgText) ?: 0L) > 0L,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("start_compost_batch_button")
          ) {
            Icon(Icons.Filled.Compost, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Start Organic Compost Batch")
          }
        }
      }
    }

    if (snap.compostBatches.isEmpty()) {
      item {
        EmptyStateCard(
          icon = Icons.Filled.Compost,
          title = "No Compost Batches Yet",
          description = "Start a windrow batch above to monitor curing temperature, moisture, and finished compost output."
        )
      }
    } else {
      items(snap.compostBatches, key = { it.id }) { cmp ->
        val approved = cmp.status == CompostStatus.APPROVED.name
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.surface,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
          Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text("${cmp.batchCode} • Turns: ${cmp.turningCount}", style = MaterialTheme.typography.titleSmall)
              StatusPill(
                text = cmp.status,
                containerColor = if (approved) SuccessGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.secondaryContainer,
                contentColor = if (approved) SuccessGreen else MaterialTheme.colorScheme.onSecondaryContainer
              )
            }
            Text(cmp.materialComposition, style = MaterialTheme.typography.bodySmall)
            Text(
              text = "Input: ${MoneyAndWeight.formatKg(cmp.inputKgMilli)} • Output: ${MoneyAndWeight.formatKg(cmp.outputCompostKgMilli)} • Loss: ${MoneyAndWeight.formatKg(cmp.lossKgMilli)}",
              style = TabularCompactStyle,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
              text = "Temp: ${cmp.temperatureCelsiusMilli / 1000.0} °C • Moisture: ${cmp.moisturePctMilli / 1000.0}%",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(
              onClick = { onInspectBatch(cmp) },
              modifier = Modifier.fillMaxWidth()
            ) {
              Text("Log Turn / Moisture / Final Output")
            }
          }
        }
      }
    }
  }
}

@Composable
private fun BuyerSalesTab(
  snap: WasteHealsSnapshot,
  onRecordCash: (WasteSale) -> Unit
) {
  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    item {
      SectionHeader(
        title = "Commercial Buyer Sales (${snap.sales.size})",
        subtitle = "UGX ${snap.policy.healthcareUgxPerKg}/kg accrues on confirmed sold kg; cash receipt funds Restricted Healthcare Cash"
      )
    }
    if (snap.sales.isEmpty()) {
      item {
        EmptyStateCard(
          icon = Icons.Filled.LocalShipping,
          title = "No Commercial Sales Recorded",
          description = "Dispatch stock from the Inventory tab to record weighbridge confirmations and trigger UGX 30/kg healthcare funding."
        )
      }
    } else {
      items(snap.sales, key = { it.id }) { sale ->
        val outstanding = (sale.totalInvoiceUgxMinor - sale.paidUgxMinor).coerceAtLeast(0L)
        val paid = sale.status == SaleStatus.PAID.name
        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
          Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text("${sale.saleRef} • ${sale.buyerName}", style = MaterialTheme.typography.titleSmall)
              StatusPill(
                text = sale.status,
                containerColor = if (paid) SuccessGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = if (paid) SuccessGreen else MaterialTheme.colorScheme.onTertiaryContainer
              )
            }
            Text(
              text = "${sale.materialName} (${sale.grade}) • Sold: ${MoneyAndWeight.formatKg(sale.confirmedSoldKgMilli)} • Rejected: ${MoneyAndWeight.formatKg(sale.rejectedKgMilli)}",
              style = TabularCompactStyle,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
              text = "Invoice: ${MoneyAndWeight.formatUgx(sale.totalInvoiceUgxMinor)} • Paid: ${MoneyAndWeight.formatUgx(sale.paidUgxMinor)}",
              style = TabularMoneyStyle
            )
            Text(
              text = "Healthcare Obligation — Accrued: ${MoneyAndWeight.formatUgx(sale.healthcareAccruedUgxMinor)} | Funded Cash: ${MoneyAndWeight.formatUgx(sale.healthcareFundedUgxMinor)}",
              style = MaterialTheme.typography.bodySmall,
              color = SuccessGreen
            )
            if (outstanding > 0L) {
              Button(
                onClick = { onRecordCash(sale) },
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("record_buyer_cash_${sale.id}")
              ) {
                Icon(Icons.Filled.Payments, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Record Buyer Cash Receipt (${MoneyAndWeight.formatUgx(outstanding)} due)")
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun CreateBuyerSaleDialog(
  batchItem: InventoryBatch,
  snap: WasteHealsSnapshot,
  onDismiss: () -> Unit,
  onConfirm: (
    buyerName: String,
    dispatchedKgMilli: Long,
    confirmedSoldKgMilli: Long,
    rejectedKgMilli: Long,
    rejectionReason: String?,
    unitSellPriceUgx: Long
  ) -> Unit
) {
  val defaultSellPrice = snap.rates
    .firstOrNull { it.materialId == batchItem.materialId }
    ?.buyerSellPriceUgxMinor
    ?: (batchItem.unitCostUgxMinor + 400L)

  var buyerName by remember { mutableStateOf("Kampala EcoPolymers Ltd") }
  var dispText by remember { mutableStateOf(MoneyAndWeight.milliKgToBigDecimal(batchItem.availableKgMilli).toPlainString()) }
  var soldText by remember { mutableStateOf(MoneyAndWeight.milliKgToBigDecimal(batchItem.availableKgMilli).toPlainString()) }
  var rejText by remember { mutableStateOf("0.000") }
  var reason by remember { mutableStateOf("") }
  var priceText by remember { mutableStateOf(defaultSellPrice.toString()) }

  val dispMilli = MoneyAndWeight.parseKgToMilli(dispText) ?: 0L
  val soldMilli = MoneyAndWeight.parseKgToMilli(soldText) ?: 0L
  val rejMilli = MoneyAndWeight.parseKgToMilli(rejText) ?: 0L
  val sellUgx = MoneyAndWeight.parseUgxToLong(priceText) ?: 0L
  val valid = buyerName.isNotBlank() && dispMilli > 0L && dispMilli <= batchItem.availableKgMilli &&
    (soldMilli + rejMilli) <= dispMilli && sellUgx > 0L

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Confirm Commercial Sale") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Batch ${batchItem.batchCode} • Avail: ${MoneyAndWeight.formatKg(batchItem.availableKgMilli)}", style = MaterialTheme.typography.bodySmall)
        OutlinedTextField(value = buyerName, onValueChange = { buyerName = it }, label = { Text("Commercial Buyer Name") }, singleLine = true)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = dispText, onValueChange = { dispText = it },
            label = { Text("Dispatched kg") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f), singleLine = true
          )
          OutlinedTextField(
            value = soldText, onValueChange = { soldText = it },
            label = { Text("Confirmed kg") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f), singleLine = true
          )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = rejText, onValueChange = { rejText = it },
            label = { Text("Rejected kg") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f), singleLine = true
          )
          OutlinedTextField(
            value = priceText, onValueChange = { priceText = it },
            label = { Text("Sell UGX/kg") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f), singleLine = true
          )
        }
        if (rejMilli > 0L) {
          OutlinedTextField(value = reason, onValueChange = { reason = it }, label = { Text("Rejection Reason") }, singleLine = true)
        }
        Text(
          text = "HC Accrual: ${MoneyAndWeight.formatUgx(MoneyAndWeight.calculateAmountUgx(soldMilli, snap.policy.healthcareUgxPerKg))}",
          style = TabularCompactStyle,
          color = SuccessGreen
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { onConfirm(buyerName, dispMilli, soldMilli, rejMilli, reason.takeIf { it.isNotBlank() }, sellUgx) },
        enabled = valid
      ) { Text("Post Sale & Ledger") }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
  )
}

@Composable
private fun RecordBuyerCashDialog(
  sale: WasteSale,
  onDismiss: () -> Unit,
  onConfirm: (Long) -> Unit
) {
  val outstanding = (sale.totalInvoiceUgxMinor - sale.paidUgxMinor).coerceAtLeast(0L)
  var amountText by remember { mutableStateOf(outstanding.toString()) }
  val parsed = MoneyAndWeight.parseUgxToLong(amountText) ?: 0L

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Record Buyer Cash Receipt") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Outstanding: ${MoneyAndWeight.formatUgx(outstanding)}", style = TabularCompactStyle)
        OutlinedTextField(
          value = amountText,
          onValueChange = { amountText = it },
          label = { Text("Cash Received (UGX)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true
        )
        Text(
          text = "Proportional UGX ${sale.healthcareRateUgxPerKg}/kg will transfer immediately into Restricted Healthcare Cash.",
          style = MaterialTheme.typography.bodySmall,
          color = SuccessGreen
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { onConfirm(parsed) },
        enabled = parsed in 1L..outstanding
      ) { Text("Confirm Receipt") }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
  )
}

@Composable
private fun CompostInspectionDialog(
  batch: CompostBatch,
  onDismiss: () -> Unit,
  onSave: (moistMilli: Long, tempMilli: Long, outKgMilli: Long, rejKgMilli: Long, approve: Boolean) -> Unit
) {
  var moistText by remember { mutableStateOf((batch.moisturePctMilli / 1000L).toString()) }
  var tempText by remember { mutableStateOf((batch.temperatureCelsiusMilli / 1000L).toString()) }
  var outText by remember {
    val defaultOut = if (batch.outputCompostKgMilli > 0L) batch.outputCompostKgMilli else (batch.inputKgMilli * 45L) / 100L
    mutableStateOf(MoneyAndWeight.milliKgToBigDecimal(defaultOut).toPlainString())
  }
  var rejText by remember { mutableStateOf(MoneyAndWeight.milliKgToBigDecimal(batch.rejectedKgMilli).toPlainString()) }
  var approveFinished by remember { mutableStateOf(true) }

  val outMilli = MoneyAndWeight.parseKgToMilli(outText) ?: 0L
  val rejMilli = MoneyAndWeight.parseKgToMilli(rejText) ?: 0L
  val moistMilli = ((moistText.toLongOrNull() ?: 50L).coerceIn(0L, 100L)) * 1000L
  val tempMilli = ((tempText.toLongOrNull() ?: 55L).coerceIn(0L, 100L)) * 1000L

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Log Compost Turn — ${batch.batchCode}") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Input Biomass: ${MoneyAndWeight.formatKg(batch.inputKgMilli)}", style = TabularCompactStyle)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(value = moistText, onValueChange = { moistText = it }, label = { Text("Moisture %") }, modifier = Modifier.weight(1f), singleLine = true)
          OutlinedTextField(value = tempText, onValueChange = { tempText = it }, label = { Text("Temp °C") }, modifier = Modifier.weight(1f), singleLine = true)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(value = outText, onValueChange = { outText = it }, label = { Text("Output Compost kg") }, modifier = Modifier.weight(1f), singleLine = true)
          OutlinedTextField(value = rejText, onValueChange = { rejText = it }, label = { Text("Rejected kg") }, modifier = Modifier.weight(1f), singleLine = true)
        }
        FilterChip(
          selected = approveFinished,
          onClick = { approveFinished = !approveFinished },
          label = { Text("Approve Finished Compost Batch") }
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { onSave(moistMilli, tempMilli, outMilli, rejMilli, approveFinished) },
        enabled = outMilli + rejMilli <= batch.inputKgMilli
      ) { Text("Save Mass Balance") }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
  )
}
