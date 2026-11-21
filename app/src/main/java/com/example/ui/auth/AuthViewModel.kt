package com.example.ui.auth

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.remote.WasteHealsCatalogRepository
import com.example.domain.model.UserRoleKey
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.FirebaseException
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.auth.userProfileChangeRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

sealed interface AuthSessionState {
  data object Unauthenticated : AuthSessionState
  data object Loading : AuthSessionState
  data class SyncingProfile(val uid: String) : AuthSessionState
  data class Authenticated(val uid: String) : AuthSessionState
  data class Error(val message: String, val uid: String? = null) : AuthSessionState
}

data class AuthUiState(
  val sessionState: AuthSessionState = AuthSessionState.Unauthenticated,
  val isLoading: Boolean = false,
  val errorMessage: String? = null,
  val infoMessage: String? = null,
  val verificationId: String? = null,
  val resendToken: PhoneAuthProvider.ForceResendingToken? = null
) {
  val currentUid: String?
    get() = when (sessionState) {
      is AuthSessionState.Authenticated -> sessionState.uid
      is AuthSessionState.SyncingProfile -> sessionState.uid
      is AuthSessionState.Error -> sessionState.uid
      else -> null
    }
}

interface AuthSessionClient {
  val currentUid: String?
  fun observeAuthUid(): Flow<String?>
  suspend fun signInWithEmailCredential(email: String, password: String): Result<Unit>
  suspend fun createEmailAccount(displayName: String, email: String, password: String): Result<Unit>
  suspend fun sendPasswordResetEmail(email: String): Result<Unit>
  suspend fun signInWithGoogleIdToken(idToken: String): Result<Unit>
  suspend fun signInWithPhoneOtp(verificationId: String, smsCode: String): Result<Unit>
  suspend fun signInWithPhoneCredential(credential: PhoneAuthCredential): Result<Unit>
  suspend fun signOut()
}

interface UserDocumentService {
  suspend fun ensureUserDocument(
    preferredDisplayName: String?,
    requestedRoleKey: UserRoleKey?
  ): Result<Unit>
}

class FirebaseAuthSessionClient(
  private val auth: FirebaseAuth = Firebase.auth
) : AuthSessionClient {

  override val currentUid: String?
    get() = auth.currentUser?.uid

  override fun observeAuthUid(): Flow<String?> = callbackFlow {
    trySend(auth.currentUser?.uid)
    val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
      trySend(firebaseAuth.currentUser?.uid)
    }
    auth.addAuthStateListener(listener)
    awaitClose { auth.removeAuthStateListener(listener) }
  }

  override suspend fun signInWithEmailCredential(email: String, password: String): Result<Unit> =
    runCatchingUnit {
      val credential = EmailAuthProvider.getCredential(email, password)
      auth.signInWithCredential(credential).await()
    }

  override suspend fun createEmailAccount(
    displayName: String,
    email: String,
    password: String
  ): Result<Unit> = runCatchingUnit {
    val result = auth.createUserWithEmailAndPassword(email, password).await()
    result.user?.updateProfile(userProfileChangeRequest { this.displayName = displayName })?.await()
  }

  override suspend fun sendPasswordResetEmail(email: String): Result<Unit> =
    runCatchingUnit {
      auth.sendPasswordResetEmail(email).await()
    }

  override suspend fun signInWithGoogleIdToken(idToken: String): Result<Unit> =
    runCatchingUnit {
      auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await()
    }

  override suspend fun signInWithPhoneOtp(verificationId: String, smsCode: String): Result<Unit> =
    runCatchingUnit {
      val credential = PhoneAuthProvider.getCredential(verificationId, smsCode)
      auth.signInWithCredential(credential).await()
    }

  override suspend fun signInWithPhoneCredential(credential: PhoneAuthCredential): Result<Unit> =
    runCatchingUnit {
      auth.signInWithCredential(credential).await()
    }

  override suspend fun signOut() {
    auth.signOut()
  }

  private inline fun runCatchingUnit(block: () -> Any?): Result<Unit> = try {
    block()
    Result.success(Unit)
  } catch (e: CancellationException) {
    throw e
  } catch (e: Exception) {
    Result.failure(e)
  }
}

class FirestoreUserDocumentService(
  private val catalogRepo: WasteHealsCatalogRepository
) : UserDocumentService {
  override suspend fun ensureUserDocument(
    preferredDisplayName: String?,
    requestedRoleKey: UserRoleKey?
  ): Result<Unit> = catalogRepo.ensureUserOnboarded(
    preferredDisplayName = preferredDisplayName,
    requestedRoleKey = requestedRoleKey
  )
}

class AuthViewModel(
  private val authClient: AuthSessionClient,
  private val userDocumentService: UserDocumentService
) : ViewModel() {

  private val _uiState = MutableStateFlow(
    AuthUiState(
      sessionState = authClient.currentUid?.let { AuthSessionState.SyncingProfile(it) }
        ?: AuthSessionState.Unauthenticated
    )
  )
  val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

  private var onboardingJob: Job? = null
  private var autoSignInAttempted = false
  private var pendingDisplayName: String? = null
  private var pendingRequestedRole: UserRoleKey? = null

  init {
    viewModelScope.launch {
      authClient.observeAuthUid()
        .distinctUntilChanged()
        .collect { uid ->
          if (uid.isNullOrBlank()) {
            onboardingJob?.cancel()
            _uiState.update { current ->
              current.copy(
                sessionState = if (current.errorMessage != null) {
                  AuthSessionState.Error(current.errorMessage, uid = null)
                } else {
                  AuthSessionState.Unauthenticated
                },
                isLoading = false
              )
            }
          } else {
            triggerUserDocumentCreation(uid)
          }
        }
    }
  }

  private fun triggerUserDocumentCreation(uid: String) {
    onboardingJob?.cancel()
    onboardingJob = viewModelScope.launch {
      _uiState.update {
        it.copy(
          sessionState = AuthSessionState.SyncingProfile(uid),
          isLoading = true,
          errorMessage = null
        )
      }
      val (fallbackName, fallbackRole) = PendingAuthOnboarding.consume()
      val resolvedName = pendingDisplayName ?: fallbackName
      val resolvedRole = pendingRequestedRole ?: fallbackRole
      pendingDisplayName = null
      pendingRequestedRole = null

      userDocumentService.ensureUserDocument(resolvedName, resolvedRole)
        .onSuccess {
          _uiState.update {
            it.copy(
              sessionState = AuthSessionState.Authenticated(uid),
              isLoading = false,
              errorMessage = null,
              verificationId = null,
              resendToken = null
            )
          }
        }
        .onFailure { err ->
          val msg = err.localizedMessage ?: "Failed to initialize user profile in Firestore."
          Log.e("AuthViewModel", "Firestore user onboarding failed for $uid", err)
          _uiState.update {
            it.copy(
              sessionState = AuthSessionState.Error(msg, uid = uid),
              isLoading = false,
              errorMessage = msg
            )
          }
        }
    }
  }

  fun retryUserDocumentCreation() {
    val uid = authClient.currentUid ?: _uiState.value.currentUid ?: return
    triggerUserDocumentCreation(uid)
  }

  fun clearMessages() {
    _uiState.update { state ->
      state.copy(
        errorMessage = null,
        infoMessage = null,
        sessionState = if (state.sessionState is AuthSessionState.Error && state.sessionState.uid == null) {
          AuthSessionState.Unauthenticated
        } else {
          state.sessionState
        }
      )
    }
  }

  fun attemptSilentGoogleSignIn(context: Context, credentialManager: CredentialManager) {
    if (autoSignInAttempted || authClient.currentUid != null) return
    autoSignInAttempted = true
    val clientId = try {
      context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
      return
    }
    val googleIdOption = GetGoogleIdOption.Builder()
      .setFilterByAuthorizedAccounts(true)
      .setServerClientId(clientId)
      .setAutoSelectEnabled(true)
      .build()
    val request = GetCredentialRequest.Builder()
      .addCredentialOption(googleIdOption)
      .build()
    viewModelScope.launch {
      try {
        val cred = credentialManager.getCredential(context, request).credential
        if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
          val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
          authClient.signInWithGoogleIdToken(token)
        }
      } catch (e: CancellationException) {
        throw e
      } catch (_: Exception) {
        // Silent auto sign-in unavailable; user signs in interactively.
      }
    }
  }

  fun signInWithGoogle(context: Context, credentialManager: CredentialManager) {
    val clientId = try {
      context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
      reportAuthError("Google Sign-In configuration missing: default_web_client_id not found")
      return
    }
    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder()
      .addCredentialOption(signInOption)
      .build()

    viewModelScope.launch {
      setLoadingState()
      try {
        val activityContext = context.findHostActivity() ?: context
        val cred = credentialManager.getCredential(activityContext, request).credential
        if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
          val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
          authClient.signInWithGoogleIdToken(token)
            .onFailure { e ->
              Log.e("AuthViewModel", "Google Firebase credential exchange failed", e)
              reportAuthError(e.localizedMessage ?: "Google Sign-In failed.")
            }
        } else {
          reportAuthError("Unexpected credential type received.")
        }
      } catch (e: CancellationException) {
        throw e
      } catch (e: GetCredentialCancellationException) {
        Log.w("Auth", "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
        _uiState.update {
          it.copy(
            isLoading = false,
            sessionState = AuthSessionState.Unauthenticated,
            errorMessage = null
          )
        }
      } catch (e: Exception) {
        Log.e("Auth", "Google Sign-In failed", e)
        reportAuthError(e.localizedMessage ?: "Sign in failed.")
      }
    }
  }

  fun signInWithEmail(email: String, password: String) {
    val cleanEmail = email.trim()
    if (cleanEmail.isEmpty() || !cleanEmail.contains("@")) {
      reportAuthError("Enter a valid email address.")
      return
    }
    if (password.length < 6) {
      reportAuthError("Password must be at least 6 characters.")
      return
    }
    viewModelScope.launch {
      setLoadingState()
      authClient.signInWithEmailCredential(cleanEmail, password)
        .onFailure { e ->
          Log.e("AuthViewModel", "Email sign-in failed", e)
          reportAuthError(
            e.localizedMessage
              ?: "Email sign-in failed. Ensure Email/Password auth is enabled in Firebase Console."
          )
        }
    }
  }

  fun createAccountWithEmail(
    displayName: String,
    email: String,
    password: String,
    requestedRole: UserRoleKey
  ) {
    val cleanName = displayName.trim()
    val cleanEmail = email.trim()
    if (cleanName.length < 2) {
      reportAuthError("Enter your full name (at least 2 characters).")
      return
    }
    if (cleanEmail.isEmpty() || !cleanEmail.contains("@")) {
      reportAuthError("Enter a valid email address.")
      return
    }
    if (password.length < 6) {
      reportAuthError("Password must be at least 6 characters.")
      return
    }
    viewModelScope.launch {
      setLoadingState()
      pendingDisplayName = cleanName
      pendingRequestedRole = requestedRole
      authClient.createEmailAccount(cleanName, cleanEmail, password)
        .onFailure { e ->
          pendingDisplayName = null
          pendingRequestedRole = null
          Log.e("AuthViewModel", "Email registration failed", e)
          reportAuthError(e.localizedMessage ?: "Account creation failed.")
        }
    }
  }

  fun sendPasswordReset(email: String) {
    val cleanEmail = email.trim()
    if (cleanEmail.isEmpty() || !cleanEmail.contains("@")) {
      reportAuthError("Enter your email address first to receive a password reset link.")
      return
    }
    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true, errorMessage = null, infoMessage = null) }
      authClient.sendPasswordResetEmail(cleanEmail)
        .onSuccess {
          _uiState.update {
            it.copy(
              isLoading = false,
              infoMessage = "Password reset email sent to $cleanEmail."
            )
          }
        }
        .onFailure { e ->
          reportAuthError(e.localizedMessage ?: "Could not send password reset email.")
        }
    }
  }

  fun startPhoneVerification(
    context: Context,
    phoneRaw: String,
    requestedRole: UserRoleKey
  ) {
    val activity = context.findHostActivity()
    if (activity == null) {
      reportAuthError("Phone verification requires an active Android Activity context.")
      return
    }
    val e164 = normalizeUgandaE164(phoneRaw)
    if (!e164.matches(Regex("^\\+[1-9]\\d{7,14}$"))) {
      reportAuthError("Enter a valid E.164 phone number (e.g. +256771234567 or 0771234567).")
      return
    }

    setLoadingState()
    pendingRequestedRole = requestedRole

    val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
      override fun onVerificationCompleted(credential: PhoneAuthCredential) {
        viewModelScope.launch {
          authClient.signInWithPhoneCredential(credential)
            .onFailure { e ->
              pendingRequestedRole = null
              reportAuthError(e.localizedMessage ?: "Automatic phone sign-in failed.")
            }
        }
      }

      override fun onVerificationFailed(e: FirebaseException) {
        pendingRequestedRole = null
        Log.e("AuthViewModel", "Phone verification failed", e)
        reportAuthError(
          e.localizedMessage
            ?: "Phone verification failed. Ensure Phone Auth is enabled in Firebase Console."
        )
      }

      override fun onCodeSent(
        verificationId: String,
        token: PhoneAuthProvider.ForceResendingToken
      ) {
        _uiState.update {
          it.copy(
            sessionState = AuthSessionState.Unauthenticated,
            isLoading = false,
            verificationId = verificationId,
            resendToken = token,
            errorMessage = null,
            infoMessage = "Verification SMS sent to $e164."
          )
        }
      }
    }

    val builder = PhoneAuthOptions.newBuilder(Firebase.auth)
      .setPhoneNumber(e164)
      .setTimeout(60L, TimeUnit.SECONDS)
      .setActivity(activity)
      .setCallbacks(callbacks)
    _uiState.value.resendToken?.let { builder.setForceResendingToken(it) }
    PhoneAuthProvider.verifyPhoneNumber(builder.build())
  }

  fun verifyPhoneOtp(smsCode: String, requestedRole: UserRoleKey) {
    val vId = _uiState.value.verificationId
    if (vId.isNullOrBlank()) {
      reportAuthError("Request an SMS verification code first.")
      return
    }
    val code = smsCode.trim()
    if (code.length < 6) {
      reportAuthError("Enter the 6-digit SMS verification code.")
      return
    }
    viewModelScope.launch {
      setLoadingState()
      pendingRequestedRole = requestedRole
      authClient.signInWithPhoneOtp(vId, code)
        .onFailure { e ->
          pendingRequestedRole = null
          Log.e("AuthViewModel", "OTP verification failed", e)
          reportAuthError(e.localizedMessage ?: "Invalid verification code.")
        }
    }
  }

  fun resetPhoneVerification() {
    _uiState.update {
      it.copy(
        verificationId = null,
        infoMessage = null,
        errorMessage = null
      )
    }
  }

  fun signOut(credentialManager: CredentialManager? = null) {
    viewModelScope.launch {
      withContext(NonCancellable) {
        onboardingJob?.cancel()
        pendingDisplayName = null
        pendingRequestedRole = null
        try {
          credentialManager?.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
          Log.w("AuthViewModel", "Failed to clear credential state: ${e.message}")
        } finally {
          authClient.signOut()
          _uiState.value = AuthUiState(sessionState = AuthSessionState.Unauthenticated)
        }
      }
    }
  }

  private fun setLoadingState() {
    _uiState.update {
      it.copy(
        sessionState = AuthSessionState.Loading,
        isLoading = true,
        errorMessage = null,
        infoMessage = null
      )
    }
  }

  private fun reportAuthError(message: String) {
    _uiState.update {
      it.copy(
        sessionState = AuthSessionState.Error(message, uid = it.currentUid),
        isLoading = false,
        errorMessage = message
      )
    }
  }

  companion object {
    fun factory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
      @Suppress("UNCHECKED_CAST")
      override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val appContext = context.applicationContext
        val catalogRepo = WasteHealsCatalogRepository(appContext)
        return AuthViewModel(
          authClient = FirebaseAuthSessionClient(Firebase.auth),
          userDocumentService = FirestoreUserDocumentService(catalogRepo)
        ) as T
      }
    }
  }
}
