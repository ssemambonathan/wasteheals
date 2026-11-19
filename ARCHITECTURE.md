# WasteHeals Architecture

## 1. High-Level Stack
- **Client Runtime**: Native Android (minSdk 24, targetSdk/compileSdk 36), Kotlin, Jetpack Compose, Material Design 3 (`WasteHealsTheme` with bundled `Space Grotesk`, `Plus Jakarta Sans`, and `JetBrains Mono` fonts).
- **Authentication**: Google Sign-In via Jetpack `CredentialManager` (`GetGoogleIdOption` + `GetSignInWithGoogleOption`) backed by Firebase Authentication.
- **Cloud Persistence & Security**: Named Cloud Firestore database (`ai-studio-android-wastehea-482690c8-0e98-4f96-8d87-e26e89b545ce`) hardened by zero-trust `firestore.rules`.
- **Offline Local Store**: Room SQLite (`wasteheals_offline.db`) for queued field weighings (`offline_collections`) and hub scale calibrations (`scale_calibrations`).

## 2. Layered Package Structure (`com.example`)
- `domain/model/`: Immutable Kotlin enums (`WasteHealsEnums.kt`) and Firestore `@Keep`/`@IgnoreExtraProperties` models (`FirestoreModels.kt`).
- `domain/engine/`: Pure deterministic domain engines (no Android/Firebase framework coupling):
  - `MoneyAndWeight.kt`: Exact `BigDecimal` (`RoundingMode.HALF_UP`) arithmetic using whole `UGX` (`Long`) and `milliKg` (`1,000 = 1.000 kg`).
  - `PricingAndWeighingEngine.kt`: Tare/deduction validation & immutable rate calculation.
  - `DoubleEntryAccountingEngine.kt`: Balanced journal builder (`Debits == Credits`), trial balance aggregator, and balance/income extracts.
  - `SurplusAndLiquidityEngine.kt`: Liquidity protection gate & 50/25/15/10 weighted picker bonus allocator.
  - `MoMoPaymentEngine.kt`: Payout state transitions, HMAC-SHA256 webhook verification, and Sandbox Failure Simulator.
- `data/local/`: Room database, entities, and DAOs.
- `data/remote/`: `WasteHealsCatalogRepository`, `WasteHealsOperationsRepository`, `WasteHealsFinanceHealthRepository`, and `FirestoreErrorHandler`.
- `ui/`: `AuthScreen`, `CredentialAuthHelper`, `WasteHealsViewModel` (`StateFlow<UiState<WasteHealsSnapshot>>`), `WasteHealsComponents`, and the 5 adaptive workspace screens (`WorkspaceHomeScreen`, `CollectionScreen`, `OperationsAndMarketScreen`, `FinanceAndGovernanceScreen`, `HealthcareCommunityScreen`).

## 3. Adaptive Navigation & Identity Lifecycle
- `MainActivity` observes `Firebase.auth.authStateFlow()`.
- Authenticated sessions are keyed by `key(uid)` so switching accounts tears down and recreates the `WasteHealsViewModel` and Firestore snapshot listeners cleanly.
- `BoxWithConstraints` switches between bottom `NavigationBar` (`< 600.dp`) and side `NavigationRail` (`>= 600.dp`).
