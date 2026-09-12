package com.debayan.ainotebook.common.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.unit.Dp
import com.debayan.ainotebook.common.theme.Dimens
import com.debayan.ainotebook.common.theme.PaperShapes
import com.debayan.ainotebook.common.theme.PaperTheme

/** The page furniture a [PaperSurface] can draw underneath its content. */
enum class PaperRuling {
    NONE,
    RULED,
    GRID,
    DOTS,
}

/**
 * A notebook page: the paper fill plus optional ruling, with content on top.
 *
 * The ruling is drawn in a [drawBehind] loop instead of being tiled from a bitmap. A tiled
 * `BitmapShader` would need a texture upload, would have to be regenerated whenever spacing or the
 * theme changed, and would resample visibly once the canvas is zoomed; a few dozen `drawLine` calls
 * cost less than that upload and stay crisp at any scale. Drawing in the modifier also keeps the
 * ruling out of layout entirely, so a page that redraws while the user writes never re-measures.
 *
 * [lineSpacing] governs all three rulings so a caller cannot end up with ruled lines and grid
 * columns on different rhythms. The lines carry no semantics: a screen reader announcing "ruled
 * line" forty times before reaching the content would be actively harmful.
 */
@Composable
fun PaperSurface(
    modifier: Modifier = Modifier,
    ruling: PaperRuling = PaperRuling.NONE,
    color: Color = PaperTheme.colors.paper,
    lineColor: Color = Color.Unspecified,
    showMarginRule: Boolean = false,
    lineSpacing: Dp = Dimens.ruledLineSpacing,
    shape: Shape = RectangleShape,
    contentPadding: PaddingValues = PaddingValues(Dimens.none),
    content: @Composable BoxScope.() -> Unit,
) {
    val paper = PaperTheme.colors
    val resolvedLineColor = lineColor.takeOrElse {
        when (ruling) {
            PaperRuling.RULED -> paper.ruledLine
            PaperRuling.GRID, PaperRuling.DOTS -> paper.gridLine
            PaperRuling.NONE -> Color.Transparent
        }
    }
    val marginColor = paper.marginRule

    Surface(
        modifier = modifier,
        shape = shape,
        color = color,
        contentColor = paper.ink,
    ) {
        Box(
            modifier = Modifier
                .drawBehind {
                    val spacing = lineSpacing.toPx()
                    if (spacing < 1f) return@drawBehind
                    val hairline = Dimens.hairline.toPx()

                    when (ruling) {
                        PaperRuling.NONE -> Unit

                        PaperRuling.RULED -> {
                            var y = spacing
                            while (y < size.height) {
                                drawLine(
                                    color = resolvedLineColor,
                                    start = Offset(0f, y),
                                    end = Offset(size.width, y),
                                    strokeWidth = hairline,
                                )
                                y += spacing
                            }
                        }

                        PaperRuling.GRID -> {
                            var y = spacing
                            while (y < size.height) {
                                drawLine(
                                    color = resolvedLineColor,
                                    start = Offset(0f, y),
                                    end = Offset(size.width, y),
                                    strokeWidth = hairline,
                                )
                                y += spacing
                            }
                            var x = spacing
                            while (x < size.width) {
                                drawLine(
                                    color = resolvedLineColor,
                                    start = Offset(x, 0f),
                                    end = Offset(x, size.height),
                                    strokeWidth = hairline,
                                )
                                x += spacing
                            }
                        }

                        PaperRuling.DOTS -> {
                            val radius = Dimens.dotRadius.toPx()
                            var y = spacing
                            while (y < size.height) {
                                var x = spacing
                                while (x < size.width) {
                                    drawCircle(
                                        color = resolvedLineColor,
                                        radius = radius,
                                        center = Offset(x, y),
                                    )
                                    x += spacing
                                }
                                y += spacing
                            }
                        }
                    }

                    if (showMarginRule) {
                        val ruleX = Dimens.marginRuleInset.toPx()
                        drawLine(
                            color = marginColor,
                            start = Offset(ruleX, 0f),
                            end = Offset(ruleX, size.height),
                            strokeWidth = hairline,
                            cap = StrokeCap.Square,
                        )
                    }
                }
                .padding(contentPadding),
            content = content,
        )
    }
}

@PaperPreviews
@Composable
private fun PaperSurfacePreview() {
    PreviewScaffold {
        PaperSurface(
            modifier = Modifier
                .fillMaxWidth()
                .height(Dimens.spaceXxxl * 3),
            ruling = PaperRuling.RULED,
            showMarginRule = true,
            shape = PaperShapes.page,
            contentPadding = PaddingValues(
                start = Dimens.marginRuleInset + Dimens.spaceMd,
                top = Dimens.spaceMd,
                end = Dimens.spaceMd,
                bottom = Dimens.spaceMd,
            ),
        ) {
            Text(text = "12 x 47 + 3", style = MaterialTheme.typography.bodyLarge)
        }
        PaperSurface(
            modifier = Modifier
                .fillMaxWidth()
                .height(Dimens.spaceXxxl * 2),
            ruling = PaperRuling.DOTS,
            lineSpacing = Dimens.dotSpacing,
            shape = PaperShapes.page,
        ) {}
    }
}
