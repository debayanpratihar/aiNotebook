package com.debayan.ainotebook.common.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import com.debayan.ainotebook.common.theme.ConfidenceLevel
import com.debayan.ainotebook.common.theme.Dimens
import com.debayan.ainotebook.common.theme.PaperShapes
import com.debayan.ainotebook.common.theme.PaperTheme
import kotlin.math.roundToInt

/**
 * How sure the recognizer is about a reading, as a pill.
 *
 * Used once per page or selection, above the recognized text. The percentage is spelled out rather
 * than implied by colour alone: colour is the fast channel but it is also the one that fails for
 * colour-blind users and in bright sunlight, and "we might have misread your question" is not
 * information to encode in hue only.
 */
@Composable
fun ConfidenceChip(
    confidence: Float,
    modifier: Modifier = Modifier,
    label: String? = null,
    showPercent: Boolean = true,
) {
    val clamped = confidence.coerceIn(0f, 1f)
    val level = ConfidenceLevel.of(clamped)
    val rampColor = PaperTheme.colors.confidenceColor(level)
    val percent = (clamped * 100).roundToInt()
    val text = label ?: level.defaultLabel()
    val shown = if (showPercent) "$text · $percent%" else text

    Surface(
        modifier = modifier.clearAndSetSemantics {
            contentDescription = "Recognition confidence $percent percent, ${level.spokenPhrase()}"
        },
        shape = PaperShapes.pill,
        color = rampColor.copy(alpha = RAMP_TINT_ALPHA),
        contentColor = rampColor,
    ) {
        Row(
            modifier = Modifier
                .defaultMinSize(minHeight = Dimens.badgeHeight)
                .padding(horizontal = Dimens.spaceSm, vertical = Dimens.spaceXxs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.spaceXs),
        ) {
            Surface(
                modifier = Modifier.size(Dimens.spaceSm),
                shape = PaperShapes.pill,
                color = rampColor,
                content = {},
            )
            Text(text = shown, style = MaterialTheme.typography.labelSmall)
        }
    }
}

/**
 * One recognized word, underlined in its heatmap colour and optionally tappable to correct.
 *
 * High-confidence words are left unmarked by default. A page where every word is underlined carries
 * no information — the underline has to mean "look here", and it only can if most words do not have
 * one. [markHighConfidence] exists for the correction editor's "show all" mode, where the user is
 * deliberately auditing every reading.
 */
@Composable
fun ConfidenceUnderline(
    text: String,
    confidence: Float,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    markHighConfidence: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val clamped = confidence.coerceIn(0f, 1f)
    val level = ConfidenceLevel.of(clamped)
    val marked = markHighConfidence || level != ConfidenceLevel.HIGH
    val rampColor = PaperTheme.colors.confidenceColor(level)
    val description = if (marked) "$text, ${level.spokenPhrase()}" else text

    Text(
        text = text,
        style = style,
        color = PaperTheme.colors.ink,
        modifier = modifier
            .clip(PaperShapes.pill)
            .then(
                if (onClick == null) {
                    Modifier
                } else {
                    Modifier.clickable(onClickLabel = "Correct this reading", onClick = onClick)
                },
            )
            .semantics { contentDescription = description }
            .padding(horizontal = Dimens.spaceXxs)
            .drawBehind {
                if (!marked) return@drawBehind
                val thickness = Dimens.underlineThickness.toPx()
                val y = size.height - thickness / 2f
                drawLine(
                    color = rampColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = thickness,
                    cap = StrokeCap.Round,
                )
            },
    )
}

/** Tint strength for a ramp colour used as a container behind itself as text. */
private const val RAMP_TINT_ALPHA = 0.12f

private fun ConfidenceLevel.defaultLabel(): String = when (this) {
    ConfidenceLevel.LOW -> "Check reading"
    ConfidenceLevel.MEDIUM -> "Mostly clear"
    ConfidenceLevel.HIGH -> "Clear reading"
}

private fun ConfidenceLevel.spokenPhrase(): String = when (this) {
    ConfidenceLevel.LOW -> "low confidence, check this reading"
    ConfidenceLevel.MEDIUM -> "moderate confidence"
    ConfidenceLevel.HIGH -> "high confidence"
}

@PaperPreviews
@Composable
private fun ConfidenceIndicatorsPreview() {
    PreviewScaffold {
        ConfidenceChip(confidence = 0.41f)
        ConfidenceChip(confidence = 0.74f)
        ConfidenceChip(confidence = 0.96f)
        ConfidenceUnderline(text = "integral", confidence = 0.38f, onClick = {})
        ConfidenceUnderline(text = "of sin(x)", confidence = 0.71f)
        ConfidenceUnderline(text = "dx", confidence = 0.98f)
    }
}
