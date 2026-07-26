package com.debayan.ainotebook.domain.model.ai

/** Model-load configuration for the inference backend. */
data class InferenceConfig(
    val contextLength: Int = 4096,
    val threads: Int = 4,
    /** Layers to offload to GPU when supported; 0 = CPU only. */
    val gpuLayers: Int = 0,
)
