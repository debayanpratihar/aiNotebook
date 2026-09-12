package com.debayan.ainotebook.domain.recognition

import com.debayan.ainotebook.domain.math.Expr
import com.debayan.ainotebook.domain.math.ExpressionParser
import com.debayan.ainotebook.domain.model.recognition.RecognitionCandidate
import javax.inject.Inject

/**
 * The chosen reading of one segment, and what the choice cost in confidence.
 *
 * [text] is what the user is shown and what the search index stores; [normalizedMath] is the ASCII
 * the offline engine and the prompt builder consume, present only when the reading really is an
 * expression. Keeping both means the page can display `12 × 47` while the solver is handed `12*47`,
 * instead of one of them having to re-derive the other and getting a different answer.
 */
data class RerankedReading(
    val text: String,
    val normalizedMath: String?,
    val confidence: Float,
    /** Feeds `RecognizedWord.correctedByMathModel`, which the UI marks so a rewrite is never silent. */
    val correctedByMathModel: Boolean,
) {
    val parsedAsMath: Boolean get() = normalizedMath != null
}

/**
 * Picks the best reading from the alternatives the recognizer offered.
 *
 * This is the whole payoff of asking the recognizer for candidates instead of just its first choice.
 * A recognizer scores glyph shapes; it has no idea that `1+l` is not a sum. The math grammar does, so
 * each candidate is repaired by [MathTokenNormalizer] and then offered to
 * [com.debayan.ainotebook.domain.math.ExpressionParser], and a candidate that parses as a solvable
 * expression outranks a better-scored one that does not.
 *
 * Parsing alone is not enough evidence, because the grammar accepts any identifier as a variable and
 * would happily read "hello world" as a product of two unknowns. So a parse only counts when the tree
 * holds a number or an operator and every variable is a single plausible letter — which is also why
 * `l`, `i` and `o` are not plausible variable names here: they are the three glyphs most often misread
 * as digits, and accepting them would let a misread word masquerade as algebra.
 *
 * The parse bonus is bounded so it can inform the decision without dictating it. When the
 * recognizer's own first choice is highly confident and is not math, the bonus is cut to almost
 * nothing: a note that reads "hello" must not be rewritten into an expression because some
 * lower-ranked alternative happened to parse.
 */
class CandidateReranker @Inject constructor() {

    /**
     * Returns the best reading of [candidates], which are expected in recognizer rank order (best
     * first), or null when there is nothing legible to choose from.
     *
     * [expectMath] comes from the heuristic pre-pass over the ink and raises the weight of the
     * grammar's opinion — on a page the user is clearly doing math on, a reading that does not parse
     * is more likely a misread than a sentence.
     */
    fun rerank(candidates: List<RecognitionCandidate>, expectMath: Boolean = false): RerankedReading? {
        val usable = candidates.filter { it.text.isNotBlank() }
        if (usable.isEmpty()) return null

        // Some recognizers report no per-candidate score at all; rank order is then the only signal,
        // and evenly spacing it keeps the blend below meaningful instead of collapsing every
        // candidate onto zero where the parse bonus alone would decide.
        val synthetic = usable.none { it.score > 0f }
        val leaderScore = scoreOf(usable, 0, synthetic)
        val leaderParses = solvableForm(usable[0].text) != null
        val parseBonus = when {
            expectMath -> MATH_PARSE_BONUS
            leaderScore >= CONFIDENT_SCORE && !leaderParses -> GUARDED_PARSE_BONUS
            else -> PARSE_BONUS
        }

        var bestIndex = 0
        var bestBlended = -1f
        var bestMath: String? = null
        usable.forEachIndexed { index, candidate ->
            val math = solvableForm(candidate.text)
            val blended = (
                scoreOf(usable, index, synthetic) +
                    (if (math != null) parseBonus else 0f) -
                    languagePenalty(candidate.text, expectMath)
                ).coerceIn(0f, 1f)
            // Strictly greater, so an equal blend leaves the recognizer's own ordering in place.
            if (blended > bestBlended) {
                bestBlended = blended
                bestIndex = index
                bestMath = math
            }
        }

        val chosen = usable[bestIndex]
        val math = bestMath
        val text = if (math != null) {
            MathTokenNormalizer.normalizeForDisplay(chosen.text)
        } else {
            chosen.text.trim()
        }
        return RerankedReading(
            text = text,
            normalizedMath = math,
            confidence = adjustConfidence(bestBlended, math != null, expectMath),
            correctedByMathModel = math != null &&
                (bestIndex != 0 || differsBeyondSpacing(text, chosen.text)),
        )
    }

    /**
     * Whether [text] reads as an expression the offline engine could be given.
     *
     * Exposed because the line- and page-level result needs the same verdict for
     * [com.debayan.ainotebook.domain.model.recognition.InkRecognitionResult.containsMath] after the
     * words have been joined back together, and a second implementation of "is this math" would drift
     * from this one.
     */
    fun parsesAsMath(text: String): Boolean = solvableForm(text) != null

    /**
     * Normalizes [raw] and returns the expression string when it is genuinely solvable, else null.
     *
     * Equations are split on `=` and each side parsed separately, because the grammar has no `=` and
     * would otherwise reject every equation the user writes — which is most of what a student writes.
     */
    private fun solvableForm(raw: String): String? {
        val cleaned = MathTokenNormalizer.normalize(raw).trimEnd(*TRAILING_NOISE)
        if (cleaned.isBlank() || cleaned.length > MAX_EXPRESSION_CHARS) return null
        val sides = cleaned.split('=')
        if (sides.size > MAX_EQUATION_SIDES) return null

        var content = false
        for (side in sides) {
            if (side.isBlank()) return null
            val tree = runCatching { ExpressionParser(side).parse() }.getOrNull() ?: return null
            val shape = Shape()
            measure(tree, shape)
            if (shape.unknownFunction) return null
            if (shape.variables.any { !isPlausibleVariable(it) }) return null
            if (shape.numbers > 0 || shape.operators > 0) content = true
        }
        return if (content) cleaned else null
    }

    private class Shape {
        var numbers = 0
        var operators = 0
        var unknownFunction = false
        val variables = mutableSetOf<String>()
    }

    private fun measure(expr: Expr, shape: Shape) {
        when (expr) {
            is Expr.Num -> shape.numbers++
            is Expr.Var -> shape.variables += expr.name
            is Expr.Unary -> {
                shape.operators++
                measure(expr.operand, shape)
            }
            is Expr.Binary -> {
                shape.operators++
                measure(expr.left, shape)
                measure(expr.right, shape)
            }
            is Expr.Func -> {
                if (expr.name.lowercase() in KNOWN_FUNCTIONS) {
                    shape.operators++
                } else {
                    shape.unknownFunction = true
                }
                measure(expr.arg, shape)
            }
        }
    }

    /**
     * A variable is a single letter people actually use as one. `pi` and `e` never reach here — the
     * parser folds both to numbers — so anything longer is a word the recognizer read as an
     * identifier, and treating it as algebra is how "solve" becomes a factor in its own expression.
     */
    private fun isPlausibleVariable(name: String): Boolean =
        name.length == 1 && name[0].lowercaseChar() in PLAUSIBLE_VARIABLES

    private fun scoreOf(candidates: List<RecognitionCandidate>, index: Int, synthetic: Boolean): Float =
        if (synthetic) {
            (candidates.size - index).toFloat() / (candidates.size + 1)
        } else {
            candidates[index].score.coerceIn(0f, 1f)
        }

    /**
     * A deliberately weak prior on "is this a plausible reading at all", used only to break the ties
     * the recognizer's score leaves.
     *
     * Two signals, both capped: characters no notation and no language uses, which is a recognizer
     * hallucinating a glyph, and letter runs with no vowel, which is the shape of a misread word
     * rather than a word. It is skipped on math-expected segments, where `sqrt` and `dx` are correct
     * readings that a vowel rule would punish.
     */
    private fun languagePenalty(text: String, expectMath: Boolean): Float {
        var exotic = 0
        for (c in text) {
            if (c.isLetterOrDigit() || c.isWhitespace() || c in ACCEPTED_SYMBOLS) continue
            exotic++
        }
        var penalty = (exotic * EXOTIC_SYMBOL_PENALTY).coerceAtMost(MAX_EXOTIC_PENALTY)
        if (!expectMath) {
            var vowelless = 0
            for (run in MathTokenNormalizer.letterRuns(text)) {
                if (run.length < MIN_WORD_SHAPE) continue
                val lower = run.lowercase()
                if (lower in MathTokenNormalizer.MATH_IDENTIFIERS) continue
                if (lower.none { it in VOWELS }) vowelless++
            }
            penalty += (vowelless * VOWELLESS_PENALTY).coerceAtMost(MAX_VOWELLESS_PENALTY)
        }
        return penalty.coerceAtMost(MAX_LANGUAGE_PENALTY)
    }

    /**
     * Moves the blended score toward certainty when the grammar confirmed the reading, and away from
     * it when math was expected and nothing parsed.
     *
     * This is what stops `RecognizedWord.isLowConfidence` from flagging a reading two independent
     * signals agree on: the recognizer's shapes and the grammar's structure. A merely plausible
     * candidate that the grammar rejected on a math page keeps its doubt, which is the case where the
     * user genuinely should look before the answer is drawn onto their page.
     */
    private fun adjustConfidence(blended: Float, parsed: Boolean, expectMath: Boolean): Float = when {
        parsed && expectMath -> blended + (1f - blended) * MATH_CONFIDENCE_LIFT
        parsed -> blended + (1f - blended) * PARSE_CONFIDENCE_LIFT
        expectMath -> blended * UNPARSED_MATH_PENALTY
        else -> blended
    }.coerceIn(0f, 1f)

    /**
     * Whitespace-insensitive, because collapsing a double space is not a correction and lighting up
     * the "corrected" marker for one would train the user to ignore it.
     */
    private fun differsBeyondSpacing(chosen: String, raw: String): Boolean =
        chosen.filterNot { it.isWhitespace() } != raw.filterNot { it.isWhitespace() }

    companion object {
        /** Weight of a successful parse when the ink already looked like math. */
        private const val MATH_PARSE_BONUS = 0.35f

        /** Weight of a successful parse otherwise: enough to overturn a near tie, not a landslide. */
        private const val PARSE_BONUS = 0.22f

        /**
         * Weight of a successful parse when the recognizer's own first choice is confident and is not
         * math. Near zero on purpose — at this point the grammar is arguing with strong evidence.
         */
        private const val GUARDED_PARSE_BONUS = 0.04f

        private const val CONFIDENT_SCORE = 0.85f

        private const val MATH_CONFIDENCE_LIFT = 0.35f
        private const val PARSE_CONFIDENCE_LIFT = 0.15f
        private const val UNPARSED_MATH_PENALTY = 0.85f

        private const val EXOTIC_SYMBOL_PENALTY = 0.04f
        private const val MAX_EXOTIC_PENALTY = 0.12f
        private const val VOWELLESS_PENALTY = 0.06f
        private const val MAX_VOWELLESS_PENALTY = 0.12f
        private const val MAX_LANGUAGE_PENALTY = 0.20f

        private const val MIN_WORD_SHAPE = 3

        private const val VOWELS = "aeiouy"

        /** An expression longer than this is a page of prose that happens to parse. */
        private const val MAX_EXPRESSION_CHARS = 240

        private const val MAX_EQUATION_SIDES = 2

        /** Trailing marks a person writes around a problem rather than inside it: `2 + 3 = ?`. */
        private val TRAILING_NOISE = charArrayOf('?', '=', ':', ';', ',', ' ')

        private const val PLAUSIBLE_VARIABLES = "abcdefghjkmnpqrstuvwxyz"

        /** Exactly the functions [com.debayan.ainotebook.domain.math.ExpressionEvaluator] can apply. */
        private val KNOWN_FUNCTIONS: Set<String> = setOf(
            "sqrt", "abs", "sin", "cos", "tan", "asin", "acos", "atan",
            "ln", "log", "exp", "floor", "ceil", "round",
        )

        private const val ACCEPTED_SYMBOLS = "+-*/^=().,%<>?!:;'\"\$&#@_[]{}|~×÷−√π∫∑°"
    }
}
