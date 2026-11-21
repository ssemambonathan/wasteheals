package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
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
import com.example.ui.dashboard.MainDashboardScreen
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

  MainDashboardScreen(
    uiState = uiState,
    currentUid = uid,
    isBusy = isBusy,
    feedback = feedback,
    onClearFeedback = { vm.clearFeedback() },
    onSignOut = onSignOut,
    onSwitchRole = { vm.switchRole(it) },
    onSyncOfflineQueue = { vm.syncOfflineQueue() },
    onSaveOfflineCollection = { origin, hubId, pickerName, hhId, rate, gross, tare, ded, uri ->
      vm.enqueueOfflineCollection(origin, hubId, pickerName, hhId, rate, gross, tare, ded, uri)
    },
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
    },
    onInitiatePayout = { earn, prov, dest -> vm.initiatePayout(earn, prov, dest) },
    onExecuteSandboxPayout = { pay, sc -> vm.executeSandboxPayout(pay, sc) },
    onReconcileUnknownPayout = { pay, ok -> vm.reconcileUnknownPayout(pay, ok) },
    onProposePolicy = { ver, hc, r, o, c, e, wc -> vm.proposePolicy(ver, hc, r, o, c, e, wc) },
    onResolveApproval = { req, ok, reason -> vm.resolveApproval(req, ok, reason) },
    onAssignUserRoles = { targetUid, keys -> vm.assignUserRoles(targetUid, keys) },
    buildCsvExport = { vm.buildCsvComplianceReport(it) },
    onIssueVoucher = { clinic, srv, maxUgx, avail -> vm.issueVoucher(clinic, srv, maxUgx, avail) },
    onRedeemVoucher = { vch, amt -> vm.redeemVoucher(vch, amt) },
    onPayClaim = { clm, name -> vm.payClaim(clm, name) },
    onRegisterHousehold = { comm, hub, members, day -> vm.registerHousehold(comm, hub, members, day) },
    onFileDispute = { cat, ref, desc -> vm.fileDispute(cat, ref, desc) },
    onResolveDispute = { dsp, notes -> vm.resolveDispute(dsp, notes) }
  )
}
