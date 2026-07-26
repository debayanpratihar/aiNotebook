package com.debayan.ainotebook.data.time

import com.debayan.ainotebook.core.time.TimeProvider
import javax.inject.Inject

/** Production [TimeProvider] reading the device wall clock. */
class SystemTimeProvider @Inject constructor() : TimeProvider {
    override fun now(): Long = System.currentTimeMillis()
}
