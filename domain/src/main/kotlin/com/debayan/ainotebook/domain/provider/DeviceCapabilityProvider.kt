package com.debayan.ainotebook.domain.provider

import com.debayan.ainotebook.domain.model.ai.DeviceCapabilities

/** Supplies a snapshot of device resources. Implemented in the data layer over Android APIs. */
interface DeviceCapabilityProvider {
    suspend fun capabilities(): DeviceCapabilities
}
