package com.debayan.ainotebook.domain.usecase.search

import com.debayan.ainotebook.core.dispatcher.DispatcherProvider
import com.debayan.ainotebook.domain.model.search.SearchResult
import com.debayan.ainotebook.domain.repository.SearchRepository
import com.debayan.ainotebook.domain.usecase.FlowUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

/** Streams search results for a query; a blank query yields no results. */
class SearchNotesUseCase @Inject constructor(
    private val searchRepository: SearchRepository,
    dispatchers: DispatcherProvider,
) : FlowUseCase<String, List<SearchResult>>(dispatchers.io) {

    override fun execute(params: String): Flow<List<SearchResult>> =
        if (params.isBlank()) flowOf(emptyList()) else searchRepository.search(params.trim())
}
