package com.debayan.ainotebook.domain.math

/**
 * Whether the argument of a trigonometric function is read as degrees or radians.
 *
 * Radians are the default because that is what the maths means when no unit is written, but a
 * handwritten `sin 30°` means thirty degrees and answering `-0.988` would be useless. [of] therefore
 * reads the intent off the source text once, before the degree sign is normalized away, and the
 * whole expression is then evaluated in that mode — mixing units inside one expression would give
 * two different answers to `sin(30°) + cos(60)` depending on parse order.
 */
enum class AngleMode {
    RADIANS,
    DEGREES,
    ;

    companion object {
        /** Reads the mode from the raw recognized text, before normalization strips the markers. */
        fun of(rawText: String): AngleMode {
            val text = rawText.lowercase()
            if (RADIAN_MARKERS.any { text.contains(it) }) return RADIANS
            return if (DEGREE_MARKERS.any { text.contains(it) }) DEGREES else RADIANS
        }

        private val DEGREE_MARKERS = listOf("°", "degree", "deg")
        private val RADIAN_MARKERS = listOf("radian", " rad")
    }
}
