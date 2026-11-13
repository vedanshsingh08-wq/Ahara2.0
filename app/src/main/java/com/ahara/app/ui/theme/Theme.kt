package com.ahara.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object AharaColors {
    val Sage = Color(0xFF6F8F72)
    val SageDark = Color(0xFF4E6B52)
    val SageLight = Color(0xFFDCE6D6)
    val Olive = Color(0xFF7D7F4E)
    val Ivory = Color(0xFFFAF6EC)
    val Beige = Color(0xFFEDE3CF)
    val Gold = Color(0xFFC9A44C)
    val Ink = Color(0xFF2E3328)
    val Muted = Color(0xFF6B6E60)
    val Clay = Color(0xFFB8735A)
    val Sky = Color(0xFF6E93A6)
}

private val scheme = lightColorScheme(
    primary = AharaColors.SageDark,
    onPrimary = Color.White,
    primaryContainer = AharaColors.SageLight,
    onPrimaryContainer = AharaColors.Ink,
    secondary = AharaColors.Olive,
    onSecondary = Color.White,
    secondaryContainer = AharaColors.Beige,
    onSecondaryContainer = AharaColors.Ink,
    tertiary = AharaColors.Gold,
    onTertiary = Color.White,
    background = AharaColors.Ivory,
    onBackground = AharaColors.Ink,
    surface = AharaColors.Ivory,
    onSurface = AharaColors.Ink,
    surfaceVariant = Color(0xFFF2ECDD),
    onSurfaceVariant = AharaColors.Muted,
    outline = Color(0xFFC9C2AE),
    error = Color(0xFFA9483A)
)

private val typography = Typography(
    headlineLarge = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, fontSize = 30.sp, lineHeight = 38.sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 32.sp),
    headlineSmall = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 28.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 17.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 11.sp)
)

@Composable
fun AharaTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, typography = typography, content = content)
}
