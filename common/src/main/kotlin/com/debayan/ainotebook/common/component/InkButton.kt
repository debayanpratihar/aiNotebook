package com.debayan.ainotebook.common.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.debayan.ainotebook.common.theme.Dimens
import com.debayan.ainotebook.common.theme.PaperShapes
import com.debayan.ainotebook.common.theme.PaperTheme

/** Weight of a button in the hierarchy of a screen. At most one [PRIMARY] per screen region. */
enum class InkButtonStyle {
    /** The one action the screen exists for. Filled, brand colour. */
    PRIMARY,

    /** A real alternative to the primary action. Outlined, so it reads as a peer, not a link. */
    SECONDARY,

    /** Frequent but unremarkable actions. Tonal, quiet enough to repeat in a list. */
    TONAL,
}

/**
 * The app's button.
 *
 * Wraps Material's three fills behind one call site so a screen cannot accidentally mix a
 * `FilledTonalButton` in one place with a `Button` in another for the same weight of action, and so
 * the pressed-state scale is consistent everywhere. The scale is 3%, springs back, and is skipped
 * entirely under reduced motion — it is there to make a tap feel acknowledged before the navigation
 * or the inference starts, which is exactly the moment when nothing else on screen has changed yet.
 *
 * [loading] takes the button out of service and shows a spinner in place of the icon rather than
 * swapping the label, so the button neither resizes nor loses the word the user was reading.
 */
@Composable
fun InkButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: InkButtonStyle = InkButtonStyle.PRIMARY,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    loading: Boolean = false,
    fillWidth: Boolean = false,
    compact: Boolean = false,
    contentDescription: String? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val reducedMotion = PaperTheme.reducedMotion
    val scale by animateFloatAsState(
        targetValue = if (pressed && !reducedMotion) PRESSED_SCALE else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 900f),
        label = "InkButtonPress",
    )

    val shape = MaterialTheme.shapes.small
    val description = contentDescription
    val contentPadding = if (compact) {
        PaddingValues(horizontal = Dimens.spaceMd, vertical = Dimens.spaceXs)
    } else {
        PaddingValues(horizontal = Dimens.spaceXl, vertical = Dimens.spaceMd)
    }
    val buttonModifier = modifier
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .defaultMinSize(
            minHeight = if (compact) Dimens.buttonHeightCompact else Dimens.buttonHeight,
        )
        .then(if (fillWidth) Modifier.fillMaxWidth() else Modifier)
        .semantics {
            if (description != null) contentDescription = description
            if (loading) stateDescription = "Working"
        }

    val body: @Composable () -> Unit = {
        Row(verticalAlignment = Alignment.CenterVertically) {
            when {
                loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(Dimens.progressRingSizeSmall),
                        color = LocalContentColor.current,
                        strokeWidth = Dimens.progressRingStrokeSmall,
                    )
                    Spacer(modifier = Modifier.width(Dimens.spaceSm))
                }

                icon != null -> {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(Dimens.iconSm),
                    )
                    Spacer(modifier = Modifier.width(Dimens.spaceSm))
                }
            }
            Text(text = text, style = MaterialTheme.typography.labelLarge)
        }
    }

    when (style) {
        InkButtonStyle.PRIMARY -> Button(
            onClick = onClick,
            modifier = buttonModifier,
            enabled = enabled && !loading,
            shape = shape,
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = Dimens.elevationNone,
                pressedElevation = Dimens.elevationNone,
            ),
            contentPadding = contentPadding,
            interactionSource = interactionSource,
            content = { body() },
        )

        InkButtonStyle.SECONDARY -> OutlinedButton(
            onClick = onClick,
            modifier = buttonModifier,
            enabled = enabled && !loading,
            shape = shape,
            contentPadding = contentPadding,
            interactionSource = interactionSource,
            content = { body() },
        )

        InkButtonStyle.TONAL -> FilledTonalButton(
            onClick = onClick,
            modifier = buttonModifier,
            enabled = enabled && !loading,
            shape = shape,
            contentPadding = contentPadding,
            interactionSource = interactionSource,
            content = { body() },
        )
    }
}

/**
 * Shallow on purpose. Anything deeper reads as a wobble rather than a press once the button is
 * wider than a couple of hundred dp, because the corners travel further than the eye expects.
 */
private const val PRESSED_SCALE = 0.97f

@PaperPreviews
@Composable
private fun InkButtonPreview() {
    PreviewScaffold {
        InkButton(text = "Solve", onClick = {}, icon = Icons.Filled.AutoAwesome)
        InkButton(text = "Solve", onClick = {}, loading = true)
        InkButton(text = "Not now", onClick = {}, style = InkButtonStyle.SECONDARY)
        InkButton(text = "Add provider", onClick = {}, style = InkButtonStyle.TONAL)
        InkButton(text = "Disabled", onClick = {}, enabled = false)
        InkButton(text = "Full width", onClick = {}, fillWidth = true)
        InkButton(text = "Compact", onClick = {}, style = InkButtonStyle.TONAL, compact = true)
    }
}
