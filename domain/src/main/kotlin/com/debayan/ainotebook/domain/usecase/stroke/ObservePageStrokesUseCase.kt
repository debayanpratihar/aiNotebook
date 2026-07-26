package com.debayan.ainotebook.domain.usecase.stroke

import com.debayan.ainotebook.core.dispatcher.DispatcherProvider
import com.debayan.ainotebook.domain.model.canvas.Stroke
import com.debayan.ainotebook.domain.repository.StrokeRepository
import com.debayan.ainotebook.domain.usecase.FlowUseCase
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Streams all strokes on a page (across its layers) for rendering. */
class ObservePageStrokesUseCase @Inject constructor(
    private val strokeRepository: StrokeRepository,
    dispatchers: DispatcherProvider,
) : FlowUseCase<String, List<Stroke>>(dispatchers.io) {

    override fun execute(params: String): Flow<List<Stroke>> =
        strokeRepository.observeStrokes(params)
}
