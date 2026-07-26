package com.debayan.ainotebook.data.logging

import android.util.Log
import com.debayan.ainotebook.core.logging.Logger
import javax.inject.Inject

/**
 * [Logger] backed by `android.util.Log`. Callers must never pass notebook content or other
 * sensitive data as a message, per the security spec.
 */
class AndroidLogger @Inject constructor() : Logger {
    override fun d(tag: String, message: String) { Log.d(tag, message) }
    override fun i(tag: String, message: String) { Log.i(tag, message) }
    override fun w(tag: String, message: String, throwable: Throwable?) { Log.w(tag, message, throwable) }
    override fun e(tag: String, message: String, throwable: Throwable?) { Log.e(tag, message, throwable) }
}
