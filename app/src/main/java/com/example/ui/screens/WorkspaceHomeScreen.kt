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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Compost
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Recycling
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import com.example.ui.theme.DeepForestHero
import com.example.ui.theme.ErrorCrimson
import com.example.ui.theme.IceBlueBorder
import com.example.ui.theme.IceBlueSurface
import com.example.ui.theme.SavannaContainer
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TabularCompactStyle
import com.example.ui.theme.TabularHeroMoneyStyle
import com.example.ui.theme.TabularMoneyStyle
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

  val totalEarned = snap.earnings.sumOf { it.amountUgxMinor }
  val settledEarnings = snap.earnings.filter { it.payoutStatus == PayoutStatus.SETTLED.name }.sumOf { it.amountUgxMinor }
  val availablePickerCash = (totalEarned - settledEarnings).coerceAtLeast(0L)
  val heroUgxAmount = if (activeRole.isPrivileged) ledger.operatingCashUgx else availablePickerCash
  val heroHeaderLabel = if (activeRole.isPrivileged) "TREASURY OPERATING CASH" else "GUARANTEED AVAILABLE CASH"
  val displayName = snap.profile?.displayName?.ifBlank { null } ?: "WasteHeals Member"
  val shortId = "#WH-${(snap.profile?.userId ?: "KMP").take(6).uppercase()}"
  val primaryHub = snap.hubs.firstOrNull()?.name ?: "Kampala Central Hub"

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .testTag("workspace_home_list"),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    // 1. Verified Operator / Collector Identity Card (Screenshot 4 top card)
    item {
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            StatusPill(
              text = activeRole.title.uppercase(),
              containerColor = MaterialTheme.colorScheme.primaryContainer,
              contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
              showDot = true
            )
            StatusPill(
              text = "POLICY v${snap.policy.version}",
              containerColor = MaterialTheme.colorScheme.surfaceVariant,
              contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Box {
              Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.size(50.dp)
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Text(
                    text = displayName.trim().take(2).uppercase(),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                  )
                }
              }
              Surface(
                color = SuccessGreen,
                shape = CircleShape,
                border = BorderStroke(2.dp, Color.White),
                modifier = Modifier
                  .size(18.dp)
                  .align(Alignment.BottomEnd)
              ) {
                Icon(
                  imageVector = Icons.Filled.Check,
                  contentDescription = "Verified",
                  tint = Color.White,
                  modifier = Modifier.padding(2.dp)
                )
              }
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text(
                text = displayName,
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                StatusPill(
                  text = shortId,
                  containerColor = MaterialTheme.colorScheme.primaryContainer,
                  contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                  Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(13.dp)
                  )
                  Text(
                    text = primaryHub,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                }
              }
            }
          }

          Surface(
            color = IceBlueSurface,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, IceBlueBorder),
            onClick = { onNavigateTab(1) }
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Filled.QrCode2, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column {
                  Text("Rapid Scale Weigh-In Pass", style = MaterialTheme.typography.titleSmall)
                  Text(
                    "Open calibrated weighing console",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }
              Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(8.dp)
              ) {
                Text(
                  text = "Collect",
                  style = MaterialTheme.typography.labelLarge,
                  color = Color.White,
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
              }
            }
          }
        }
      }
    }

    // 2. Dark Forest Emerald Hero Cash & MoMo Card (Screenshot 4 signature hero card)
    item {
      Surface(
        color = DeepForestHero,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              Icon(
                imageVector = Icons.Filled.AccountBalanceWallet,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(16.dp)
              )
              Text(
                text = heroHeaderLabel,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White.copy(alpha = 0.85f)
              )
            }
            StatusPill(
              text = "MTN & Airtel MoMo",
              containerColor = Color.White.copy(alpha = 0.16f),
              contentColor = Color.White
            )
          }

          Text(
            text = MoneyAndWeight.formatUgx(heroUgxAmount),
            style = TabularHeroMoneyStyle,
            color = Color.White
          )

          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(
              imageVector = Icons.Filled.CheckCircle,
              contentDescription = null,
              tint = Color(0xFF7ADBA5),
              modifier = Modifier.size(15.dp)
            )
            Text(
              text = "100% Double-Entry Verified • Direct MoMo (Wallet Off)",
              style = MaterialTheme.typography.bodySmall,
              color = Color.White.copy(alpha = 0.88f)
            )
          }

          Button(
            onClick = { onNavigateTab(3) },
            colors = ButtonDefaults.buttonColors(
              containerColor = Color.White,
              contentColor = DeepForestHero
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Icon(Icons.Filled.Payments, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = if (activeRole.isPrivileged) "Open Treasury Ledger & MoMo" else "View Earnings & MoMo Payouts",
              style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
            )
          }

          Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            HeroStatTile(
              label = "ACCEPTED KG",
              value = MoneyAndWeight.formatKg(snap.totalAcceptedKgMilli),
              modifier = Modifier.weight(1f)
            )
            HeroStatTile(
              label = "SOLD KG",
              value = MoneyAndWeight.formatKg(snap.totalConfirmedSoldKgMilli),
              modifier = Modifier.weight(1f)
            )
            HeroStatTile(
              label = "HC FUNDED",
              value = MoneyAndWeight.formatUgx(ledger.restrictedHealthcareCashUgx),
              modifier = Modifier.weight(1f)
            )
          }
        }
      }
    }

    // Offline Sync Alert Banner if queued events exist
    if (pendingSyncCount > 0) {
      item {
        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = SavannaContainer),
          border = BorderStroke(1.dp, WarningAmber.copy(alpha = 0.45f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "$pendingSyncCount Offline Collection(s) Buffered",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer
              )
              Text(
                text = "Provisional receipts sync idempotently before hub acceptance.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer
              )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
              onClick = onSyncOffline,
              modifier = Modifier.testTag("home_sync_offline_button")
            ) {
              Icon(Icons.Filled.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Sync")
            }
          }
        }
      }
    }

    // 3. Core Platform Views Switcher Cards
    item {
      SectionHeader(
        title = "Core Platform Views",
        subtitle = "Switch directly to Waste Collection, Financial Ledger, or Healthcare Services"
      )
    }

    item {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        DashboardModuleCard(
          title = "Waste Collection & Weighing",
          subtitle = "${MoneyAndWeight.formatKg(snap.totalAcceptedKgMilli)} verified • $pendingSyncCount offline buffered",
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

    // 4. Community Healthcare Shield Card (Screenshot 4 Healthcare Shield)
    item {
      val topVoucher = snap.vouchers.firstOrNull()
      val topClinic = snap.clinics.firstOrNull()
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Icon(Icons.Filled.LocalHospital, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
              Column {
                Text(
                  text = "EVERY KG PROTECTS YOUR HEALTH",
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.primary
                )
                Text("Community Healthcare Shield", style = MaterialTheme.typography.titleLarge)
              }
            }
            StatusPill(
              text = "ACTIVE",
              containerColor = IceBlueSurface,
              contentColor = MaterialTheme.colorScheme.primary,
              showDot = true
            )
          }

          Surface(
            color = IceBlueSurface,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, IceBlueBorder),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              Image(
                painter = painterResource(id = R.drawable.img_hero_kampala),
                contentDescription = "Kampala Healthcare Coverage",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                  .size(48.dp)
                  .clip(RoundedCornerShape(10.dp))
              )
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "Restricted Healthcare Reserve",
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.primary
                )
                Text(
                  text = "Funded Cash: ${MoneyAndWeight.formatUgx(ledger.restrictedHealthcareCashUgx)}",
                  style = MaterialTheme.typography.titleSmall
                )
                Text(
                  text = topVoucher?.let { "Active Voucher: ${it.voucherCode} (${it.serviceName})" }
                    ?: "Accrued UGX ${snap.policy.healthcareUgxPerKg}/kg on confirmed buyer sales",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
            }
          }

          if (topClinic != null) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = SuccessGreen,
                modifier = Modifier.size(16.dp)
              )
              Text(
                text = "Partner Clinic: ${topClinic.clinicName} (${topClinic.location})",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          Button(
            onClick = { onNavigateTab(4) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Icon(Icons.Filled.HealthAndSafety, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Open Healthcare Vouchers & Clinics")
          }
        }
      }
    }

    // 5. 30% Monthly Surplus Bonus Pool Card (Screenshot 4)
    item {
      val gateOk = surplus.liquidityGatePassed
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
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Icon(Icons.Filled.Star, contentDescription = null, tint = WarningAmber)
              Text("${snap.policy.pickerRewardPct}% Monthly Surplus Bonus Pool", style = MaterialTheme.typography.titleMedium)
            }
            StatusPill(
              text = if (gateOk) "Gate Passed" else "Gate Locked",
              containerColor = if (gateOk) MaterialTheme.colorScheme.primaryContainer else SavannaContainer,
              contentColor = if (gateOk) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onTertiaryContainer
            )
          }

          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("DISTRIBUTABLE REWARD POOL", style = MaterialTheme.typography.labelSmall)
                Text(MoneyAndWeight.formatUgx(surplus.pickerRewardPoolUgx), style = TabularCompactStyle)
              }
              LinearProgressIndicator(
                progress = { if (gateOk) 0.78f else 0.25f },
                modifier = Modifier
                  .fillMaxWidth()
                  .height(7.dp)
                  .clip(RoundedCornerShape(999.dp)),
                color = if (gateOk) SuccessGreen else WarningAmber
              )
              Text(
                text = if (gateOk) "Solvency & liquidity checks passed across ${snap.rewardAllocations.size} eligible picker(s)"
                else (surplus.blockedReason ?: "Requires positive surplus and working capital reserve"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      }
    }

    // 6. Role-Specific Workspace KPIs
    item {
      SectionHeader(
        title = "${activeRole.title} Workspace",
        subtitle = "Live Firestore & Room operational telemetry"
      )
    }

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
                  contentColor = if (ledger.isBalanced) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                  showDot = true
                )
              }
              Text(
                text = "Pending Verifications: $pendingVerifCount • Maker-Checker Queue: $pendingApprovals • Unresolved MoMo: $unresolvedMomo",
                style = TabularCompactStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
        item {
          Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            MetricCard(
              title = "Guaranteed Earnings",
              value = MoneyAndWeight.formatUgx(totalEarned),
              subtitle = "Settled: ${MoneyAndWeight.formatUgx(settledEarnings)}",
              icon = Icons.Filled.Payments,
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
              subtitle = "Accrued Unfunded: ${MoneyAndWeight.formatUgx(ledger.healthcareAccruedUnfundedUgx)}",
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

    // 7. Recent Certified Weigh-Ins / Kampala Pricebook
    if (snap.deliveries.isNotEmpty()) {
      item {
        SectionHeader(
          title = "Recent Certified Weigh-Ins",
          subtitle = "Audit-proof ledger trail",
          actionLabel = "All",
          onAction = { onNavigateTab(1) }
        )
      }
      items(snap.deliveries.take(3), key = { it.id }) { del ->
        Surface(
          shape = RoundedCornerShape(14.dp),
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
              Surface(
                color = IceBlueSurface,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.size(38.dp)
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(Icons.Filled.Recycling, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
              }
              Column {
                Text("${MoneyAndWeight.formatKg(del.acceptedKgMilli)} • ${del.materialName}", style = MaterialTheme.typography.titleSmall)
                Text(
                  "${del.receiptRef} • ${del.verificationStatus}",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
            Text(
              text = "+${MoneyAndWeight.formatUgx(del.totalEarningsUgxMinor)}",
              style = TabularMoneyStyle,
              color = SuccessGreen
            )
          }
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
private fun HeroStatTile(
  label: String,
  value: String,
  modifier: Modifier = Modifier
) {
  Surface(
    color = Color.White.copy(alpha = 0.12f),
    shape = RoundedCornerShape(10.dp),
    modifier = modifier
  ) {
    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = Color.White.copy(alpha = 0.78f),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      Text(
        text = value,
        style = TabularCompactStyle.copy(fontWeight = FontWeight.Bold),
        color = Color.White,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
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
        color = accentColor.copy(alpha = 0.12f)
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
