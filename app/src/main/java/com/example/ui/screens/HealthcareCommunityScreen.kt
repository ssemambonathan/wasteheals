package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Recycling
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
import com.example.domain.model.ClaimStatus
import com.example.domain.model.DisputeRecord
import com.example.domain.model.DisputeStatus
import com.example.domain.model.HealthcareClaim
import com.example.domain.model.HealthcareClinic
import com.example.domain.model.HealthcareVoucher
import com.example.domain.model.VoucherStatus
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.MetricCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.state.WasteHealsSnapshot
import com.example.ui.theme.ErrorCrimson
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TabularCompactStyle
import com.example.ui.theme.TabularMoneyStyle
import com.example.ui.theme.WarningAmber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthcareCommunityScreen(
  snap: WasteHealsSnapshot,
  isBusy: Boolean,
  onIssueVoucher: (clinic: HealthcareClinic, serviceName: String, maxUgx: Long, availableFundedUgx: Long) -> Unit,
  onRedeemVoucher: (voucher: HealthcareVoucher, claimUgx: Long) -> Unit,
  onPayClaim: (claim: HealthcareClaim, clinicName: String) -> Unit,
  onRegisterHousehold: (community: String, hubId: String, members: Long, day: String) -> Unit,
  onFileDispute: (category: String, refId: String, desc: String) -> Unit,
  onResolveDispute: (dispute: DisputeRecord, notes: String) -> Unit
) {
  var subTab by remember { mutableIntStateOf(0) }
  val tabs = listOf("Healthcare Fund", "Households", "Disputes & Audit")

  Column(modifier = Modifier.fillMaxSize()) {
    PrimaryScrollableTabRow(selectedTabIndex = subTab, edgePadding = 12.dp) {
      tabs.forEachIndexed { idx, title ->
        Tab(
          selected = subTab == idx,
          onClick = { subTab = idx },
          text = { Text(title) },
          modifier = Modifier.testTag("health_subtab_$idx")
        )
      }
    }

    when (subTab) {
      0 -> HealthcareTab(snap, isBusy, onIssueVoucher, onRedeemVoucher, onPayClaim)
      1 -> HouseholdsTab(snap, isBusy, onRegisterHousehold)
      2 -> DisputesAndAuditTab(snap, isBusy, onFileDispute, onResolveDispute)
    }
  }
}

@Composable
private fun HealthcareTab(
  snap: WasteHealsSnapshot,
  isBusy: Boolean,
  onIssueVoucher: (HealthcareClinic, String, Long, Long) -> Unit,
  onRedeemVoucher: (HealthcareVoucher, Long) -> Unit,
  onPayClaim: (HealthcareClaim, String) -> Unit
) {
  val led = snap.ledgerSummary
  val fundedCash = led.restrictedHealthcareCashUgx
  val accruedUnfunded = led.healthcareAccruedUnfundedUgx
  var selectedClinicId by remember(snap.clinics) { mutableStateOf(snap.clinics.firstOrNull()?.id ?: "") }
  var serviceName by remember { mutableStateOf("Outpatient Consult & Malaria Screening") }
  var capUgxText by remember { mutableStateOf("15000") }
  var redeemTarget by remember { mutableStateOf<HealthcareVoucher?>(null) }

  val selectedClinic = snap.clinics.firstOrNull { it.id == selectedClinicId } ?: snap.clinics.firstOrNull()
  val capUgx = MoneyAndWeight.parseUgxToLong(capUgxText) ?: 0L

  redeemTarget?.let { vch ->
    var claimText by remember { mutableStateOf(vch.maxAmountUgxMinor.toString()) }
    val claimAmt = MoneyAndWeight.parseUgxToLong(claimText) ?: 0L
    AlertDialog(
      onDismissRequest = { redeemTarget = null },
      title = { Text("Clinic Voucher Redemption") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("Voucher ${vch.voucherCode} • Max Cap: ${MoneyAndWeight.formatUgx(vch.maxAmountUgxMinor)}", style = TabularCompactStyle)
          OutlinedTextField(
            value = claimText,
            onValueChange = { claimText = it },
            label = { Text("Actual Clinic Invoice (UGX)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true
          )
        }
      },
      confirmButton = {
        Button(
          onClick = { onRedeemVoucher(vch, claimAmt); redeemTarget = null },
          enabled = claimAmt in 1L..vch.maxAmountUgxMinor
        ) { Text("Redeem & Submit Claim") }
      },
      dismissButton = { TextButton(onClick = { redeemTarget = null }) { Text("Cancel") } }
    )
  }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    item {
      SectionHeader(
        title = "Restricted Healthcare Fund",
        subtitle = "UGX ${snap.policy.healthcareUgxPerKg}/kg sold • Vouchers strictly backed by collected cash"
      )
    }
    item {
      Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        MetricCard(
          title = "Funded Available Cash",
          value = MoneyAndWeight.formatUgx(fundedCash),
          subtitle = "Ready for voucher issuance",
          icon = Icons.Filled.HealthAndSafety,
          accentColor = SuccessGreen,
          modifier = Modifier.weight(1f)
        )
        MetricCard(
          title = "Accrued Unfunded",
          value = MoneyAndWeight.formatUgx(accruedUnfunded),
          subtitle = "Awaiting buyer receipt",
          icon = Icons.Filled.LocalHospital,
          accentColor = WarningAmber,
          modifier = Modifier.weight(1f)
        )
      }
    }

    item {
      Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text("Issue Single-Use Clinic Voucher", style = MaterialTheme.typography.titleSmall)
          if (snap.clinics.isNotEmpty()) {
            Row(
              modifier = Modifier.horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              snap.clinics.forEach { cl ->
                FilterChip(
                  selected = selectedClinic?.id == cl.id,
                  onClick = { selectedClinicId = cl.id },
                  label = { Text(cl.clinicName) }
                )
              }
            }
          }
          OutlinedTextField(
            value = serviceName,
            onValueChange = { serviceName = it },
            label = { Text("Medical Service / Care Scope") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
            value = capUgxText,
            onValueChange = { capUgxText = it },
            label = { Text("Voucher Ceiling (UGX)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
          if (fundedCash <= 0L) {
            Text(
              "Funded Healthcare Cash is UGX 0. Confirm a buyer sale & record buyer cash receipt before issuing vouchers.",
              style = MaterialTheme.typography.bodySmall,
              color = WarningAmber
            )
          }
          Button(
            onClick = {
              val cl = selectedClinic ?: return@Button
              onIssueVoucher(cl, serviceName, capUgx, fundedCash)
            },
            enabled = !isBusy && selectedClinic != null && capUgx in 1L..fundedCash,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("issue_voucher_button")
          ) {
            Icon(Icons.Filled.QrCode2, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Issue Single-Use Clinic Voucher")
          }
        }
      }
    }

    item {
      SectionHeader(title = "Issued Vouchers (${snap.vouchers.size})", subtitle = "14-day expiry • Clinic-bound redemption")
    }
    if (snap.vouchers.isEmpty()) {
      item {
        EmptyStateCard(
          icon = Icons.Filled.LocalHospital,
          title = "No Vouchers Issued",
          description = "Once buyer receipts fund the Restricted Healthcare Cash pool, issue single-use vouchers above."
        )
      }
    } else {
      items(snap.vouchers, key = { it.id }) { v ->
        val redeemed = v.status == VoucherStatus.REDEEMED.name
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
              Text("${v.voucherCode} • ${v.clinicName}", style = MaterialTheme.typography.titleSmall)
              Text("${v.serviceName} • Cap: ${MoneyAndWeight.formatUgx(v.maxAmountUgxMinor)}", style = TabularCompactStyle)
            }
            if (!redeemed) {
              Button(
                onClick = { redeemTarget = v },
                enabled = !isBusy,
                modifier = Modifier.testTag("redeem_voucher_${v.id}")
              ) { Text("Redeem") }
            } else {
              StatusPill("REDEEMED", SuccessGreen.copy(alpha = 0.15f), SuccessGreen)
            }
          }
        }
      }
    }

    item {
      SectionHeader(title = "Clinic Claims (${snap.claims.size})", subtitle = "Paid strictly from Restricted Healthcare Cash")
    }
    items(snap.claims, key = { it.id }) { clm ->
      val paid = clm.status == ClaimStatus.PAID.name
      val clinicName = snap.clinics.firstOrNull { it.id == clm.clinicId }?.clinicName ?: "Partner Clinic"
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
            Text("$clinicName • ${MoneyAndWeight.formatUgx(clm.claimedAmountUgxMinor)}", style = MaterialTheme.typography.titleSmall)
            Text("Voucher: ${clm.voucherId} • Status: ${clm.status}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
          if (!paid) {
            Button(
              onClick = { onPayClaim(clm, clinicName) },
              enabled = !isBusy,
              modifier = Modifier.testTag("pay_claim_${clm.id}")
            ) { Text("Pay Claim") }
          } else {
            StatusPill("PAID", SuccessGreen.copy(alpha = 0.15f), SuccessGreen)
          }
        }
      }
    }
  }
}

@Composable
private fun HouseholdsTab(
  snap: WasteHealsSnapshot,
  isBusy: Boolean,
  onRegisterHousehold: (String, String, Long, String) -> Unit
) {
  var community by remember { mutableStateOf("Naguru Ward, Kampala") }
  var membersText by remember { mutableStateOf("4") }
  var day by remember { mutableStateOf("Wednesday") }
  val days = listOf("Monday", "Wednesday", "Friday", "Saturday")

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    item {
      SectionHeader(
        title = "Household Separation Portal",
        subtitle = "Source separation schedules, non-cash green points & community impact"
      )
    }
    item {
      Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text("Register Household", style = MaterialTheme.typography.titleSmall)
          OutlinedTextField(
            value = community,
            onValueChange = { community = it },
            label = { Text("Parish / Community") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
            value = membersText,
            onValueChange = { membersText = it },
            label = { Text("Household Members") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
          Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            days.forEach { d ->
              FilterChip(selected = day == d, onClick = { day = d }, label = { Text(d) })
            }
          }
          Button(
            onClick = {
              onRegisterHousehold(
                community,
                snap.hubs.firstOrNull()?.id ?: "hub_nakawa",
                membersText.toLongOrNull() ?: 4L,
                day
              )
            },
            enabled = !isBusy && community.isNotBlank(),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("register_household_button")
          ) {
            Icon(Icons.Filled.Home, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Register Household (+10 Welcome Points)")
          }
        }
      }
    }

    if (snap.households.isEmpty()) {
      item {
        EmptyStateCard(
          icon = Icons.Filled.Recycling,
          title = "No Registered Households",
          description = "Register a Kampala household above to track weekly segregated pickups and community points."
        )
      }
    } else {
      items(snap.households, key = { it.id }) { hh ->
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
              Text("${hh.householdRef} • ${hh.community}", style = MaterialTheme.typography.titleSmall)
              Text(
                "Pickup: ${hh.collectionDay} • Members: ${hh.memberCount} • Contamination: ${hh.contaminationCount}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            StatusPill("${hh.pointsBalance} pts", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
          }
        }
      }
    }
  }
}

@Composable
private fun DisputesAndAuditTab(
  snap: WasteHealsSnapshot,
  isBusy: Boolean,
  onFileDispute: (String, String, String) -> Unit,
  onResolveDispute: (DisputeRecord, String) -> Unit
) {
  val categories = listOf("WEIGHING", "QUALITY_DEDUCTION", "MOMO_PAYOUT", "VOUCHER_CLAIM")
  var selectedCat by remember { mutableStateOf("WEIGHING") }
  var refId by remember { mutableStateOf("") }
  var desc by remember { mutableStateOf("") }
  var resolveTarget by remember { mutableStateOf<DisputeRecord?>(null) }

  resolveTarget?.let { dsp ->
    var notes by remember { mutableStateOf("Scale & receipt verified; adjustment logged.") }
    AlertDialog(
      onDismissRequest = { resolveTarget = null },
      title = { Text("Resolve Dispute ${dsp.disputeRef}") },
      text = {
        OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Resolution Audit Notes") })
      },
      confirmButton = {
        Button(onClick = { onResolveDispute(dsp, notes); resolveTarget = null }) { Text("Resolve") }
      },
      dismissButton = { TextButton(onClick = { resolveTarget = null }) { Text("Cancel") } }
    )
  }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    item {
      SectionHeader(
        title = "Formal Disputes & Appeals (${snap.disputes.size})",
        subtitle = "Immutable investigation workflow for weights, rates, payouts & claims"
      )
    }
    item {
      Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("File a Formal Dispute", style = MaterialTheme.typography.titleSmall)
          Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            categories.forEach { c ->
              FilterChip(selected = selectedCat == c, onClick = { selectedCat = c }, label = { Text(c) })
            }
          }
          OutlinedTextField(
            value = refId,
            onValueChange = { refId = it },
            label = { Text("Receipt / Payout / Voucher Ref (optional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
            value = desc,
            onValueChange = { desc = it },
            label = { Text("Dispute Details (min 4 chars)") },
            modifier = Modifier.fillMaxWidth()
          )
          Button(
            onClick = {
              onFileDispute(selectedCat, refId, desc)
              desc = ""
            },
            enabled = !isBusy && desc.trim().length >= 4,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("file_dispute_button")
          ) {
            Icon(Icons.Filled.Gavel, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Submit Dispute")
          }
        }
      }
    }

    items(snap.disputes, key = { it.id }) { dsp ->
      val open = dsp.status == DisputeStatus.OPEN.name
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
      ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("${dsp.disputeRef} • ${dsp.category}", style = MaterialTheme.typography.titleSmall)
            StatusPill(
              text = dsp.status,
              containerColor = if (open) ErrorCrimson.copy(alpha = 0.14f) else SuccessGreen.copy(alpha = 0.15f),
              contentColor = if (open) ErrorCrimson else SuccessGreen
            )
          }
          Text(dsp.description, style = MaterialTheme.typography.bodySmall)
          if (!dsp.resolutionNotes.isNullOrBlank()) {
            Text("Resolution: ${dsp.resolutionNotes}", style = MaterialTheme.typography.labelSmall, color = SuccessGreen)
          }
          if (open) {
            OutlinedButton(onClick = { resolveTarget = dsp }, modifier = Modifier.fillMaxWidth()) {
              Text("Investigate & Resolve")
            }
          }
        }
      }
    }

    item {
      SectionHeader(
        title = "Append-Only Audit Trail (${snap.auditLogs.size})",
        subtitle = "Tamper-evident operational & financial event log"
      )
    }
    items(snap.auditLogs.take(15), key = { it.id }) { log ->
      Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
      ) {
        Column(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
          Text("${log.action} • ${log.entityType}", style = MaterialTheme.typography.labelLarge)
          Text(log.summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      }
    }
  }
}
