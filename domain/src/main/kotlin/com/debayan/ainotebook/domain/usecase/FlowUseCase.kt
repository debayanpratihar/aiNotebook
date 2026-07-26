package com.debayan.ainotebook.domain.usecase

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn

/**
 * Base class for a use case that exposes an ongoing stream of results.
 *
 * Use this for "observe" style operations (e.g. observing the notebook list). The returned [Flow]
 * is moved onto [dispatcher] via [flowOn] so collection never blocks the caller's thread.
 */
abstract class FlowUseCase<in P, R>(
    private val dispatcher: CoroutineDispatcher,
) {
    operator fun invoke(params: P): Flow<R> = execute(params).flowOn(dispatcher)

    protected abstract fun execute(params: P): Flow<R>
}
