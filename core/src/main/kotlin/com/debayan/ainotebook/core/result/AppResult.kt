package com.debayan.ainotebook.core.result

/**
 * A discriminated result type used across the domain and data layers instead of throwing.
 *
 * Repositories and use cases return [AppResult] so that callers must explicitly handle both the
 * success and failure branches. Failures carry a typed [AppError] rather than a raw exception.
 */
sealed interface AppResult<out T> {

    data class Success<out T>(val data: T) : AppResult<T>

    data class Failure(val error: AppError) : AppResult<Nothing>
}

/** `true` when this result is [AppResult.Success]. */
val AppResult<*>.isSuccess: Boolean
    get() = this is AppResult.Success

/** Returns the wrapped value or `null` when this is a failure. */
fun <T> AppResult<T>.getOrNull(): T? = when (this) {
    is AppResult.Success -> data
    is AppResult.Failure -> null
}

/** Returns the wrapped value or [fallback] when this is a failure. */
fun <T> AppResult<T>.getOrDefault(fallback: T): T = when (this) {
    is AppResult.Success -> data
    is AppResult.Failure -> fallback
}

/** Transforms a success value while preserving a failure unchanged. */
inline fun <T, R> AppResult<T>.map(transform: (T) -> R): AppResult<R> = when (this) {
    is AppResult.Success -> AppResult.Success(transform(data))
    is AppResult.Failure -> this
}

/** Collapses both branches into a single value. */
inline fun <T, R> AppResult<T>.fold(
    onSuccess: (T) -> R,
    onFailure: (AppError) -> R,
): R = when (this) {
    is AppResult.Success -> onSuccess(data)
    is AppResult.Failure -> onFailure(error)
}

/** Runs [action] for its side effect when successful; returns the receiver for chaining. */
inline fun <T> AppResult<T>.onSuccess(action: (T) -> Unit): AppResult<T> {
    if (this is AppResult.Success) action(data)
    return this
}

/** Runs [action] for its side effect when failed; returns the receiver for chaining. */
inline fun <T> AppResult<T>.onFailure(action: (AppError) -> Unit): AppResult<T> {
    if (this is AppResult.Failure) action(error)
    return this
}
