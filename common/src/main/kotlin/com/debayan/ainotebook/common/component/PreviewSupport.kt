package com.debayan.ainotebook.common.component

import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.debayan.ainotebook.common.theme.AiNotebookTheme
import com.debayan.ainotebook.common.theme.Dimens

/**
 * Renders every annotated preview twice, light and dark.
 *
 * A multipreview annotation rather than two functions per component: the dark scheme is where
 * hand-picked colours break, so making it impossible to add a component preview without also
 * getting the dark one is the point.
 */
@Preview(name = "Light")
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
internal annotation class PaperPreviews

/**
 * Theme + surface wrapper for component previews. Dark mode comes from the preview's `uiMode`, which
 * [isSystemInDarkTheme] reads out of the preview configuration, so one body serves both variants.
 */
@Composable
internal fun PreviewScaffold(content: @Composable ColumnScope.() -> Unit) {
    AiNotebookTheme(darkTheme = isSystemInDarkTheme(), dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Column(
                modifier = Modifier.padding(Dimens.spaceLg),
                verticalArrangement = Arrangement.spacedBy(Dimens.spaceMd),
                content = content,
            )
        }
    }
}
