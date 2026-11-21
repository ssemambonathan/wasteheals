package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val SpaceGroteskFontFamily = FontFamily(Font(R.font.space_grotesk, FontWeight.Normal))
val PlusJakartaSansFontFamily = FontFamily(Font(R.font.plus_jakarta_sans, FontWeight.Normal))
val JetBrainsMonoFontFamily = FontFamily(Font(R.font.jetbrains_mono, FontWeight.Normal))
val SourceSerifFontFamily = FontFamily(Font(R.font.source_serif_4, FontWeight.Normal))

val WasteHealsTypography = Typography(
  displaySmall = TextStyle(
    fontFamily = SourceSerifFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 28.sp,
    lineHeight = 34.sp,
    letterSpacing = (-0.3).sp
  ),
  headlineMedium = TextStyle(
    fontFamily = SourceSerifFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 23.sp,
    lineHeight = 29.sp,
    letterSpacing = (-0.2).sp
  ),
  headlineSmall = TextStyle(
    fontFamily = SpaceGroteskFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 20.sp,
    lineHeight = 26.sp
  ),
  titleLarge = TextStyle(
    fontFamily = SpaceGroteskFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 18.sp,
    lineHeight = 24.sp
  ),
  titleMedium = TextStyle(
    fontFamily = PlusJakartaSansFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 15.sp,
    lineHeight = 21.sp
  ),
  titleSmall = TextStyle(
    fontFamily = PlusJakartaSansFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 13.5.sp,
    lineHeight = 19.sp
  ),
  bodyLarge = TextStyle(
    fontFamily = PlusJakartaSansFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 15.sp,
    lineHeight = 22.sp
  ),
  bodyMedium = TextStyle(
    fontFamily = PlusJakartaSansFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 13.5.sp,
    lineHeight = 19.sp
  ),
  bodySmall = TextStyle(
    fontFamily = PlusJakartaSansFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 12.sp,
    lineHeight = 16.sp
  ),
  labelLarge = TextStyle(
    fontFamily = PlusJakartaSansFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 13.sp,
    lineHeight = 18.sp
  ),
  labelMedium = TextStyle(
    fontFamily = JetBrainsMonoFontFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 11.5.sp,
    lineHeight = 15.sp
  ),
  labelSmall = TextStyle(
    fontFamily = JetBrainsMonoFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 10.5.sp,
    lineHeight = 14.sp
  )
)

val TabularMoneyStyle = TextStyle(
  fontFamily = JetBrainsMonoFontFamily,
  fontWeight = FontWeight.Bold,
  fontSize = 18.sp,
  lineHeight = 24.sp,
  fontFeatureSettings = "tnum"
)

val TabularHeroMoneyStyle = TextStyle(
  fontFamily = SpaceGroteskFontFamily,
  fontWeight = FontWeight.Bold,
  fontSize = 28.sp,
  lineHeight = 34.sp,
  fontFeatureSettings = "tnum"
)

val ScaleDigitalDisplayStyle = TextStyle(
  fontFamily = JetBrainsMonoFontFamily,
  fontWeight = FontWeight.Bold,
  fontSize = 36.sp,
  lineHeight = 40.sp,
  fontFeatureSettings = "tnum"
)

val TabularCompactStyle = TextStyle(
  fontFamily = JetBrainsMonoFontFamily,
  fontWeight = FontWeight.Medium,
  fontSize = 12.5.sp,
  lineHeight = 17.sp,
  fontFeatureSettings = "tnum"
)
