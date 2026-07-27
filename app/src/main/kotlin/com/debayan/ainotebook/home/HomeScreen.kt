package com.debayan.ainotebook.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.debayan.ainotebook.common.component.EmptyState
import com.debayan.ainotebook.domain.model.Notebook

@Composable
fun HomeRoute(
    onOpenNotebook: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenModels: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val notebooks by viewModel.notebooks.collectAsStateWithLifecycle()
    HomeScreen(
        notebooks = notebooks,
        onCreateNotebook = { viewModel.createNotebook(onOpenNotebook) },
        onOpenNotebook = onOpenNotebook,
        onOpenSettings = onOpenSettings,
        onOpenModels = onOpenModels,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    notebooks: List<Notebook>,
    onCreateNotebook: () -> Unit,
    onOpenNotebook: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenModels: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Notebook") },
                actions = { HomeOverflowMenu(onOpenModels = onOpenModels, onOpenSettings = onOpenSettings) },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateNotebook,
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
                    NotebookCard(notebook = notebook, onClick = { onOpenNotebook(notebook.id) })
                }
            }
        }
    }
}

@Composable
private fun HomeOverflowMenu(onOpenModels: () -> Unit, onOpenSettings: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    IconButton(onClick = { expanded = true }) {
        Icon(Icons.Filled.MoreVert, contentDescription = "More")
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        DropdownMenuItem(
            text = { Text("Model Manager") },
            onClick = {
                expanded = false
                onOpenModels()
            },
        )
        DropdownMenuItem(
            text = { Text("Settings") },
            onClick = {
                expanded = false
                onOpenSettings()
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotebookCard(notebook: Notebook, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = notebook.title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = "${notebook.pageCount} page(s)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
