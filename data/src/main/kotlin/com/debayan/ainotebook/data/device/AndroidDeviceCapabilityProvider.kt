package com.debayan.ainotebook.data.device

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import com.debayan.ainotebook.core.dispatcher.DispatcherProvider
import com.debayan.ainotebook.domain.model.ai.DeviceCapabilities
import com.debayan.ainotebook.domain.provider.DeviceCapabilityProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** Reads device resources via Android system services. */
class AndroidDeviceCapabilityProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dispatchers: DispatcherProvider,
) : DeviceCapabilityProvider {

    override suspend fun capabilities(): DeviceCapabilities = withContext(dispatchers.io) {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo().also(activityManager::getMemoryInfo)

        DeviceCapabilities(
            totalRamMb = memoryInfo.totalMem / BYTES_PER_MB,
            availableRamMb = memoryInfo.availMem / BYTES_PER_MB,
            freeStorageBytes = context.filesDir.usableSpace,
            supportedAbis = Build.SUPPORTED_ABIS?.toList().orEmpty(),
            sdkInt = Build.VERSION.SDK_INT,
            cpuCores = Runtime.getRuntime().availableProcessors(),
        )
    }

    private companion object {
        const val BYTES_PER_MB = 1024L * 1024L
    }
}
