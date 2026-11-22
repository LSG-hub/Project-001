package app.project001.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.project001.R

/**
 * Fixed brand colours (not wallpaper-based dynamic colour), so that green,
 * amber and brick always mean the same thing: safe, needs you, scam.
 */
@Immutable
data class Tone(val ink: Color, val tint: Color)

@Immutable
data class AppColors(
    val paper: Color,
    val card: Color,
    val ink: Color,
    val muted: Color,
    val line: Color,
    val chipLine: Color,
    val safe: Tone,
    val info: Tone,
    val caution: Tone,
    val scam: Tone,
    val neutral: Tone,
    val inverse: Color,
    val onInverse: Color,
    val accent: Color,
    val onAccent: Color,
)

val LightAppColors = AppColors(
    paper = Color(0xFFFAF8F3),
    card = Color(0xFFFFFFFF),
    ink = Color(0xFF16181A),
    muted = Color(0xFF5F6368),
    line = Color(0xFFF1EEE7),
    chipLine = Color(0xFFDAD6CC),
    safe = Tone(Color(0xFF167443), Color(0xFFE3F1E8)),
    info = Tone(Color(0xFF2F5FA8), Color(0xFFEEF2F9)),
    caution = Tone(Color(0xFF9A5B00), Color(0xFFFBF0DC)),
    scam = Tone(Color(0xFFA8331F), Color(0xFFFBE9E5)),
    neutral = Tone(Color(0xFF5F6368), Color(0xFFF1EFEA)),
    inverse = Color(0xFF16181A),
    onInverse = Color(0xFFFFFFFF),
    accent = Color(0xFF167443),
    onAccent = Color(0xFFFFFFFF),
)

val DarkAppColors = AppColors(
    paper = Color(0xFF121413),
    card = Color(0xFF1C1F1D),
    ink = Color(0xFFECEAE4),
    muted = Color(0xFF9AA09C),
    line = Color(0xFF262A27),
    chipLine = Color(0xFF3A3F3B),
    safe = Tone(Color(0xFF6BD69A), Color(0xFF173A28)),
    info = Tone(Color(0xFF9DBBEB), Color(0xFF1C2A40)),
    caution = Tone(Color(0xFFE8B04A), Color(0xFF3D2E12)),
    scam = Tone(Color(0xFFF28B7D), Color(0xFF4A1F1A)),
    neutral = Tone(Color(0xFF9AA09C), Color(0xFF262A27)),
    inverse = Color(0xFFECEAE4),
    onInverse = Color(0xFF121413),
    accent = Color(0xFF6BD69A),
    onAccent = Color(0xFF10291C),
)

val LocalAppColors = staticCompositionLocalOf { LightAppColors }

private fun AppColors.toScheme(dark: Boolean): ColorScheme {
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = accent,
        onPrimary = onAccent,
        primaryContainer = safe.tint,
        onPrimaryContainer = safe.ink,
        secondaryContainer = safe.tint,
        onSecondaryContainer = ink,
        background = paper,
        onBackground = ink,
        surface = paper,
        onSurface = ink,
        surfaceVariant = card,
        onSurfaceVariant = muted,
        surfaceContainer = card,
        surfaceContainerLow = card,
        surfaceContainerHigh = card,
        outline = chipLine,
        outlineVariant = line,
        error = scam.ink,
    )
}

/** Bricolage for screen titles and caller names only; Geist for everything else. */
private val Bricolage = FontFamily(
    Font(R.font.bricolage_variable, FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
    Font(R.font.bricolage_variable, FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700))),
)

private val Geist = FontFamily(
    Font(R.font.geist_variable, FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.geist_variable, FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
    Font(R.font.geist_variable, FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
)

/** Indian scripts fall back to the system's Noto fonts automatically. */
val DisplayFamily = Bricolage
val BodyFamily = Geist

private val AppTypography = Typography(
    displaySmall = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 36.sp, letterSpacing = (-0.02).em),
    headlineMedium = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 34.sp, letterSpacing = (-0.02).em),
    headlineSmall = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 30.sp, letterSpacing = (-0.02).em),
    titleLarge = TextStyle(fontFamily = Geist, fontWeight = FontWeight.SemiBold, fontSize = 19.sp, lineHeight = 25.sp),
    titleMedium = TextStyle(fontFamily = Geist, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontFamily = Geist, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = Geist, fontWeight = FontWeight.Normal, fontSize = 17.sp, lineHeight = 25.sp),
    bodyMedium = TextStyle(fontFamily = Geist, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontFamily = Geist, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = Geist, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = Geist, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 18.sp),
    labelSmall = TextStyle(fontFamily = Geist, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
)

@Composable
fun Project001Theme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (dark) DarkAppColors else LightAppColors
    CompositionLocalProvider(LocalAppColors provides colors) {
        MaterialTheme(colorScheme = colors.toScheme(dark), typography = AppTypography, content = content)
    }
}

object AppTheme {
    val colors: AppColors
        @Composable get() = LocalAppColors.current
}
