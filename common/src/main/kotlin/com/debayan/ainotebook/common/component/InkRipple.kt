package com.debayan.ainotebook.common.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.Dp
import com.debayan.ainotebook.common.theme.Dimens
import com.debayan.ainotebook.common.theme.PaperTheme

/**
 * The ink-bleed flourish played at the point where the AI starts writing.
 *
 * This is the app's one signature motion, and it earns its place by answering a question the user
 * would otherwise ask: *where* is the answer going to appear? A spinner in a corner cannot say
 * that. Three rings bleed outward from the insertion point over 420ms with a quadratic alpha decay
 * — the decay is what makes it read as ink spreading into paper rather than as a Material ripple,
 * which fades linearly and keeps a hard edge.
 *
 * Under reduced motion it degrades to a single 180ms fade of a soft disc: the location is still
 * communicated, nothing travels across the screen. It is purely decorative to accessibility
 * services; the surrounding answer surface is what announces the state change.
 *
 * Replays whenever [trigger] changes to a new non-null value, so a caller can pass the id of the
 * answer being written. While idle it draws nothing but still occupies [size] — place it in a [Box]
 * behind the content it points at rather than in a layout that would be reflowed by it.
 */
@Composable
fun InkRipple(
    trigger: Any?,
    modifier: Modifier = Modifier,
    color: Color = PaperTheme.colors.accentTeal,
    size: Dp = Dimens.inkRippleSize,
    reducedMotion: Boolean = PaperTheme.reducedMotion,
    onFinished: () -> Unit = {},
) {
    // Starts at 1f, which is the finished state, so an untriggered ripple never flashes on first
    // composition — the alternative is every answer card blooming as it scrolls into view.
    val bleed = remember { Animatable(1f) }
    val finished = rememberUpdatedState(onFinished)

    LaunchedEffect(trigger) {
        if (trigger == null) return@LaunchedEffect
        bleed.snapTo(0f)
        bleed.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = if (reducedMotion) FADE_MS else BLEED_MS,
                easing = FastOutSlowInEasing,
            ),
        )
        finished.value()
    }

    // A preview renders one static frame, and the resting frame of this component is empty. Pinning
    // it mid-bleed is the only way the flourish is reviewable without running the app.
    val progress = if (LocalInspectionMode.current) PREVIEW_PROGRESS else bleed.value

    Canvas(modifier = modifier.size(size)) {
        if (progress < 1f) {
            val maxRadius = this.size.minDimension / 2f
            if (reducedMotion) {
                drawCircle(
                    color = color.copy(alpha = CORE_ALPHA * (1f - progress)),
                    radius = maxRadius * STILL_RADIUS_FRACTION,
                )
            } else {
                drawCircle(
                    color = color.copy(alpha = CORE_ALPHA * (1f - progress)),
                    radius = maxRadius * (0.18f + 0.34f * progress),
                )
                val ringStroke = Dimens.underlineThickness.toPx()
                for (ring in 0 until RING_COUNT) {
                    val delay = ring * RING_STAGGER
                    val ringProgress = ((progress - delay) / (1f - delay)).coerceIn(0f, 1f)
                    if (ringProgress > 0f) {
                        val fade = (1f - ringProgress) * (1f - ringProgress)
                        drawCircle(
                            color = color.copy(alpha = fade * RING_ALPHA),
                            radius = maxRadius * (0.25f + 0.75f * ringProgress),
                            style = Stroke(width = ringStroke * (1f - 0.55f * ringProgress)),
                        )
                    }
                }
            }
        }
    }
}

private const val BLEED_MS = 420
private const val FADE_MS = 180
private const val RING_COUNT = 3
private const val RING_STAGGER = 0.18f
private const val RING_ALPHA = 0.40f
private const val CORE_ALPHA = 0.22f
private const val STILL_RADIUS_FRACTION = 0.7f
private const val PREVIEW_PROGRESS = 0.45f

@PaperPreviews
@Composable
private fun InkRipplePreview() {
    PreviewScaffold {
        Box(contentAlignment = Alignment.Center) {
            InkRipple(trigger = "preview")
            Text(text = "567", style = MaterialTheme.typography.headlineSmall)
        }
    }
}
