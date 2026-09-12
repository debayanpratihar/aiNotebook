package com.debayan.ainotebook.common.theme

import android.os.Build
import android.provider.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode

/**
 * Root theme. Installs the Material scheme plus the two ambients Material cannot carry: the paper
 * tokens ([LocalPaperColors]) and the user's motion preference ([LocalReducedMotion]).
 *
 * The presentation layer resolves [darkTheme] from the user's stored theme mode, keeping this
 * composable free of any domain dependency. When [dynamicColor] is on and the device supports
 * Material You (Android 12+), the wallpaper-derived scheme wins and the paper tokens are re-derived
 * from it by [paperColorsFor] — otherwise a Material You user would get lavender chrome with
 * stubbornly beige ruled lines.
 */
@Composable
fun AiNotebookTheme(
    darkTheme: Boolean,
    dynamicColor: Boolean,
    reducedMotion: Boolean = rememberReducedMotion(),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val useDynamic = dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val colorScheme = remember(useDynamic, darkTheme, context) {
        when {
            useDynamic && darkTheme -> dynamicDarkColorScheme(context)
            useDynamic -> dynamicLightColorScheme(context)
            darkTheme -> DarkColors
            else -> LightColors
        }
    }

    val paperColors = remember(colorScheme, useDynamic, darkTheme) {
        when {
            useDynamic -> paperColorsFor(colorScheme, darkTheme)
            darkTheme -> DarkPaperColors
            else -> LightPaperColors
        }
    }

    CompositionLocalProvider(
        LocalPaperColors provides paperColors,
        LocalReducedMotion provides reducedMotion,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            shapes = AppShapes,
            typography = AppTypography,
            content = content,
        )
    }
}

/**
 * Reads the system animator scale to decide whether animations should be suppressed.
 *
 * Android has no first-class "prefers reduced motion" flag; zeroing the animator duration scale
 * (developer options, or the toggle several accessibility suites drive) is the signal every app
 * has, and it is what the platform itself honours for window transitions. Read once per theme
 * change rather than observed: the setting can only be altered from outside the app, which restarts
 * activities anyway, and registering a `ContentObserver` for it would outlive the composition.
 *
 * Returns false in previews so component previews render their resting state rather than a fade.
 */
@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    val inspecting = LocalInspectionMode.current
    return remember(context, inspecting) {
        if (inspecting) {
            false
        } else {
            runCatching {
                Settings.Global.getFloat(
                    context.contentResolver,
                    Settings.Global.ANIMATOR_DURATION_SCALE,
                    1f,
                ) == 0f
            }.getOrDefault(false)
        }
    }
}
