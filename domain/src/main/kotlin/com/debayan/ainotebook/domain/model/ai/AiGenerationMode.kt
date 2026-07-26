package com.debayan.ainotebook.domain.model.ai

/**
 * When AI generation is triggered. [MANUAL] only runs on an explicit Generate action; [AUTOMATIC]
 * additionally runs after the user pauses. Automatic generation can always be disabled.
 */
enum class AiGenerationMode {
    MANUAL,
    AUTOMATIC,
}
