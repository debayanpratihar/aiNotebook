package com.debayan.ainotebook.data.util

import com.debayan.ainotebook.core.dispatcher.DispatcherProvider
import com.debayan.ainotebook.core.result.AppError
import com.debayan.ainotebook.core.result.AppResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext

/**
 * Runs a database [block] on the IO dispatcher and wraps the outcome in an [AppResult], translating
 * any thrown exception into [AppError.Database]. Coroutine cancellation is re-thrown so structured
 * concurrency is preserved.
 */
suspend fun <T> DispatcherProvider.runDbCatching(block: suspend () -> T): AppResult<T> =
    withContext(io) {
        try {
            AppResult.Success(block())
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            AppResult.Failure(AppError.Database(throwable.message, throwable))
        }
    }
