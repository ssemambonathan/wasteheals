package com.example.ui.screens

import android.content.Intent
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
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Approval
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.domain.engine.MoneyAndWeight
import com.example.domain.engine.SandboxScenario
import com.example.domain.model.ApprovalRequest
import com.example.domain.model.ApprovalStatus
import com.example.domain.model.MoMoLifecycleStatus
import com.example.domain.model.MoMoProvider
import com.example.domain.model.PayoutStatus
import com.example.domain.model.PickerEarning
import com.example.domain.model.PickerPayout
import com.example.domain.model.UserRoleKey
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
fun FinanceAndGovernanceScreen(
  snap: WasteHealsSnapshot,
  currentUid: String,
  isBusy: Boolean,
  onInitiatePayout: (PickerEarning, MoMoProvider, String) -> Unit,
  onExecuteSandboxPayout: (PickerPayout, SandboxScenario) -> Unit,
  onReconcileUnknownPayout: (PickerPayout, Boolean) -> Unit,
  onProposePolicy: (
    currentVersion: Long,
    hcUgx: Long,
    rewardPct: Long,
    opsPct: Long,
    contPct: Long,
    expPct: Long,
    minWcUgx: Long
  ) -> Unit,
  onResolveApproval: (ApprovalRequest, Boolean, String?) -> Unit,
  onAssignUserRoles: (targetUid: String, roleKeys: List<UserRoleKey>) -> Unit,
  buildCsvExport: (WasteHealsSnapshot) -> String
) {
  var subTab by remember { mutableIntStateOf(0) }
  val tabs = listOf("Ledger & Trial", "MoMo Payouts", "Surplus & Gate", "Maker-Checker")

  Column(modifier = Modifier.fillMaxSize()) {
    PrimaryScrollableTabRow(selectedTabIndex = subTab, edgePadding = 12.dp) {
      tabs.forEachIndexed { idx, title ->
        Tab(
          selected = subTab == idx,
          onClick = { subTab = idx },
          text = { Text(title) },
          modifier = Modifier.testTag("finance_subtab_$idx")
        )
      }
    }

    when (subTab) {
      0 -> LedgerTab(snap, buildCsvExport)
      1 -> MoMoPayoutsTab(snap, currentUid, isBusy, onInitiatePayout, onExecuteSandboxPayout, onReconcileUnknownPayout)
      2 -> SurplusAndLiquidityTab(snap)
      3 -> MakerCheckerTab(snap, currentUid, isBusy, onProposePolicy, onResolveApproval, onAssignUserRoles)
    }
  }
}

@Composable
private fun LedgerTab(
  snap: WasteHealsSnapshot,
  buildCsvExport: (WasteHealsSnapshot) -> String
) {
  val context = LocalContext.current
  val led = snap.ledgerSummary

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    item {
      SectionHeader(
        title = "Double-Entry General Ledger",
        subtitle = "Immutable posted entries • Debits = Credits enforced at engine & Firestore rules",
        actionLabel = "Export CSV",
        onAction = {
          val csv = buildCsvExport(snap)
          val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_SUBJECT, "WasteHeals Audit & Trial Balance Export")
            putExtra(Intent.EXTRA_TEXT, csv)
          }
          context.startActivity(Intent.createChooser(intent, "Share Audit CSV"))
        }
      )
    }

    item {
      Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        MetricCard(
          title = "Total Debits",
          value = MoneyAndWeight.formatUgx(led.totalDebitsUgx),
          subtitle = if (led.isBalanced) "Balanced vs Credits" else "IMBALANCE",
          icon = Icons.Filled.AccountBalance,
          modifier = Modifier.weight(1f)
        )
        MetricCard(
          title = "Total Credits",
          value = MoneyAndWeight.formatUgx(led.totalCreditsUgx),
          subtitle = "${snap.journalEntries.size} posted journal(s)",
          icon = Icons.Filled.Security,
          accentColor = SuccessGreen,
          modifier = Modifier.weight(1f)
        )
      }
    }

    item {
      OutlinedButton(
        onClick = {
          val csv = buildCsvExport(snap)
          val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_SUBJECT, "WasteHeals Compliance Export")
            putExtra(Intent.EXTRA_TEXT, csv)
          }
          context.startActivity(Intent.createChooser(intent, "Export Compliance CSV"))
        },
        modifier = Modifier
          .fillMaxWidth()
          .testTag("export_csv_button")
      ) {
        Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Share Trial Balance & Impact CSV Report")
      }
    }

    item {
      Text("Trial Balance by Chart of Accounts", style = MaterialTheme.typography.titleMedium)
    }

    if (led.trialBalance.isEmpty()) {
      item {
        EmptyStateCard(
          icon = Icons.Filled.AccountBalance,
          title = "No Posted Ledger Entries",
          description = "Accepting deliveries or confirming buyer sales automatically posts balanced double-entry journals."
        )
      }
    } else {
      items(led.trialBalance, key = { it.accountCode }) { row ->
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
              Text("${row.accountCode} • ${row.accountName}", style = MaterialTheme.typography.titleSmall)
              Text(
                "${row.category.name} | DR ${MoneyAndWeight.formatUgx(row.totalDebitsUgx)} | CR ${MoneyAndWeight.formatUgx(row.totalCreditsUgx)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            Text(
              text = MoneyAndWeight.formatUgx(row.netBalanceUgx),
              style = TabularCompactStyle,
              color = MaterialTheme.colorScheme.primary
            )
          }
        }
      }
    }

    item {
      Text("Recent Immutable Journal Entries (${snap.journalEntries.size})", style = MaterialTheme.typography.titleMedium)
    }
    items(snap.journalEntries.take(15), key = { it.id }) { je ->
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
      ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("${je.entryRef} • ${je.eventType}", style = MaterialTheme.typography.titleSmall)
            StatusPill(je.status, MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
          }
          Text(je.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          Text(
            "DR = CR = ${MoneyAndWeight.formatUgx(je.totalDebitsUgxMinor)}",
            style = TabularCompactStyle,
            color = SuccessGreen
          )
        }
      }
    }
  }
}

@Composable
private fun MoMoPayoutsTab(
  snap: WasteHealsSnapshot,
  currentUid: String,
  isBusy: Boolean,
  onInitiatePayout: (PickerEarning, MoMoProvider, String) -> Unit,
  onExecuteSandboxPayout: (PickerPayout, SandboxScenario) -> Unit,
  onReconcileUnknownPayout: (PickerPayout, Boolean) -> Unit
) {
  var selectedProvider by remember { mutableStateOf(MoMoProvider.MTN_MOMO) }
  var selectedScenario by remember { mutableStateOf(SandboxScenario.IMMEDIATE_SUCCESS) }
  var destMsisdn by remember { mutableStateOf(snap.pii?.momoDestinationMasked ?: "+25677***000") }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    item {
      Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text("Direct MoMo Sandbox Mode • Stored-Value Wallet OFF", style = MaterialTheme.typography.titleSmall)
          Text(
            "Wallet feature flag (${snap.policy.walletEnabled}) is OFF pending Bank of Uganda NPS clearance. " +
              "Timeouts lock payouts in UNKNOWN/UNRESOLVED until reconciled.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onTertiaryContainer
          )
        }
      }
    }

    item {
      Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("Disbursement Channel & Sandbox Failure Simulator", style = MaterialTheme.typography.titleSmall)
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MoMoProvider.entries.forEach { p ->
              FilterChip(
                selected = selectedProvider == p,
                onClick = { selectedProvider = p },
                label = { Text(p.displayName) }
              )
            }
          }
          OutlinedTextField(
            value = destMsisdn,
            onValueChange = { destMsisdn = it },
            label = { Text("Masked Destination MSISDN") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
          Text("Sandbox Outcome Scenario", style = MaterialTheme.typography.labelLarge)
          Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            SandboxScenario.entries.forEach { sc ->
              FilterChip(
                selected = selectedScenario == sc,
                onClick = { selectedScenario = sc },
                label = { Text(sc.name.replace("_", " ")) },
                modifier = Modifier.testTag("sandbox_scenario_${sc.name}")
              )
            }
          }
        }
      }
    }

    item {
      SectionHeader(
        title = "Guaranteed Picker Earnings (${snap.earnings.size})",
        subtitle = "Initiate Maker payout draft for unsettled earnings"
      )
    }
    val payable = snap.earnings.filter {
      it.payoutStatus == PayoutStatus.UNSETTLED.name || it.payoutStatus == PayoutStatus.FAILED.name
    }
    if (payable.isEmpty()) {
      item {
        EmptyStateCard(
          icon = Icons.Filled.Payments,
          title = "No Unsettled Picker Earnings",
          description = "Verified deliveries create guaranteed unsettled earnings ready for MoMo disbursement."
        )
      }
    } else {
      items(payable, key = { it.id }) { earn ->
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
              Text("${earn.pickerName} • ${MoneyAndWeight.formatKg(earn.acceptedKgMilli)}", style = MaterialTheme.typography.titleSmall)
              Text(
                "${MoneyAndWeight.formatUgx(earn.amountUgxMinor)} • ${earn.payoutStatus}",
                style = TabularMoneyStyle,
                color = MaterialTheme.colorScheme.primary
              )
            }
            Button(
              onClick = { onInitiatePayout(earn, selectedProvider, destMsisdn) },
              enabled = !isBusy && destMsisdn.length >= 4,
              modifier = Modifier.testTag("queue_payout_${earn.id}")
            ) {
              Text("Queue Draft")
            }
          }
        }
      }
    }

    item {
      SectionHeader(
        title = "MoMo Disbursements & Reconciliation (${snap.payouts.size})",
        subtitle = "Execute via Sandbox Adapter or resolve UNKNOWN locks"
      )
    }
    items(snap.payouts, key = { it.id }) { pay ->
      val isUnknown = pay.lifecycleStatus == MoMoLifecycleStatus.UNKNOWN.name
      val isSettled = pay.lifecycleStatus == MoMoLifecycleStatus.SUCCESS.name
      val isBootstrap = snap.pii?.email == "nathanssemmambo08@gmail.com"
      val canExecute = !isSettled && (isBootstrap || pay.makerUid != currentUid)
      Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("${pay.pickerName} • ${pay.provider}", style = MaterialTheme.typography.titleSmall)
            StatusPill(
              text = "${pay.lifecycleStatus} (${pay.reconciliationStatus})",
              containerColor = when {
                isSettled -> SuccessGreen.copy(alpha = 0.15f)
                isUnknown -> WarningAmber.copy(alpha = 0.20f)
                else -> MaterialTheme.colorScheme.secondaryContainer
              },
              contentColor = when {
                isSettled -> SuccessGreen
                isUnknown -> WarningAmber
                else -> MaterialTheme.colorScheme.onSecondaryContainer
              }
            )
          }
          Text(
            "${MoneyAndWeight.formatUgx(pay.amountUgxMinor)} -> ${pay.destinationMasked} • Ref: ${pay.providerRef ?: "Pending"}",
            style = TabularCompactStyle
          )
          if (!pay.failureReason.isNullOrBlank()) {
            Text(pay.failureReason, style = MaterialTheme.typography.bodySmall, color = ErrorCrimson)
          }
          if (isUnknown) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
              Button(
                onClick = { onReconcileUnknownPayout(pay, true) },
                enabled = !isBusy,
                modifier = Modifier.weight(1f)
              ) { Text("Reconcile: Confirmed Paid") }
              OutlinedButton(
                onClick = { onReconcileUnknownPayout(pay, false) },
                enabled = !isBusy,
                modifier = Modifier.weight(1f)
              ) { Text("Reconcile: Mark Failed") }
            }
          } else if (!isSettled) {
            Button(
              onClick = { onExecuteSandboxPayout(pay, selectedScenario) },
              enabled = !isBusy && canExecute,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("execute_sandbox_${pay.id}")
            ) {
              Text(if (canExecute) "Run ${selectedScenario.label}" else "Awaiting Different Checker UID")
            }
          }
        }
      }
    }
  }
}

@Composable
private fun SurplusAndLiquidityTab(snap: WasteHealsSnapshot) {
  val sur = snap.surplusEvaluation
  val rewards = snap.rewardAllocations

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    item {
      SectionHeader(
        title = "Surplus & Liquidity Protection Gate",
        subtitle = "Policy v${snap.policy.version}: ${snap.policy.pickerRewardPct}% Reward / ${snap.policy.operationsPct}% Ops / ${snap.policy.contingencyPct}% Cont / ${snap.policy.expansionPct}% Exp"
      )
    }
    item {
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Liquidity Gate Status", style = MaterialTheme.typography.titleMedium)
            StatusPill(
              text = if (sur.liquidityGatePassed) "GATE PASSED" else "GATE BLOCKED",
              containerColor = if (sur.liquidityGatePassed) SuccessGreen.copy(alpha = 0.15f) else ErrorCrimson.copy(alpha = 0.15f),
              contentColor = if (sur.liquidityGatePassed) SuccessGreen else ErrorCrimson
            )
          }
          if (!sur.blockedReason.isNullOrBlank()) {
            Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(10.dp)) {
              Row(modifier = Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.Warning, contentDescription = null, tint = ErrorCrimson)
                Text(sur.blockedReason, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
              }
            }
          }
          HorizontalDivider()
          GateRow("Accounting Net Surplus", MoneyAndWeight.formatUgx(sur.accountingSurplusUgx))
          GateRow("Available Cash (after payables & Min WC)", MoneyAndWeight.formatUgx(sur.availableDistributableCashUgx))
          GateRow("Distributable Pool", MoneyAndWeight.formatUgx(sur.distributablePoolUgx))
          GateRow("Picker Reward Pool (${snap.policy.pickerRewardPct}%)", MoneyAndWeight.formatUgx(sur.pickerRewardPoolUgx))
          GateRow("Operations Reinvestment (${snap.policy.operationsPct}%)", MoneyAndWeight.formatUgx(sur.operationsAllocationUgx))
          GateRow("Contingency Reserve (${snap.policy.contingencyPct}%)", MoneyAndWeight.formatUgx(sur.contingencyAllocationUgx))
          GateRow("Expansion Pool (${snap.policy.expansionPct}%)", MoneyAndWeight.formatUgx(sur.expansionAllocationUgx))
        }
      }
    }

    item {
      SectionHeader(
        title = "Picker Performance Bonus Allocations (${rewards.size})",
        subtitle = "Weighted 50% verified kg, 25% quality, 15% consistency, 10% safety"
      )
    }
    if (rewards.isEmpty()) {
      item {
        EmptyStateCard(
          icon = Icons.Filled.AccountBalance,
          title = "No Accepted Picker Deliveries Yet",
          description = "Verified deliveries generate composite performance scores for surplus bonus eligibility."
        )
      }
    } else {
      items(rewards, key = { it.pickerUid }) { r ->
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
            Column {
              Text(r.pickerName, style = MaterialTheme.typography.titleSmall)
              Text("Composite Score: ${r.compositeScore.toPlainString()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(MoneyAndWeight.formatUgx(r.rewardUgx), style = TabularMoneyStyle, color = MaterialTheme.colorScheme.primary)
          }
        }
      }
    }
  }
}

@Composable
private fun GateRow(label: String, value: String) {
  Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
    Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(value, style = TabularCompactStyle)
  }
}

@Composable
private fun MakerCheckerTab(
  snap: WasteHealsSnapshot,
  currentUid: String,
  isBusy: Boolean,
  onProposePolicy: (Long, Long, Long, Long, Long, Long, Long) -> Unit,
  onResolveApproval: (ApprovalRequest, Boolean, String?) -> Unit,
  onAssignUserRoles: (targetUid: String, roleKeys: List<UserRoleKey>) -> Unit
) {
  val isAdminUser = snap.profile?.roles?.contains(UserRoleKey.ADMIN.key) == true || snap.activeRole.isPrivileged
  var hcUgxText by remember(snap.policy) { mutableStateOf(snap.policy.healthcareUgxPerKg.toString()) }
  var rewText by remember(snap.policy) { mutableStateOf(snap.policy.pickerRewardPct.toString()) }
  var opsText by remember(snap.policy) { mutableStateOf(snap.policy.operationsPct.toString()) }
  var contText by remember(snap.policy) { mutableStateOf(snap.policy.contingencyPct.toString()) }
  var expText by remember(snap.policy) { mutableStateOf(snap.policy.expansionPct.toString()) }
  var minWcText by remember(snap.policy) { mutableStateOf(snap.policy.minWorkingCapitalUgx.toString()) }
  var rejectDialogTarget by remember { mutableStateOf<ApprovalRequest?>(null) }

  rejectDialogTarget?.let { req ->
    var reason by remember { mutableStateOf("Policy/payout check failed review") }
    AlertDialog(
      onDismissRequest = { rejectDialogTarget = null },
      title = { Text("Reject Maker Request") },
      text = {
        OutlinedTextField(value = reason, onValueChange = { reason = it }, label = { Text("Rejection Reason") })
      },
      confirmButton = {
        Button(onClick = { onResolveApproval(req, false, reason); rejectDialogTarget = null }) { Text("Confirm Reject") }
      },
      dismissButton = { TextButton(onClick = { rejectDialogTarget = null }) { Text("Cancel") } }
    )
  }

  val hcVal = hcUgxText.toLongOrNull() ?: 30L
  val rVal = rewText.toLongOrNull() ?: 30L
  val oVal = opsText.toLongOrNull() ?: 35L
  val cVal = contText.toLongOrNull() ?: 20L
  val eVal = expText.toLongOrNull() ?: 15L
  val wcVal = minWcText.toLongOrNull() ?: 2_000_000L
  val splitSumOk = (rVal + oVal + cVal + eVal) == 100L

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    item {
      SectionHeader(
        title = "Maker-Checker Approval Queue (${snap.approvals.size})",
        subtitle = "Strict Separation of Duties: Checker UID must differ from Maker UID"
      )
    }

    if (snap.approvals.isEmpty()) {
      item {
        EmptyStateCard(
          icon = Icons.Filled.Approval,
          title = "No Pending Maker-Checker Requests",
          description = "Propose a policy version below or queue a MoMo payout draft to trigger dual authorization."
        )
      }
    } else {
      items(snap.approvals, key = { it.id }) { req ->
        val pending = req.status == ApprovalStatus.PENDING.name
        val canSign = pending && req.makerUid != currentUid
        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
          Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text(req.actionSummary, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
              StatusPill(req.status, MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Text(req.payloadSummary, style = MaterialTheme.typography.bodySmall)
            Text("Maker: ${req.makerUid.take(10)}…", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (pending) {
              if (!canSign) {
                Text(
                  "Separation of duties: You are the Maker. A second authorized Checker account must sign.",
                  style = MaterialTheme.typography.labelSmall,
                  color = WarningAmber
                )
              } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                  Button(
                    onClick = { onResolveApproval(req, true, null) },
                    enabled = !isBusy,
                    modifier = Modifier.weight(1f)
                  ) {
                    Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Approve")
                  }
                  OutlinedButton(
                    onClick = { rejectDialogTarget = req },
                    enabled = !isBusy,
                    modifier = Modifier.weight(1f)
                  ) {
                    Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reject")
                  }
                }
              }
            }
          }
        }
      }
    }

    item {
      Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text("Propose New Immutable Policy Version (v${snap.policy.version + 1})", style = MaterialTheme.typography.titleSmall)
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = hcUgxText, onValueChange = { hcUgxText = it },
              label = { Text("HC UGX/kg") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              modifier = Modifier.weight(1f), singleLine = true
            )
            OutlinedTextField(
              value = minWcText, onValueChange = { minWcText = it },
              label = { Text("Min Working Cap UGX") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              modifier = Modifier.weight(1.4f), singleLine = true
            )
          }
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedTextField(value = rewText, onValueChange = { rewText = it }, label = { Text("Picker%") }, modifier = Modifier.weight(1f), singleLine = true)
            OutlinedTextField(value = opsText, onValueChange = { opsText = it }, label = { Text("Ops%") }, modifier = Modifier.weight(1f), singleLine = true)
            OutlinedTextField(value = contText, onValueChange = { contText = it }, label = { Text("Cont%") }, modifier = Modifier.weight(1f), singleLine = true)
            OutlinedTextField(value = expText, onValueChange = { expText = it }, label = { Text("Exp%") }, modifier = Modifier.weight(1f), singleLine = true)
          }
          if (!splitSumOk) {
            Text("Split percentages must sum to exactly 100%.", style = MaterialTheme.typography.bodySmall, color = ErrorCrimson)
          }
          Button(
            onClick = { onProposePolicy(snap.policy.version, hcVal, rVal, oVal, cVal, eVal, wcVal) },
            enabled = !isBusy && splitSumOk,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("propose_policy_button")
          ) {
            Text("Submit Policy v${snap.policy.version + 1} as Maker")
          }
        }
      }
    }

    item {
      SectionHeader(
        title = "User Roles & RBAC (${snap.allUsers.size})",
        subtitle = "Firestore roles: picker, collector, admin, health_provider"
      )
    }

    if (snap.allUsers.isEmpty()) {
      item {
        EmptyStateCard(
          icon = Icons.Filled.ManageAccounts,
          title = "No User Profiles Loaded",
          description = "Signed-in user accounts and pending role requests appear here."
        )
      }
    } else {
      items(snap.allUsers, key = { it.userId.ifBlank { it.id } }) { userAccount ->
        val existingKeys = remember(userAccount.userId, userAccount.roles, userAccount.role) {
          val parsed = userAccount.roles.map { UserRoleKey.fromKey(it) }.distinct()
          if (parsed.isNotEmpty()) parsed else listOf(UserRoleKey.fromKey(userAccount.role))
        }
        var selectedKeys by remember(userAccount.userId, existingKeys) { mutableStateOf(existingKeys) }

        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
          Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(userAccount.displayName.ifBlank { userAccount.userId }, style = MaterialTheme.typography.titleSmall)
                Text(
                  "Provider: ${userAccount.authProvider} • Primary role: ${userAccount.role}",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
              StatusPill(
                text = userAccount.roles.ifEmpty { listOf(userAccount.role) }.joinToString(", "),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
              )
            }
            if (!userAccount.requestedRole.isNullOrBlank()) {
              Text(
                text = "Requested role on sign-up: ${userAccount.requestedRole}",
                style = MaterialTheme.typography.bodySmall,
                color = WarningAmber
              )
            }
            if (isAdminUser) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                UserRoleKey.entries.forEach { roleKey ->
                  val checked = selectedKeys.contains(roleKey)
                  FilterChip(
                    selected = checked,
                    onClick = {
                      selectedKeys = if (checked && selectedKeys.size > 1) {
                        selectedKeys - roleKey
                      } else if (!checked) {
                        selectedKeys + roleKey
                      } else {
                        selectedKeys
                      }
                    },
                    label = { Text(roleKey.key) },
                    modifier = Modifier.testTag("user_role_chip_${userAccount.userId}_${roleKey.key}")
                  )
                }
              }
              Button(
                onClick = { onAssignUserRoles(userAccount.userId.ifBlank { userAccount.id }, selectedKeys) },
                enabled = !isBusy && selectedKeys != existingKeys,
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("save_roles_${userAccount.userId}")
              ) {
                Text("Update Firestore Roles")
              }
            }
          }
        }
      }
    }
  }
}
