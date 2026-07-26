package com.debayan.ainotebook.domain.usecase.stroke

import com.debayan.ainotebook.core.dispatcher.DispatcherProvider
import com.debayan.ainotebook.core.result.AppResult
import com.debayan.ainotebook.domain.repository.StrokeRepository
import com.debayan.ainotebook.domain.usecase.UseCase
import javax.inject.Inject

/** Deletes a single stroke (e.g. via the stroke eraser). */
class DeleteStrokeUseCase @Inject constructor(
    private val strokeRepository: StrokeRepository,
    dispatchers: DispatcherProvider,
) : UseCase<String, Unit>(dispatchers.io) {

    override suspend fun execute(params: String): AppResult<Unit> =
        strokeRepository.deleteStroke(params)
}
