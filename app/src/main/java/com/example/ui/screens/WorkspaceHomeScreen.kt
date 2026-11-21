package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Compost
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Recycling
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.engine.MoneyAndWeight
import com.example.domain.model.ApprovalStatus
import com.example.domain.model.MoMoLifecycleStatus
import com.example.domain.model.PayoutStatus
import com.example.domain.model.SyncStatus
import com.example.domain.model.VerificationStatus
import com.example.domain.model.WorkspaceGroup
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.MetricCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.state.WasteHealsSnapshot
import com.example.ui.theme.ErrorCrimson
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TabularCompactStyle
import com.example.ui.theme.WarningAmber

@Composable
fun WorkspaceHomeScreen(
  snap: WasteHealsSnapshot,
  onNavigateTab: (Int) -> Unit,
  onSyncOffline: () -> Unit
) {
  val ledger = snap.ledgerSummary
  val surplus = snap.surplusEvaluation
  val activeRole = snap.activeRole
  val pendingSyncCount = snap.offlineQueue.count {
    it.syncStatus == SyncStatus.PENDING_SYNC.name || it.syncStatus == SyncStatus.SYNC_FAILED.name
  }
  val pendingVerifCount = snap.deliveries.count {
    it.verificationStatus == VerificationStatus.PENDING_VERIFICATION.name
  }
  val unresolvedMomo = snap.payouts.count {
    it.lifecycleStatus == MoMoLifecycleStatus.UNKNOWN.name || it.lifecycleStatus == MoMoLifecycleStatus.FAILED.name
  }
  val pendingApprovals = snap.approvals.count { it.status == ApprovalStatus.PENDING.name }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .testTag("workspace_home_list"),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    item {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(142.dp)
          .clip(RoundedCornerShape(18.dp))
      ) {
        Image(
          painter = painterResource(id = R.drawable.img_hero_kampala),
          contentDescription = "Kampala Circular Network Banner",
          modifier = Modifier.fillMaxSize(),
          contentScale = ContentScale.Crop
        )
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(
              Brush.verticalGradient(
                listOf(
                  Color.Black.copy(alpha = 0.20f),
                  Color.Black.copy(alpha = 0.72f)
                )
              )
            )
        )
        Column(
          modifier = Modifier
            .align(Alignment.BottomStart)
            .padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          StatusPill(
            text = "${activeRole.workspaceGroup.label.uppercase()} • POLICY v${snap.policy.version}",
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
          )
          Text(
            text = "Welcome, ${snap.profile?.displayName ?: "Member"}",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White
          )
          Text(
            text = "HC Rate: UGX ${snap.policy.healthcareUgxPerKg}/kg sold • Reward Pool: ${snap.policy.pickerRewardPct}% surplus",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.92f)
          )
        }
      }
    }

    if (pendingSyncCount > 0) {
      item {
        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "$pendingSyncCount Offline Collection(s) Queued",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer
              )
              Text(
                text = "Provisional receipts stay pending until idempotently synced and verified.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer
              )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
              onClick = onSyncOffline,
              modifier = Modifier.testTag("home_sync_offline_button")
            ) {
              Icon(Icons.Filled.Sync, contentDescription = null)
              Spacer(modifier = Modifier.width(6.dp))
              Text("Sync")
            }
          }
        }
      }
    }

    // Core Operational Views Quick Switch
    item {
      SectionHeader(
        title = "Core Platform Views",
        subtitle = "Jump directly to Collection, Financial Ledger, or Healthcare Services"
      )
    }

    item {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        DashboardModuleCard(
          title = "Waste Collection & Weighing",
          subtitle = "${MoneyAndWeight.formatKg(snap.totalAcceptedKgMilli)} verified • $pendingSyncCount offline queued",
          badgeText = if (pendingVerifCount > 0) "$pendingVerifCount pending" else "${snap.hubs.size} hubs",
          icon = Icons.Filled.Scale,
          accentColor = MaterialTheme.colorScheme.primary,
          testTag = "dashboard_card_collection",
          onClick = { onNavigateTab(1) }
        )
        DashboardModuleCard(
          title = "Financial Ledger & MoMo",
          subtitle = "Cash ${MoneyAndWeight.formatUgx(ledger.operatingCashUgx)} • ${snap.journalEntries.size} balanced journal(s)",
          badgeText = if (ledger.isBalanced) "Balanced" else "Check",
          icon = Icons.Filled.AccountBalance,
          accentColor = MaterialTheme.colorScheme.secondary,
          testTag = "dashboard_card_ledger",
          onClick = { onNavigateTab(3) }
        )
        DashboardModuleCard(
          title = "Healthcare Services (UGX ${snap.policy.healthcareUgxPerKg}/kg)",
          subtitle = "Funded ${MoneyAndWeight.formatUgx(ledger.restrictedHealthcareCashUgx)} • ${snap.vouchers.size} vouchers, ${snap.clinics.size} clinics",
          badgeText = "${snap.claims.size} claims",
          icon = Icons.Filled.HealthAndSafety,
          accentColor = SuccessGreen,
          testTag = "dashboard_card_healthcare",
          onClick = { onNavigateTab(4) }
        )
      }
    }

    item {
      SectionHeader(
        title = "${activeRole.title} Workspace",
        subtitle = "Live Firestore & Room database metrics"
      )
    }

    // Role-specific KPI rows
    when (activeRole.workspaceGroup) {
      WorkspaceGroup.FINANCE_TREASURY, WorkspaceGroup.ADMIN_GOVERNANCE -> {
        item {
          Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            MetricCard(
              title = "Operating Cash",
              value = MoneyAndWeight.formatUgx(ledger.operatingCashUgx),
              subtitle = "Receivables: ${MoneyAndWeight.formatUgx(ledger.buyerReceivablesUgx)}",
              icon = Icons.Filled.AccountBalanceWallet,
              modifier = Modifier.weight(1f)
            )
            MetricCard(
              title = "Picker Liabilities",
              value = MoneyAndWeight.formatUgx(ledger.pickerEarningsPayableUgx),
              subtitle = "Guaranteed earnings payable",
              icon = Icons.Filled.Payments,
              accentColor = MaterialTheme.colorScheme.secondary,
              modifier = Modifier.weight(1f)
            )
          }
        }
        item {
          Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            MetricCard(
              title = "HC Cash Funded",
              value = MoneyAndWeight.formatUgx(ledger.restrictedHealthcareCashUgx),
              subtitle = "Accrued Unfunded: ${MoneyAndWeight.formatUgx(ledger.healthcareAccruedUnfundedUgx)}",
              icon = Icons.Filled.HealthAndSafety,
              accentColor = SuccessGreen,
              modifier = Modifier.weight(1f)
            )
            MetricCard(
              title = "Verified Sold",
              value = MoneyAndWeight.formatKg(snap.totalConfirmedSoldKgMilli),
              subtitle = "Accepted: ${MoneyAndWeight.formatKg(snap.totalAcceptedKgMilli)}",
              icon = Icons.Filled.Eco,
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
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text("Control & Risk Indicators", style = MaterialTheme.typography.titleSmall)
                StatusPill(
                  text = if (ledger.isBalanced) "TRIAL BALANCE OK" else "IMBALANCE ALERT",
                  containerColor = if (ledger.isBalanced) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                  contentColor = if (ledger.isBalanced) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                )
              }
              Text(
                text = "Pending Hub Verifications: $pendingVerifCount • Maker-Checker Queue: $pendingApprovals • Unresolved MoMo: $unresolvedMomo",
                style = TabularCompactStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Text(
                text = if (surplus.liquidityGatePassed) {
                  "Liquidity Gate PASSED — Reward Pool: ${MoneyAndWeight.formatUgx(surplus.pickerRewardPoolUgx)}"
                } else {
                  "Liquidity Gate: ${surplus.blockedReason ?: "Insufficient distributable cash"}"
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (surplus.liquidityGatePassed) SuccessGreen else WarningAmber
              )
            }
          }
        }
      }

      WorkspaceGroup.HUB_OPERATIONS, WorkspaceGroup.FIELD_COLLECTION -> {
        item {
          Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            MetricCard(
              title = "Accepted Stock",
              value = MoneyAndWeight.formatKg(snap.totalAcceptedKgMilli),
              subtitle = "$pendingVerifCount awaiting hub verification",
              icon = Icons.Filled.Scale,
              modifier = Modifier.weight(1f)
            )
            MetricCard(
              title = "Available Inventory",
              value = MoneyAndWeight.formatKg(snap.inventory.sumOf { it.availableKgMilli }),
              subtitle = "${snap.inventory.size} traceable batch(es)",
              icon = Icons.Filled.Inventory2,
              accentColor = MaterialTheme.colorScheme.secondary,
              modifier = Modifier.weight(1f)
            )
          }
        }
        item {
          Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            MetricCard(
              title = "Compost Output",
              value = MoneyAndWeight.formatKg(snap.compostBatches.sumOf { it.outputCompostKgMilli }),
              subtitle = "${snap.compostBatches.size} active/cured organic batches",
              icon = Icons.Filled.Compost,
              accentColor = SuccessGreen,
              modifier = Modifier.weight(1f)
            )
            MetricCard(
              title = "CO2e Avoided",
              value = MoneyAndWeight.formatKg(snap.totalCo2AvoidedKgMilli),
              subtitle = "Based on confirmed sold materials",
              icon = Icons.Filled.Eco,
              modifier = Modifier.weight(1f)
            )
          }
        }
      }

      WorkspaceGroup.PICKER_PORTAL -> {
        val myEarnings = snap.earnings
        val totalEarned = myEarnings.sumOf { it.amountUgxMinor }
        val settled = myEarnings.filter { it.payoutStatus == PayoutStatus.SETTLED.name }.sumOf { it.amountUgxMinor }
        val unsettled = totalEarned - settled
        item {
          Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            MetricCard(
              title = "Guaranteed Earnings",
              value = MoneyAndWeight.formatUgx(totalEarned),
              subtitle = "Settled: ${MoneyAndWeight.formatUgx(settled)}",
              icon = Icons.Filled.Payments,
              modifier = Modifier.weight(1f)
            )
            MetricCard(
              title = "Unsettled Balance",
              value = MoneyAndWeight.formatUgx(unsettled),
              subtitle = "${snap.deliveries.size} verified/pending deliveries",
              icon = Icons.Filled.AccountBalanceWallet,
              accentColor = MaterialTheme.colorScheme.tertiary,
              modifier = Modifier.weight(1f)
            )
          }
        }
        item {
          Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            MetricCard(
              title = "HC Vouchers",
              value = "${snap.vouchers.size}",
              subtitle = "Funded Pool: ${MoneyAndWeight.formatUgx(ledger.restrictedHealthcareCashUgx)}",
              icon = Icons.Filled.HealthAndSafety,
              accentColor = SuccessGreen,
              modifier = Modifier.weight(1f)
            )
            MetricCard(
              title = "Active Disputes",
              value = "${snap.disputes.size}",
              subtitle = "Formal audit trail protection",
              icon = Icons.Filled.Gavel,
              accentColor = ErrorCrimson,
              modifier = Modifier.weight(1f)
            )
          }
        }
      }

      WorkspaceGroup.HOUSEHOLD_PORTAL -> {
        item {
          Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            MetricCard(
              title = "Registered Homes",
              value = "${snap.households.size}",
              subtitle = "Next pickup: ${snap.households.firstOrNull()?.collectionDay ?: "Schedule"}",
              icon = Icons.Filled.Recycling,
              modifier = Modifier.weight(1f)
            )
            MetricCard(
              title = "Separation Points",
              value = "${snap.households.sumOf { it.pointsBalance }} pts",
              subtitle = "Non-cash community incentives",
              icon = Icons.Filled.Eco,
              accentColor = SuccessGreen,
              modifier = Modifier.weight(1f)
            )
          }
        }
      }

      WorkspaceGroup.BUYER_MARKETPLACE -> {
        item {
          Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            MetricCard(
              title = "Available Stock",
              value = MoneyAndWeight.formatKg(snap.inventory.sumOf { it.availableKgMilli }),
              subtitle = "${snap.rates.size} active material grades",
              icon = Icons.Filled.Inventory2,
              modifier = Modifier.weight(1f)
            )
            MetricCard(
              title = "Buyer Receivables",
              value = MoneyAndWeight.formatUgx(ledger.buyerReceivablesUgx),
              subtitle = "${snap.sales.size} recorded commercial sales",
              icon = Icons.Filled.Payments,
              accentColor = MaterialTheme.colorScheme.secondary,
              modifier = Modifier.weight(1f)
            )
          }
        }
      }

      WorkspaceGroup.HEALTHCARE_CLINIC -> {
        item {
          Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            MetricCard(
              title = "Restricted HC Cash",
              value = MoneyAndWeight.formatUgx(ledger.restrictedHealthcareCashUgx),
              subtitle = "Unfunded Accrued: ${MoneyAndWeight.formatUgx(ledger.healthcareAccruedUnfundedUgx)}",
              icon = Icons.Filled.HealthAndSafety,
              accentColor = SuccessGreen,
              modifier = Modifier.weight(1f)
            )
            MetricCard(
              title = "Vouchers / Claims",
              value = "${snap.vouchers.size} / ${snap.claims.size}",
              subtitle = "${snap.clinics.size} verified Kampala clinics",
              icon = Icons.Filled.Security,
              modifier = Modifier.weight(1f)
            )
          }
        }
      }
    }

    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Button(
          onClick = { onNavigateTab(1) },
          modifier = Modifier.weight(1f)
        ) {
          Text("New Collection")
        }
        OutlinedButton(
          onClick = { onNavigateTab(2) },
          modifier = Modifier.weight(1f)
        ) {
          Text("Hub & Sales")
        }
        OutlinedButton(
          onClick = { onNavigateTab(3) },
          modifier = Modifier.weight(1f)
        ) {
          Text("Ledger & MoMo")
        }
      }
    }

    item {
      SectionHeader(
        title = "Kampala Hub Network & Active Rates",
        subtitle = "Immutable versioned purchase rates (v1+)"
      )
    }

    if (snap.rates.isEmpty()) {
      item {
        EmptyStateCard(
          icon = Icons.Filled.Recycling,
          title = "Catalog Initializing",
          description = "Kampala hubs, materials, and approved rates appear here once configured."
        )
      }
    } else {
      items(snap.rates.take(6)) { rate ->
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
              Text("${rate.materialName} (Grade ${rate.grade})", style = MaterialTheme.typography.titleSmall)
              Text(
                "Hub: ${rate.hubId} • v${rate.version} • ${rate.status}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            Column(horizontalAlignment = Alignment.End) {
              Text(
                text = "Buy: ${MoneyAndWeight.formatUgx(rate.purchasePriceUgxMinor)}/kg",
                style = TabularCompactStyle,
                color = MaterialTheme.colorScheme.primary
              )
              Text(
                text = "Sell: ${MoneyAndWeight.formatUgx(rate.buyerSellPriceUgxMinor)}/kg",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun DashboardModuleCard(
  title: String,
  subtitle: String,
  badgeText: String,
  icon: ImageVector,
  accentColor: Color,
  testTag: String,
  onClick: () -> Unit
) {
  Card(
    onClick = onClick,
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    modifier = Modifier
      .fillMaxWidth()
      .testTag(testTag)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = accentColor.copy(alpha = 0.14f)
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = accentColor,
          modifier = Modifier
            .padding(10.dp)
            .size(22.dp)
        )
      }
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleSmall)
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
      StatusPill(
        text = badgeText,
        containerColor = accentColor.copy(alpha = 0.14f),
        contentColor = accentColor
      )
      Icon(
        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
        contentDescription = "Open $title",
        tint = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}
