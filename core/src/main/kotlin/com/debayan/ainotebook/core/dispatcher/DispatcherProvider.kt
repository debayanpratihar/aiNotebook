package com.debayan.ainotebook.core.dispatcher

import kotlinx.coroutines.CoroutineDispatcher

/**
 * Abstraction over Kotlin coroutine dispatchers.
 *
 * Injecting this instead of referencing [kotlinx.coroutines.Dispatchers] directly keeps the
 * domain and data layers free of hard-coded threading, which makes them deterministically
 * testable (tests can supply a single test dispatcher for all four).
 */
interface DispatcherProvider {
    /** UI / main thread. */
    val main: CoroutineDispatcher

    /** Immediate main-thread dispatch (no re-post when already on main). */
    val mainImmediate: CoroutineDispatcher

    /** Disk and network I/O. */
    val io: CoroutineDispatcher

    /** CPU-bound work (parsing, sorting, geometry). */
    val default: CoroutineDispatcher
}
