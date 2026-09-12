package com.debayan.ainotebook.common.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/*
 * Ink on paper. Warm off-white page, teal ink for anything the app itself says, warm gold for the
 * things that came from somewhere else.
 *
 * The brand teal is #0AA6A6 and the brand gold is #F2C94C, but neither can be dropped straight into
 * a light scheme's `primary`/`tertiary` slot. Material 3 uses those two roles both as *containers*
 * (white label on top) and as *accent text on the surface* (`TextButton`, selected tab, link), and
 * measured against the #FFF8F0 page the brand teal reaches only 2.84:1 and the brand gold 1.51:1 —
 * both well under the 4.5:1 that body text needs. Deepening the same hue one tonal step to #00807F
 * yields 4.53:1 on paper and 4.77:1 under white, so a single value works in both directions, and
 * #7A5B00 does the same for gold at 6.0:1. The unmodified brand colours stay available through
 * [PaperColors.accentTeal] / [PaperColors.accentGold] for marks that carry no text — the ink
 * ripple, the highlighter, the page furniture — where the 3:1 graphics threshold applies instead.
 *
 * Dark mode inverts the reasoning rather than the colours: against a true-dark surface the brand
 * teal measures 6.27:1 and the brand gold 11.8:1, so both appear verbatim there. What dark mode
 * must not do is lighten the beige — a dim paper surface reads as a stained page and loses the
 * ink-on-surface contrast the whole app is built on, so the dark surfaces are warm-neutral
 * charcoals and only the *ink* keeps its warmth.
 */

private val BrandTeal = Color(0xFF0AA6A6)
private val BrandGold = Color(0xFFF2C94C)
private val PaperWhite = Color(0xFFFFF8F0)
private val InkBlack = Color(0xFF1F1B16)
private val InkWhite = Color(0xFFE8E6E1)

/** Deepened brand teal: the only teal that passes AA both as accent text and as a filled container. */
private val TealInk = Color(0xFF00807F)

/** Deepened brand gold, for the same reason. */
private val GoldInk = Color(0xFF7A5B00)

val LightColors: ColorScheme = lightColorScheme(
    primary = TealInk,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFBFEDEC),
    onPrimaryContainer = Color(0xFF00312F),
    inversePrimary = Color(0xFF5CDAD8),
    secondary = Color(0xFF3F5B59),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD3E5E2),
    onSecondaryContainer = Color(0xFF14302E),
    tertiary = GoldInk,
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = BrandGold,
    onTertiaryContainer = Color(0xFF2A1E00),
    background = PaperWhite,
    onBackground = InkBlack,
    surface = PaperWhite,
    onSurface = InkBlack,
    surfaceVariant = Color(0xFFEDE3D6),
    onSurfaceVariant = Color(0xFF4C463D),
    surfaceTint = TealInk,
    inverseSurface = Color(0xFF34312B),
    inverseOnSurface = Color(0xFFFAEFE3),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410E0B),
    outline = Color(0xFF857F74),
    outlineVariant = Color(0xFFD6CBBC),
    scrim = Color(0xFF000000),
    surfaceBright = PaperWhite,
    surfaceDim = Color(0xFFE7DCCD),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFDF3E8),
    surfaceContainer = Color(0xFFF8EDE0),
    surfaceContainerHigh = Color(0xFFF2E7D9),
    surfaceContainerHighest = Color(0xFFECE1D3),
)

val DarkColors: ColorScheme = darkColorScheme(
    primary = BrandTeal,
    onPrimary = Color(0xFF00201F),
    primaryContainer = Color(0xFF00504F),
    onPrimaryContainer = Color(0xFF9CF1EF),
    inversePrimary = TealInk,
    secondary = Color(0xFFA8C6C3),
    onSecondary = Color(0xFF14302E),
    secondaryContainer = Color(0xFF2C4846),
    onSecondaryContainer = Color(0xFFC4E3E0),
    tertiary = BrandGold,
    onTertiary = Color(0xFF3D2E00),
    tertiaryContainer = Color(0xFF574000),
    onTertiaryContainer = Color(0xFFFFE08A),
    background = Color(0xFF121211),
    onBackground = InkWhite,
    surface = Color(0xFF121211),
    onSurface = InkWhite,
    surfaceVariant = Color(0xFF43413C),
    onSurfaceVariant = Color(0xFFCBC6BD),
    surfaceTint = BrandTeal,
    inverseSurface = InkWhite,
    inverseOnSurface = Color(0xFF313029),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFF97928A),
    outlineVariant = Color(0xFF47443F),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF383632),
    surfaceDim = Color(0xFF121211),
    surfaceContainerLowest = Color(0xFF0C0C0B),
    surfaceContainerLow = Color(0xFF1A1917),
    surfaceContainer = Color(0xFF1E1D1B),
    surfaceContainerHigh = Color(0xFF292824),
    surfaceContainerHighest = Color(0xFF34332E),
)

val LightPaperColors = PaperColors(
    paper = PaperWhite,
    paperEdge = Color(0xFFEADCC9),
    ruledLine = Color(0xFFE2D6C4),
    gridLine = Color(0xFFEDE2D2),
    marginRule = Color(0xFFE39B93),
    ink = InkBlack,
    inkMuted = Color(0xFF6B6558),
    accentTeal = BrandTeal,
    accentGold = BrandGold,
    highlighter = Color(0x4DF2C94C),
    aiAnswerTint = Color(0xFFEAF6F4),
    aiAnswerBorder = Color(0xFFC3E5E1),
    confidenceLow = Color(0xFFB3261E),
    confidenceMedium = Color(0xFF8A5A00),
    confidenceHigh = Color(0xFF0E6E4F),
)

val DarkPaperColors = PaperColors(
    // One step lighter than the dark surface: a page has to be distinguishable from the chrome it
    // sits on, and at 0.5% luminance difference it is not.
    paper = Color(0xFF1A1A18),
    paperEdge = Color(0xFF2B2A26),
    ruledLine = Color(0xFF2E2D28),
    gridLine = Color(0xFF26251F),
    marginRule = Color(0xFF8C4A44),
    ink = Color(0xFFEDEAE4),
    inkMuted = Color(0xFFA8A39A),
    accentTeal = Color(0xFF3ED3D1),
    accentGold = BrandGold,
    highlighter = Color(0x4DF2C94C),
    aiAnswerTint = Color(0xFF122926),
    aiAnswerBorder = Color(0xFF1F4B47),
    confidenceLow = Color(0xFFFFB4AB),
    confidenceMedium = BrandGold,
    confidenceHigh = Color(0xFF5BD6B0),
)

/**
 * Derives page furniture from a Material You scheme so the notebook still looks intentional when the
 * user has handed colour selection to the system.
 *
 * The heatmap is pointedly *not* derived: red-amber-green encodes how much the reading can be
 * trusted, and a wallpaper that recolours "probably wrong" to lavender destroys information rather
 * than restyling it. Chrome follows the wallpaper; meaning does not.
 */
fun paperColorsFor(scheme: ColorScheme, darkTheme: Boolean): PaperColors {
    val brand = if (darkTheme) DarkPaperColors else LightPaperColors
    return brand.copy(
        paper = scheme.surface,
        paperEdge = scheme.outlineVariant,
        ruledLine = scheme.outlineVariant,
        gridLine = scheme.surfaceContainerHighest,
        marginRule = scheme.error,
        ink = scheme.onSurface,
        inkMuted = scheme.onSurfaceVariant,
        accentTeal = scheme.primary,
        accentGold = scheme.tertiary,
        highlighter = scheme.tertiary.copy(alpha = 0.30f),
        aiAnswerTint = scheme.secondaryContainer,
        aiAnswerBorder = scheme.outlineVariant,
    )
}
