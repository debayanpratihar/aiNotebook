package com.debayan.ainotebook.common.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.graphics.Shape

/**
 * The Material shape scale, one step softer than the default at the small end and one step tighter
 * at the large end.
 *
 * Small chips at 4dp look mechanical next to a warm paper surface, and the default 28dp extra-large
 * makes a full-width card read as a pill once it is wider than about 300dp — the corner starts
 * competing with the content for attention. Radii come from [Dimens] so shapes and spacing cannot
 * drift apart.
 */
val AppShapes: Shapes = Shapes(
    extraSmall = RoundedCornerShape(Dimens.radiusXs),
    small = RoundedCornerShape(Dimens.radiusSm),
    medium = RoundedCornerShape(Dimens.radiusMd),
    large = RoundedCornerShape(Dimens.radiusLg),
    extraLarge = RoundedCornerShape(Dimens.radiusXl),
)

/** Shapes for cases the Material scale does not cover. */
object PaperShapes {

    /** Badges, confidence pills, and anything else whose height defines its silhouette. */
    val pill: Shape = RoundedCornerShape(Dimens.radiusPill)

    /**
     * Bottom sheets: rounded at the top, square where they meet the screen edge. A sheet with four
     * rounded corners implies it is a floating card that could be dragged sideways.
     */
    val bottomSheet: Shape = RoundedCornerShape(
        topStart = Dimens.radiusXl,
        topEnd = Dimens.radiusXl,
        bottomStart = Dimens.none,
        bottomEnd = Dimens.none,
    )

    /** A notebook page: barely rounded, because paper has corners. */
    val page: Shape = RoundedCornerShape(Dimens.radiusSm)
}
