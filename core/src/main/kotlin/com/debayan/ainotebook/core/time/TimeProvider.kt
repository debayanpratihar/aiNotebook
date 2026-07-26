package com.debayan.ainotebook.core.time

/**
 * Abstraction over "the current time" so components that stamp timestamps stay deterministically
 * testable (a test supplies a fixed clock instead of reading the wall clock).
 */
interface TimeProvider {
    /** Current time in epoch milliseconds. */
    fun now(): Long
}
