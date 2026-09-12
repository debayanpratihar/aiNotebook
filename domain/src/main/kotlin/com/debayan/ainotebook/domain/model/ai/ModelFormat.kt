package com.debayan.ainotebook.domain.model.ai

/**
 * On-disk format of a downloadable local model, which determines which backend can run it.
 *
 * The catalog advertises both because they trade off differently: `.task` bundles run on the GPU
 * through a prebuilt library and need no native toolchain, while GGUF opens the entire Hugging Face
 * quantized ecosystem but only works once the NDK layer has been built.
 */
enum class ModelFormat {
    /** MediaPipe LLM Inference bundle (`.task`). Runs on [InferenceBackend.MEDIAPIPE]. */
    MEDIAPIPE_TASK,

    /** llama.cpp quantized model (`.gguf`). Runs on [InferenceBackend.LLAMA_CPP]. */
    GGUF,
    ;

    val fileExtension: String
        get() = when (this) {
            MEDIAPIPE_TASK -> "task"
            GGUF -> "gguf"
        }

    val backend: InferenceBackend
        get() = when (this) {
            MEDIAPIPE_TASK -> InferenceBackend.MEDIAPIPE
            GGUF -> InferenceBackend.LLAMA_CPP
        }

    companion object {
        /** Infers the format from a file name, defaulting to GGUF for unrecognized extensions. */
        fun fromFileName(fileName: String): ModelFormat =
            if (fileName.endsWith(".task", ignoreCase = true)) MEDIAPIPE_TASK else GGUF
    }
}

/** Which local engine executes generation. */
enum class InferenceBackend {
    /**
     * MediaPipe LLM Inference. GPU-accelerated, ships as a prebuilt library, and is the default:
     * it needs no C++ build and is several times faster than an unoptimized CPU decode on the
     * low-end hardware this app targets.
     */
    MEDIAPIPE,

    /** llama.cpp via JNI. Requires the NDK build; unlocks arbitrary GGUF models. */
    LLAMA_CPP,
    ;

    val displayName: String
        get() = when (this) {
            MEDIAPIPE -> "MediaPipe (GPU)"
            LLAMA_CPP -> "llama.cpp (GGUF)"
        }
}
