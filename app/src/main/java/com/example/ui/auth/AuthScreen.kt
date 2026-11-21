package com.example.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.domain.model.UserRoleKey

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
  authViewModel: AuthViewModel
) {
  val context = LocalContext.current
  val credentialManager = remember(context) { CredentialManager.create(context) }
  val uiState by authViewModel.uiState.collectAsStateWithLifecycle()

  var authTab by remember { mutableIntStateOf(0) } // 0 = Email, 1 = Phone OTP
  var isSignUpMode by remember { mutableStateOf(false) }

  var displayName by remember { mutableStateOf("") }
  var email by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }
  var passwordVisible by remember { mutableStateOf(false) }

  var phoneNumber by remember { mutableStateOf("+256") }
  var otpCode by remember { mutableStateOf("") }

  var requestedRole by remember { mutableStateOf(UserRoleKey.PICKER) }
  val selfSelectableRoles = listOf(UserRoleKey.PICKER, UserRoleKey.COLLECTOR, UserRoleKey.HEALTH_PROVIDER)

  LaunchedEffect(Unit) {
    authViewModel.attemptSilentGoogleSignIn(context, credentialManager)
  }

  val isLoading = uiState.isLoading
  val verificationId = uiState.verificationId
  val errorMessage = uiState.errorMessage
  val infoMessage = uiState.infoMessage

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          listOf(
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
          )
        )
      )
      .windowInsetsPadding(WindowInsets.safeDrawing)
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(20.dp)
        .widthIn(max = 560.dp)
        .align(Alignment.TopCenter),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(174.dp)
          .clip(RoundedCornerShape(22.dp))
      ) {
        Image(
          painter = painterResource(id = R.drawable.img_hero_kampala),
          contentDescription = stringResource(R.string.hero_kampala_desc),
          modifier = Modifier.fillMaxSize(),
          contentScale = ContentScale.Crop
        )
        Column(
          modifier = Modifier
            .align(Alignment.BottomStart)
            .padding(18.dp)
        ) {
          Surface(
            color = MaterialTheme.colorScheme.tertiaryContainer,
            shape = RoundedCornerShape(999.dp)
          ) {
            Text(
              text = stringResource(R.string.kampala_pilot_badge),
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onTertiaryContainer,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
          }
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onPrimary
          )
          Text(
            text = stringResource(R.string.app_tagline),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.92f)
          )
        }
      }

      Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          PrimaryTabRow(selectedTabIndex = authTab) {
            Tab(
              selected = authTab == 0,
              onClick = {
                authTab = 0
                authViewModel.clearMessages()
              },
              icon = { Icon(Icons.Filled.Email, contentDescription = null, modifier = Modifier.size(18.dp)) },
              text = { Text("Email") },
              modifier = Modifier.testTag("auth_tab_email")
            )
            Tab(
              selected = authTab == 1,
              onClick = {
                authTab = 1
                authViewModel.clearMessages()
              },
              icon = { Icon(Icons.Filled.Phone, contentDescription = null, modifier = Modifier.size(18.dp)) },
              text = { Text("Phone OTP") },
              modifier = Modifier.testTag("auth_tab_phone")
            )
          }

          if (authTab == 0) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              FilterChip(
                selected = !isSignUpMode,
                onClick = { isSignUpMode = false; authViewModel.clearMessages() },
                label = { Text("Sign In") },
                modifier = Modifier.testTag("email_mode_signin")
              )
              FilterChip(
                selected = isSignUpMode,
                onClick = { isSignUpMode = true; authViewModel.clearMessages() },
                label = { Text("Create Account") },
                modifier = Modifier.testTag("email_mode_signup")
              )
            }

            if (isSignUpMode) {
              OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { Text("Full Name") },
                leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("signup_display_name_input")
              )
              Text(
                text = "Operational Role Request (assigned in Firestore)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                selfSelectableRoles.forEach { roleOpt ->
                  FilterChip(
                    selected = requestedRole == roleOpt,
                    onClick = { requestedRole = roleOpt },
                    label = { Text("${roleOpt.label} (${roleOpt.key})") },
                    modifier = Modifier.testTag("role_request_${roleOpt.key}")
                  )
                }
              }
            }

            OutlinedTextField(
              value = email,
              onValueChange = { email = it },
              label = { Text("Email Address") },
              leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
              singleLine = true,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("email_input")
            )

            OutlinedTextField(
              value = password,
              onValueChange = { password = it },
              label = { Text("Password (min 6 chars)") },
              leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
              trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                  Icon(
                    imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = if (passwordVisible) "Hide password" else "Show password"
                  )
                }
              },
              visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
              singleLine = true,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("password_input")
            )

            if (!isSignUpMode) {
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(
                  onClick = { authViewModel.sendPasswordReset(email) },
                  enabled = !isLoading
                ) {
                  Text("Forgot password?")
                }
              }
            }

            Button(
              onClick = {
                if (isSignUpMode) {
                  authViewModel.createAccountWithEmail(displayName, email, password, requestedRole)
                } else {
                  authViewModel.signInWithEmail(email, password)
                }
              },
              enabled = !isLoading,
              modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("email_submit_button"),
              shape = RoundedCornerShape(12.dp)
            ) {
              if (isLoading) {
                CircularProgressIndicator(
                  modifier = Modifier.size(20.dp),
                  color = MaterialTheme.colorScheme.onPrimary,
                  strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.signing_in_label))
              } else {
                Text(if (isSignUpMode) "Create Account with Email" else "Sign In with Email")
              }
            }
          } else {
            OutlinedTextField(
              value = phoneNumber,
              onValueChange = { phoneNumber = it },
              label = { Text("Phone Number (E.164, e.g. +256771234567)") },
              leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = null) },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
              singleLine = true,
              enabled = verificationId == null && !isLoading,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("phone_input")
            )

            Text(
              text = "Role on New Registration",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              selfSelectableRoles.forEach { roleOpt ->
                FilterChip(
                  selected = requestedRole == roleOpt,
                  onClick = { requestedRole = roleOpt },
                  label = { Text("${roleOpt.label} (${roleOpt.key})") }
                )
              }
            }

            if (verificationId == null) {
              Button(
                onClick = {
                  authViewModel.startPhoneVerification(context, phoneNumber, requestedRole)
                },
                enabled = !isLoading,
                modifier = Modifier
                  .fillMaxWidth()
                  .height(50.dp)
                  .testTag("send_otp_button"),
                shape = RoundedCornerShape(12.dp)
              ) {
                if (isLoading) {
                  CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Text("Sending OTP…")
                } else {
                  Icon(Icons.Filled.Sms, contentDescription = null, modifier = Modifier.size(18.dp))
                  Spacer(modifier = Modifier.width(8.dp))
                  Text("Send SMS Verification Code")
                }
              }
            } else {
              OutlinedTextField(
                value = otpCode,
                onValueChange = { if (it.length <= 6) otpCode = it },
                label = { Text("6-Digit SMS OTP Code") },
                leadingIcon = { Icon(Icons.Filled.Sms, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("otp_input")
              )
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                OutlinedButton(
                  onClick = {
                    otpCode = ""
                    authViewModel.resetPhoneVerification()
                  },
                  enabled = !isLoading,
                  modifier = Modifier.weight(1f)
                ) {
                  Text("Change Number")
                }
                Button(
                  onClick = { authViewModel.verifyPhoneOtp(otpCode, requestedRole) },
                  enabled = !isLoading && otpCode.length >= 6,
                  modifier = Modifier
                    .weight(1.4f)
                    .testTag("verify_otp_button")
                ) {
                  Text("Verify & Sign In")
                }
              }
            }
          }
        }
      }

      if (infoMessage != null) {
        Surface(
          color = MaterialTheme.colorScheme.primaryContainer,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = infoMessage,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(12.dp)
          )
        }
      }

      if (errorMessage != null) {
        Surface(
          color = MaterialTheme.colorScheme.errorContainer,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("auth_error_banner")
        ) {
          Text(
            text = errorMessage,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(12.dp)
          )
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        HorizontalDivider(modifier = Modifier.weight(1f))
        Text("OR", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        HorizontalDivider(modifier = Modifier.weight(1f))
      }

      OutlinedButton(
        onClick = { authViewModel.signInWithGoogle(context, credentialManager) },
        enabled = !isLoading,
        modifier = Modifier
          .fillMaxWidth()
          .height(52.dp)
          .testTag("google_sign_in_button"),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.outlinedButtonColors(
          containerColor = MaterialTheme.colorScheme.surface,
          contentColor = MaterialTheme.colorScheme.primary
        )
      ) {
        Icon(
          imageVector = Icons.Filled.VerifiedUser,
          contentDescription = null,
          modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
          text = stringResource(R.string.sign_in_with_google),
          style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
        )
      }

      Text(
        text = stringResource(R.string.auth_security_footnote),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}
