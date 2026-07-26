package com.debayan.ainotebook.core.result

/**
 * Domain-friendly, framework-agnostic error type.
 *
 * The data layer translates low-level exceptions (SQL, IO, network) into one of these so that
 * upper layers can reason about failures without depending on platform classes.
 */
sealed class AppError {
    abstract val message: String?
    abstract val cause: Throwable?

    /** A local persistence (Room / SQLite) failure. */
    data class Database(
        override val message: String? = null,
        override val cause: Throwable? = null,
    ) : AppError()

    /** A file-system / storage failure (read, write, missing file, low space). */
    data class Storage(
        override val message: String? = null,
        override val cause: Throwable? = null,
    ) : AppError()

    /** A network failure (only ever for model/config downloads — the app is offline-first). */
    data class Network(
        override val message: String? = null,
        override val cause: Throwable? = null,
    ) : AppError()

    /** Input failed a domain validation rule. */
    data class Validation(
        override val message: String? = null,
        override val cause: Throwable? = null,
    ) : AppError()

    /** A requested entity does not exist. */
    data class NotFound(
        override val message: String? = null,
        override val cause: Throwable? = null,
    ) : AppError()

    /** Any failure that does not map to a more specific case. */
    data class Unknown(
        override val message: String? = null,
        override val cause: Throwable? = null,
    ) : AppError()
}
