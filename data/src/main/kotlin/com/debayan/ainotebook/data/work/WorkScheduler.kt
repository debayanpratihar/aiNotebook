package com.debayan.ainotebook.data.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Entry point for enqueuing the app's background work. Kept in the data layer so scheduling policy
 * lives next to the workers it schedules; the app module simply calls [scheduleMaintenance] once at
 * startup.
 *
 * [WorkManager] is resolved lazily inside [scheduleMaintenance] (rather than injected) so that its
 * on-demand initialization — which reads the app's `Configuration.Provider` — happens only after the
 * Application has finished member injection, guaranteeing the `HiltWorkerFactory` is available.
 */
@Singleton
class WorkScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /** Enqueues the recurring maintenance job (idempotent — keeps any existing schedule). */
    fun scheduleMaintenance() {
        val request = PeriodicWorkRequestBuilder<MaintenanceWorker>(
            repeatInterval = MAINTENANCE_INTERVAL_HOURS,
            repeatIntervalTimeUnit = TimeUnit.HOURS,
        ).setConstraints(
            Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build(),
        ).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            MaintenanceWorker.UNIQUE_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    private companion object {
        const val MAINTENANCE_INTERVAL_HOURS = 24L
    }
}
