package com.example.ui.dashboard

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Scale
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.engine.SandboxScenario
import com.example.domain.model.ApprovalRequest
import com.example.domain.model.ApprovalStatus
import com.example.domain.model.CompostBatch
import com.example.domain.model.DisputeRecord
import com.example.domain.model.HealthcareClaim
import com.example.domain.model.HealthcareClinic
import com.example.domain.model.HealthcareVoucher
import com.example.domain.model.InventoryBatch
import com.example.domain.model.MoMoProvider
import com.example.domain.model.OriginType
import com.example.domain.model.PickerEarning
import com.example.domain.model.PickerPayout
import com.example.domain.model.PurchaseRate
import com.example.domain.model.SyncStatus
import com.example.domain.model.UserRole
import com.example.domain.model.UserRoleKey
import com.example.domain.model.VerificationStatus
import com.example.domain.model.WasteDelivery
import com.example.domain.model.WasteSale
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.RoleSwitcherSheet
import com.example.ui.components.RoleWorkspaceChip
import com.example.ui.screens.CollectionScreen
import com.example.ui.screens.FinanceAndGovernanceScreen
import com.example.ui.screens.HealthcareCommunityScreen
import com.example.ui.screens.OperationsAndMarketScreen
import com.example.ui.screens.WorkspaceHomeScreen
import com.example.ui.state.UiState
import com.example.ui.state.WasteHealsSnapshot

private data class DashboardNavTab(
  val labelRes: Int,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
  val tag: String
)

private val DASHBOARD_NAV_TABS = listOf(
  DashboardNavTab(R.string.nav_home, Icons.Filled.Dashboard, Icons.Outlined.Dashboard, "nav_home"),
  DashboardNavTab(R.string.nav_collect, Icons.Filled.Scale, Icons.Outlined.Scale, "nav_collect"),
  DashboardNavTab(R.string.nav_hub_market, Icons.Filled.Inventory2, Icons.Outlined.Inventory2, "nav_hub_market"),
  DashboardNavTab(R.string.nav_finance, Icons.Filled.AccountBalance, Icons.Outlined.AccountBalance, "nav_finance"),
  DashboardNavTab(R.string.nav_health_care, Icons.Filled.HealthAndSafety, Icons.Outlined.HealthAndSafety, "nav_health_care")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboardScreen(
  uiState: UiState<WasteHealsSnapshot>,
  currentUid: String,
  isBusy: Boolean,
  feedback: String?,
  onClearFeedback: () -> Unit,
  onSignOut: () -> Unit,
  onSwitchRole: (UserRole) -> Unit,
  onSyncOfflineQueue: () -> Unit,
  onSaveOfflineCollection: (
    originType: OriginType,
    hubId: String,
    pickerName: String,
    householdId: String?,
    rate: PurchaseRate,
    grossKgMilli: Long,
    tareKgMilli: Long,
    deductionKgMilli: Long,
    evidenceUri: String?
  ) -> Unit,
  onAcceptDelivery: (WasteDelivery) -> Unit,
  onCreateAndConfirmSale: (
    batch: InventoryBatch,
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
  ) -> Unit,
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
  buildCsvExport: (WasteHealsSnapshot) -> String,
  onIssueVoucher: (clinic: HealthcareClinic, serviceName: String, maxUgx: Long, availableFundedUgx: Long) -> Unit,
  onRedeemVoucher: (voucher: HealthcareVoucher, claimUgx: Long) -> Unit,
  onPayClaim: (claim: HealthcareClaim, clinicName: String) -> Unit,
  onRegisterHousehold: (community: String, hubId: String, members: Long, day: String) -> Unit,
  onFileDispute: (category: String, refId: String, desc: String) -> Unit,
  onResolveDispute: (dispute: DisputeRecord, notes: String) -> Unit
) {
  val snackbarHostState = remember { SnackbarHostState() }
  var selectedTab by rememberSaveable { mutableIntStateOf(0) }
  var showRoleSheet by remember { mutableStateOf(false) }

  BackHandler(enabled = selectedTab != 0) {
    selectedTab = 0
  }

  LaunchedEffect(feedback) {
    if (feedback != null) {
      snackbarHostState.showSnackbar(feedback)
      onClearFeedback()
    }
  }

  val snapshot = (uiState as? UiState.Success)?.data

  if (showRoleSheet && snapshot != null) {
    RoleSwitcherSheet(
      assignedRoles = snapshot.assignedRoles,
      activeRole = snapshot.activeRole,
      onSelectRole = onSwitchRole,
      onDismiss = { showRoleSheet = false }
    )
  }

  fun badgeCountForTab(idx: Int): Int {
    val snap = snapshot ?: return 0
    return when (idx) {
      1 -> snap.offlineQueue.count {
        it.syncStatus == SyncStatus.PENDING_SYNC.name || it.syncStatus == SyncStatus.SYNC_FAILED.name
      }
      2 -> snap.deliveries.count {
        it.verificationStatus == VerificationStatus.PENDING_VERIFICATION.name
      }
      3 -> snap.approvals.count { it.status == ApprovalStatus.PENDING.name }
      4 -> snap.claims.size
      else -> 0
    }
  }

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val useRail = maxWidth >= 600.dp

    Scaffold(
      contentWindowInsets = WindowInsets.safeDrawing,
      snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
      topBar = {
        TopAppBar(
          title = {
            Column {
              Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              Text(
                text = stringResource(R.string.kampala_pilot_badge),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
              )
            }
          },
          actions = {
            if (snapshot != null) {
              RoleWorkspaceChip(
                activeRole = snapshot.activeRole,
                onClick = { showRoleSheet = true }
              )
            }
            IconButton(
              onClick = onSignOut,
              modifier = Modifier.testTag("sign_out_button")
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.Logout,
                contentDescription = "Sign out"
              )
            }
          },
          colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
          )
        )
      },
      bottomBar = {
        if (!useRail) {
          NavigationBar(
            modifier = Modifier.testTag("main_bottom_navigation_bar")
          ) {
            DASHBOARD_NAV_TABS.forEachIndexed { idx, tab ->
              val selected = selectedTab == idx
              val count = badgeCountForTab(idx)
              val label = stringResource(tab.labelRes)
              NavigationBarItem(
                selected = selected,
                onClick = { selectedTab = idx },
                icon = {
                  BadgedBox(
                    badge = {
                      if (count > 0) {
                        Badge { Text(if (count > 9) "9+" else count.toString()) }
                      }
                    }
                  ) {
                    Icon(
                      imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                      contentDescription = label
                    )
                  }
                },
                label = {
                  Text(
                    text = label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                },
                modifier = Modifier.testTag(tab.tag)
              )
            }
          }
        }
      }
    ) { innerPadding ->
      Row(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding)
      ) {
        if (useRail) {
          NavigationRail(
            modifier = Modifier
              .fillMaxHeight()
              .testTag("main_navigation_rail")
          ) {
            DASHBOARD_NAV_TABS.forEachIndexed { idx, tab ->
              val selected = selectedTab == idx
              val count = badgeCountForTab(idx)
              val label = stringResource(tab.labelRes)
              NavigationRailItem(
                selected = selected,
                onClick = { selectedTab = idx },
                icon = {
                  BadgedBox(
                    badge = {
                      if (count > 0) {
                        Badge { Text(if (count > 9) "9+" else count.toString()) }
                      }
                    }
                  ) {
                    Icon(
                      imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                      contentDescription = label
                    )
                  }
                },
                label = { Text(label) },
                modifier = Modifier.testTag(tab.tag)
              )
            }
          }
        }

        Box(modifier = Modifier.weight(1f).fillMaxSize()) {
          when (uiState) {
            is UiState.Loading -> {
              Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
              ) {
                CircularProgressIndicator()
                Text(
                  text = "Loading Kampala Circular Network…",
                  style = MaterialTheme.typography.bodyMedium,
                  modifier = Modifier.padding(top = 12.dp)
                )
              }
            }
            is UiState.Error -> {
              Box(modifier = Modifier.padding(20.dp)) {
                EmptyStateCard(
                  icon = Icons.Filled.AccountBalance,
                  title = "Synchronization Notice",
                  description = uiState.message
                )
              }
            }
            is UiState.Success -> {
              val snap = uiState.data
              Crossfade(
                targetState = selectedTab,
                animationSpec = tween(durationMillis = 180),
                label = "dashboard_tab_transition"
              ) { tabIndex ->
                when (tabIndex) {
                  0 -> WorkspaceHomeScreen(
                    snap = snap,
                    onNavigateTab = { selectedTab = it },
                    onSyncOffline = onSyncOfflineQueue
                  )
                  1 -> CollectionScreen(
                    snap = snap,
                    isBusy = isBusy,
                    onSaveOfflineCollection = onSaveOfflineCollection,
                    onSyncOfflineQueue = onSyncOfflineQueue
                  )
                  2 -> OperationsAndMarketScreen(
                    snap = snap,
                    isBusy = isBusy,
                    onAcceptDelivery = onAcceptDelivery,
                    onCreateAndConfirmSale = onCreateAndConfirmSale,
                    onRecordBuyerReceipt = onRecordBuyerReceipt,
                    onCreateCompostBatch = onCreateCompostBatch,
                    onUpdateCompostBatch = onUpdateCompostBatch
                  )
                  3 -> FinanceAndGovernanceScreen(
                    snap = snap,
                    currentUid = currentUid,
                    isBusy = isBusy,
                    onInitiatePayout = onInitiatePayout,
                    onExecuteSandboxPayout = onExecuteSandboxPayout,
                    onReconcileUnknownPayout = onReconcileUnknownPayout,
                    onProposePolicy = onProposePolicy,
                    onResolveApproval = onResolveApproval,
                    onAssignUserRoles = onAssignUserRoles,
                    buildCsvExport = buildCsvExport
                  )
                  4 -> HealthcareCommunityScreen(
                    snap = snap,
                    isBusy = isBusy,
                    onIssueVoucher = onIssueVoucher,
                    onRedeemVoucher = onRedeemVoucher,
                    onPayClaim = onPayClaim,
                    onRegisterHousehold = onRegisterHousehold,
                    onFileDispute = onFileDispute,
                    onResolveDispute = onResolveDispute
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}
