package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val WasteHealsLightColorScheme = lightColorScheme(
  primary = EmeraldPrimary,
  onPrimary = OnEmeraldPrimary,
  primaryContainer = EmeraldContainer,
  onPrimaryContainer = OnEmeraldContainer,
  secondary = TerracottaSecondary,
  onSecondary = OnTerracottaSecondary,
  secondaryContainer = TerracottaContainer,
  onSecondaryContainer = OnTerracottaContainer,
  tertiary = SavannaTertiary,
  onTertiary = OnSavannaTertiary,
  tertiaryContainer = SavannaContainer,
  onTertiaryContainer = OnSavannaContainer,
  background = MineralBackground,
  onBackground = OnMineralBackground,
  surface = CardSurface,
  onSurface = OnMineralBackground,
  surfaceVariant = SurfaceVariantLight,
  onSurfaceVariant = OnSurfaceVariantLight,
  outline = OutlineSubtleLight,
  error = ErrorCrimson,
  errorContainer = ErrorContainerLight,
  onErrorContainer = OnErrorContainerLight
)

private val WasteHealsDarkColorScheme = darkColorScheme(
  primary = EmeraldDarkPrimary,
  onPrimary = OnEmeraldDarkPrimary,
  primaryContainer = EmeraldDarkContainer,
  onPrimaryContainer = OnEmeraldDarkContainer,
  secondary = TerracottaDarkSecondary,
  onSecondary = OnTerracottaDarkSecondary,
  secondaryContainer = TerracottaDarkContainer,
  onSecondaryContainer = OnTerracottaDarkContainer,
  tertiary = SavannaDarkTertiary,
  onTertiary = OnSavannaDarkTertiary,
  tertiaryContainer = SavannaDarkContainer,
  onTertiaryContainer = OnSavannaDarkContainer,
  background = DarkBackground,
  onBackground = OnDarkBackground,
  surface = DarkSurface,
  onSurface = OnDarkBackground,
  surfaceVariant = DarkSurfaceVariant,
  onSurfaceVariant = OnDarkSurfaceVariant,
  outline = OutlineSubtleDark
)

val WasteHealsShapes = Shapes(
  small = RoundedCornerShape(8.dp),
  medium = RoundedCornerShape(14.dp),
  large = RoundedCornerShape(20.dp)
)

@Composable
fun WasteHealsTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit
) {
  val colorScheme = if (darkTheme) WasteHealsDarkColorScheme else WasteHealsLightColorScheme
  MaterialTheme(
    colorScheme = colorScheme,
    typography = WasteHealsTypography,
    shapes = WasteHealsShapes,
    content = content
  )
}
