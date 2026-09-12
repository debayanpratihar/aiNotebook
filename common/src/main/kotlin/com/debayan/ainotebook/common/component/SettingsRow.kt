package com.debayan.ainotebook.common.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import com.debayan.ainotebook.common.theme.Dimens

/**
 * Opacity for a row whose control is unavailable. Matches Material's disabled content alpha; the
 * row stays readable so the user can still find out *what* is unavailable.
 */
private const val DISABLED_ALPHA = 0.38f

/**
 * One line of a settings list: title, optional subtitle, optional leading icon, and whatever
 * control belongs on the right.
 *
 * The subtitle is where a setting earns its keep — most of this app's switches trade battery,
 * privacy, or accuracy against each other, and a bare label like "Solve as you write" tells a user
 * nothing about which one they are spending. Rows are sized to a real touch target even when the
 * visible control is a 20dp switch thumb.
 */
@Composable
fun SettingsRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    val clickModifier = if (onClick != null) {
        Modifier.clickable(enabled = enabled, onClick = onClick)
    } else {
        Modifier
    }
    RowBody(
        title = title,
        subtitle = subtitle,
        icon = icon,
        enabled = enabled,
        modifier = modifier.then(clickModifier),
        trailing = trailing,
    )
}

/**
 * A settings row whose whole surface toggles the switch.
 *
 * The [Switch] itself is handed `onCheckedChange = null` so the row is a single accessibility node
 * with `Role.Switch`: two separate targets for one setting means a screen reader announces the
 * switch twice and a thumb-sized hit area for something that has a full-width row available.
 */
@Composable
fun SettingsSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    RowBody(
        title = title,
        subtitle = subtitle,
        icon = icon,
        enabled = enabled,
        modifier = modifier.toggleable(
            value = checked,
            enabled = enabled,
            role = Role.Switch,
            onValueChange = onCheckedChange,
        ),
        trailing = {
            Switch(
                checked = checked,
                onCheckedChange = null,
                enabled = enabled,
            )
        },
    )
}

/** A settings row that navigates somewhere, with an optional current [value] shown before the chevron. */
@Composable
fun SettingsNavRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    value: String? = null,
    enabled: Boolean = true,
) {
    RowBody(
        title = title,
        subtitle = subtitle,
        icon = icon,
        enabled = enabled,
        modifier = modifier.clickable(
            enabled = enabled,
            role = Role.Button,
            onClick = onClick,
        ),
        trailing = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.spaceXs),
            ) {
                if (value != null) {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodyMedium,
                        color = contentColor(
                            base = MaterialTheme.colorScheme.onSurfaceVariant,
                            enabled = enabled,
                        ),
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(Dimens.iconMd),
                    tint = contentColor(
                        base = MaterialTheme.colorScheme.onSurfaceVariant,
                        enabled = enabled,
                    ),
                )
            }
        },
    )
}

/**
 * A settings row that displays a current value without navigating anywhere — a model size, a
 * detected backend, a language tag. [onClick] is optional so the same row can become tappable when
 * there is somewhere to go.
 */
@Composable
fun SettingsValueRow(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    SettingsRow(
        title = title,
        modifier = modifier,
        subtitle = subtitle,
        icon = icon,
        enabled = enabled,
        onClick = onClick,
        trailing = {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor(
                    base = MaterialTheme.colorScheme.onSurfaceVariant,
                    enabled = enabled,
                ),
            )
        },
    )
}

@Composable
private fun RowBody(
    title: String,
    subtitle: String?,
    icon: ImageVector?,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(
                minHeight = if (subtitle == null) Dimens.rowMinHeight else Dimens.rowMinHeightTwoLine,
            )
            .padding(horizontal = Dimens.screenPadding, vertical = Dimens.spaceMd),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.spaceLg),
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(Dimens.iconMd),
                tint = contentColor(base = MaterialTheme.colorScheme.primary, enabled = enabled),
            )
        }
        Column(modifier = Modifier.weight(1f, fill = true)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = contentColor(base = MaterialTheme.colorScheme.onSurface, enabled = enabled),
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor(
                        base = MaterialTheme.colorScheme.onSurfaceVariant,
                        enabled = enabled,
                    ),
                    modifier = Modifier.padding(top = Dimens.spaceXxs),
                )
            }
        }
        if (trailing != null) {
            trailing()
        }
    }
}

private fun contentColor(base: Color, enabled: Boolean): Color =
    if (enabled) base else base.copy(alpha = DISABLED_ALPHA)

@PaperPreviews
@Composable
private fun SettingsRowPreview() {
    PreviewScaffold {
        SettingsSwitchRow(
            title = "Solve as you write",
            checked = false,
            onCheckedChange = {},
            subtitle = "Costs battery and can interrupt you mid-thought.",
            icon = Icons.Filled.Edit,
        )
        SettingsSwitchRow(
            title = "Allow cloud answers",
            checked = true,
            onCheckedChange = {},
            subtitle = "Only recognized text is sent. Never ink or page images.",
            icon = Icons.Filled.Cloud,
        )
        SettingsNavRow(
            title = "Local model",
            onClick = {},
            value = "Qwen 1.5B",
            icon = Icons.Filled.Memory,
        )
        SettingsValueRow(
            title = "Recognition language",
            value = "en-US",
            subtitle = "Downloaded",
        )
        SettingsSwitchRow(
            title = "Handwritten replies",
            checked = false,
            onCheckedChange = {},
            subtitle = "Needs a trained handwriting sample.",
            enabled = false,
        )
    }
}
