package com.debayan.ainotebook.domain.usecase

import com.debayan.ainotebook.core.result.AppError
import com.debayan.ainotebook.core.result.AppResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext

/**
 * Base class for a suspending use case that produces a single [AppResult].
 *
 * Concrete use cases implement [execute] with pure business logic. This base guarantees the work
 * runs on the supplied [dispatcher] and that any thrown exception is converted into a typed
 * [AppResult.Failure] — while still allowing coroutine cancellation to propagate.
 *
 * @param P input parameter type ([Unit] when none is needed).
 * @param R success payload type.
 */
abstract class UseCase<in P, R>(
    private val dispatcher: CoroutineDispatcher,
) {
    suspend operator fun invoke(params: P): AppResult<R> = withContext(dispatcher) {
        try {
            execute(params)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            AppResult.Failure(AppError.Unknown(throwable.message, throwable))
        }
    }

    protected abstract suspend fun execute(params: P): AppResult<R>
}
