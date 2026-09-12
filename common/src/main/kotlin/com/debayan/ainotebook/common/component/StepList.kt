package com.debayan.ainotebook.common.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import com.debayan.ainotebook.common.theme.Dimens
import com.debayan.ainotebook.common.theme.PaperShapes

/**
 * The worked steps of a solution, numbered.
 *
 * Each row is collapsed into a single accessibility node reading "Step 2 of 5: ..." — without that,
 * a screen reader walks the marker and the prose separately and announces a bare "3" between two
 * sentences, which is worse than no numbering at all.
 *
 * [highlightLast] marks the final row as the conclusion. Steps are read top-down but *scanned* for
 * the answer, so the last row earns a different colour and weight rather than being one more
 * identical bullet.
 */
@Composable
fun StepList(
    steps: List<String>,
    modifier: Modifier = Modifier,
    dense: Boolean = false,
    highlightLast: Boolean = false,
) {
    if (steps.isEmpty()) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(if (dense) Dimens.spaceSm else Dimens.spaceMd),
    ) {
        steps.forEachIndexed { index, step ->
            val isConclusion = highlightLast && index == steps.lastIndex
            val markerColor = if (isConclusion) {
                MaterialTheme.colorScheme.tertiaryContainer
            } else {
                MaterialTheme.colorScheme.primaryContainer
            }
            val markerContent = if (isConclusion) {
                MaterialTheme.colorScheme.onTertiaryContainer
            } else {
                MaterialTheme.colorScheme.onPrimaryContainer
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clearAndSetSemantics {
                        contentDescription = "Step ${index + 1} of ${steps.size}: $step"
                    },
                horizontalArrangement = Arrangement.spacedBy(Dimens.spaceMd),
            ) {
                Surface(
                    shape = PaperShapes.pill,
                    color = markerColor,
                    contentColor = markerContent,
                ) {
                    Box(
                        modifier = Modifier.size(Dimens.stepMarker),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "${index + 1}",
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
                Text(
                    text = step,
                    style = if (isConclusion) {
                        MaterialTheme.typography.titleSmall
                    } else if (dense) {
                        MaterialTheme.typography.bodySmall
                    } else {
                        MaterialTheme.typography.bodyMedium
                    },
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f, fill = true),
                )
            }
        }
    }
}

@PaperPreviews
@Composable
private fun StepListPreview() {
    PreviewScaffold {
        StepList(
            steps = listOf(
                "Multiply 12 by 47 to get 564.",
                "Add the remaining 3.",
                "12 × 47 + 3 = 567",
            ),
            highlightLast = true,
        )
        StepList(
            steps = listOf("Isolate x.", "Divide both sides by 4."),
            dense = true,
        )
    }
}
