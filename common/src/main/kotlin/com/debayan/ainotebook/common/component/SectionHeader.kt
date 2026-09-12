package com.debayan.ainotebook.common.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.debayan.ainotebook.common.theme.Dimens

/**
 * Label above a group of related rows or cards.
 *
 * Marked as a heading for accessibility services so a screen reader user can jump between groups
 * instead of walking every switch in a long settings screen. Deliberately not all-caps: uppercasing
 * a localized string breaks in Turkish and German, and the tracking that makes small caps work at
 * 11sp makes a two-word header look stretched.
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = Dimens.screenPadding,
                end = Dimens.screenPadding,
                top = Dimens.spaceXl,
                bottom = Dimens.spaceSm,
            ),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f, fill = true)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.semantics { heading() },
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Dimens.spaceXxs),
                )
            }
        }
        if (trailing != null) {
            trailing()
        }
    }
}

@PaperPreviews
@Composable
private fun SectionHeaderPreview() {
    PreviewScaffold {
        SectionHeader(title = "Handwriting")
        SectionHeader(
            title = "Cloud providers",
            subtitle = "Your key stays on this device.",
            trailing = {
                InkButton(
                    text = "Add",
                    onClick = {},
                    style = InkButtonStyle.TONAL,
                    compact = true,
                )
            },
        )
    }
}
