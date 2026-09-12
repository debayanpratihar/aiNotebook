package com.debayan.ainotebook.common.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.debayan.ainotebook.common.R

/*
 * Two faces, one job each.
 *
 * Inter carries every piece of UI text: it was designed for screens at small sizes, its digits are
 * unambiguous (a slashed zero would be better still, but 0/O and 1/l stay distinguishable), and that
 * matters more here than in most apps because half the text on screen is a machine's reading of the
 * user's own arithmetic.
 *
 * Caveat is exposed only as [handwritingFamily] and never wired into the Material scale. It exists
 * for one purpose: marking a block as "this is standing in for your handwriting" when the stroke
 * synthesizer has no glyph to draw. Using it for chrome would make the app look like a greeting
 * card and, worse, would put a script face on the recognized-text editor — the one place the user
 * needs to read characters unambiguously.
 *
 * Both files are variable fonts, so one resource per face covers every weight instead of shipping
 * four static cuts. Deriving a weight requires the `wght` axis to be applied at load time, which
 * needs API 26 — our minSdk is exactly 26, so this is safe without a fallback path. Note that the
 * `weight` argument is what the family resolver *matches* on and `variationSettings` is what
 * actually deforms the outlines: omitting either gives a family that silently renders everything at
 * 400 with faux-bold synthesis on top.
 */

/** Inter's `opsz` axis runs 14..32; 14 is the end tuned for running text. */
private val TextOpticalSize: TextUnit = 14.sp

/** Not the axis maximum: 32 thins the strokes further than a 36sp heading on warm paper wants. */
private val DisplayOpticalSize: TextUnit = 28.sp

private fun interFont(weight: Int, opticalSize: TextUnit): Font = Font(
    resId = R.font.inter_variable,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(
        FontVariation.weight(weight),
        FontVariation.opticalSizing(opticalSize),
    ),
)

private fun caveatFont(weight: Int): Font = Font(
    resId = R.font.caveat_variable,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

/** Inter with the `opsz` axis pinned to its text end (14), where the spacing is loosest. */
val interFamily: FontFamily = FontFamily(
    interFont(400, TextOpticalSize),
    interFont(500, TextOpticalSize),
    interFont(600, TextOpticalSize),
    interFont(700, TextOpticalSize),
)

/**
 * Inter with `opsz` near its display end (28).
 *
 * Inter's optical-size axis tightens sidebearings and thins the strokes as it rises, which is what
 * keeps a 36sp heading from looking spaced-out and slightly bloated. It is a separate family rather
 * than a per-style setting because the axis has to be baked into the loaded typeface.
 */
val interDisplayFamily: FontFamily = FontFamily(
    interFont(500, DisplayOpticalSize),
    interFont(600, DisplayOpticalSize),
    interFont(700, DisplayOpticalSize),
)

/** Caveat. Only for standing in for handwriting; never for UI chrome. Its axis stops at 700. */
val handwritingFamily: FontFamily = FontFamily(
    caveatFont(400),
    caveatFont(500),
    caveatFont(600),
    caveatFont(700),
)

/**
 * The Material 3 scale in Inter.
 *
 * Sizes deviate from the Material defaults in the reading range: `bodyLarge` is 17sp rather than
 * 16sp and its line height is 1.55x rather than 1.5x, because the body styles here hold recognized
 * handwriting and streamed explanations that get read at arm's length on a page full of ink, not
 * one-line list subtitles. Titles are 600 rather than 500 so a section still announces itself
 * without needing a heavier colour.
 */
val AppTypography: Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = interDisplayFamily,
        fontWeight = FontWeight.W600,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.5).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = interDisplayFamily,
        fontWeight = FontWeight.W600,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = (-0.25).sp,
    ),
    displaySmall = TextStyle(
        fontFamily = interDisplayFamily,
        fontWeight = FontWeight.W600,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = interDisplayFamily,
        fontWeight = FontWeight.W600,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.2).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = interDisplayFamily,
        fontWeight = FontWeight.W600,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.2).sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = interDisplayFamily,
        fontWeight = FontWeight.W600,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = interFamily,
        fontWeight = FontWeight.W600,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = interFamily,
        fontWeight = FontWeight.W600,
        fontSize = 17.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.1.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = interFamily,
        fontWeight = FontWeight.W600,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = interFamily,
        fontWeight = FontWeight.W400,
        fontSize = 17.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = interFamily,
        fontWeight = FontWeight.W400,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.1.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = interFamily,
        fontWeight = FontWeight.W400,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.2.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = interFamily,
        fontWeight = FontWeight.W600,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = interFamily,
        fontWeight = FontWeight.W600,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = interFamily,
        fontWeight = FontWeight.W600,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.4.sp,
    ),
)

/**
 * Styles the Material scale has no slot for.
 *
 * Reachable as `PaperTheme.text`. None of them carry a colour: a style that bakes in a colour
 * cannot be reused on a tinted card, which is precisely where the answer styles get used.
 */
object PaperTextStyles {

    /**
     * The answer itself, when it is short enough to be the whole message ("567", "x = 4").
     *
     * Tabular figures are mandatory here. Proportional digits make a result that updates in place
     * jitter horizontally, and the one thing a calculator-shaped surface must never do is wobble
     * while the user is reading it.
     */
    val answerHero: TextStyle = TextStyle(
        fontFamily = interDisplayFamily,
        fontWeight = FontWeight.W600,
        fontSize = 40.sp,
        lineHeight = 48.sp,
        letterSpacing = (-0.5).sp,
        fontFeatureSettings = "tnum",
    )

    /** The answer when it is prose: a worked explanation, several lines of it. */
    val answerBody: TextStyle = TextStyle(
        fontFamily = interFamily,
        fontWeight = FontWeight.W400,
        fontSize = 17.sp,
        lineHeight = 27.sp,
        letterSpacing = 0.sp,
    )

    /** The recognized expression, shown above its answer. Tabular so columns of digits line up. */
    val expression: TextStyle = TextStyle(
        fontFamily = interFamily,
        fontWeight = FontWeight.W500,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.2.sp,
        fontFeatureSettings = "tnum",
    )

    /** Small caps-ish label for section headers; tracking does the work, not size. */
    val overline: TextStyle = TextStyle(
        fontFamily = interFamily,
        fontWeight = FontWeight.W600,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.9.sp,
    )

    /** Caveat, at the size a typed stand-in for handwriting needs to be legible. */
    val handwriting: TextStyle = TextStyle(
        fontFamily = handwritingFamily,
        fontWeight = FontWeight.W500,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp,
    )
}
