package com.example

import com.example.domain.model.UserRoleKey
import com.example.ui.auth.AuthSessionClient
import com.example.ui.auth.AuthSessionState
import com.example.ui.auth.AuthViewModel
import com.example.ui.auth.UserDocumentService
import com.google.firebase.auth.PhoneAuthCredential
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AuthViewModelTest {

  private val testDispatcher = UnconfinedTestDispatcher()

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  private class FakeAuthSessionClient : AuthSessionClient {
    val authUidFlow = MutableStateFlow<String?>(null)
    var nextEmailSignInResult: Result<Unit> = Result.success(Unit)
    var nextEmailCreateResult: Result<Unit> = Result.success(Unit)
    var nextResetResult: Result<Unit> = Result.success(Unit)
    var signedInUidOnSuccess: String = "uid_user_01"

    override val currentUid: String?
      get() = authUidFlow.value

    override fun observeAuthUid(): Flow<String?> = authUidFlow

    override suspend fun signInWithEmailCredential(email: String, password: String): Result<Unit> {
      if (nextEmailSignInResult.isSuccess) {
        authUidFlow.value = signedInUidOnSuccess
      }
      return nextEmailSignInResult
    }

    override suspend fun createEmailAccount(
      displayName: String,
      email: String,
      password: String
    ): Result<Unit> {
      if (nextEmailCreateResult.isSuccess) {
        authUidFlow.value = signedInUidOnSuccess
      }
      return nextEmailCreateResult
    }

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> = nextResetResult

    override suspend fun signInWithGoogleIdToken(idToken: String): Result<Unit> {
      authUidFlow.value = signedInUidOnSuccess
      return Result.success(Unit)
    }

    override suspend fun signInWithPhoneOtp(verificationId: String, smsCode: String): Result<Unit> {
      authUidFlow.value = signedInUidOnSuccess
      return Result.success(Unit)
    }

    override suspend fun signInWithPhoneCredential(credential: PhoneAuthCredential): Result<Unit> {
      authUidFlow.value = signedInUidOnSuccess
      return Result.success(Unit)
    }

    override suspend fun signOut() {
      authUidFlow.value = null
    }
  }

  private class FakeUserDocumentService : UserDocumentService {
    var calls = mutableListOf<Pair<String?, UserRoleKey?>>()
    var nextResult: Result<Unit> = Result.success(Unit)

    override suspend fun ensureUserDocument(
      preferredDisplayName: String?,
      requestedRoleKey: UserRoleKey?
    ): Result<Unit> {
      calls += preferredDisplayName to requestedRoleKey
      return nextResult
    }
  }

  @Test
  fun `email sign-up establishes auth session and triggers Firestore user document creation`() {
    val fakeAuth = FakeAuthSessionClient().apply { signedInUidOnSuccess = "uid_collector_99" }
    val fakeDocs = FakeUserDocumentService()
    val vm = AuthViewModel(fakeAuth, fakeDocs)

    assertEquals(AuthSessionState.Unauthenticated, vm.uiState.value.sessionState)

    vm.createAccountWithEmail(
      displayName = "Samuel Okello",
      email = "samuel@wasteheals.org",
      password = "password123",
      requestedRole = UserRoleKey.COLLECTOR
    )

    assertEquals(AuthSessionState.Authenticated("uid_collector_99"), vm.uiState.value.sessionState)
    assertEquals(1, fakeDocs.calls.size)
    assertEquals("Samuel Okello" to UserRoleKey.COLLECTOR, fakeDocs.calls.first())
    assertNull(vm.uiState.value.errorMessage)
  }

  @Test
  fun `invalid credentials and failed sign-in update Error StateFlow`() {
    val fakeAuth = FakeAuthSessionClient().apply {
      nextEmailSignInResult = Result.failure(IllegalArgumentException("Wrong password"))
    }
    val fakeDocs = FakeUserDocumentService()
    val vm = AuthViewModel(fakeAuth, fakeDocs)

    vm.signInWithEmail("bad-email", "123456")
    assertTrue(vm.uiState.value.sessionState is AuthSessionState.Error)
    assertEquals("Enter a valid email address.", vm.uiState.value.errorMessage)

    vm.signInWithEmail("picker@wasteheals.org", "wrongpass")
    val errState = vm.uiState.value.sessionState as AuthSessionState.Error
    assertNull(errState.uid)
    assertEquals("Wrong password", errState.message)
    assertEquals(0, fakeDocs.calls.size)
  }

  @Test
  fun `firestore onboarding failure surfaces retryable Error and signOut resets to Unauthenticated`() {
    val fakeAuth = FakeAuthSessionClient().apply { signedInUidOnSuccess = "uid_retry_01" }
    val fakeDocs = FakeUserDocumentService().apply {
      nextResult = Result.failure(IllegalStateException("Network unavailable"))
    }
    val vm = AuthViewModel(fakeAuth, fakeDocs)

    vm.signInWithEmail("grace@wasteheals.org", "secret123")
    val errState = vm.uiState.value.sessionState as AuthSessionState.Error
    assertEquals("uid_retry_01", errState.uid)
    assertEquals("Network unavailable", errState.message)

    fakeDocs.nextResult = Result.success(Unit)
    vm.retryUserDocumentCreation()
    assertEquals(AuthSessionState.Authenticated("uid_retry_01"), vm.uiState.value.sessionState)
    assertEquals(2, fakeDocs.calls.size)

    vm.signOut(credentialManager = null)
    assertEquals(AuthSessionState.Unauthenticated, vm.uiState.value.sessionState)
  }
}
