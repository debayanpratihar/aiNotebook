package com.debayan.ainotebook.common.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.debayan.ainotebook.common.theme.Dimens

/** How loudly a banner should speak. One level up is never the safe choice; it is just louder. */
enum class BannerSeverity {
    /** Something the user should know. No action failed. */
    INFO,

    /** Something will not work as well as it could until the user acts. */
    WARNING,

    /** Something the user asked for did not happen. */
    ERROR,
}

/**
 * An in-place message attached to the content it is about.
 *
 * Chosen over a snackbar for anything that persists: "no API key configured" is a condition, not an
 * event, and a message that slides away after four seconds cannot explain a condition. Marked as a
 * polite live region so a banner that appears after an action is announced without interrupting
 * whatever the screen reader is already saying.
 *
 * [actionLabel] and [onAction] exist so a dead end always ships with its own way out; a banner that
 * only describes a problem forces the user to go hunting through settings for the fix.
 */
@Composable
fun InlineBanner(
    message: String,
    modifier: Modifier = Modifier,
    severity: BannerSeverity = BannerSeverity.INFO,
    title: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
) {
    val container: Color
    val content: Color
    val icon: ImageVector

    when (severity) {
        BannerSeverity.INFO -> {
            container = MaterialTheme.colorScheme.secondaryContainer
            content = MaterialTheme.colorScheme.onSecondaryContainer
            icon = Icons.Filled.Info
        }

        BannerSeverity.WARNING -> {
            container = MaterialTheme.colorScheme.tertiaryContainer
            content = MaterialTheme.colorScheme.onTertiaryContainer
            icon = Icons.Filled.Warning
        }

        BannerSeverity.ERROR -> {
            container = MaterialTheme.colorScheme.errorContainer
            content = MaterialTheme.colorScheme.onErrorContainer
            icon = Icons.Filled.ErrorOutline
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite },
        shape = MaterialTheme.shapes.medium,
        color = container,
        contentColor = content,
    ) {
        Row(
            modifier = Modifier.padding(
                start = Dimens.cardPadding,
                top = Dimens.spaceMd,
                end = Dimens.spaceSm,
                bottom = Dimens.spaceMd,
            ),
            horizontalArrangement = Arrangement.spacedBy(Dimens.spaceMd),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier
                    .padding(top = Dimens.spaceXxs)
                    .size(Dimens.iconSm),
            )
            Column(modifier = Modifier.weight(1f, fill = true)) {
                if (title != null) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = if (title == null) {
                        Modifier
                    } else {
                        Modifier.padding(top = Dimens.spaceXxs)
                    },
                )
                if (actionLabel != null && onAction != null) {
                    TextButton(
                        onClick = onAction,
                        modifier = Modifier.padding(top = Dimens.spaceXs),
                        contentPadding = ButtonDefaults.TextButtonContentPadding,
                        colors = ButtonDefaults.textButtonColors(contentColor = content),
                    ) {
                        Text(text = actionLabel, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
            if (onDismiss != null) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Dismiss",
                        modifier = Modifier.size(Dimens.iconSm),
                    )
                }
            }
        }
    }
}

@PaperPreviews
@Composable
private fun InlineBannerPreview() {
    PreviewScaffold {
        InlineBanner(
            message = "Answers are solved on this device. Nothing is sent anywhere.",
        )
        InlineBanner(
            message = "The reading below looks uncertain. Check it before solving.",
            severity = BannerSeverity.WARNING,
            title = "Low confidence",
            actionLabel = "Review reading",
            onAction = {},
        )
        InlineBanner(
            message = "No provider is configured, so this question cannot be answered.",
            severity = BannerSeverity.ERROR,
            title = "Nothing to answer with",
            actionLabel = "Add an API key",
            onAction = {},
            onDismiss = {},
        )
    }
}
