package com.debayan.ainotebook.domain.model.ai

/**
 * Model-load configuration for a local inference backend.
 *
 * Every field here is a latency or memory lever, and the defaults are deliberately far more
 * conservative than a desktop backend's. The prior 4096-token context with a fixed 4 threads was
 * the main reason on-device answers felt broken on the hardware this app targets: the KV cache
 * alone dominated the memory budget of a 4 GB phone, and the thread count neither matched the big
 * cores nor left one free for the UI.
 */
data class InferenceConfig(
    /**
     * Context window in tokens.
     *
     * KV-cache memory grows linearly with this, and prefill cost grows with the prompt that fills
     * it. A notebook page's worth of context plus an answer fits comfortably in 1024, so that is the
     * floor-tier default rather than a number inherited from server deployments.
     */
    val contextLength: Int = 1024,

    /**
     * Decode threads.
     *
     * Should match the count of **big** cores, not total cores. Oversubscribing a
     * big.LITTLE phone is actively slower than using half the cores: the little cores finish their
     * share late and every other thread waits at the barrier, and the extra contention steals the
     * cycles the UI thread needs to stay at 60 fps.
     */
    val threads: Int = 4,

    /**
     * Prompt-processing batch size.
     *
     * Prefill is compute-bound and batches well, so this is the knob that decides how long the user
     * waits before the first token. It is capped rather than maximized because the scratch buffer
     * scales with it, and a 2048-token batch allocates more than a low-end device can spare.
     */
    val batchSize: Int = 256,

    /** Physical micro-batch; bounds peak scratch memory during prefill. */
    val microBatchSize: Int = 128,

    /** Layers to offload to GPU where the backend supports it; 0 = CPU only. */
    val gpuLayers: Int = 0,

    /**
     * Store the KV cache quantized to 8 bits instead of fp16.
     *
     * Halves KV memory for a negligible quality change on the short contexts used here, which is
     * what makes a longer conversation possible at all on a 4 GB device.
     */
    val quantizeKvCache: Boolean = true,

    /**
     * Use a fused attention kernel where available. Reduces both attention memory traffic and the
     * scratch allocation; required for KV quantization to pay off.
     */
    val flashAttention: Boolean = true,

    /**
     * Memory-map the weights instead of reading them into the heap.
     *
     * Essential on low-RAM devices: the pages stay file-backed and evictable, so loading a 600 MB
     * model does not need 600 MB of anonymous memory and the process is far less likely to be killed
     * while backgrounded.
     */
    val useMemoryMap: Boolean = true,

    /**
     * Reuse the KV cache across turns when the new prompt shares a prefix with the last one.
     *
     * The largest single latency win available. Without it every follow-up re-prefills the entire
     * system prompt and page context from scratch, which on a phone costs more than generating the
     * answer.
     */
    val reusePromptCache: Boolean = true,
) {
    companion object {
        /**
         * Tuned for ~4 GB devices: the smallest context that still fits a page of notes, a thread
         * count that leaves the UI responsive, and a batch small enough that prefill scratch memory
         * stays under control.
         */
        val LOW_MEMORY = InferenceConfig(
            contextLength = 768,
            threads = 2,
            batchSize = 128,
            microBatchSize = 64,
            quantizeKvCache = true,
            flashAttention = true,
        )

        /** Tuned for 6 GB+ devices, where a longer context is affordable. */
        val STANDARD = InferenceConfig(
            contextLength = 2048,
            threads = 4,
            batchSize = 256,
            microBatchSize = 128,
        )

        /**
         * Picks a profile from a device snapshot and scales threads to the big-core count.
         *
         * [bigCoreCount] is passed separately because [DeviceCapabilities.cpuCores] reports every
         * core, including the little ones this must not schedule onto.
         */
        fun forDevice(capabilities: DeviceCapabilities, bigCoreCount: Int): InferenceConfig {
            val base = if (capabilities.totalRamMb < LOW_MEMORY_RAM_MB) LOW_MEMORY else STANDARD
            val threads = bigCoreCount.coerceIn(MIN_THREADS, MAX_THREADS)
            return base.copy(threads = threads)
        }

        /** Below this the device is treated as memory-constrained. */
        const val LOW_MEMORY_RAM_MB: Long = 5_000L

        const val MIN_THREADS: Int = 2
        const val MAX_THREADS: Int = 6
    }
}
