package com.debayan.ainotebook.domain.usecase.stroke

import com.debayan.ainotebook.core.dispatcher.DispatcherProvider
import com.debayan.ainotebook.core.result.AppError
import com.debayan.ainotebook.core.result.AppResult
import com.debayan.ainotebook.domain.model.canvas.Stroke
import com.debayan.ainotebook.domain.repository.StrokeRepository
import com.debayan.ainotebook.domain.usecase.UseCase
import javax.inject.Inject

/** Persists a completed stroke, rejecting empty strokes. */
class SaveStrokeUseCase @Inject constructor(
    private val strokeRepository: StrokeRepository,
    dispatchers: DispatcherProvider,
) : UseCase<Stroke, Unit>(dispatchers.io) {

    override suspend fun execute(params: Stroke): AppResult<Unit> {
        if (params.points.isEmpty()) {
            return AppResult.Failure(AppError.Validation("Cannot save a stroke with no points"))
        }
        return strokeRepository.saveStroke(params)
    }
}
