package com.debayan.ainotebook.common.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import com.debayan.ainotebook.common.theme.Dimens
import com.debayan.ainotebook.common.theme.PaperShapes

/**
 * Where an answer came from, at the granularity the user actually cares about.
 *
 * The distinction that matters is not which vendor served the request but whether the request left
 * the device, which is why privacy is encoded in the enum rather than left to the label string. A
 * badge that reads "Groq" in the same colour as "Offline math" would be a privacy regression
 * dressed up as consistency.
 */
enum class AnswerSource {
    /** The offline math engine. No model, no network, exact. */
    OFFLINE_MATH,

    /** A language model running on this device. */
    ON_DEVICE_MODEL,

    /** A cloud provider the user configured. The question left the device. */
    CLOUD,

    /** Attribution is genuinely unknown — a restored note, a cached answer of unclear origin. */
    UNKNOWN,
}

/**
 * Small chip naming what served an answer: "Offline math", "On-device", "Groq".
 *
 * Always rendered next to the answer rather than behind a disclosure, because the only useful place
 * to tell someone their handwriting was sent to a third party is where the result appears.
 */
@Composable
fun ProviderBadge(
    label: String,
    source: AnswerSource,
    modifier: Modifier = Modifier,
    cached: Boolean = false,
) {
    val container: Color
    val content: Color
    val icon: ImageVector
    val spoken: String

    when (source) {
        AnswerSource.OFFLINE_MATH -> {
            container = MaterialTheme.colorScheme.primaryContainer
            content = MaterialTheme.colorScheme.onPrimaryContainer
            icon = Icons.Filled.Functions
            spoken = "Answered by $label. Nothing left this device."
        }

        AnswerSource.ON_DEVICE_MODEL -> {
            container = MaterialTheme.colorScheme.secondaryContainer
            content = MaterialTheme.colorScheme.onSecondaryContainer
            icon = Icons.Filled.Memory
            spoken = "Answered by $label, running on this device. Nothing left this device."
        }

        AnswerSource.CLOUD -> {
            container = MaterialTheme.colorScheme.tertiaryContainer
            content = MaterialTheme.colorScheme.onTertiaryContainer
            icon = Icons.Filled.Cloud
            spoken = "Answered by $label in the cloud. The recognized text left this device."
        }

        AnswerSource.UNKNOWN -> {
            container = MaterialTheme.colorScheme.surfaceContainerHighest
            content = MaterialTheme.colorScheme.onSurfaceVariant
            icon = Icons.Filled.Info
            spoken = "Answered by $label."
        }
    }

    val text = if (cached) "$label · cached" else label
    val description = if (cached) "$spoken Reused from the local cache." else spoken

    Surface(
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
        shape = PaperShapes.pill,
        color = container,
        contentColor = content,
    ) {
        Row(
            modifier = Modifier
                .defaultMinSize(minHeight = Dimens.badgeHeight)
                .padding(horizontal = Dimens.spaceSm, vertical = Dimens.spaceXxs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.spaceXs),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(Dimens.iconXs),
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@PaperPreviews
@Composable
private fun ProviderBadgePreview() {
    PreviewScaffold {
        ProviderBadge(label = "Offline math", source = AnswerSource.OFFLINE_MATH)
        ProviderBadge(label = "On-device", source = AnswerSource.ON_DEVICE_MODEL)
        ProviderBadge(label = "Groq", source = AnswerSource.CLOUD)
        ProviderBadge(label = "Groq", source = AnswerSource.CLOUD, cached = true)
        ProviderBadge(label = "Unknown", source = AnswerSource.UNKNOWN)
    }
}
