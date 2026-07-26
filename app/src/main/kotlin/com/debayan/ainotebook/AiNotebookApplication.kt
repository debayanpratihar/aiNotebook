package com.debayan.ainotebook

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.debayan.ainotebook.data.work.WorkScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Application entry point and Hilt/WorkManager composition root.
 *
 * Implements [Configuration.Provider] so WorkManager is initialized on demand with the
 * [HiltWorkerFactory], enabling constructor injection into `@HiltWorker`s. The automatic
 * WorkManager initializer is disabled in the manifest so this configuration is authoritative.
 */
@HiltAndroidApp
class AiNotebookApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    @Inject lateinit var workScheduler: WorkScheduler

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(if (BuildConfig.DEBUG) Log.DEBUG else Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()
        // Hilt member injection completes during super.onCreate(), so dependencies are ready here.
        workScheduler.scheduleMaintenance()
    }
}
