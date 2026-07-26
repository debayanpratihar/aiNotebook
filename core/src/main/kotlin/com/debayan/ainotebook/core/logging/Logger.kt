package com.debayan.ainotebook.core.logging

/**
 * Logging abstraction so that pure Kotlin modules can log without depending on `android.util.Log`.
 *
 * The Android-backed implementation is provided in the data layer and bound via Hilt. Per the
 * security spec, implementations must never log notebook content or other sensitive data.
 */
interface Logger {
    fun d(tag: String, message: String)
    fun i(tag: String, message: String)
    fun w(tag: String, message: String, throwable: Throwable? = null)
    fun e(tag: String, message: String, throwable: Throwable? = null)
}
