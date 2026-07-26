package com.debayan.ainotebook.domain.model.canvas

/**
 * Stroke smoothing strength. [ADAPTIVE] increases smoothing at low drawing speeds (per the drawing
 * and handwriting specs). Consumed by the drawing engine; stored as a user preference.
 */
enum class SmoothingMode {
    OFF,
    LOW,
    MEDIUM,
    HIGH,
    ADAPTIVE,
}
