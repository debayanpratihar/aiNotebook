package com.debayan.ainotebook.feature.canvas.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.debayan.ainotebook.common.component.LoadingIndicator
import com.debayan.ainotebook.domain.model.canvas.StrokePoint
import com.debayan.ainotebook.domain.model.canvas.ToolType
import com.debayan.ainotebook.feature.canvas.engine.CanvasToolMode
import com.debayan.ainotebook.feature.canvas.presentation.NotebookCanvasUiState
import com.debayan.ainotebook.feature.canvas.render.CanvasTemplate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotebookCanvasScreen(
    state: NotebookCanvasUiState,
    onBack: () -> Unit,
    onSelectTool: (ToolType) -> Unit,
    onSelectEraser: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onStrokeCompleted: (List<StrokePoint>) -> Unit,
    onEraseAt: (Float, Float) -> Unit,
    onZoomChanged: (Float) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notebook") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Text(
                        text = "${state.zoomPercent}%",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(end = 4.dp),
                    )
                    TextButton(onClick = onUndo, enabled = state.canUndo) { Text("Undo") }
                    TextButton(onClick = onRedo, enabled = state.canRedo) { Text("Redo") }
                },
            )
        },
        bottomBar = {
            CanvasToolbar(state = state, onSelectTool = onSelectTool, onSelectEraser = onSelectEraser)
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        ) {
            if (state.isLoading) {
                LoadingIndicator()
            } else {
                InfiniteCanvas(
                    strokes = state.strokes,
                    toolMode = state.toolMode,
                    brush = state.brush,
                    template = CanvasTemplate.BLANK,
                    templateColor = MaterialTheme.colorScheme.outlineVariant,
                    onStrokeCompleted = onStrokeCompleted,
                    onEraseAt = onEraseAt,
                    onZoomChanged = onZoomChanged,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CanvasToolbar(
    state: NotebookCanvasUiState,
    onSelectTool: (ToolType) -> Unit,
    onSelectEraser: () -> Unit,
) {
    val writingTools = listOf(
        ToolType.BALL_PEN to "Pen",
        ToolType.PENCIL to "Pencil",
        ToolType.MARKER to "Marker",
        ToolType.HIGHLIGHTER to "Highlighter",
    )
    Surface(tonalElevation = 3.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            writingTools.forEach { (tool, label) ->
                FilterChip(
                    selected = state.toolMode == CanvasToolMode.DRAW && state.brush.tool == tool,
                    onClick = { onSelectTool(tool) },
                    label = { Text(label) },
                )
            }
            FilterChip(
                selected = state.toolMode == CanvasToolMode.STROKE_ERASER,
                onClick = onSelectEraser,
                label = { Text("Eraser") },
            )
        }
    }
}
