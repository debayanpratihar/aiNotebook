package com.debayan.ainotebook.common.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.progressSemantics
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import com.debayan.ainotebook.common.theme.Dimens

/**
 * A ring that reports progress: determinate when [progress] is known, sweeping when it is not.
 *
 * Hand-drawn rather than wrapping `CircularProgressIndicator` for two reasons — model downloads and
 * inference both want a percentage *inside* the ring, and the stroke has to stay thin enough to
 * read as a pencil line instead of a Material spinner. The infinite animation is only composed on
 * the indeterminate branch, so a list of finished downloads does not keep a frame-clock
 * subscription alive per row.
 *
 * The sweep keeps animating under reduced motion. Suppressing it would leave a static circle that
 * says nothing: here the movement *is* the information, and the only thing worse than motion for a
 * motion-sensitive user is an app that looks frozen.
 */
@Composable
fun ProgressRing(
    modifier: Modifier = Modifier,
    progress: Float? = null,
    size: Dp = Dimens.progressRingSize,
    strokeWidth: Dp = Dimens.progressRingStroke,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    centerLabel: String? = null,
    contentDescription: String? = null,
) {
    val clamped = progress?.coerceIn(0f, 1f)
    val description = contentDescription
    val semanticsModifier = if (clamped != null) {
        Modifier.progressSemantics(clamped)
    } else {
        Modifier.progressSemantics()
    }

    Box(
        modifier = modifier
            .size(size)
            .then(semanticsModifier)
            .then(
                if (description == null) {
                    Modifier
                } else {
                    Modifier.semantics { this.contentDescription = description }
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (clamped != null) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = strokeWidth.toPx()
                drawRingTrack(trackColor, stroke)
                if (clamped > 0f) {
                    drawRingArc(
                        color = color,
                        startAngle = -90f,
                        sweepAngle = 360f * clamped,
                        stroke = stroke,
                    )
                }
            }
        } else {
            IndeterminateRing(
                color = color,
                trackColor = trackColor,
                strokeWidth = strokeWidth,
            )
        }
        if (centerLabel != null) {
            Text(
                text = centerLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun IndeterminateRing(
    color: Color,
    trackColor: Color,
    strokeWidth: Dp,
) {
    val transition = rememberInfiniteTransition(label = "ProgressRing")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = SWEEP_ROTATION_MS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "ProgressRingRotation",
    )
    val sweep by transition.animateFloat(
        initialValue = MIN_SWEEP_DEGREES,
        targetValue = MAX_SWEEP_DEGREES,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = SWEEP_BREATH_MS, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "ProgressRingSweep",
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val stroke = strokeWidth.toPx()
        drawRingTrack(trackColor, stroke)
        drawRingArc(
            color = color,
            startAngle = rotation - 90f,
            sweepAngle = sweep,
            stroke = stroke,
        )
    }
}

private fun DrawScope.drawRingTrack(color: Color, stroke: Float) {
    drawCircle(
        color = color,
        radius = (size.minDimension - stroke) / 2f,
        style = Stroke(width = stroke),
    )
}

private fun DrawScope.drawRingArc(
    color: Color,
    startAngle: Float,
    sweepAngle: Float,
    stroke: Float,
) {
    val inset = stroke / 2f
    drawArc(
        color = color,
        startAngle = startAngle,
        sweepAngle = sweepAngle,
        useCenter = false,
        topLeft = Offset(inset, inset),
        size = Size(width = size.width - stroke, height = size.height - stroke),
        style = Stroke(width = stroke, cap = StrokeCap.Round),
    )
}

private const val SWEEP_ROTATION_MS = 1_150
private const val SWEEP_BREATH_MS = 780
private const val MIN_SWEEP_DEGREES = 36f
private const val MAX_SWEEP_DEGREES = 268f

@PaperPreviews
@Composable
private fun ProgressRingPreview() {
    PreviewScaffold {
        ProgressRing(contentDescription = "Preparing the model")
        ProgressRing(progress = 0.62f, centerLabel = "62%")
        ProgressRing(
            progress = 1f,
            size = Dimens.iconLg,
            strokeWidth = Dimens.progressRingStrokeSmall,
        )
    }
}
