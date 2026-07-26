package com.debayan.ainotebook.common.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * Root Material 3 theme.
 *
 * The presentation layer resolves [darkTheme] from the user's [ThemeMode][com.debayan.ainotebook.domain.model.ThemeMode]
 * preference, keeping this composable free of any domain dependency. When [dynamicColor] is enabled
 * and the device supports Material You (Android 12+), the system-derived scheme is used; otherwise
 * the brand [LightColors]/[DarkColors] apply.
 */
@Composable
fun AiNotebookTheme(
    darkTheme: Boolean,
    dynamicColor: Boolean,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
