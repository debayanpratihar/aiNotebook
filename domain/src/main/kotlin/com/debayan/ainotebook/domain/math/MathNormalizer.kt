package com.debayan.ainotebook.domain.math

/**
 * Turns raw recognized handwriting into a clean expression string the parser can read.
 *
 * Handwriting recognition is noisy: it emits Unicode operators (× ÷ − √ π), superscript digits (x²),
 * vulgar fractions (½), thousands separators, spelled-out operators ("plus"), and framing words
 * ("solve", "= ?"). This collapses all of that to plain ASCII math while being conservative — it
 * never rewrites letters that could be variables unless the context is clearly arithmetic.
 *
 * Percent signs deliberately survive. `240 + 15%` means 276, not 240.15, so the phrase forms are
 * recognized by [PercentSolver] before [expandPercentLiterals] is allowed to turn a leftover `15%`
 * into a plain `15/100`.
 */
object MathNormalizer {

    private val SUPERSCRIPTS = mapOf(
        '⁰' to '0', '¹' to '1', '²' to '2', '³' to '3', '⁴' to '4',
        '⁵' to '5', '⁶' to '6', '⁷' to '7', '⁸' to '8', '⁹' to '9',
    )

    private val VULGAR_FRACTIONS = mapOf(
        "½" to "1/2", "⅓" to "1/3", "⅔" to "2/3", "¼" to "1/4", "¾" to "3/4",
    )

    private val FRAMING_WORDS = listOf(
        "solve for", "solve", "evaluate", "calculate", "compute", "simplify",
        "what is", "whats", "what's", "find the value of", "find", "the value of", "answer",
        "please", "result of", "result", "the", "in simplest form", "simplest form",
        "in lowest terms", "lowest terms", "work out", "give",
    )

    /** Spelled-out operators. Only fire on word boundaries, so "explain" keeps its "plai" intact. */
    private val WORD_OPERATORS = listOf(
        "square root of" to " sqrt ",
        "square root" to " sqrt ",
        "cube root of" to " cbrt ",
        "cube root" to " cbrt ",
        "to the power of" to "^",
        "to the power" to "^",
        "multiplied by" to "*",
        "divided by" to "/",
        "percentage" to "%",
        "percent" to "%",
        "pct" to "%",
        "squared" to "^2",
        "cubed" to "^3",
        "plus" to "+",
        "minus" to "-",
        "times" to "*",
        "mod" to "%",
    )

    /**
     * Functions the parser knows. Used to repair the recognizer's habit of dropping the brackets:
     * `sqrt9` and `sin 30` both have to become calls, or they tokenize as an unknown variable times
     * a number and the whole solve is abandoned.
     */
    private val FUNCTION_NAMES = listOf(
        "nthroot", "sqrt", "cbrt", "asin", "acos", "atan", "sin", "cos", "tan",
        "sec", "csc", "cot", "log2", "log", "ln", "exp", "abs", "floor", "ceil", "round",
    )

    private val MISSING_BRACKETS = Regex(
        "(?<![a-z_])(${FUNCTION_NAMES.joinToString("|")})\\s*(\\d+(?:\\.\\d+)?|[a-z])(?![a-z0-9.])",
    )

    private val ORDINAL_ROOT = Regex("(\\d+)\\s*(?:st|nd|rd|th)\\s*root\\s*(?:of\\s*)?(\\d+(?:\\.\\d+)?)")

    private val KEYWORDS_WITHOUT_DIGITS = listOf(
        "d/d", "derivative", "differentiate", "integral", "integrate",
        "gcd", "hcf", "lcm", "prime", "factor",
    )

    fun normalize(raw: String): String {
        if (raw.isBlank()) return ""
        var s = raw.lowercase().trim()

        s = expandSuperscripts(s)

        // Spelled-out operators before framing words, so "square root of" is consumed as a phrase
        // and cannot be shredded by the removal of "the" or "find".
        for ((word, replacement) in WORD_OPERATORS) {
            s = Regex("\\b${Regex.escape(word)}\\b").replace(s, replacement)
        }
        s = ORDINAL_ROOT.replace(s) { "nthroot(${it.groupValues[2]}, ${it.groupValues[1]})" }

        // Framing words → drop (longest first so "solve for" beats "solve").
        for (word in FRAMING_WORDS.sortedByDescending { it.length }) {
            s = Regex("\\b${Regex.escape(word)}\\b").replace(s, " ")
        }

        // Unicode operators and symbols → ASCII.
        s = s
            .replace('×', '*')
            .replace('✕', '*')
            .replace('⋅', '*')
            .replace('·', '*')
            .replace('∗', '*')
            .replace('÷', '/')
            .replace('⁄', '/')
            .replace('−', '-')
            .replace('–', '-')
            .replace('—', '-')
            .replace("π", "pi")
            .replace("∫", " integral ")

        // A vulgar fraction after a digit is a mixed number: 2½ is two and a half, not one.
        for ((glyph, fraction) in VULGAR_FRACTIONS) {
            s = Regex("(?<=\\d)\\s*$glyph").replace(s, "+($fraction)")
            s = s.replace(glyph, "($fraction)")
        }

        // Radical signs: √8 → sqrt(8) so the bracket-less form parses; √(x+1) only needs the name.
        s = Regex("√\\s*(\\d+(?:\\.\\d+)?|[a-z])").replace(s) { "sqrt(${it.groupValues[1]})" }
        s = Regex("∛\\s*(\\d+(?:\\.\\d+)?|[a-z])").replace(s) { "cbrt(${it.groupValues[1]})" }
        s = s.replace("√", "sqrt").replace("∛", "cbrt")

        s = MISSING_BRACKETS.replace(s) { "${it.groupValues[1]}(${it.groupValues[2]})" }

        // Thousands separators between digits: 1,000 → 1000. Runs after function repair so a real
        // argument separator in nthroot(32, 5) is never mistaken for one.
        s = Regex("(?<=\\d),(?=\\d{3}\\b)").replace(s, "")

        // "x" as a multiplication sign between two numbers (arithmetic only, no '=' present),
        // e.g. "12 x 3" → "12 * 3", while leaving algebraic "2x + 1" untouched.
        if (!s.contains('=')) {
            s = Regex("(?<=\\d)\\s*[x]\\s*(?=\\d)").replace(s, "*")
        }

        // The degree sign has already told AngleMode what it needed to know.
        s = s.replace("°", "")

        // Drop a trailing "= ?", "=", or "?".
        s = s.replace(Regex("=\\s*\\?\\s*$"), "").trim()
        s = s.trimEnd('?', '=', ':', ' ')

        // Collapse whitespace.
        s = s.replace(Regex("\\s+"), " ").trim()
        return s
    }

    /**
     * Rewrites a leftover percent literal as a hundredth: `50%` → `(50/100)`.
     *
     * Skipped when a number follows, because `17 % 5` is the modulo the normalizer produced from
     * "17 mod 5" and turning that into `(17/100) 5` would answer 0.85 to a remainder question.
     */
    fun expandPercentLiterals(input: String): String =
        Regex("(\\d+(?:\\.\\d+)?)\\s*%(?!\\s*[\\d.(])").replace(input) { "(${it.groupValues[1]}/100)" }

    /**
     * A cheap gate before anything is parsed. Prose with no digits, no equals sign and no maths
     * keyword cannot be a calculation, and trying to parse it wastes time and risks reading
     * "chapter 5" as a sum.
     */
    fun looksLikeMath(normalized: String): Boolean {
        if (normalized.isBlank()) return false
        if (normalized.any { it.isDigit() } || normalized.contains('=')) return true
        return KEYWORDS_WITHOUT_DIGITS.any { normalized.contains(it) }
    }

    private fun expandSuperscripts(input: String): String {
        val out = StringBuilder()
        var i = 0
        while (i < input.length) {
            val c = input[i]
            val digit = SUPERSCRIPTS[c]
            if (digit != null) {
                out.append('^')
                out.append(digit)
                // Absorb a run of superscripts into one exponent, e.g. x²³ → x^23.
                var j = i + 1
                while (j < input.length && SUPERSCRIPTS[input[j]] != null) {
                    out.append(SUPERSCRIPTS[input[j]])
                    j++
                }
                i = j
            } else {
                out.append(c)
                i++
            }
        }
        return out.toString()
    }
}
