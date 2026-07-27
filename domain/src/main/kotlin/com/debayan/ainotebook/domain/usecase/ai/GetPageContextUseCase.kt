package com.debayan.ainotebook.domain.usecase.ai

import com.debayan.ainotebook.core.dispatcher.DispatcherProvider
import com.debayan.ainotebook.core.result.AppResult
import com.debayan.ainotebook.domain.repository.SearchRepository
import com.debayan.ainotebook.domain.usecase.UseCase
import javax.inject.Inject

/**
 * Returns the recognized handwriting/text for a page, used as AI context so generation reflects what
 * the user actually wrote. Empty when the page has not been OCR-indexed yet.
 */
class GetPageContextUseCase @Inject constructor(
    private val searchRepository: SearchRepository,
    dispatchers: DispatcherProvider,
) : UseCase<String, String>(dispatchers.io) {

    override suspend fun execute(params: String): AppResult<String> =
        AppResult.Success(searchRepository.getPageText(params))
}
