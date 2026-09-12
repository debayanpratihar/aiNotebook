package com.debayan.ainotebook.domain.model.recognition

import com.debayan.ainotebook.domain.model.canvas.BoundingBox
import com.debayan.ainotebook.domain.model.canvas.Stroke

/**
 * A spatially coherent group of strokes handed to the recognizer as one unit.
 *
 * Segmenting before recognizing is the single largest accuracy lever available. A recognizer given a
 * whole page of ink at once has to guess at line breaks, word boundaries, and baseline, and it
 * guesses badly; given one word's strokes plus the box they occupy, it has a well-posed problem.
 * Segments also make correction tractable — the user taps one wrong word, not a wall of text.
 */
data class InkSegment(
    val id: String,
    /** Reading order within the page (top-to-bottom, then left-to-right). */
    val index: Int,
    /** Index of the text line this segment belongs to. */
    val lineIndex: Int,
    val strokes: List<Stroke>,
    val bounds: BoundingBox,
) {
    val strokeIds: List<String> get() = strokes.map { it.id }

    /**
     * Estimated x-height of the segment, used to normalize the writing-area hint. Falls back to the
     * full height for single-stroke segments where no better estimate exists.
     */
    val estimatedHeight: Float get() = bounds.height
}

/** How the page was broken into [InkSegment]s. */
enum class SegmentationGranularity {
    /** One segment per detected word. Best accuracy and best correction UX. */
    WORD,

    /** One segment per detected line. Used when word gaps are ambiguous (e.g. dense math). */
    LINE,

    /** The whole selection as one segment. Used for short, deliberate selections. */
    WHOLE,
}
