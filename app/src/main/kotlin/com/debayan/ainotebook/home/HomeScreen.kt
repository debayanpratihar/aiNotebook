package com.debayan.ainotebook.home

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.debayan.ainotebook.common.component.EmptyState
import com.debayan.ainotebook.domain.model.Notebook
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@Composable
fun HomeRoute(
    onOpenNotebook: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenModels: () -> Unit,
    onOpenSearch: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val notebooks by viewModel.notebooks.collectAsStateWithLifecycle()
    val importError by viewModel.importError.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val path = withContext(Dispatchers.IO) { copyUriToCache(context, uri) }
                if (path != null) viewModel.importNotebook(path, onOpenNotebook)
            }
        }
    }

    LaunchedEffect(importError) {
        importError?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.consumeImportError()
        }
    }

    HomeScreen(
        notebooks = notebooks,
        snackbarHostState = snackbarHostState,
        onCreateNotebook = { name -> viewModel.createNotebook(name, onOpenNotebook) },
        onOpenNotebook = onOpenNotebook,
        onRenameNotebook = viewModel::renameNotebook,
        onDeleteNotebook = viewModel::deleteNotebook,
        onOpenSettings = onOpenSettings,
        onOpenModels = onOpenModels,
        onOpenSearch = onOpenSearch,
        onImport = { importLauncher.launch(arrayOf("*/*")) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    notebooks: List<Notebook>,
    snackbarHostState: SnackbarHostState,
    onCreateNotebook: (String) -> Unit,
    onOpenNotebook: (String) -> Unit,
    onRenameNotebook: (String, String) -> Unit,
    onDeleteNotebook: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenModels: () -> Unit,
    onOpenSearch: () -> Unit,
    onImport: () -> Unit,
) {
    var renameTarget by remember { mutableStateOf<Notebook?>(null) }
    var deleteTarget by remember { mutableStateOf<Notebook?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Notebook") },
                actions = {
                    HomeOverflowMenu(
                        onOpenSearch = onOpenSearch,
                        onImport = onImport,
                        onOpenModels = onOpenModels,
                        onOpenSettings = onOpenSettings,
                    )
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateDialog = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("New notebook") },
            )
        },
    ) { innerPadding ->
        if (notebooks.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.Edit,
                title = "No notebooks yet",
                message = "Tap New notebook to start writing.",
                modifier = Modifier.padding(innerPadding),
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(notebooks, key = { it.id }) { notebook ->
                    NotebookCard(
                        notebook = notebook,
                        onClick = { onOpenNotebook(notebook.id) },
                        onRename = { renameTarget = notebook },
                        onDelete = { deleteTarget = notebook },
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        NotebookNameDialog(
            dialogTitle = "New notebook",
            confirmLabel = "Create",
            initialTitle = "",
            onConfirm = { name ->
                onCreateNotebook(name)
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false },
        )
    }

    renameTarget?.let { target ->
        NotebookNameDialog(
            dialogTitle = "Rename notebook",
            confirmLabel = "Save",
            initialTitle = target.title,
            onConfirm = { newTitle ->
                onRenameNotebook(target.id, newTitle)
                renameTarget = null
            },
            onDismiss = { renameTarget = null },
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete notebook?") },
            text = { Text("\"${target.title}\" and all its pages will be permanently deleted.") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteNotebook(target.id)
                    deleteTarget = null
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Cancel") } },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotebookCard(
    notebook: Notebook,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    val accent = if (notebook.color != 0L) {
        Color(notebook.color.toInt())
    } else {
        MaterialTheme.colorScheme.primaryContainer
    }
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accent),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = notebook.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "${notebook.pageCount} page(s)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            NotebookCardMenu(onRename = onRename, onDelete = onDelete)
        }
    }
}

@Composable
private fun NotebookCardMenu(onRename: () -> Unit, onDelete: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    IconButton(onClick = { expanded = true }) {
        Icon(Icons.Filled.MoreVert, contentDescription = "Notebook options")
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        DropdownMenuItem(text = { Text("Rename") }, onClick = { expanded = false; onRename() })
        DropdownMenuItem(text = { Text("Delete") }, onClick = { expanded = false; onDelete() })
    }
}

@Composable
private fun NotebookNameDialog(
    dialogTitle: String,
    confirmLabel: String,
    initialTitle: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf(initialTitle) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(dialogTitle) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                label = { Text("Title") },
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text.trim()) }, enabled = text.isNotBlank()) {
                Text(confirmLabel)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun HomeOverflowMenu(
    onOpenSearch: () -> Unit,
    onImport: () -> Unit,
    onOpenModels: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    IconButton(onClick = { expanded = true }) {
        Icon(Icons.Filled.MoreVert, contentDescription = "More")
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        DropdownMenuItem(text = { Text("Search") }, onClick = { expanded = false; onOpenSearch() })
        DropdownMenuItem(text = { Text("Import notebook") }, onClick = { expanded = false; onImport() })
        DropdownMenuItem(text = { Text("Model Manager") }, onClick = { expanded = false; onOpenModels() })
        DropdownMenuItem(text = { Text("Settings") }, onClick = { expanded = false; onOpenSettings() })
    }
}

/** Copies a picked document into app cache so the domain layer can import from a plain file path. */
private fun copyUriToCache(context: Context, uri: Uri): String? = try {
    val dir = File(context.cacheDir, "imports").apply { mkdirs() }
    val destination = File(dir, "import_${System.currentTimeMillis()}.ainb")
    val input = context.contentResolver.openInputStream(uri) ?: return null
    input.use { source -> FileOutputStream(destination).use { output -> source.copyTo(output) } }
    destination.absolutePath
} catch (throwable: Exception) {
    null
}
