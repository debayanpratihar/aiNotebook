package com.debayan.ainotebook.data.ai

import com.debayan.ainotebook.core.dispatcher.DispatcherProvider
import com.debayan.ainotebook.core.result.AppError
import com.debayan.ainotebook.core.result.AppResult
import com.debayan.ainotebook.domain.ai.PromptBuilder
import com.debayan.ainotebook.domain.math.MathSolution
import com.debayan.ainotebook.domain.math.MathSolver
import com.debayan.ainotebook.domain.model.ai.AiGenerationRequest
import com.debayan.ainotebook.domain.model.ai.AiGenerationState
import com.debayan.ainotebook.domain.model.ai.InferenceConfig
import com.debayan.ainotebook.domain.provider.AiEngine
import com.debayan.ainotebook.domain.provider.DeviceCapabilityProvider
import com.debayan.ainotebook.domain.provider.InferenceEngine
import com.debayan.ainotebook.domain.repository.ModelRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The AI Manager: resolves the active model, loads it on demand (with dynamic thread allocation),
 * builds the prompt, and streams generation state. Only one model is loaded at a time.
 */
@Singleton
class AiEngineImpl @Inject constructor(
    private val inferenceEngine: InferenceEngine,
    private val modelRepository: ModelRepository,
    private val promptBuilder: PromptBuilder,
    private val mathSolver: MathSolver,
    private val deviceCapabilityProvider: DeviceCapabilityProvider,
    private val dispatchers: DispatcherProvider,
) : AiEngine {

    override fun generate(request: AiGenerationRequest): Flow<AiGenerationState> = flow {
        // 1) Offline math engine first: instant, correct, and works on every device with no model.
        solveMath(request)?.let { solution ->
            emit(AiGenerationState.Completed(solution.displayText))
            return@flow
        }

        // 2) Otherwise fall back to the on-device language model (word problems / free-form text).
        emit(AiGenerationState.Preparing)

        val model = modelRepository.observeActiveModel().first()
        if (model == null) {
            emit(
                AiGenerationState.Failed(
                    "I couldn't solve that with the built-in math engine. For word problems or " +
                        "free-form questions, download an AI model in Settings → Models.",
                ),
            )
            return@flow
        }

        if (inferenceEngine.loadedModelPath != model.localPath) {
            when (val result = inferenceEngine.loadModel(model.localPath, buildConfig())) {
                is AppResult.Failure ->
                    return@flow emit(AiGenerationState.Failed(result.error.message ?: "Failed to load the model"))
                is AppResult.Success -> Unit
            }
        }
        modelRepository.markUsed(model.id)

        emit(AiGenerationState.Thinking)
        val prompt = promptBuilder.build(request.userInstruction, request.contextText)
        val accumulated = StringBuilder()
        inferenceEngine.generate(prompt, request.params).collect { token ->
            accumulated.append(token)
            emit(AiGenerationState.Writing(accumulated.toString()))
        }
        if (accumulated.isBlank()) {
            // The model loaded but produced nothing (e.g. a native decode error) — don't hang on
            // "Thinking…". Surface a clear message instead.
            emit(AiGenerationState.Failed("The model didn't produce a response. Try a smaller model, or write a calculation the math engine can solve instantly."))
        } else {
            emit(AiGenerationState.Completed(accumulated.toString()))
        }
    }.catch { throwable ->
        if (throwable is CancellationException) throw throwable
        emit(AiGenerationState.Failed(throwable.message ?: "Generation failed"))
    }.flowOn(dispatchers.default)

    override suspend fun preloadActiveModel(): AppResult<Unit> {
        val model = modelRepository.observeActiveModel().first()
            ?: return AppResult.Failure(AppError.NotFound("No active model"))
        if (inferenceEngine.loadedModelPath == model.localPath) return AppResult.Success(Unit)
        return inferenceEngine.loadModel(model.localPath, buildConfig())
    }

    override suspend fun releaseModel() = inferenceEngine.unload()

    /** Tries the offline math engine on the user's instruction, then the page context. */
    private fun solveMath(request: AiGenerationRequest): MathSolution? {
        for (candidate in listOf(request.userInstruction, request.contextText)) {
            if (candidate.isNotBlank()) mathSolver.solve(candidate)?.let { return it }
        }
        return null
    }

    private suspend fun buildConfig(): InferenceConfig {
        val cores = deviceCapabilityProvider.capabilities().cpuCores
        return InferenceConfig(threads = cores.coerceIn(MIN_THREADS, MAX_THREADS))
    }

    private companion object {
        const val MIN_THREADS = 1
        const val MAX_THREADS = 8
    }
}
