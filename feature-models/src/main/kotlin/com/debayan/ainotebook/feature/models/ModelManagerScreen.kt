package com.debayan.ainotebook.feature.models

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.debayan.ainotebook.common.component.LoadingIndicator
import com.debayan.ainotebook.domain.model.ai.InstalledModel
import com.debayan.ainotebook.domain.model.ai.ModelDownloadProgress
import com.debayan.ainotebook.domain.model.ai.ModelDownloadState
import com.debayan.ainotebook.domain.model.ai.RemoteModel

private val ACTIVE_DOWNLOAD_STATES = setOf(
    ModelDownloadState.QUEUED,
    ModelDownloadState.DOWNLOADING,
    ModelDownloadState.VERIFYING,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelManagerScreen(
    state: ModelManagerUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onDownload: (RemoteModel) -> Unit,
    onCancelDownload: (String) -> Unit,
    onActivate: (String) -> Unit,
    onDelete: (String) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Model Manager") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = { TextButton(onClick = onRefresh) { Text("Refresh") } },
            )
        },
    ) { innerPadding ->
        when {
            state.isLoading -> LoadingIndicator(modifier = Modifier.padding(innerPadding))

            else -> LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                state.errorMessage?.let { message ->
                    item { ErrorBanner(message) }
                }

                if (state.installed.isNotEmpty()) {
                    item { SectionHeader("Installed") }
                    items(state.installed, key = { it.id }) { model ->
                        InstalledModelCard(
                            model = model,
                            isActive = model.id == state.activeModelId,
                            onActivate = { onActivate(model.id) },
                            onDelete = { onDelete(model.id) },
                        )
                    }
                }

                item { SectionHeader("Available") }
                items(state.available, key = { it.model.id }) { ui ->
                    AvailableModelCard(
                        ui = ui,
                        onDownload = { onDownload(ui.model) },
                        onCancel = { onCancelDownload(ui.model.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun ErrorBanner(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        ),
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.onErrorContainer,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Composable
private fun InstalledModelCard(
    model: InstalledModel,
    isActive: Boolean,
    onActivate: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(model.name, style = MaterialTheme.typography.titleMedium)
            Text(
                text = "${model.tier.name.lowercase().replaceFirstChar { it.uppercase() }} · ${formatBytes(model.sizeBytes)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isActive) {
                    Text(
                        text = "Active",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    Button(onClick = onActivate, modifier = Modifier.weight(1f)) { Text("Activate") }
                }
                OutlinedButton(onClick = onDelete) { Text("Delete") }
            }
        }
    }
}

@Composable
private fun AvailableModelCard(
    ui: AvailableModelUi,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(ui.model.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (ui.isRecommended) {
                    Text(
                        text = "Recommended",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Text(
                text = "${ui.model.provider} · ${formatBytes(ui.model.sizeBytes)} · ${ui.model.recommendedRamMb} MB RAM",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (ui.model.description.isNotBlank()) {
                Text(
                    text = ui.model.description,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            if (!ui.compatibility.isCompatible && ui.compatibility.reason != null) {
                Text(
                    text = ui.compatibility.reason!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }

            val download = ui.download
            Column(modifier = Modifier.padding(top = 12.dp)) {
                when {
                    ui.isInstalled -> Text(
                        text = "Installed",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )

                    download != null && download.state in ACTIVE_DOWNLOAD_STATES -> {
                        if (download.state == ModelDownloadState.VERIFYING) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        } else {
                            LinearProgressIndicator(
                                progress = { download.percent / 100f },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(download.statusText(), style = MaterialTheme.typography.bodySmall)
                            TextButton(onClick = onCancel) { Text("Cancel") }
                        }
                    }

                    download?.state == ModelDownloadState.FAILED -> {
                        Text(
                            text = download.errorMessage ?: "Download failed",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Button(
                            onClick = onDownload,
                            enabled = ui.compatibility.isCompatible,
                            modifier = Modifier.padding(top = 8.dp),
                        ) { Text("Retry") }
                    }

                    else -> Button(onClick = onDownload, enabled = ui.compatibility.isCompatible) {
                        Text("Download")
                    }
                }
            }
        }
    }
}

private fun ModelDownloadProgress.statusText(): String = when (state) {
    ModelDownloadState.QUEUED -> "Queued…"
    ModelDownloadState.DOWNLOADING -> "Downloading $percent%"
    ModelDownloadState.VERIFYING -> "Verifying…"
    ModelDownloadState.INSTALLED -> "Installed"
    ModelDownloadState.FAILED -> errorMessage ?: "Download failed"
    ModelDownloadState.CANCELLED -> "Cancelled"
}

private const val BYTES_PER_MB = 1024.0 * 1024.0
private const val BYTES_PER_GB = BYTES_PER_MB * 1024.0

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "—"
    val gb = bytes / BYTES_PER_GB
    if (gb >= 1.0) return "%.1f GB".format(gb)
    return "%.0f MB".format(bytes / BYTES_PER_MB)
}
