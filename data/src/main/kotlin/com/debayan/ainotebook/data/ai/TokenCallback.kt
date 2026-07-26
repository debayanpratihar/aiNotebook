package com.debayan.ainotebook.data.ai

/**
 * Invoked by the native decode loop once per generated token. Returning false asks the native side
 * to stop generating (used to implement interruption). Kept as a SAM interface so JNI can call it.
 */
fun interface TokenCallback {
    fun onToken(token: String): Boolean
}
