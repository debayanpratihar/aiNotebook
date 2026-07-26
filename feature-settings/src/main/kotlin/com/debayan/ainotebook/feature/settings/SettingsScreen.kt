package com.debayan.ainotebook.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.debayan.ainotebook.domain.model.ThemeMode
import com.debayan.ainotebook.domain.model.UserPreferences
import com.debayan.ainotebook.domain.model.canvas.SmoothingMode
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    prefs: UserPreferences,
    onEvent: (SettingsEvent) -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            SettingsSection("Appearance") {
                ChoiceRow(
                    title = "Theme",
                    options = ThemeMode.entries.map { it to it.label() },
                    selected = prefs.themeMode,
                    onSelect = { onEvent(SettingsEvent.SetThemeMode(it)) },
                )
                SwitchRow(
                    title = "Dynamic color",
                    subtitle = "Use wallpaper-based colors on Android 12+",
                    checked = prefs.useDynamicColor,
                    onCheckedChange = { onEvent(SettingsEvent.SetDynamicColor(it)) },
                )
            }

            SettingsSection("AI") {
                SwitchRow("Enable AI", null, prefs.aiEnabled) { onEvent(SettingsEvent.SetAiEnabled(it)) }
                SwitchRow(
                    "Automatic generation",
                    "Generate after you pause writing",
                    prefs.automaticAiGeneration,
                ) { onEvent(SettingsEvent.SetAutomaticAi(it)) }
                SwitchRow("Stream responses", null, prefs.streamAiResponses) {
                    onEvent(SettingsEvent.SetStreamAi(it))
                }
                SliderRow(
                    title = "Inactivity timeout",
                    valueLabel = "${prefs.aiInactivityTimeoutSeconds}s",
                    value = prefs.aiInactivityTimeoutSeconds.toFloat(),
                    range = 1f..10f,
                    steps = 8,
                    onChange = { onEvent(SettingsEvent.SetAiTimeout(it.roundToInt())) },
                )
            }

            SettingsSection("Drawing") {
                ChoiceRow(
                    title = "Smoothing",
                    options = SmoothingMode.entries.map { it to it.label() },
                    selected = prefs.defaultSmoothing,
                    onSelect = { onEvent(SettingsEvent.SetSmoothing(it)) },
                )
                SwitchRow("Pressure sensitivity", null, prefs.pressureSensitivityEnabled) {
                    onEvent(SettingsEvent.SetPressure(it))
                }
                SliderRow(
                    title = "Default pen width",
                    valueLabel = "${prefs.defaultPenWidth.roundToInt()} px",
                    value = prefs.defaultPenWidth,
                    range = 1f..20f,
                    steps = 18,
                    onChange = { onEvent(SettingsEvent.SetPenWidth(it)) },
                )
            }

            SettingsSection("OCR & Search") {
                SwitchRow("Enable OCR", "Recognize handwriting for search", prefs.ocrEnabled) {
                    onEvent(SettingsEvent.SetOcrEnabled(it))
                }
                SwitchRow("Automatic indexing", null, prefs.automaticIndexing) {
                    onEvent(SettingsEvent.SetAutomaticIndexing(it))
                }
            }

            SettingsSection("Downloads") {
                SwitchRow("Wi-Fi only", "Only download models on Wi-Fi", prefs.wifiOnlyDownloads) {
                    onEvent(SettingsEvent.SetWifiOnly(it))
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
        )
        content()
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SliderRow(
    title: String,
    valueLabel: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    onChange: (Float) -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                valueLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Slider(value = value, onValueChange = onChange, valueRange = range, steps = steps)
    }
}

@Composable
private fun <T> ChoiceRow(
    title: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            options.forEach { (value, label) ->
                FilterChip(
                    selected = value == selected,
                    onClick = { onSelect(value) },
                    label = { Text(label) },
                )
            }
        }
    }
}

private fun ThemeMode.label(): String = when (this) {
    ThemeMode.SYSTEM -> "System"
    ThemeMode.LIGHT -> "Light"
    ThemeMode.DARK -> "Dark"
}

private fun SmoothingMode.label(): String = when (this) {
    SmoothingMode.OFF -> "Off"
    SmoothingMode.LOW -> "Low"
    SmoothingMode.MEDIUM -> "Medium"
    SmoothingMode.HIGH -> "High"
    SmoothingMode.ADAPTIVE -> "Adaptive"
}
