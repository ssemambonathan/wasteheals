package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.remote.WasteHealsCatalogRepository
import com.example.data.remote.WasteHealsFinanceHealthRepository
import com.example.data.remote.WasteHealsOperationsRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.AuthSessionState
import com.example.ui.auth.AuthViewModel
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.RoleSwitcherSheet
import com.example.ui.components.RoleWorkspaceChip
import com.example.ui.screens.CollectionScreen
import com.example.ui.screens.FinanceAndGovernanceScreen
import com.example.ui.screens.HealthcareCommunityScreen
import com.example.ui.screens.OperationsAndMarketScreen
import com.example.ui.screens.WorkspaceHomeScreen
import com.example.ui.state.UiState
import com.example.ui.state.WasteHealsViewModel
import com.example.ui.theme.WasteHealsTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      WasteHealsTheme {
        WasteHealsRoot()
      }
    }
  }
}

private data class NavTab(
  val labelRes: Int,
  val icon: ImageVector,
  val tag: String
)

private val NAV_TABS = listOf(
  NavTab(R.string.nav_home, Icons.Filled.Dashboard, "nav_home"),
  NavTab(R.string.nav_collect, Icons.Filled.Scale, "nav_collect"),
  NavTab(R.string.nav_hub_market, Icons.Filled.Inventory2, "nav_hub_market"),
  NavTab(R.string.nav_finance, Icons.Filled.AccountBalance, "nav_finance"),
  NavTab(R.string.nav_health_care, Icons.Filled.HealthAndSafety, "nav_health_care")
)

@Composable
fun WasteHealsRoot() {
  val context = LocalContext.current
  val credentialManager = remember(context) { CredentialManager.create(context) }
  val authViewModel: AuthViewModel = viewModel(
    factory = AuthViewModel.factory(context.applicationContext)
  )
  val authState by authViewModel.uiState.collectAsStateWithLifecycle()

  when (val session = authState.sessionState) {
    is AuthSessionState.Unauthenticated,
    is AuthSessionState.Loading -> AuthScreen(authViewModel = authViewModel)
    is AuthSessionState.SyncingProfile -> {
      Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
      ) {
        CircularProgressIndicator()
        Text(
          text = "Initializing Firestore user profile…",
          style = MaterialTheme.typography.bodyMedium,
          modifier = Modifier.padding(top = 12.dp)
        )
      }
    }
    is AuthSessionState.Error -> {
      if (session.uid == null) {
        AuthScreen(authViewModel = authViewModel)
      } else {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center
        ) {
          EmptyStateCard(
            icon = Icons.Filled.AccountBalance,
            title = "Profile Setup Error",
            description = session.message
          )
          Button(
            onClick = { authViewModel.retryUserDocumentCreation() },
            modifier = Modifier
              .padding(top = 16.dp)
              .testTag("retry_profile_sync_button")
          ) {
            Text("Retry Firestore Profile Setup")
          }
          OutlinedButton(
            onClick = { authViewModel.signOut(credentialManager) },
            modifier = Modifier
              .padding(top = 8.dp)
              .testTag("sign_out_button")
          ) {
            Text("Sign Out")
          }
        }
      }
    }
    is AuthSessionState.Authenticated -> {
      key(session.uid) {
        WasteHealsAuthenticatedScaffold(
          uid = session.uid,
          onSignOut = { authViewModel.signOut(credentialManager) }
        )
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WasteHealsAuthenticatedScaffold(
  uid: String,
  onSignOut: () -> Unit
) {
  val context = LocalContext.current

  val vm: WasteHealsViewModel = viewModel(
    key = uid,
    factory = object : ViewModelProvider.Factory {
      @Suppress("UNCHECKED_CAST")
      override fun <T : ViewModel> create(modelClass: Class<T>): T = WasteHealsViewModel(
        catalogRepo = WasteHealsCatalogRepository(context.applicationContext),
        opsRepo = WasteHealsOperationsRepository(context.applicationContext),
        finRepo = WasteHealsFinanceHealthRepository(context.applicationContext),
        currentUserId = uid
      ) as T
    }
  )

  val uiState by vm.uiState.collectAsStateWithLifecycle()
  val isBusy by vm.isBusy.collectAsStateWithLifecycle()
  val feedback by vm.feedback.collectAsStateWithLifecycle()
  val snackbarHostState = remember { SnackbarHostState() }

  var selectedTab by rememberSaveable { mutableIntStateOf(0) }
  var showRoleSheet by remember { mutableStateOf(false) }

  LaunchedEffect(feedback) {
    val msg = feedback
    if (msg != null) {
      snackbarHostState.showSnackbar(msg)
      vm.clearFeedback()
    }
  }

  val snapshot = (uiState as? UiState.Success)?.data

  if (showRoleSheet && snapshot != null) {
    RoleSwitcherSheet(
      assignedRoles = snapshot.assignedRoles,
      activeRole = snapshot.activeRole,
      onSelectRole = { vm.switchRole(it) },
      onDismiss = { showRoleSheet = false }
    )
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
          NavigationBar {
            NAV_TABS.forEachIndexed { idx, tab ->
              NavigationBarItem(
                selected = selectedTab == idx,
                onClick = { selectedTab = idx },
                icon = { Icon(tab.icon, contentDescription = stringResource(tab.labelRes)) },
                label = {
                  Text(
                    text = stringResource(tab.labelRes),
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
          NavigationRail(modifier = Modifier.fillMaxHeight()) {
            NAV_TABS.forEachIndexed { idx, tab ->
              NavigationRailItem(
                selected = selectedTab == idx,
                onClick = { selectedTab = idx },
                icon = { Icon(tab.icon, contentDescription = stringResource(tab.labelRes)) },
                label = { Text(stringResource(tab.labelRes)) },
                modifier = Modifier.testTag(tab.tag)
              )
            }
          }
        }

        Box(modifier = Modifier.weight(1f).fillMaxSize()) {
          when (val state = uiState) {
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
                  description = state.message
                )
              }
            }
            is UiState.Success -> {
              val snap = state.data
              when (selectedTab) {
                0 -> WorkspaceHomeScreen(
                  snap = snap,
                  onNavigateTab = { selectedTab = it },
                  onSyncOffline = { vm.syncOfflineQueue() }
                )
                1 -> CollectionScreen(
                  snap = snap,
                  isBusy = isBusy,
                  onSaveOfflineCollection = { origin, hubId, pickerName, hhId, rate, gross, tare, ded, uri ->
                    vm.enqueueOfflineCollection(origin, hubId, pickerName, hhId, rate, gross, tare, ded, uri)
                  },
                  onSyncOfflineQueue = { vm.syncOfflineQueue() }
                )
                2 -> OperationsAndMarketScreen(
                  snap = snap,
                  isBusy = isBusy,
                  onAcceptDelivery = { vm.acceptDelivery(it) },
                  onCreateAndConfirmSale = { batch, buyer, disp, sold, rej, reason, sellPrice, hcRate ->
                    vm.createAndConfirmSale(batch, buyer, disp, sold, rej, reason, sellPrice, hcRate)
                  },
                  onRecordBuyerReceipt = { sale, cash -> vm.recordBuyerReceipt(sale, cash) },
                  onCreateCompostBatch = { hubId, inputKg, comp, cost ->
                    vm.createCompostBatch(hubId, inputKg, comp, cost)
                  },
                  onUpdateCompostBatch = { batch, moist, temp, outKg, rejKg, approve ->
                    vm.updateCompostBatch(batch, moist, temp, outKg, rejKg, approve)
                  }
                )
                3 -> FinanceAndGovernanceScreen(
                  snap = snap,
                  currentUid = uid,
                  isBusy = isBusy,
                  onInitiatePayout = { earn, prov, dest -> vm.initiatePayout(earn, prov, dest) },
                  onExecuteSandboxPayout = { pay, sc -> vm.executeSandboxPayout(pay, sc) },
                  onReconcileUnknownPayout = { pay, ok -> vm.reconcileUnknownPayout(pay, ok) },
                  onProposePolicy = { ver, hc, r, o, c, e, wc -> vm.proposePolicy(ver, hc, r, o, c, e, wc) },
                  onResolveApproval = { req, ok, reason -> vm.resolveApproval(req, ok, reason) },
                  onAssignUserRoles = { targetUid, keys -> vm.assignUserRoles(targetUid, keys) },
                  buildCsvExport = { vm.buildCsvComplianceReport(it) }
                )
                4 -> HealthcareCommunityScreen(
                  snap = snap,
                  isBusy = isBusy,
                  onIssueVoucher = { clinic, srv, maxUgx, avail -> vm.issueVoucher(clinic, srv, maxUgx, avail) },
                  onRedeemVoucher = { vch, amt -> vm.redeemVoucher(vch, amt) },
                  onPayClaim = { clm, name -> vm.payClaim(clm, name) },
                  onRegisterHousehold = { comm, hub, members, day -> vm.registerHousehold(comm, hub, members, day) },
                  onFileDispute = { cat, ref, desc -> vm.fileDispute(cat, ref, desc) },
                  onResolveDispute = { dsp, notes -> vm.resolveDispute(dsp, notes) }
                )
              }
            }
          }
        }
      }
    }
  }
}
