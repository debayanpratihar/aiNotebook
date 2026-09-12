package com.debayan.ainotebook.domain.model.recognition

import com.debayan.ainotebook.domain.model.canvas.BoundingBox

/**
 * One alternative reading of a segment, as offered by the recognizer.
 *
 * Keeping the full candidate list rather than only the winner is what lets the math reranker fix
 * the classic handwriting confusions — `x` vs `×`, `l` vs `1`, `O` vs `0`, `S` vs `5` — by asking
 * which alternative actually parses as an expression. [score] is normalized to 0..1, higher is
 * better; recognizers that report no score get an evenly-spaced synthetic one by rank.
 */
data class RecognitionCandidate(
    val text: String,
    val score: Float,
)

/**
 * A recognized word, with everything the correction UI needs: the chosen [text], the [candidates]
 * offered as one-tap alternatives, the [confidence] driving the heatmap, and the [bounds] used to
 * anchor the correction popup over the user's own ink.
 */
data class RecognizedWord(
    val segmentId: String,
    val text: String,
    val candidates: List<RecognitionCandidate>,
    val confidence: Float,
    val bounds: BoundingBox,
    /** True when the math reranker overrode the recognizer's first choice. */
    val correctedByMathModel: Boolean = false,
    /** True when the user hand-edited this word; it is then never re-overridden. */
    val editedByUser: Boolean = false,
) {
    /** Below this, the word is highlighted for review rather than trusted silently. */
    val isLowConfidence: Boolean get() = confidence < LOW_CONFIDENCE_THRESHOLD

    companion object {
        const val LOW_CONFIDENCE_THRESHOLD: Float = 0.62f
    }
}

/** A line of recognized words, in reading order. */
data class RecognizedLine(
    val index: Int,
    val words: List<RecognizedWord>,
    val bounds: BoundingBox,
) {
    val text: String get() = words.joinToString(" ") { it.text }
}

/**
 * The full result of recognizing a page or selection.
 *
 * [plainText] is what gets fed to the solver and the search index. [meanConfidence] and
 * [lowConfidenceWords] drive whether the UI nudges the user to check the reading before solving —
 * answering the wrong question confidently is the worst failure mode this app has.
 */
data class InkRecognitionResult(
    val lines: List<RecognizedLine>,
    val containsMath: Boolean,
    /** Present when the math reranker produced a normalized, parseable expression. */
    val normalizedMathExpression: String? = null,
) {
    val words: List<RecognizedWord> get() = lines.flatMap { it.words }

    val plainText: String get() = lines.joinToString("\n") { it.text }

    val meanConfidence: Float
        get() = words.takeIf { it.isNotEmpty() }?.map { it.confidence }?.average()?.toFloat() ?: 0f

    val lowConfidenceWords: List<RecognizedWord> get() = words.filter { it.isLowConfidence }

    val isEmpty: Boolean get() = words.isEmpty()

    companion object {
        val EMPTY = InkRecognitionResult(lines = emptyList(), containsMath = false)
    }
}
