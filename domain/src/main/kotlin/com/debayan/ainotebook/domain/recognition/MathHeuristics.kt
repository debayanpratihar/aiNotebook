package com.debayan.ainotebook.domain.recognition

import com.debayan.ainotebook.domain.model.ai.ProblemKind

/**
 * Decides whether a reading is math, and what kind, by counting characters.
 *
 * This runs everywhere on the hot path: once before the recognizer sees the ink, to set
 * [com.debayan.ainotebook.domain.model.recognition.RecognitionHint.expectMath], once per candidate
 * inside [CandidateReranker], and again on every recognition update while the user is still writing.
 * A model that decides whether to call a model would cost exactly the latency the offline path exists
 * to remove, and the signals that matter here — digits, operators, equation shape, ordinary words —
 * are all a single scan.
 *
 * The signals are counted on [MathTokenNormalizer.normalize] output rather than on the raw reading,
 * so no rule here has to know about `×`, `÷`, superscripts, or the handwritten `x` that means times.
 * Counting raw text instead makes "12 × 47" look like prose with a variable in it.
 *
 * The prose guard is what makes this safe to trust. "Meeting on 12/03 about the budget" satisfies
 * "has digits and has an operator" without being math at all, so any reading carrying several
 * ordinary words is ruled out unless it is shaped like an equation. Erring that way is deliberate:
 * classifying prose as math sends the user's notes to an arithmetic prompt, while classifying math as
 * prose only means the answer comes from a model instead of instantly.
 */
object MathHeuristics {

    /** Whether the reading is dominated by mathematical notation. */
    fun looksLikeMath(text: String): Boolean {
        val signals = analyze(text)
        return when {
            signals.ascii.isBlank() -> false
            signals.calculus -> true
            signals.pureNumber -> true
            signals.proseWords >= PROSE_WORD_LIMIT && !signals.hasEquals -> false
            signals.hasEquals && (signals.digits > 0 || signals.variables.isNotEmpty()) -> true
            signals.digits > 0 && signals.operators > 0 -> true
            signals.variables.isNotEmpty() && signals.operators > 0 -> true
            else -> false
        }
    }

    /**
     * Which flavour of problem the reading is.
     *
     * Kept consistent with [looksLikeMath] by construction — both read the same [Signals] — because
     * [com.debayan.ainotebook.domain.ai.DefaultComplexityClassifier] requires both to agree before it
     * will try the offline engine, and a disagreement there produces an arithmetic prompt for a page
     * of notes.
     */
    fun classify(text: String): ProblemKind {
        val signals = analyze(text)
        if (signals.ascii.isBlank()) return ProblemKind.PROSE
        if (signals.calculus && (signals.digits > 0 || signals.variables.isNotEmpty())) {
            return ProblemKind.CALCULUS
        }
        if (signals.proseWords >= PROSE_WORD_LIMIT) {
            // Numbers embedded in sentences are a word problem only when something asks for a
            // quantity. Without that cue a date, a page number or a time of day would turn a page of
            // notes into a problem the app then tries to answer.
            return when {
                signals.digits > 0 && (signals.quantityCue || signals.question) -> ProblemKind.WORD_PROBLEM
                signals.question -> ProblemKind.FREEFORM_QUESTION
                else -> ProblemKind.PROSE
            }
        }
        if (signals.variables.isNotEmpty() && (signals.hasEquals || signals.operators > 0)) {
            return ProblemKind.ALGEBRA
        }
        if (signals.digits > 0 && (signals.hasEquals || signals.operators > 0)) {
            return ProblemKind.ARITHMETIC
        }
        if (signals.pureNumber) return ProblemKind.ARITHMETIC
        if (signals.question) return ProblemKind.FREEFORM_QUESTION
        return ProblemKind.PROSE
    }

    private class Signals(
        val ascii: String,
        val digits: Int,
        val operators: Int,
        val hasEquals: Boolean,
        val calculus: Boolean,
        val variables: Set<Char>,
        val proseWords: Int,
        val question: Boolean,
        val quantityCue: Boolean,
        val pureNumber: Boolean,
    )

    private fun analyze(text: String): Signals {
        val ascii = MathTokenNormalizer.normalize(text)
        val lower = ascii.lowercase()
        val runs = MathTokenNormalizer.letterRuns(lower)
        var digits = 0
        var operators = 0
        for (c in ascii) {
            if (c.isDigit()) digits++
            if (c in OPERATOR_CHARS) operators++
        }
        // A function application is an operation; without this, `sqrt(9)` counts as a bare number and
        // never reaches the offline engine that can answer it exactly.
        operators += FUNCTION_CALL.findAll(lower).count()
        return Signals(
            ascii = ascii,
            digits = digits,
            operators = operators,
            hasEquals = ascii.contains('='),
            calculus = isCalculus(ascii, lower),
            variables = runs.filter { it.length == 1 }
                .map { it[0] }
                .filterNot { it in AMBIGUOUS_VARIABLES }
                .toSet(),
            proseWords = runs.count { run ->
                run.length >= MIN_PROSE_WORD &&
                    run !in MathTokenNormalizer.MATH_IDENTIFIERS &&
                    run !in FRAMING_WORDS
            },
            question = isQuestion(lower, runs),
            quantityCue = QUANTITY_CUES.any { lower.contains(it) },
            pureNumber = PURE_NUMBER.matches(ascii),
        )
    }

    /**
     * Word markers are matched on word boundaries, not as substrings: "limited to 3 items" would
     * otherwise be a limit and get a calculus prompt.
     */
    private fun isCalculus(ascii: String, lower: String): Boolean =
        CALCULUS_SYMBOLS.any { ascii.indexOf(it) >= 0 } ||
            lower.contains(DERIVATIVE_OPERATOR) ||
            CALCULUS_WORDS.containsMatchIn(lower)

    private fun isQuestion(lower: String, runs: List<String>): Boolean {
        if (lower.trimEnd().endsWith('?')) return true
        val opening = runs.firstOrNull() ?: return false
        return opening in QUESTION_WORDS
    }

    /** Three ordinary words is a sentence; two is a label like "total cost". */
    private const val PROSE_WORD_LIMIT = 3

    /** Shorter runs than this are variables, units or articles, none of which are prose. */
    private const val MIN_PROSE_WORD = 3

    private const val OPERATOR_CHARS = "+-*/^%"

    private const val DERIVATIVE_OPERATOR = "d/d"

    /** Single letters that are more often a misread digit than a variable, per the confusion classes. */
    private const val AMBIGUOUS_VARIABLES = "ilo"

    private val CALCULUS_SYMBOLS = charArrayOf('∫', '∑', 'Σ', '∂')

    private val CALCULUS_WORDS =
        Regex("""\b(lim|limit|integral|integrate|derivative|differentiate)\b""")

    /**
     * Words that frame a problem without being part of it. Excluded from the prose count so that
     * "what is 12 × 47?" is arithmetic rather than a sentence containing a sum.
     */
    private val FRAMING_WORDS: Set<String> = setOf(
        "solve", "solved", "evaluate", "calculate", "compute", "simplify", "factor", "expand",
        "find", "what", "whats", "which", "value", "values", "answer", "please", "result", "equals",
    )

    private val QUESTION_WORDS: Set<String> = setOf(
        "who", "what", "whats", "why", "when", "where", "how", "which", "explain", "describe",
        "define", "summarize", "summarise", "compare", "list",
    )

    /**
     * Cues that a sentence containing numbers is asking for a computation. Multi-word cues are
     * matched as substrings because "how many" is the cue, while "how" alone is just a question.
     */
    private val QUANTITY_CUES: List<String> = listOf(
        "how many", "how much", "how far", "how long", "how fast", "how old",
        "total", "sum of", "difference", "product of", "average", "mean of", "percent", "percentage",
        "each", "per ", "altogether", "remaining", "left over", "combined", "ratio", "twice",
        "half of", "more than", "less than", "faster", "slower", "cheaper", "cost", "costs",
        "price", "speed", "distance", "profit", "interest", "discount",
    )

    private val FUNCTION_CALL =
        Regex("""\b(sqrt|sin|cos|tan|asin|acos|atan|ln|log|exp|abs|floor|ceil|round)\s*\(""")

    private val PURE_NUMBER = Regex("""[+-]?\d+(?:\.\d+)?""")
}
