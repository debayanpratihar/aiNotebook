package com.debayan.ainotebook.feature.export

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.debayan.ainotebook.core.result.AppResult
import com.debayan.ainotebook.domain.model.export.ExportFormat
import com.debayan.ainotebook.domain.model.export.ExportedFile
import com.debayan.ainotebook.domain.usecase.export.ExportNotebookUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExportUiState(
    val isExporting: Boolean = false,
    val lastFormat: ExportFormat? = null,
    val exportedFile: ExportedFile? = null,
    val errorMessage: String? = null,
)

@HiltViewModel
class ExportViewModel @Inject constructor(
    private val exportNotebook: ExportNotebookUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val notebookId: String = requireNotNull(
        savedStateHandle.get<String>(ExportDestination.ARG_NOTEBOOK_ID),
    ) { "notebookId navigation argument is required" }

    private val _uiState = MutableStateFlow(ExportUiState())
    val uiState: StateFlow<ExportUiState> = _uiState.asStateFlow()

    fun export(format: ExportFormat) {
        _uiState.update { it.copy(isExporting = true, lastFormat = format, exportedFile = null, errorMessage = null) }
        viewModelScope.launch {
            when (val result = exportNotebook(ExportNotebookUseCase.Params(notebookId, format))) {
                is AppResult.Success ->
                    _uiState.update { it.copy(isExporting = false, exportedFile = result.data) }

                is AppResult.Failure ->
                    _uiState.update { it.copy(isExporting = false, errorMessage = "Export failed. Please try again.") }
            }
        }
    }
}
