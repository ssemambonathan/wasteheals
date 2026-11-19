package com.example.ui.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.domain.model.UserRoleKey
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.FirebaseException
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.auth.userProfileChangeRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Transient onboarding hint passed from registration UI to Firestore user creation,
 * never bypassing server-side RBAC rules (non-admin accounts always initialize as `picker`).
 */
object PendingAuthOnboarding {
  @Volatile
  var displayName: String? = null
  @Volatile
  var requestedRole: UserRoleKey? = null

  fun consume(): Pair<String?, UserRoleKey?> {
    val result = displayName to requestedRole
    displayName = null
    requestedRole = null
    return result
  }
}

fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
  val listener = FirebaseAuth.AuthStateListener { auth -> trySend(auth.currentUser) }
  addAuthStateListener(listener)
  awaitClose { removeAuthStateListener(listener) }
}

fun attemptAutoSignIn(
  context: Context,
  credentialManager: CredentialManager,
  onAuthSuccess: () -> Unit,
  onUnauthenticated: () -> Unit,
  scope: CoroutineScope
) {
  if (Firebase.auth.currentUser != null) {
    onAuthSuccess()
    return
  }
  val clientId = try {
    context.getString(R.string.default_web_client_id)
  } catch (_: Exception) {
    onUnauthenticated()
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
  scope.launch {
    try {
      val cred = credentialManager.getCredential(context, request).credential
      if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
        Firebase.auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
        onAuthSuccess()
      } else {
        onUnauthenticated()
      }
    } catch (_: Exception) {
      onUnauthenticated()
    }
  }
}

fun onGoogleSignInClicked(
  context: Context,
  credentialManager: CredentialManager,
  onAuthSuccess: () -> Unit,
  onAuthError: (String) -> Unit,
  scope: CoroutineScope,
  onAuthCancelled: () -> Unit = {}
) {
  val clientId = try {
    context.getString(R.string.default_web_client_id)
  } catch (_: Exception) {
    onAuthError("Google Sign-In configuration missing: default_web_client_id not found")
    return
  }
  val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
  val request = GetCredentialRequest.Builder()
    .addCredentialOption(signInOption)
    .build()
  scope.launch {
    try {
      val activityContext = context.findActivity() ?: context
      val cred = credentialManager.getCredential(activityContext, request).credential
      if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
        Firebase.auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
        onAuthSuccess()
      } else {
        onAuthError("Unexpected credential type received.")
      }
    } catch (e: GetCredentialCancellationException) {
      Log.w("Auth", "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
      onAuthCancelled()
    } catch (e: Exception) {
      Log.e("Auth", "Google Sign-In failed", e)
      onAuthError(e.localizedMessage ?: "Sign in failed")
    }
  }
}

fun signInWithEmailCredential(
  email: String,
  password: String,
  scope: CoroutineScope,
  onAuthSuccess: () -> Unit,
  onAuthError: (String) -> Unit
) {
  val cleanEmail = email.trim()
  if (cleanEmail.isEmpty() || !cleanEmail.contains("@")) {
    onAuthError("Enter a valid email address.")
    return
  }
  if (password.length < 6) {
    onAuthError("Password must be at least 6 characters.")
    return
  }
  scope.launch {
    try {
      val credential = EmailAuthProvider.getCredential(cleanEmail, password)
      Firebase.auth.signInWithCredential(credential).await()
      onAuthSuccess()
    } catch (e: Exception) {
      Log.e("Auth", "Email sign-in failed", e)
      onAuthError(e.localizedMessage ?: "Email sign-in failed. Ensure Email/Password auth is enabled in Firebase Console.")
    }
  }
}

fun createEmailAccount(
  displayName: String,
  email: String,
  password: String,
  requestedRole: UserRoleKey,
  scope: CoroutineScope,
  onAuthSuccess: () -> Unit,
  onAuthError: (String) -> Unit
) {
  val cleanName = displayName.trim()
  val cleanEmail = email.trim()
  if (cleanName.length < 2) {
    onAuthError("Enter your full name (at least 2 characters).")
    return
  }
  if (cleanEmail.isEmpty() || !cleanEmail.contains("@")) {
    onAuthError("Enter a valid email address.")
    return
  }
  if (password.length < 6) {
    onAuthError("Password must be at least 6 characters.")
    return
  }
  scope.launch {
    try {
      PendingAuthOnboarding.displayName = cleanName
      PendingAuthOnboarding.requestedRole = requestedRole
      val result = Firebase.auth.createUserWithEmailAndPassword(cleanEmail, password).await()
      result.user?.updateProfile(userProfileChangeRequest { this.displayName = cleanName })?.await()
      onAuthSuccess()
    } catch (e: Exception) {
      PendingAuthOnboarding.consume()
      Log.e("Auth", "Email registration failed", e)
      onAuthError(e.localizedMessage ?: "Account creation failed.")
    }
  }
}

fun sendEmailPasswordReset(
  email: String,
  scope: CoroutineScope,
  onSent: (String) -> Unit,
  onError: (String) -> Unit
) {
  val cleanEmail = email.trim()
  if (cleanEmail.isEmpty() || !cleanEmail.contains("@")) {
    onError("Enter your email address first to receive a password reset link.")
    return
  }
  scope.launch {
    try {
      Firebase.auth.sendPasswordResetEmail(cleanEmail).await()
      onSent("Password reset email sent to $cleanEmail.")
    } catch (e: Exception) {
      onError(e.localizedMessage ?: "Could not send password reset email.")
    }
  }
}

fun normalizeUgandaE164(raw: String): String {
  val digitsOnly = raw.trim().replace(" ", "").replace("-", "")
  return when {
    digitsOnly.startsWith("+") -> digitsOnly
    digitsOnly.startsWith("0") && digitsOnly.length >= 10 -> "+256" + digitsOnly.drop(1)
    digitsOnly.startsWith("256") -> "+$digitsOnly"
    else -> digitsOnly
  }
}

fun startPhoneVerification(
  context: Context,
  phoneRaw: String,
  requestedRole: UserRoleKey,
  resendToken: PhoneAuthProvider.ForceResendingToken?,
  scope: CoroutineScope,
  onCodeSent: (verificationId: String, token: PhoneAuthProvider.ForceResendingToken) -> Unit,
  onAutoSignedIn: () -> Unit,
  onError: (String) -> Unit
) {
  val activity = context.findActivity()
  if (activity == null) {
    onError("Phone verification requires an active Android Activity context.")
    return
  }
  val e164 = normalizeUgandaE164(phoneRaw)
  if (!e164.matches(Regex("^\\+[1-9]\\d{7,14}$"))) {
    onError("Enter a valid E.164 phone number (e.g. +256771234567 or 0771234567).")
    return
  }

  PendingAuthOnboarding.requestedRole = requestedRole
  val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
    override fun onVerificationCompleted(credential: PhoneAuthCredential) {
      scope.launch {
        try {
          Firebase.auth.signInWithCredential(credential).await()
          onAutoSignedIn()
        } catch (e: Exception) {
          PendingAuthOnboarding.consume()
          onError(e.localizedMessage ?: "Automatic phone sign-in failed.")
        }
      }
    }

    override fun onVerificationFailed(e: FirebaseException) {
      PendingAuthOnboarding.consume()
      Log.e("Auth", "Phone verification failed", e)
      onError(e.localizedMessage ?: "Phone verification failed. Ensure Phone Auth is enabled in Firebase Console.")
    }

    override fun onCodeSent(
      verificationId: String,
      token: PhoneAuthProvider.ForceResendingToken
    ) {
      onCodeSent(verificationId, token)
    }
  }

  val builder = PhoneAuthOptions.newBuilder(Firebase.auth)
    .setPhoneNumber(e164)
    .setTimeout(60L, TimeUnit.SECONDS)
    .setActivity(activity)
    .setCallbacks(callbacks)
  if (resendToken != null) {
    builder.setForceResendingToken(resendToken)
  }
  PhoneAuthProvider.verifyPhoneNumber(builder.build())
}

fun verifyPhoneOtpCode(
  verificationId: String,
  smsCode: String,
  requestedRole: UserRoleKey,
  scope: CoroutineScope,
  onAuthSuccess: () -> Unit,
  onError: (String) -> Unit
) {
  val code = smsCode.trim()
  if (code.length < 6) {
    onError("Enter the 6-digit SMS verification code.")
    return
  }
  scope.launch {
    try {
      PendingAuthOnboarding.requestedRole = requestedRole
      val credential = PhoneAuthProvider.getCredential(verificationId, code)
      Firebase.auth.signInWithCredential(credential).await()
      onAuthSuccess()
    } catch (e: Exception) {
      PendingAuthOnboarding.consume()
      Log.e("Auth", "OTP verification failed", e)
      onError(e.localizedMessage ?: "Invalid verification code.")
    }
  }
}

fun signOutWasteHeals(
  credentialManager: CredentialManager,
  scope: CoroutineScope,
  onSignOutComplete: () -> Unit = {}
) {
  scope.launch {
    withContext(NonCancellable) {
      try {
        credentialManager.clearCredentialState(ClearCredentialStateRequest())
      } catch (e: Exception) {
        Log.w("Auth", "Failed to clear credential state: ${e.message}")
      } finally {
        Firebase.auth.signOut()
        onSignOutComplete()
      }
    }
  }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
  is Activity -> this
  is ContextWrapper -> baseContext.findActivity()
  else -> null
}

