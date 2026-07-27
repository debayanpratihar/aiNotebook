package com.debayan.ainotebook.feature.canvas.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.debayan.ainotebook.core.result.AppResult
import com.debayan.ainotebook.core.result.getOrDefault
import com.debayan.ainotebook.core.time.TimeProvider
import com.debayan.ainotebook.domain.model.ai.AiGenerationRequest
import com.debayan.ainotebook.domain.model.ai.AiGenerationState
import com.debayan.ainotebook.domain.model.canvas.BoundingBox
import com.debayan.ainotebook.domain.model.canvas.Stroke
import com.debayan.ainotebook.domain.model.canvas.StrokePoint
import com.debayan.ainotebook.domain.model.canvas.ToolType
import com.debayan.ainotebook.domain.repository.SettingsRepository
import com.debayan.ainotebook.domain.usecase.ai.GenerateAiResponseUseCase
import com.debayan.ainotebook.domain.usecase.ai.GetPageContextUseCase
import com.debayan.ainotebook.domain.usecase.notebook.ObserveNotebookUseCase
import com.debayan.ainotebook.domain.usecase.notebook.OpenNotebookUseCase
import com.debayan.ainotebook.domain.usecase.notebook.RenameNotebookUseCase
import com.debayan.ainotebook.domain.usecase.ocr.RequestPageOcrUseCase
import com.debayan.ainotebook.domain.usecase.stroke.DeleteStrokeUseCase
import com.debayan.ainotebook.domain.usecase.stroke.ObservePageStrokesUseCase
import com.debayan.ainotebook.domain.usecase.stroke.SaveStrokeUseCase
import com.debayan.ainotebook.feature.canvas.engine.BrushSettings
import com.debayan.ainotebook.feature.canvas.engine.CanvasToolMode
import com.debayan.ainotebook.feature.canvas.navigation.NotebookCanvasDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * Drives the notebook canvas: opens the notebook, streams its strokes, commits new strokes (that is,
 * autosaves immediately on completion — the "autosave while drawing" behavior), handles stroke
 * erasing, and maintains a persistent command-based undo/redo history.
 */
@HiltViewModel
class NotebookCanvasViewModel @Inject constructor(
    private val openNotebook: OpenNotebookUseCase,
    private val observePageStrokes: ObservePageStrokesUseCase,
    private val saveStroke: SaveStrokeUseCase,
    private val deleteStroke: DeleteStrokeUseCase,
    private val generateAiResponse: GenerateAiResponseUseCase,
    private val getPageContext: GetPageContextUseCase,
    private val requestPageOcr: RequestPageOcrUseCase,
    private val observeNotebook: ObserveNotebookUseCase,
    private val renameNotebookUseCase: RenameNotebookUseCase,
    private val settingsRepository: SettingsRepository,
    private val timeProvider: TimeProvider,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val notebookId: String = requireNotNull(
        savedStateHandle.get<String>(NotebookCanvasDestination.ARG_NOTEBOOK_ID),
    ) { "notebookId navigation argument is required" }

    private val _uiState = MutableStateFlow(NotebookCanvasUiState())
    val uiState: StateFlow<NotebookCanvasUiState> = _uiState.asStateFlow()

    private var activeLayerId: String? = null
    private var pageId: String? = null
    private var strokesJob: Job? = null
    private var aiJob: Job? = null
    private var autoAiJob: Job? = null

    private var aiEnabledPref = true
    private var autoAiEnabled = false
    private var inactivityMs = 3_000L

    private val undoStack = ArrayDeque<CanvasCommand>()
    private val redoStack = ArrayDeque<CanvasCommand>()

    /** Ids deleted but not yet gone from the observed list, so a fast erase drag can't double-delete. */
    private val pendingErased = mutableSetOf<String>()

    /** Current drawing ink for pen-like tools, and whether the user has chosen it explicitly. */
    private var inkColor: Long = BrushSettings.DEFAULT_INK
    private var hasCustomColor: Boolean = false

    init {
        loadNotebook()
        observeNotebookTitle()
        observeAiSettings()
    }

    private fun observeAiSettings() {
        settingsRepository.userPreferences
            .onEach { prefs ->
                aiEnabledPref = prefs.aiEnabled
                autoAiEnabled = prefs.automaticAiGeneration
                inactivityMs = prefs.aiInactivityTimeoutSeconds.coerceAtLeast(1) * 1000L
            }
            .launchIn(viewModelScope)
    }

    private fun observeNotebookTitle() {
        observeNotebook(notebookId)
            .onEach { notebook ->
                _uiState.update { it.copy(notebookTitle = notebook?.title ?: "Notebook") }
            }
            .launchIn(viewModelScope)
    }

    fun renameNotebook(title: String) {
        viewModelScope.launch {
            renameNotebookUseCase(RenameNotebookUseCase.Params(notebookId = notebookId, title = title))
        }
    }

    private fun loadNotebook() {
        viewModelScope.launch {
            when (val result = openNotebook(notebookId)) {
                is AppResult.Success -> {
                    activeLayerId = result.data.activeLayerId
                    pageId = result.data.page.id
                    _uiState.update { it.copy(isLoading = false, page = result.data.page) }
                    observeStrokes(result.data.page.id)
                }

                is AppResult.Failure ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Unable to open notebook.") }
            }
        }
    }

    private fun observeStrokes(pageId: String) {
        strokesJob?.cancel()
        strokesJob = observePageStrokes(pageId)
            .onEach { strokes ->
                pendingErased.retainAll(strokes.mapTo(HashSet()) { it.id })
                _uiState.update { it.copy(strokes = strokes) }
            }
            .launchIn(viewModelScope)
    }

    fun onStrokeCompleted(points: List<StrokePoint>) {
        val layerId = activeLayerId ?: return
        if (points.isEmpty()) return
        val brush = _uiState.value.brush
        val stroke = Stroke(
            id = UUID.randomUUID().toString(),
            layerId = layerId,
            tool = brush.tool,
            color = brush.color,
            width = brush.width,
            opacity = brush.effectiveOpacity,
            points = points,
            boundingBox = BoundingBox.fromPoints(points),
            createdAt = timeProvider.now(),
        )
        viewModelScope.launch {
            when (saveStroke(stroke)) {
                is AppResult.Success -> {
                    undoStack.addLast(CanvasCommand.AddStroke(stroke))
                    redoStack.clear()
                    refreshHistory()
                    requestOcr()
                    scheduleAutoAi()
                }

                is AppResult.Failure ->
                    _uiState.update { it.copy(errorMessage = "Couldn't save your stroke.") }
            }
        }
    }

    fun onEraseAt(x: Float, y: Float) {
        val hit = _uiState.value.strokes
            .lastOrNull { it.id !in pendingErased && it.hitTest(x, y, ERASE_RADIUS) } ?: return
        pendingErased.add(hit.id)
        viewModelScope.launch {
            when (deleteStroke(hit.id)) {
                is AppResult.Success -> {
                    undoStack.addLast(CanvasCommand.RemoveStroke(hit))
                    redoStack.clear()
                    refreshHistory()
                    requestOcr()
                }

                is AppResult.Failure -> {
                    pendingErased.remove(hit.id)
                    _uiState.update { it.copy(errorMessage = "Couldn't erase.") }
                }
            }
        }
    }

    fun undo() {
        val command = undoStack.removeLastOrNull() ?: return
        viewModelScope.launch {
            val applied = when (command) {
                is CanvasCommand.AddStroke -> deleteStroke(command.stroke.id) is AppResult.Success
                is CanvasCommand.RemoveStroke -> saveStroke(command.stroke) is AppResult.Success
            }
            if (applied) {
                redoStack.addLast(command)
                requestOcr()
            } else {
                undoStack.addLast(command)
            }
            refreshHistory()
        }
    }

    fun redo() {
        val command = redoStack.removeLastOrNull() ?: return
        viewModelScope.launch {
            val applied = when (command) {
                is CanvasCommand.AddStroke -> saveStroke(command.stroke) is AppResult.Success
                is CanvasCommand.RemoveStroke -> deleteStroke(command.stroke.id) is AppResult.Success
            }
            if (applied) {
                undoStack.addLast(command)
                requestOcr()
            } else {
                redoStack.addLast(command)
            }
            refreshHistory()
        }
    }

    /** Schedules background OCR indexing for the current page when enabled in settings. */
    private fun requestOcr() {
        val page = pageId ?: return
        viewModelScope.launch {
            val prefs = settingsRepository.userPreferences.first()
            if (prefs.ocrEnabled && prefs.automaticIndexing) {
                requestPageOcr(RequestPageOcrUseCase.Params(notebookId = notebookId, pageId = page))
            }
        }
    }

    fun selectTool(tool: ToolType) =
        _uiState.update { it.copy(toolMode = CanvasToolMode.DRAW, brush = brushFor(tool)) }

    fun selectEraser() = _uiState.update { it.copy(toolMode = CanvasToolMode.STROKE_ERASER) }

    /** Sets an explicit drawing color (from a swatch or the custom picker). */
    fun selectColor(color: Long) {
        hasCustomColor = true
        inkColor = color
        _uiState.update { it.copy(toolMode = CanvasToolMode.DRAW, brush = it.brush.copy(color = color)) }
    }

    /**
     * Applies a theme-appropriate default ink (white in dark mode, near-black in light) unless the
     * user has already picked a color, so strokes stay visible against the canvas background.
     */
    fun applyThemeInk(isDark: Boolean) {
        if (hasCustomColor) return
        inkColor = if (isDark) WHITE_INK else BrushSettings.DEFAULT_INK
        _uiState.update { current ->
            if (current.brush.tool == ToolType.HIGHLIGHTER) {
                current
            } else {
                current.copy(brush = current.brush.copy(color = inkColor))
            }
        }
    }

    fun onZoomChanged(scale: Float) =
        _uiState.update { it.copy(zoomPercent = (scale * 100f).toInt()) }

    fun toggleAiPanel() = _uiState.update { it.copy(aiPanelVisible = !it.aiPanelVisible) }

    /**
     * Runs an AI generation using the page's recognized handwriting as context, streaming state into
     * [NotebookCanvasUiState.aiState]. Cancel via [stopAi] to interrupt.
     */
    fun generateAi(instruction: String) {
        aiJob?.cancel()
        aiJob = viewModelScope.launch {
            val context = pageId?.let { getPageContext(it).getOrDefault("") }.orEmpty()
            generateAiResponse(AiGenerationRequest(userInstruction = instruction, contextText = context))
                .collect { state -> _uiState.update { it.copy(aiState = state) } }
        }
    }

    /** After the user pauses (per settings), auto-generates from the handwriting — real-time assist. */
    private fun scheduleAutoAi() {
        if (!aiEnabledPref || !autoAiEnabled) return
        autoAiJob?.cancel()
        autoAiJob = viewModelScope.launch {
            delay(inactivityMs)
            _uiState.update { it.copy(aiPanelVisible = true) }
            generateAi("")
        }
    }

    /** Stops generation, keeping any text produced so far. */
    fun stopAi() {
        aiJob?.cancel()
        aiJob = null
        _uiState.update { current ->
            val partial = (current.aiState as? AiGenerationState.Writing)?.text
            current.copy(aiState = partial?.let { AiGenerationState.Completed(it) } ?: AiGenerationState.Idle)
        }
    }

    fun consumeError() = _uiState.update { it.copy(errorMessage = null) }

    private fun refreshHistory() =
        _uiState.update { it.copy(canUndo = undoStack.isNotEmpty(), canRedo = redoStack.isNotEmpty()) }

    private fun brushFor(tool: ToolType): BrushSettings = when (tool) {
        ToolType.HIGHLIGHTER -> BrushSettings(tool = tool, color = HIGHLIGHTER_COLOR, width = 18f, opacity = 0.4f)
        ToolType.MARKER -> BrushSettings(tool = tool, color = inkColor, width = 8f)
        ToolType.PENCIL -> BrushSettings(tool = tool, color = inkColor, width = 2.5f)
        else -> BrushSettings(tool = ToolType.BALL_PEN, color = inkColor, width = 3f)
    }

    override fun onCleared() {
        strokesJob?.cancel()
        aiJob?.cancel()
        autoAiJob?.cancel()
        super.onCleared()
    }

    private companion object {
        const val ERASE_RADIUS = 12f
        const val HIGHLIGHTER_COLOR = 0xFFFFF176
        const val WHITE_INK = 0xFFFFFFFF
    }
}

/** True if [x],[y] (world space) falls within [radius] of any point of this stroke. */
private fun Stroke.hitTest(x: Float, y: Float, radius: Float): Boolean {
    val box = boundingBox
    if (x < box.left - radius || x > box.right + radius ||
        y < box.top - radius || y > box.bottom + radius
    ) {
        return false
    }
    val radiusSq = radius * radius
    return points.any { point ->
        val dx = point.x - x
        val dy = point.y - y
        dx * dx + dy * dy <= radiusSq
    }
}
