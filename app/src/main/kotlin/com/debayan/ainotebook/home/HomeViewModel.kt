package com.debayan.ainotebook.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.debayan.ainotebook.core.result.AppResult
import com.debayan.ainotebook.domain.model.Notebook
import com.debayan.ainotebook.domain.repository.NotebookRepository
import com.debayan.ainotebook.domain.usecase.notebook.CreateNotebookUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val createNotebookUseCase: CreateNotebookUseCase,
    notebookRepository: NotebookRepository,
) : ViewModel() {

    val notebooks: StateFlow<List<Notebook>> = notebookRepository.observeNotebooks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    /** Creates a new notebook (with a first page/layer) and invokes [onCreated] with its id. */
    fun createNotebook(onCreated: (String) -> Unit) {
        viewModelScope.launch {
            val result = createNotebookUseCase(
                CreateNotebookUseCase.Params(title = "Untitled Notebook"),
            )
            if (result is AppResult.Success) onCreated(result.data)
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
