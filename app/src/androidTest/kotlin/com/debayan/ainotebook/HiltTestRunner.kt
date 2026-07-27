package com.debayan.ainotebook

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication

/**
 * Instrumented-test runner that swaps in [HiltTestApplication] so `@HiltAndroidTest` classes get a
 * Hilt component. Referenced by `testInstrumentationRunner` in the app build script.
 */
class HiltTestRunner : AndroidJUnitRunner() {
    override fun newApplication(cl: ClassLoader?, className: String?, context: Context?): Application =
        super.newApplication(cl, HiltTestApplication::class.java.name, context)
}
