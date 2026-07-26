package com.debayan.ainotebook.domain.model.search

/** A search hit: the page (within a notebook) whose recognized text matched the query. */
data class SearchResult(
    val notebookId: String,
    val pageId: String,
    val recognizedText: String,
)
