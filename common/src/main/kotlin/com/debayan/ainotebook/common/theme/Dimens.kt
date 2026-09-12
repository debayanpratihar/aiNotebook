package com.debayan.ainotebook.common.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The single source of spacing, sizing, and radius values for the whole app.
 *
 * Everything here is a multiple of 4dp except three deliberate exceptions — [screenPadding] (20dp),
 * [sectionGap] (28dp), and [ruledLineSpacing] (28dp) — which exist because a 16dp gutter crowds a
 * full-bleed page and a 24dp one wastes it on a phone, and because ruled lines have to land on a
 * rhythm that matches the body line height rather than the layout grid.
 *
 * Call sites must never write their own `dp` literals for layout. Two screens that each pick "about
 * 16" drift apart the moment one of them is tuned, and drift is exactly what makes an app feel
 * unfinished; a named constant makes the next tuning pass a one-line change.
 */
object Dimens {

    /** Explicit zero, so a `PaddingValues` default does not need a literal. */
    val none: Dp = 0.dp

    val spaceXxs: Dp = 2.dp
    val spaceXs: Dp = 4.dp
    val spaceSm: Dp = 8.dp
    val spaceMd: Dp = 12.dp
    val spaceLg: Dp = 16.dp
    val spaceXl: Dp = 24.dp
    val spaceXxl: Dp = 32.dp
    val spaceXxxl: Dp = 48.dp

    /** Horizontal gutter for every screen's primary content column. */
    val screenPadding: Dp = 20.dp

    /** Vertical breathing room between unrelated groups of content. */
    val sectionGap: Dp = 28.dp

    /** Interior padding of cards, banners, and sheets. */
    val cardPadding: Dp = 16.dp

    /** Interior padding of the answer card, which carries more visual weight than a list card. */
    val cardPaddingLarge: Dp = 20.dp

    /** Gap between sibling cards in a vertical list. */
    val listGap: Dp = 10.dp

    /**
     * Caps the content column on tablets and unfolded foldables. Lines longer than roughly 70
     * characters are measurably harder to scan, and an unconstrained settings list on a 10" screen
     * puts the switch a hand-span away from its label.
     */
    val maxContentWidth: Dp = 560.dp

    val iconXs: Dp = 14.dp
    val iconSm: Dp = 18.dp
    val iconMd: Dp = 22.dp
    val iconLg: Dp = 28.dp
    val iconXl: Dp = 40.dp

    /** Diameter of the tonal disc behind an empty-state icon. */
    val emptyStateIconBox: Dp = 80.dp

    /** Android's minimum comfortable touch target; every interactive row honours it. */
    val touchTarget: Dp = 48.dp

    val rowMinHeight: Dp = 56.dp
    val rowMinHeightTwoLine: Dp = 68.dp

    val buttonHeight: Dp = 48.dp
    val buttonHeightCompact: Dp = 36.dp

    /** Height of the small attribution/confidence pills. Deliberately below chip height. */
    val badgeHeight: Dp = 26.dp

    val progressRingSize: Dp = 44.dp
    val progressRingSizeSmall: Dp = 18.dp
    val progressRingStroke: Dp = 4.dp
    val progressRingStrokeSmall: Dp = 2.dp

    /** Footprint the ink-bleed flourish occupies where an answer starts being written. */
    val inkRippleSize: Dp = 72.dp

    val hairline: Dp = 1.dp
    val borderWidth: Dp = 1.dp
    val underlineThickness: Dp = 2.dp

    /** Diameter of the numbered marker in a step list. */
    val stepMarker: Dp = 22.dp

    val radiusXs: Dp = 6.dp
    val radiusSm: Dp = 10.dp
    val radiusMd: Dp = 14.dp
    val radiusLg: Dp = 20.dp
    val radiusXl: Dp = 28.dp

    /** Larger than any component we draw, so a rounded rect resolves to a pill. */
    val radiusPill: Dp = 1_000.dp

    val elevationNone: Dp = 0.dp
    val elevationCard: Dp = 1.dp
    val elevationRaised: Dp = 3.dp
    val elevationSheet: Dp = 6.dp

    /** Baseline-to-baseline spacing of ruled paper; matches `bodyLarge`'s line height plus one step. */
    val ruledLineSpacing: Dp = 28.dp
    val gridSpacing: Dp = 24.dp
    val dotSpacing: Dp = 22.dp
    val dotRadius: Dp = 1.dp

    /** Distance from the page's left edge to the margin rule. */
    val marginRuleInset: Dp = 40.dp
}
