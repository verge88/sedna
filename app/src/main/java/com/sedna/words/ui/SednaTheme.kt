package com.sedna.words.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val Ink = Color(0xFF101010)
private val Snow = Color(0xFFFFFFFF)

private val LightPalette = lightColorScheme(
    primary = Ink, onPrimary = Snow,
    secondary = Color(0xFF555555), onSecondary = Snow,
    background = Snow, onBackground = Ink,
    surface = Snow, onSurface = Ink,
    surfaceVariant = Color(0xFFF3F3F3), onSurfaceVariant = Color(0xFF444444),
    outline = Color(0xFFD7D7D7), outlineVariant = Color(0xFFE9E9E9),
    error = Color(0xFF333333), onError = Snow
)

private val DarkPalette = darkColorScheme(
    primary = Snow, onPrimary = Ink,
    secondary = Color(0xFFCCCCCC), onSecondary = Ink,
    background = Color(0xFF0E0E0E), onBackground = Snow,
    surface = Color(0xFF151515), onSurface = Snow,
    surfaceVariant = Color(0xFF242424), onSurfaceVariant = Color(0xFFBBBBBB),
    outline = Color(0xFF4A4A4A), outlineVariant = Color(0xFF333333),
    error = Color(0xFFCCCCCC), onError = Ink
)

private val SednaTypography = Typography(
    headlineLarge = TextStyle(fontSize = 38.sp, lineHeight = 42.sp, fontWeight = FontWeight.Black),
    headlineMedium = TextStyle(fontSize = 28.sp, lineHeight = 33.sp, fontWeight = FontWeight.ExtraBold),
    headlineSmall = TextStyle(fontSize = 23.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 21.sp),
    labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.Bold),
    labelSmall = TextStyle(fontSize = 11.sp, lineHeight = 15.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
)

@Composable
fun SednaTheme(dark: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (dark) DarkPalette else LightPalette,
        typography = SednaTypography,
        content = content
    )
}
