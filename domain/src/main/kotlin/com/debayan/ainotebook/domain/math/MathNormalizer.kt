package com.debayan.ainotebook.domain.math

/**
 * Turns raw recognized handwriting into a clean expression string the parser can read.
 *
 * Handwriting OCR is noisy: it emits Unicode operators (× ÷ − √ π), superscript digits (x²), thousands
 * separators, and framing words ("solve", "= ?"). This collapses all of that to plain ASCII math while
 * being conservative — it never rewrites letters that could be variables unless the context is clearly
 * arithmetic (a digit on both sides).
 */
object MathNormalizer {

    private val SUPERSCRIPTS = mapOf(
        '⁰' to '0', '¹' to '1', '²' to '2', '³' to '3', '⁴' to '4',
        '⁵' to '5', '⁶' to '6', '⁷' to '7', '⁸' to '8', '⁹' to '9',
    )

    private val FRAMING_WORDS = listOf(
        "solve for", "solve", "evaluate", "calculate", "compute", "simplify",
        "what is", "whats", "what's", "find the value of", "find", "the value of", "answer",
        "please", "result of", "result",
    )

    fun normalize(raw: String): String {
        if (raw.isBlank()) return ""
        var s = raw.lowercase().trim()

        // Framing words → drop (longest first so "solve for" beats "solve").
        for (word in FRAMING_WORDS.sortedByDescending { it.length }) {
            s = s.replace(word, " ")
        }

        // Superscript digits: x² → x^2, 2³ → 2^3.
        val sup = StringBuilder()
        var i = 0
        while (i < s.length) {
            val c = s[i]
            val digit = SUPERSCRIPTS[c]
            if (digit != null) {
                sup.append('^')
                sup.append(digit)
                // Absorb a run of superscripts into one exponent, e.g. x²³ → x^23.
                var j = i + 1
                while (j < s.length && SUPERSCRIPTS[s[j]] != null) {
                    sup.append(SUPERSCRIPTS[s[j]])
                    j++
                }
                i = j
            } else {
                sup.append(c)
                i++
            }
        }
        s = sup.toString()

        // Unicode operators and symbols → ASCII.
        s = s
            .replace('×', '*') // ×
            .replace('✕', '*') // ✕
            .replace('⋅', '*') // ⋅
            .replace('·', '*') // ·
            .replace('∗', '*') // ∗
            .replace('÷', '/') // ÷
            .replace('⁄', '/') // ⁄
            .replace('−', '-') // − (minus sign)
            .replace('–', '-') // – en dash
            .replace('—', '-') // — em dash
            .replace("√", "sqrt") // √
            .replace("π", "pi") // π
            .replace("∫", " integral ") // ∫ (marker word, handled by the solver)

        // Thousands separators between digits: 1,000 → 1000.
        s = Regex("(?<=\\d),(?=\\d{3}\\b)").replace(s, "")

        // Percent literal: 50% → (50/100). Only when it follows a number.
        s = Regex("(\\d+(?:\\.\\d+)?)\\s*%").replace(s) { "(${it.groupValues[1]}/100)" }

        // "x" as a multiplication sign between two numbers (arithmetic only, no '=' present),
        // e.g. "12 x 3" → "12 * 3", while leaving algebraic "2x + 1" untouched.
        if (!s.contains('=')) {
            s = Regex("(?<=\\d)\\s*[x]\\s*(?=\\d)").replace(s, "*")
        }

        // Drop a trailing "= ?", "=", or "?".
        s = s.replace(Regex("=\\s*\\?\\s*$"), "").trim()
        s = s.trimEnd('?', '=', ':', ' ')

        // Collapse whitespace.
        s = s.replace(Regex("\\s+"), " ").trim()
        return s
    }
}
