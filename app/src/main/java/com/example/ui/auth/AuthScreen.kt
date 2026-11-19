package com.example.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Recycling
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager

@Composable
fun AuthScreen(onAuthSuccess: () -> Unit) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }
  var isLoading by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  LaunchedEffect(Unit) {
    attemptAutoSignIn(
      context = context,
      credentialManager = credentialManager,
      onAuthSuccess = onAuthSuccess,
      onUnauthenticated = { },
      scope = scope
    )
  }

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
      verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(190.dp)
          .clip(RoundedCornerShape(22.dp))
      ) {
        Image(
          painter = painterResource(id = com.example.R.drawable.img_hero_kampala),
          contentDescription = stringResource(com.example.R.string.hero_kampala_desc),
          modifier = Modifier.fillMaxSize(),
          contentScale = ContentScale.Crop
        )
        Column(
          modifier = Modifier
            .align(Alignment.BottomStart)
            .padding(20.dp)
        ) {
          Surface(
            color = MaterialTheme.colorScheme.tertiaryContainer,
            shape = RoundedCornerShape(999.dp)
          ) {
            Text(
              text = stringResource(com.example.R.string.kampala_pilot_badge),
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onTertiaryContainer,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
          }
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = stringResource(com.example.R.string.app_name),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onPrimary
          )
          Text(
            text = stringResource(com.example.R.string.app_tagline),
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
          modifier = Modifier.padding(18.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Text(
            text = stringResource(com.example.R.string.auth_pillars_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
          )
          PillarRow(
            icon = Icons.Filled.Recycling,
            title = "Traceable Collection & Organic Composting",
            detail = "Offline-resilient Room weighing with immutable rate snapshots and zero-negative stock."
          )
          PillarRow(
            icon = Icons.Filled.AccountBalance,
            title = "Double-Entry Ledger & Maker-Checker",
            detail = "Guaranteed picker earnings, balanced debits = credits, and liquidity-gated 30% reward pools."
          )
          PillarRow(
            icon = Icons.Filled.HealthAndSafety,
            title = "UGX 30/kg Healthcare Fund",
            detail = "Accrued on confirmed sales, funded strictly from collected buyer cash, redeemed at partner clinics."
          )
        }
      }

      if (errorMessage != null) {
        Surface(
          color = MaterialTheme.colorScheme.errorContainer,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = errorMessage!!,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(12.dp)
          )
        }
      }

      Button(
        onClick = {
          errorMessage = null
          isLoading = true
          onGoogleSignInClicked(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {
              isLoading = false
              onAuthSuccess()
            },
            onAuthError = { msg ->
              isLoading = false
              errorMessage = msg
            },
            scope = scope,
            onAuthCancelled = { isLoading = false }
          )
        },
        enabled = !isLoading,
        modifier = Modifier
          .fillMaxWidth()
          .height(54.dp)
          .testTag("google_sign_in_button"),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.primary,
          contentColor = MaterialTheme.colorScheme.onPrimary
        )
      ) {
        if (isLoading) {
          CircularProgressIndicator(
            modifier = Modifier.size(22.dp),
            color = MaterialTheme.colorScheme.onPrimary,
            strokeWidth = 2.dp
          )
          Spacer(modifier = Modifier.width(10.dp))
          Text(stringResource(com.example.R.string.signing_in_label))
        } else {
          Icon(
            imageVector = Icons.Filled.VerifiedUser,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(10.dp))
          Text(
            text = stringResource(com.example.R.string.sign_in_with_google),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
          )
        }
      }

      Text(
        text = stringResource(com.example.R.string.auth_security_footnote),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}

@Composable
private fun PillarRow(icon: ImageVector, title: String, detail: String) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalAlignment = Alignment.Top
  ) {
    Surface(
      color = MaterialTheme.colorScheme.primaryContainer,
      shape = RoundedCornerShape(10.dp),
      modifier = Modifier.size(38.dp)
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onPrimaryContainer,
          modifier = Modifier.size(20.dp)
        )
      }
    }
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface
      )
      Text(
        text = detail,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}
