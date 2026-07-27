package com.debayan.ainotebook.domain.math

import kotlin.math.abs
import kotlin.math.round

/** Formats doubles for display: whole numbers as integers, others rounded and trimmed. */
object MathFormat {

    fun number(value: Double): String {
        if (value.isNaN()) return "undefined"
        if (value.isInfinite()) return if (value > 0) "∞" else "-∞"
        val rounded = round(value * SCALE) / SCALE
        if (abs(rounded) < 1e-12) return "0"
        return if (abs(rounded - round(rounded)) < 1e-9 && abs(rounded) < 1e15) {
            rounded.toLong().toString()
        } else {
            rounded.toString().trimEnd('0').trimEnd('.')
        }
    }

    private const val SCALE = 1_000_000.0
}
