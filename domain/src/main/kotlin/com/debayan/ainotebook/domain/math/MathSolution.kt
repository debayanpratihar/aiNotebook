package com.debayan.ainotebook.domain.math

/** Kind of problem the [MathSolver] recognized and solved. */
enum class MathProblemType { ARITHMETIC, EQUATION, DERIVATIVE, INTEGRAL }

/**
 * A solved math problem produced by [MathSolver].
 *
 * @param type what kind of problem was detected
 * @param normalizedInput the cleaned expression the engine actually parsed (OCR noise removed)
 * @param answer the concise final answer, e.g. "42", "x = 2 or x = 3", "2x + 3"
 * @param steps optional short human-readable working, most-relevant first
 */
data class MathSolution(
    val type: MathProblemType,
    val normalizedInput: String,
    val answer: String,
    val steps: List<String> = emptyList(),
) {
    /** A single terse line suitable for writing back onto the canvas, e.g. "12 × 3 = 36". */
    val canvasLine: String
        get() = when (type) {
            MathProblemType.ARITHMETIC -> "$normalizedInput = $answer"
            MathProblemType.EQUATION -> answer
            MathProblemType.DERIVATIVE -> answer
            MathProblemType.INTEGRAL -> answer
        }

    /** Fuller text for the AI panel: the problem, the answer, and any working. */
    val displayText: String
        get() = buildString {
            when (type) {
                MathProblemType.ARITHMETIC -> append("$normalizedInput = $answer")
                MathProblemType.EQUATION -> {
                    append(normalizedInput)
                    append('\n')
                    append(answer)
                }
                MathProblemType.DERIVATIVE, MathProblemType.INTEGRAL -> append(answer)
            }
            if (steps.isNotEmpty()) {
                append("\n\n")
                append(steps.joinToString("\n"))
            }
        }
}
