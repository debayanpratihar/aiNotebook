package com.debayan.ainotebook.domain.math

import kotlin.math.abs
import kotlin.math.round

/** Formats numbers for display: whole numbers as integers, others rounded and trimmed. */
object MathFormat {

    /** Rounded to six decimals, which is where handwritten working stops caring. */
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

    /**
     * A rational as a fraction: `5`, `-3/4`. Falls back to a decimal for the unreadable denominators
     * that come out of converting a double, so `pi * 2` never prints as a ratio of 18-digit numbers.
     */
    fun rational(value: Rational): String = when {
        value.isInteger -> value.numerator.toString()
        value.isSimpleFraction -> "${value.numerator}/${value.denominator}"
        else -> number(value.toDouble())
    }

    /**
     * Fraction first. Used where the fraction *is* the answer the user asked for — they wrote
     * `1/3 + 1/6`, so `1/2` is the answer and `0.5` is a footnote.
     */
    fun preferFraction(value: Rational): String = when {
        value.isInteger -> value.numerator.toString()
        value.isSimpleFraction -> "${value.numerator}/${value.denominator}"
        else -> value.terminatingDecimalOrNull() ?: number(value.toDouble())
    }

    /**
     * Decimal first, but only when the decimal is exact. Used where the user wrote decimals or a
     * percentage, so `0.1 + 0.2` answers `0.3` rather than `3/10`.
     */
    fun preferDecimal(value: Rational): String = when {
        value.isInteger -> value.numerator.toString()
        else -> value.terminatingDecimalOrNull()
            ?: if (value.isSimpleFraction) "${value.numerator}/${value.denominator}" else number(value.toDouble())
    }

    /** The exact decimal expansion, or null when the fraction does not terminate. */
    fun exactDecimal(value: Rational): String? = value.terminatingDecimalOrNull()

    private const val SCALE = 1_000_000.0
}
