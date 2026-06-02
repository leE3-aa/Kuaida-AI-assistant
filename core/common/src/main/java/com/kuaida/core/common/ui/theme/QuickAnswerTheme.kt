package com.kuaida.core.common.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import com.kuaida.core.common.R
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val GoogleFontsProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

private val NotoSansSc = FontFamily(
    Font(GoogleFont("Noto Sans SC"), GoogleFontsProvider, FontWeight.Normal),
    Font(GoogleFont("Noto Sans SC"), GoogleFontsProvider, FontWeight.Medium),
    Font(GoogleFont("Noto Sans SC"), GoogleFontsProvider, FontWeight.SemiBold),
    Font(GoogleFont("Noto Sans SC"), GoogleFontsProvider, FontWeight.Bold),
    Font(GoogleFont("Noto Sans SC"), GoogleFontsProvider, FontWeight.ExtraBold),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF6D28D9),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEDE9FE),
    onPrimaryContainer = Color(0xFF2E1065),
    secondary = Color(0xFF0284C7),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = Color(0xFF0C4A6E),
    tertiary = Color(0xFF92400E),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFEF3C7),
    onTertiaryContainer = Color(0xFF451A03),
    background = Color(0xFFF5F3FF),
    onBackground = Color(0xFF1C1917),
    surface = Color(0xFFFEFEFF),
    onSurface = Color(0xFF1C1917),
    surfaceVariant = Color(0xFFEDE9FE),
    onSurfaceVariant = Color(0xFF4A3F6B),
    surfaceTint = Color(0xFF6D28D9),
    inverseSurface = Color(0xFF1C1917),
    inverseOnSurface = Color(0xFFF5F3FF),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    outline = Color(0xFF9CA3AF),
    outlineVariant = Color(0xFFD1D5DB),
    scrim = Color(0x99000000),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFC4B5FD),
    onPrimary = Color(0xFF3B0764),
    primaryContainer = Color(0xFF5B21B6),
    onPrimaryContainer = Color(0xFFEDE9FE),
    secondary = Color(0xFF7DD3FC),
    onSecondary = Color(0xFF082F49),
    secondaryContainer = Color(0xFF0369A1),
    onSecondaryContainer = Color(0xFFE0F2FE),
    tertiary = Color(0xFFFCD34D),
    onTertiary = Color(0xFF451A03),
    tertiaryContainer = Color(0xFF78350F),
    onTertiaryContainer = Color(0xFFFEF3C7),
    background = Color(0xFF0F0B17),
    onBackground = Color(0xFFEDE9FE),
    surface = Color(0xFF160D24),
    onSurface = Color(0xFFEDE9FE),
    surfaceVariant = Color(0xFF2E1F4A),
    onSurfaceVariant = Color(0xFFC4B5FD),
    surfaceTint = Color(0xFFC4B5FD),
    inverseSurface = Color(0xFFEDE9FE),
    inverseOnSurface = Color(0xFF1C1432),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFF6B7280),
    outlineVariant = Color(0xFF374151),
    scrim = Color(0xCC000000),
)

private val QuickAnswerTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = NotoSansSc,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 52.sp,
        lineHeight = 60.sp,
        letterSpacing = (-0.75).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = NotoSansSc,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 46.sp,
        lineHeight = 54.sp,
        letterSpacing = (-0.5).sp,
    ),
    displaySmall = TextStyle(
        fontFamily = NotoSansSc,
        fontWeight = FontWeight.Bold,
        fontSize = 38.sp,
        lineHeight = 46.sp,
        letterSpacing = (-0.25).sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = NotoSansSc,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 42.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = NotoSansSc,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 38.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = NotoSansSc,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 34.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = NotoSansSc,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 30.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = NotoSansSc,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 26.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = NotoSansSc,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 22.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = NotoSansSc,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.15.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = NotoSansSc,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.1.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = NotoSansSc,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.2.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = NotoSansSc,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.25.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = NotoSansSc,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = NotoSansSc,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.45.sp,
    ),
)

private val QuickAnswerShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(32.dp),
)

@Immutable
data class QuickAnswerSpacing(
    val xSmall: Dp = 4.dp,
    val small: Dp = 8.dp,
    val medium: Dp = 12.dp,
    val large: Dp = 16.dp,
    val xLarge: Dp = 20.dp,
    val xxLarge: Dp = 24.dp,
    val xxxLarge: Dp = 32.dp,
)

// ── Alpha 常量（避免散落的内联 alpha 值）──────────────────────────────────
private const val TONE_TONAL_ALPHA = 0.62f
private const val TONE_BRAND_ALPHA = 0.78f
private const val TONE_WARNING_ALPHA = 0.90f
private const val BRUSH_SURFACE_ALPHA = 0.55f

// ── 语义色（统一 success / warning / critical / info 四组）────────────────
@Immutable
data class QuickAnswerSemanticColors(
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val criticalContainer: Color,
    val onCriticalContainer: Color,
    val infoContainer: Color,
    val onInfoContainer: Color,
)

private val LightSemanticColors = QuickAnswerSemanticColors(
    successContainer = Color(0xFFD1FAE5),
    onSuccessContainer = Color(0xFF064E3B),
    warningContainer = Color(0xFFFEF3C7),
    onWarningContainer = Color(0xFF451A03),
    criticalContainer = Color(0xFFFFDAD6),
    onCriticalContainer = Color(0xFF410002),
    infoContainer = Color(0xFFEDE9FE),
    onInfoContainer = Color(0xFF2E1065),
)

private val DarkSemanticColors = QuickAnswerSemanticColors(
    successContainer = Color(0xFF064E3B),
    onSuccessContainer = Color(0xFFD1FAE5),
    warningContainer = Color(0xFF78350F),
    onWarningContainer = Color(0xFFFCD34D),
    criticalContainer = Color(0xFF93000A),
    onCriticalContainer = Color(0xFFFFB4AB),
    infoContainer = Color(0xFF3B0764),
    onInfoContainer = Color(0xFFC4B5FD),
)

private val LocalSpacing = staticCompositionLocalOf { QuickAnswerSpacing() }
private val LocalSemanticColors = staticCompositionLocalOf { LightSemanticColors }

object QaTheme {
    val spacing: QuickAnswerSpacing
        @Composable
        @ReadOnlyComposable
        get() = LocalSpacing.current

    val semanticColors: QuickAnswerSemanticColors
        @Composable
        @ReadOnlyComposable
        get() = LocalSemanticColors.current

    val backgroundBrush: Brush
        @Composable
        @ReadOnlyComposable
        get() = Brush.verticalGradient(
            colors = listOf(
                MaterialTheme.colorScheme.background,
                MaterialTheme.colorScheme.surface,
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = BRUSH_SURFACE_ALPHA),
            ),
        )

    val heroBrush: Brush
        @Composable
        @ReadOnlyComposable
        get() = Brush.linearGradient(
            colorStops = arrayOf(
                0.00f to MaterialTheme.colorScheme.primaryContainer,
                0.55f to MaterialTheme.colorScheme.secondaryContainer,
                1.00f to MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.65f),
            ),
        )
}

@Composable
fun QuickAnswerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    val semanticColors = if (darkTheme) DarkSemanticColors else LightSemanticColors

    CompositionLocalProvider(
        LocalSpacing provides QuickAnswerSpacing(),
        LocalSemanticColors provides semanticColors,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = QuickAnswerTypography,
            shapes = QuickAnswerShapes,
            content = content,
        )
    }
}

@Composable
fun quickAnswerSurfaceColor(
    tone: QuickAnswerTone,
    colorScheme: ColorScheme = MaterialTheme.colorScheme,
): Color {
    val semantic = QaTheme.semanticColors
    return when (tone) {
        QuickAnswerTone.Surface -> colorScheme.surface
        QuickAnswerTone.Tonal -> colorScheme.surfaceVariant.copy(alpha = TONE_TONAL_ALPHA)
        QuickAnswerTone.Brand -> colorScheme.primaryContainer.copy(alpha = TONE_BRAND_ALPHA)
        QuickAnswerTone.Positive -> semantic.successContainer
        QuickAnswerTone.Warning -> semantic.warningContainer.copy(alpha = TONE_WARNING_ALPHA)
        QuickAnswerTone.Critical -> semantic.criticalContainer
        QuickAnswerTone.Info -> semantic.infoContainer
    }
}

@Composable
fun quickAnswerContentColor(
    tone: QuickAnswerTone,
    colorScheme: ColorScheme = MaterialTheme.colorScheme,
): Color {
    val semantic = QaTheme.semanticColors
    return when (tone) {
        QuickAnswerTone.Surface,
        QuickAnswerTone.Tonal -> colorScheme.onSurface
        QuickAnswerTone.Brand -> colorScheme.onPrimaryContainer
        QuickAnswerTone.Positive -> semantic.onSuccessContainer
        QuickAnswerTone.Warning -> semantic.onWarningContainer
        QuickAnswerTone.Critical -> semantic.onCriticalContainer
        QuickAnswerTone.Info -> semantic.onInfoContainer
    }
}

enum class QuickAnswerTone {
    Surface,
    Tonal,
    Brand,
    Positive,
    Warning,
    Critical,
    Info,
}
