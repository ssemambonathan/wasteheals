package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.example.domain.model.UserAccount
import com.example.domain.model.UserRole
import com.example.ui.dashboard.MainDashboardScreen
import com.example.ui.state.UiState
import com.example.ui.state.WasteHealsSnapshot
import com.example.ui.theme.WasteHealsTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MainDashboardUiTest {

  @get:Rule
  val composeRule = createComposeRule()

  private fun sampleSnapshot(): WasteHealsSnapshot = WasteHealsSnapshot(
    profile = UserAccount(
      userId = "uid_kampala_1",
      displayName = "Amina Namukasa",
      role = "admin",
      roles = listOf("admin", "picker", "collector", "health_provider"),
      assignedRoles = listOf(UserRole.SUPER_ADMIN.name, UserRole.COLLECTION_AGENT.name),
      activeRole = UserRole.SUPER_ADMIN.name
    )
  )

  private fun mountDashboard() {
    composeRule.setContent {
      WasteHealsTheme {
        MainDashboardScreen(
          uiState = UiState.Success(sampleSnapshot()),
          currentUid = "uid_kampala_1",
          isBusy = false,
          feedback = null,
          onClearFeedback = {},
          onSignOut = {},
          onSwitchRole = {},
          onSyncOfflineQueue = {},
          onSaveOfflineCollection = { _, _, _, _, _, _, _, _, _ -> },
          onAcceptDelivery = {},
          onCreateAndConfirmSale = { _, _, _, _, _, _, _, _ -> },
          onRecordBuyerReceipt = { _, _ -> },
          onCreateCompostBatch = { _, _, _, _ -> },
          onUpdateCompostBatch = { _, _, _, _, _, _ -> },
          onInitiatePayout = { _, _, _ -> },
          onExecuteSandboxPayout = { _, _ -> },
          onReconcileUnknownPayout = { _, _ -> },
          onProposePolicy = { _, _, _, _, _, _, _ -> },
          onResolveApproval = { _, _, _ -> },
          onAssignUserRoles = { _, _ -> },
          buildCsvExport = { "" },
          onIssueVoucher = { _, _, _, _ -> },
          onRedeemVoucher = { _, _ -> },
          onPayClaim = { _, _ -> },
          onRegisterHousehold = { _, _, _, _ -> },
          onFileDispute = { _, _, _ -> },
          onResolveDispute = { _, _ -> }
        )
      }
    }
  }

  @Test
  fun `bottom navigation bar switches between Dashboard, Waste Collection, Financial Ledger, and Healthcare Services`() {
    mountDashboard()

    composeRule.onNodeWithTag("main_bottom_navigation_bar").assertIsDisplayed()
    composeRule.onNodeWithText("Core Platform Views").assertIsDisplayed()

    // Switch to Waste Collection
    composeRule.onNodeWithTag("nav_collect").performClick()
    composeRule.onNodeWithText("Field Collection & Weighing").assertIsDisplayed()

    // Switch to Financial Ledger
    composeRule.onNodeWithTag("nav_finance").performClick()
    composeRule.onNodeWithText("Double-Entry General Ledger").assertIsDisplayed()

    // Switch to Healthcare Services
    composeRule.onNodeWithTag("nav_health_care").performClick()
    composeRule.onNodeWithTag("health_subtab_0").assertIsDisplayed()

    // Switch back to Dashboard
    composeRule.onNodeWithTag("nav_home").performClick()
    composeRule.onNodeWithTag("dashboard_card_collection").assertIsDisplayed()
  }

  @Test
  fun `dashboard quick-switch cards navigate directly to Ledger and Healthcare views`() {
    mountDashboard()

    composeRule.onNodeWithTag("workspace_home_list")
      .performScrollToNode(hasTestTag("dashboard_card_ledger"))
    composeRule.onNodeWithTag("dashboard_card_ledger").performClick()
    composeRule.onNodeWithText("Double-Entry General Ledger").assertIsDisplayed()

    composeRule.onNodeWithTag("nav_home").performClick()
    composeRule.onNodeWithTag("workspace_home_list")
      .performScrollToNode(hasTestTag("dashboard_card_healthcare"))
    composeRule.onNodeWithTag("dashboard_card_healthcare").performClick()
    composeRule.onNodeWithTag("health_subtab_0").assertIsDisplayed()
  }
}
