package com.debayan.ainotebook.data.ai

import com.debayan.ainotebook.core.dispatcher.DispatcherProvider
import com.debayan.ainotebook.core.result.AppError
import com.debayan.ainotebook.core.result.AppResult
import com.debayan.ainotebook.domain.model.ai.AiGenerationParams
import com.debayan.ainotebook.domain.model.ai.InferenceConfig
import com.debayan.ainotebook.domain.provider.InferenceEngine
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [InferenceEngine] backed by llama.cpp via JNI (`libainotebook_llama.so`).
 *
 * The native library is loaded defensively: if it is not present in the build (the C++/NDK layer is
 * an integration step — see `data/src/main/cpp/README.md`), [NATIVE_AVAILABLE] is false and all
 * operations fail gracefully with a clear message instead of crashing the app, so everything else
 * keeps working while inference is unavailable.
 *
 * The `native*` functions are the JNI contract the C++ glue must implement.
 */
@Singleton
class LlamaInferenceEngine @Inject constructor(
    private val dispatchers: DispatcherProvider,
) : InferenceEngine {

    private var nativeHandle: Long = 0L

    @Volatile
    override var loadedModelPath: String? = null
        private set

    override suspend fun loadModel(modelPath: String, config: InferenceConfig): AppResult<Unit> =
        withContext(dispatchers.default) {
            if (!NATIVE_AVAILABLE) {
                return@withContext AppResult.Failure(
                    AppError.Unknown("On-device inference is not available in this build"),
                )
            }
            try {
                if (nativeHandle != 0L) {
                    nativeFree(nativeHandle)
                    nativeHandle = 0L
                    loadedModelPath = null
                }
                val handle = nativeLoadModel(modelPath, config.contextLength, config.threads, config.gpuLayers)
                if (handle == 0L) {
                    AppResult.Failure(AppError.Unknown("Failed to load model at $modelPath"))
                } else {
                    nativeHandle = handle
                    loadedModelPath = modelPath
                    AppResult.Success(Unit)
                }
            } catch (throwable: Throwable) {
                AppResult.Failure(AppError.Unknown(throwable.message, throwable))
            }
        }

    override fun generate(prompt: String, params: AiGenerationParams): Flow<String> = callbackFlow {
        if (!NATIVE_AVAILABLE || nativeHandle == 0L) {
            close(IllegalStateException("No model is loaded"))
            return@callbackFlow
        }
        val stopRequested = AtomicBoolean(false)
        val worker = launch(dispatchers.default) {
            try {
                nativeGenerate(
                    nativeHandle,
                    prompt,
                    params.maxTokens,
                    params.temperature,
                    params.topP,
                    params.topK,
                    params.repeatPenalty,
                    TokenCallback { token ->
                        if (stopRequested.get()) {
                            false
                        } else {
                            trySend(token)
                            true
                        }
                    },
                )
                close()
            } catch (throwable: Throwable) {
                close(throwable)
            }
        }
        awaitClose {
            stopRequested.set(true)
            worker.cancel()
        }
    }

    override suspend fun unload() {
        withContext(dispatchers.default) {
            if (nativeHandle != 0L) {
                nativeFree(nativeHandle)
                nativeHandle = 0L
                loadedModelPath = null
            }
        }
    }

    // --- JNI contract (implemented in data/src/main/cpp/ainotebook_llama.cpp) ---

    private external fun nativeLoadModel(modelPath: String, contextLength: Int, threads: Int, gpuLayers: Int): Long

    private external fun nativeGenerate(
        handle: Long,
        prompt: String,
        maxTokens: Int,
        temperature: Float,
        topP: Float,
        topK: Int,
        repeatPenalty: Float,
        callback: TokenCallback,
    )

    private external fun nativeFree(handle: Long)

    private companion object {
        val NATIVE_AVAILABLE: Boolean = try {
            System.loadLibrary("ainotebook_llama")
            true
        } catch (throwable: Throwable) {
            false
        }
    }
}
