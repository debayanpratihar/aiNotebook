package com.debayan.ainotebook.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.debayan.ainotebook.core.result.AppResult
import com.debayan.ainotebook.domain.model.Notebook
import com.debayan.ainotebook.domain.repository.NotebookRepository
import com.debayan.ainotebook.domain.usecase.export.ImportNotebookUseCase
import com.debayan.ainotebook.domain.usecase.notebook.CreateNotebookUseCase
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
    notebookRepository: NotebookRepository,
) : ViewModel() {

    val notebooks: StateFlow<List<Notebook>> = notebookRepository.observeNotebooks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    private val _importError = MutableStateFlow<String?>(null)
    val importError: StateFlow<String?> = _importError.asStateFlow()

    /** Creates a new notebook (with a first page/layer) and invokes [onCreated] with its id. */
    fun createNotebook(onCreated: (String) -> Unit) {
        viewModelScope.launch {
            val result = createNotebookUseCase(CreateNotebookUseCase.Params(title = "Untitled Notebook"))
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

    fun consumeImportError() {
        _importError.value = null
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
