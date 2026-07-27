package com.debayan.ainotebook.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.debayan.ainotebook.core.result.AppResult
import com.debayan.ainotebook.domain.model.Notebook
import com.debayan.ainotebook.domain.repository.NotebookRepository
import com.debayan.ainotebook.domain.usecase.export.ImportNotebookUseCase
import com.debayan.ainotebook.domain.usecase.notebook.CreateNotebookUseCase
import com.debayan.ainotebook.domain.usecase.notebook.DeleteNotebookUseCase
import com.debayan.ainotebook.domain.usecase.notebook.RenameNotebookUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val createNotebookUseCase: CreateNotebookUseCase,
    private val importNotebookUseCase: ImportNotebookUseCase,
    private val renameNotebookUseCase: RenameNotebookUseCase,
    private val deleteNotebookUseCase: DeleteNotebookUseCase,
    notebookRepository: NotebookRepository,
) : ViewModel() {

    val notebooks: StateFlow<List<Notebook>> = notebookRepository.observeNotebooks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    private val _importError = MutableStateFlow<String?>(null)
    val importError: StateFlow<String?> = _importError.asStateFlow()

    /** Creates a new notebook with [title] (with a first page/layer) and invokes [onCreated]. */
    fun createNotebook(title: String, onCreated: (String) -> Unit) {
        viewModelScope.launch {
            val result = createNotebookUseCase(CreateNotebookUseCase.Params(title = title))
            if (result is AppResult.Success) onCreated(result.data)
        }
    }

    /** Imports a validated `.ainb` package from [sourcePath], then opens the new notebook. */
    fun importNotebook(sourcePath: String, onImported: (String) -> Unit) {
        viewModelScope.launch {
            when (val result = importNotebookUseCase(sourcePath)) {
                is AppResult.Success -> onImported(result.data)
                is AppResult.Failure -> _importError.value = "Couldn't import this file. It may be invalid or corrupted."
            }
        }
    }

    fun renameNotebook(id: String, title: String) {
        viewModelScope.launch {
            renameNotebookUseCase(RenameNotebookUseCase.Params(notebookId = id, title = title))
        }
    }

    fun deleteNotebook(id: String) {
        viewModelScope.launch { deleteNotebookUseCase(id) }
    }

    fun consumeImportError() {
        _importError.value = null
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
