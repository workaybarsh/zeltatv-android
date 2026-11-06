package tv.zelta.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import tv.zelta.app.R
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Amber accent used by the default ZeltaTV tokens. */
val ZeltaAmber = Color(0xFFF6C445)
val ZeltaTurquoise = Color(0xFF2EE6F6)

/** Inter, the same variable typeface the desktop application ships. */
val ZeltaFont = FontFamily(
    Font(R.font.inter, weight = FontWeight.Normal),
    Font(R.font.inter, weight = FontWeight.Medium),
    Font(R.font.inter, weight = FontWeight.SemiBold),
    Font(R.font.inter, weight = FontWeight.Bold),
)

/** Remix Icon, the icon font used by the desktop UI. */
val RemixFont = FontFamily(Font(R.font.remixicon))

/**
 * Theme presets copied 1:1 from the desktop theme store. The palette itself is
 * derived from the background colour with the same HSL rules as generator.ts so
 * the Android shell matches the desktop pixel colours.
 */
data class ZeltaPalette(
    val id: String,
    val background: Color,
    val accent: Color?,
)

val ZELTA_PALETTES = listOf(
    ZeltaPalette("default", Color(0xFF141414), Color(0xFF2EE6F6)),
    ZeltaPalette("mono", Color(0xFF000000), Color(0xFFF2F2F2)),
    ZeltaPalette("light", Color(0xFFF5F5F5), null),
    ZeltaPalette("navy", Color(0xFF0A0E1A), Color(0xFF6AA8FF)),
    ZeltaPalette("forest", Color(0xFF0A1410), Color(0xFF5CE0A0)),
    ZeltaPalette("plum", Color(0xFF1A0A1E), Color(0xFFD0A2FF)),
    ZeltaPalette("warm", Color(0xFF1E1A14), Color(0xFFF6C445)),
)

/** Colours that do not map onto a Material slot. */
data class ZeltaTokens(
    val focus: Color,
    val halo: Color,
    val card: Color,
    val popover: Color,
    val border: Color,
    val input: Color,
    val muted: Color,
    val sidebar: Color,
    val destructive: Color,
    val isDark: Boolean,
)

val LocalZeltaTokens = staticCompositionLocalOf {
    ZeltaTokens(
        focus = ZeltaAmber,
        halo = ZeltaAmber.copy(alpha = 0.22f),
        card = Color(0xFF232323),
        popover = Color(0xFF2A2A2A),
        border = Color(0xFF2C2C2C),
        input = Color(0xFF373737),
        muted = Color(0xFF2A2A2A),
        sidebar = Color(0xFF232323),
        destructive = Color(0xFFFF6B6B),
        isDark = true,
    )
}

private val ZeltaTypography = Typography(
    displaySmall = TextStyle(fontFamily = ZeltaFont, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 36.sp, letterSpacing = (-0.4).sp),
    headlineMedium = TextStyle(fontFamily = ZeltaFont, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 32.sp, letterSpacing = (-0.3).sp),
    headlineSmall = TextStyle(fontFamily = ZeltaFont, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 28.sp),
    titleLarge = TextStyle(fontFamily = ZeltaFont, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 24.sp),
    titleMedium = TextStyle(fontFamily = ZeltaFont, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    titleSmall = TextStyle(fontFamily = ZeltaFont, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp),
    bodyLarge = TextStyle(fontFamily = ZeltaFont, fontSize = 14.sp, lineHeight = 20.sp),
    bodyMedium = TextStyle(fontFamily = ZeltaFont, fontSize = 12.sp, lineHeight = 16.sp),
    bodySmall = TextStyle(fontFamily = ZeltaFont, fontSize = 11.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = ZeltaFont, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    labelMedium = TextStyle(fontFamily = ZeltaFont, fontWeight = FontWeight.Medium, fontSize = 10.sp, lineHeight = 14.sp, letterSpacing = 0.2.sp),
    labelSmall = TextStyle(fontFamily = ZeltaFont, fontWeight = FontWeight.SemiBold, fontSize = 9.sp, lineHeight = 12.sp, letterSpacing = 0.6.sp),
)

@Composable
fun ZeltaTheme(
    themeId: String = "default",
    content: @Composable () -> Unit,
) {
    val preset = ZELTA_PALETTES.firstOrNull { it.id == themeId } ?: ZELTA_PALETTES.first()
    val tokens = buildTokens(preset.background)
    val primary = preset.accent ?: ZeltaTurquoise
    val focus = primary
    val onPrimary = if (primary.isBrightColor()) Color(0xFF141414) else Color(0xFFFFFFFF)
    val foreground = tokens.foreground

    val scheme = if (tokens.isDark) {
        darkColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primary.copy(alpha = 0.15f),
            onPrimaryContainer = foreground,
            secondary = focus,
            onSecondary = if (focus.isBrightColor()) Color(0xFF141414) else Color.White,
            background = preset.background,
            onBackground = foreground,
            surface = tokens.card,
            onSurface = foreground,
            surfaceVariant = tokens.popover,
            onSurfaceVariant = tokens.mutedForeground,
            outline = tokens.border,
            outlineVariant = tokens.input,
            error = tokens.destructive,
            onError = Color(0xFF141414),
        )
    } else {
        lightColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primary.copy(alpha = 0.15f),
            onPrimaryContainer = foreground,
            secondary = focus,
            onSecondary = if (focus.isBrightColor()) Color(0xFF141414) else Color.White,
            background = preset.background,
            onBackground = foreground,
            surface = tokens.card,
            onSurface = foreground,
            surfaceVariant = tokens.popover,
            onSurfaceVariant = tokens.mutedForeground,
            outline = tokens.border,
            outlineVariant = tokens.input,
            error = tokens.destructive,
            onError = Color.White,
        )
    }

    val zelta = ZeltaTokens(
        focus = focus,
        halo = focus.copy(alpha = 0.22f),
        card = tokens.card,
        popover = tokens.popover,
        border = tokens.border,
        input = tokens.input,
        muted = tokens.muted,
        sidebar = tokens.card,
        destructive = tokens.destructive,
        isDark = tokens.isDark,
    )

    CompositionLocalProvider(LocalZeltaTokens provides zelta) {
        MaterialTheme(
            colorScheme = scheme,
            typography = ZeltaTypography,
            content = content,
        )
    }
}

val zeltaColors: ZeltaTokens
    @Composable get() = LocalZeltaTokens.current

private data class Derived(
    val foreground: Color,
    val mutedForeground: Color,
    val card: Color,
    val popover: Color,
    val border: Color,
    val input: Color,
    val muted: Color,
    val destructive: Color,
    val isDark: Boolean,
)

private fun buildTokens(background: Color): Derived {
    val (h, s, l) = background.toHsl()
    val dark = l < 50
    val sc = (s * 0.3).roundToInt()
    fun hsl(hue: Int, sat: Int, light: Int) = Color.hsl(hue.toFloat(), (sat / 100f).coerceIn(0f, 1f), (light / 100f).coerceIn(0f, 1f))
    return if (dark) {
        Derived(
            foreground = hsl(h, sc, 98),
            mutedForeground = hsl(h, sc, 71),
            card = hsl(h, sc, clamp(l + 5)),
            popover = hsl(h, sc, clamp(l + 10)),
            border = hsl(h, sc, clamp(l + 12)),
            input = hsl(h, sc, clamp(l + 17)),
            muted = hsl(h, sc, clamp(l + 10)),
            destructive = hsl(0, 84, 60),
            isDark = true,
        )
    } else {
        Derived(
            foreground = hsl(h, sc, 5),
            mutedForeground = hsl(h, sc, 40),
            card = hsl(h, sc, clamp(l - 3)),
            popover = hsl(h, sc, 98),
            border = hsl(h, sc, clamp(l - 5)),
            input = hsl(h, sc, clamp(l - 8)),
            muted = hsl(h, sc, clamp(l - 5)),
            destructive = hsl(0, 84, 55),
            isDark = false,
        )
    }
}

private fun clamp(v: Int): Int = max(0, min(100, v))

private fun Color.toHsl(): Triple<Int, Int, Int> {
    val r = red
    val g = green
    val b = blue
    val max = max(r, max(g, b))
    val min = min(r, min(g, b))
    val l = (max + min) / 2f
    var h = 0f
    var s = 0f
    if (abs(max - min) > 0.00001f) {
        val d = max - min
        s = if (l > 0.5f) d / (2f - max - min) else d / (max + min)
        h = when (max) {
            r -> (g - b) / d + (if (g < b) 6f else 0f)
            g -> (b - r) / d + 2f
            else -> (r - g) / d + 4f
        }
        h *= 60f
    }
    return Triple(h.roundToInt(), (s * 100).roundToInt(), (l * 100).roundToInt())
}

fun Color.isBrightColor(): Boolean = (0.2126f * red + 0.7152f * green + 0.0722f * blue) > 0.55f
