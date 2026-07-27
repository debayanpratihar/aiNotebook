package com.debayan.ainotebook.feature.canvas.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.debayan.ainotebook.common.component.LoadingIndicator
import com.debayan.ainotebook.domain.model.ai.AiGenerationState
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
    onToggleAiPanel: () -> Unit,
    onGenerateAi: (String) -> Unit,
    onStopAi: () -> Unit,
    onExport: () -> Unit,
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
                    TextButton(onClick = onExport) { Text("Export") }
                },
            )
        },
        bottomBar = {
            CanvasToolbar(
                state = state,
                onSelectTool = onSelectTool,
                onSelectEraser = onSelectEraser,
                onToggleAiPanel = onToggleAiPanel,
            )
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

            if (state.aiPanelVisible) {
                AiPanel(
                    aiState = state.aiState,
                    onGenerate = onGenerateAi,
                    onStop = onStopAi,
                    onClose = onToggleAiPanel,
                    modifier = Modifier.align(Alignment.BottomCenter),
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
    onToggleAiPanel: () -> Unit,
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
            FilterChip(
                selected = state.aiPanelVisible,
                onClick = onToggleAiPanel,
                label = { Text("AI") },
            )
        }
    }
}

@Composable
private fun AiPanel(
    aiState: AiGenerationState,
    onGenerate: (String) -> Unit,
    onStop: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var prompt by remember { mutableStateOf("") }
    val busy = aiState is AiGenerationState.Preparing ||
        aiState is AiGenerationState.Thinking ||
        aiState is AiGenerationState.Writing

    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 4.dp,
        shadowElevation = 8.dp,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "AI assistant",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onClose) {
                    Icon(Icons.Filled.Close, contentDescription = "Close AI panel")
                }
            }
            OutlinedTextField(
                value = prompt,
                onValueChange = { prompt = it },
                label = { Text("Ask or instruct the AI…") },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onGenerate(prompt) }, enabled = !busy) { Text("Generate") }
                OutlinedButton(onClick = onStop, enabled = busy) { Text("Stop") }
            }
            AiStatus(aiState)
        }
    }
}

@Composable
private fun AiStatus(aiState: AiGenerationState) {
    when (aiState) {
        AiGenerationState.Idle -> Unit
        AiGenerationState.Preparing -> StatusText("Preparing model…")
        AiGenerationState.Thinking -> StatusText("Thinking…")
        is AiGenerationState.Writing -> ResponseText(aiState.text)
        is AiGenerationState.Completed -> ResponseText(aiState.text)
        is AiGenerationState.Failed -> Text(
            text = aiState.message,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun StatusText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun ResponseText(text: String) {
    if (text.isBlank()) return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 220.dp)
            .verticalScroll(rememberScrollState())
            .padding(top = 8.dp),
    ) {
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }
}
