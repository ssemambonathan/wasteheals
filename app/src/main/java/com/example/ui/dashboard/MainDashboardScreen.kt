package com.example.ui.dashboard

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocationOn
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
import com.example.ui.components.StatusPill
import com.example.ui.screens.CollectionScreen
import com.example.ui.screens.FinanceAndGovernanceScreen
import com.example.ui.screens.HealthcareCommunityScreen
import com.example.ui.screens.OperationsAndMarketScreen
import com.example.ui.screens.WorkspaceHomeScreen
import com.example.ui.state.UiState
import com.example.ui.state.WasteHealsSnapshot
import com.example.ui.theme.IceBlueBorder
import com.example.ui.theme.IceBlueSurface
import com.example.ui.theme.SuccessGreen

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
    val useSidebar = maxWidth >= 600.dp

    Scaffold(
      containerColor = MaterialTheme.colorScheme.background,
      contentWindowInsets = WindowInsets.safeDrawing,
      snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
      topBar = {
        Column {
          TopAppBar(
            title = {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                Surface(
                  color = MaterialTheme.colorScheme.primary,
                  shape = RoundedCornerShape(10.dp),
                  modifier = Modifier.size(34.dp)
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Icon(
                      imageVector = Icons.Filled.Eco,
                      contentDescription = null,
                      tint = Color.White,
                      modifier = Modifier.size(20.dp)
                    )
                  }
                }
                Column {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                  ) {
                    Text(
                      text = stringResource(R.string.app_name),
                      style = MaterialTheme.typography.titleLarge,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                    Surface(
                      color = MaterialTheme.colorScheme.surfaceVariant,
                      shape = RoundedCornerShape(6.dp)
                    ) {
                      Text(
                        text = "KAMPALA",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                      )
                    }
                  }
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                  ) {
                    Box(
                      modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(SuccessGreen)
                    )
                    Text(
                      text = "SYNC READY • UGX",
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                      maxLines = 1
                    )
                  }
                }
              }
            },
            actions = {
              if (snapshot != null) {
                RoleWorkspaceChip(
                  activeRole = snapshot.activeRole,
                  onClick = { showRoleSheet = true }
                )
              }
              Spacer(modifier = Modifier.width(4.dp))
              Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = CircleShape,
                modifier = Modifier.size(38.dp)
              ) {
                IconButton(
                  onClick = onSignOut,
                  modifier = Modifier.testTag("sign_out_button")
                ) {
                  Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = "Sign out",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                  )
                }
              }
              Spacer(modifier = Modifier.width(8.dp))
            },
            colors = TopAppBarDefaults.topAppBarColors(
              containerColor = MaterialTheme.colorScheme.surface
            )
          )
          HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        }
      },
      bottomBar = {
        if (!useSidebar) {
          Column {
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            NavigationBar(
              containerColor = MaterialTheme.colorScheme.surface,
              tonalElevation = 0.dp,
              modifier = Modifier.testTag("main_bottom_navigation_bar")
            ) {
              DASHBOARD_NAV_TABS.forEachIndexed { idx, tab ->
                val selected = selectedTab == idx
                val count = badgeCountForTab(idx)
                val label = stringResource(tab.labelRes)
                NavigationBarItem(
                  selected = selected,
                  onClick = { selectedTab = idx },
                  colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                  ),
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
                      style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                      ),
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
      }
    ) { innerPadding ->
      Row(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding)
      ) {
        if (useSidebar) {
          EnterpriseSidebarRail(
            selectedTab = selectedTab,
            activeHubName = snapshot?.hubs?.firstOrNull()?.name ?: "Kampala Central Hub",
            badgeProvider = ::badgeCountForTab,
            onSelectTab = { selectedTab = it }
          )
          VerticalDivider(color = MaterialTheme.colorScheme.outline)
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

@Composable
private fun EnterpriseSidebarRail(
  selectedTab: Int,
  activeHubName: String,
  badgeProvider: (Int) -> Int,
  onSelectTab: (Int) -> Unit
) {
  Surface(
    color = MaterialTheme.colorScheme.surface,
    modifier = Modifier
      .width(236.dp)
      .fillMaxHeight()
      .testTag("main_navigation_rail")
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(14.dp)
        .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Surface(
        color = IceBlueSurface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, IceBlueBorder),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text(
            text = "ACTIVE OPERATING HUB",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(
              imageVector = Icons.Filled.LocationOn,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(16.dp)
            )
            Text(
              text = activeHubName,
              style = MaterialTheme.typography.titleSmall,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
          Text(
            text = "Enterprise Circular Core • v2.4",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      Text(
        text = "OPERATIONS & TREASURY",
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 4.dp)
      )

      DASHBOARD_NAV_TABS.forEachIndexed { idx, tab ->
        val selected = selectedTab == idx
        val count = badgeProvider(idx)
        val label = stringResource(tab.labelRes)
        Surface(
          color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelectTab(idx) }
            .testTag(tab.tag)
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
              modifier = Modifier.weight(1f)
            ) {
              Icon(
                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                contentDescription = label,
                tint = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(19.dp)
              )
              Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
            if (count > 0) {
              StatusPill(
                text = count.toString(),
                containerColor = if (selected) Color.White.copy(alpha = 0.22f) else MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = if (selected) Color.White else MaterialTheme.colorScheme.onTertiaryContainer
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.weight(1f))

      Surface(
        color = IceBlueSurface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, IceBlueBorder),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier.padding(10.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Column {
            Text("Firestore + Room Sync", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
            Text("Named DB • Offline Ready", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
          Box(
            modifier = Modifier
              .size(8.dp)
              .clip(CircleShape)
              .background(SuccessGreen)
          )
        }
      }
    }
  }
}
