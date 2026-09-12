package com.debayan.ainotebook.domain.math

/**
 * Shared text helpers for the solver family.
 *
 * Everything here produces linear plain text. Answers and working are written back onto the page by
 * the handwriting renderer, which has one glyph per character and no concept of a fraction bar, so
 * LaTeX or markdown would come out as literal backslashes and asterisks in the middle of someone's
 * notes.
 */
internal object MathText {

    const val APPROX = "≈"

    /** Restores the ASCII the parser needed into something that reads like handwriting. */
    fun prettify(ascii: String): String = ascii
        .replace("*", " × ")
        .replace("+", " + ")
        .replace("=", " = ")
        .replace(Regex("(?<=[\\da-z)])\\s*-"), " - ")
        .replace(Regex("\\s+"), " ")
        .replace(Regex("\\(\\s+"), "(")
        .replace(Regex("\\s+\\)"), ")")
        .trim()

    fun symbol(op: Char): String = when (op) {
        '*' -> "×"
        '/' -> "÷"
        '%' -> "mod"
        else -> op.toString()
    }

    /** Joins alternative answers the way a student writes them. */
    fun joinAlternatives(parts: List<String>): String = parts.joinToString(" or ")

    /**
     * An `≈ 1.414214` line for an exact answer whose decimal a reader still wants, and nothing at
     * all when the exact form already is the decimal.
     */
    fun approximationOf(value: Double, exactText: String): String? {
        val decimal = MathFormat.number(value)
        return if (decimal == exactText) null else "$APPROX $decimal"
    }
}
