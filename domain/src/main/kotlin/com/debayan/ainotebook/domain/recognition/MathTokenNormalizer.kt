package com.debayan.ainotebook.domain.recognition

/**
 * Repairs the character confusions that stop handwritten math from parsing, and only when the
 * surrounding characters license the repair.
 *
 * Recognizers do not confuse characters at random; they confuse the ones that are drawn the same way.
 * `l`/`I`/`1`/`|` are one vertical stroke, `O`/`o`/`0` one loop, and `S`/`5`, `z`/`2`, `G`/`6`,
 * `g`/`9`, `B`/`8` differ only in how far a terminal closes. Which member of a class was meant is not
 * recoverable from the glyph — it is recoverable from the neighbours. `1+l` is arithmetic with a
 * misread `1`, `I love` is prose with a correctly read `I`, and the only difference between them is
 * what sits beside the ambiguous character.
 *
 * Every rewrite is therefore gated on context instead of applied as a blanket replace. A blanket
 * replace is what turns "Is 5 = 5" into "15 5 = 5" — losing the user's words in order to fix their
 * digits. The bias throughout is toward leaving text alone: a repair this misses costs one candidate
 * in [CandidateReranker], which still has the others to choose from, while a repair it gets wrong
 * silently changes what the page says.
 *
 * [normalize] produces the ASCII the parser and the offline math engine read. [normalizeForDisplay]
 * produces the same reading with the operators the user actually wrote (`×`, `÷`, `−`), because
 * showing `12 * 47` back to someone who wrote `12 × 47` reads as a transcription error even when the
 * value is right.
 */
object MathTokenNormalizer {

    /**
     * The parser-facing form: ASCII operators, explicit products, radicals wrapped in the parentheses
     * [com.debayan.ainotebook.domain.math.ExpressionParser] needs to treat `sqrt` as a function rather
     * than as a variable multiplied by its argument.
     */
    fun normalize(text: String): String {
        if (text.isBlank()) return ""
        var working = canonicalize(text)
        working = expandSymbols(working)
        working = repairConfusions(working)
        working = resolveTimesSign(working)
        working = normalizeSeparators(working)
        if (allowsImplicitProduct(working)) working = insertImplicitProducts(working)
        return collapseSpaces(working)
    }

    /**
     * The user-facing form: the same repairs, none of the parser's scaffolding.
     *
     * No implicit `*` is inserted — the user did not write one, and `2*x` shown over ink that reads
     * `2x` looks like the app misread the page. Unicode operators are restored only when the reading
     * is actually math, so a hyphen in prose is never promoted to a minus sign.
     */
    fun normalizeForDisplay(text: String): String {
        if (text.isBlank()) return ""
        var working = canonicalize(text)
        working = repairConfusions(working)
        working = resolveTimesSign(working)
        working = normalizeSeparators(working)
        working = collapseSpaces(working)
        return if (MathHeuristics.looksLikeMath(working)) prettify(working) else working
    }

    /**
     * Words that are math even though they are spelled with letters. Shared with [MathHeuristics] so
     * that "integral 2x dx" counts as notation in both the prose guard and the implicit-product
     * guard; two divergent copies of this list is how the two agree on a page's meaning until the day
     * one of them is edited.
     */
    internal val MATH_IDENTIFIERS: Set<String> = setOf(
        "pi", "sqrt", "sin", "cos", "tan", "asin", "acos", "atan", "ln", "log", "exp",
        "abs", "floor", "ceil", "round", "mod", "dx", "dy", "dz", "dt", "lim", "limit",
        "integral", "integrate", "derivative", "differentiate", "inf",
    )

    /** Maximal runs of letters, the unit both the prose guard and the language prior reason about. */
    internal fun letterRuns(text: String): List<String> {
        val runs = mutableListOf<String>()
        var index = 0
        while (index < text.length) {
            if (!text[index].isLetter()) {
                index++
                continue
            }
            val start = index
            while (index < text.length && text[index].isLetter()) index++
            runs += text.substring(start, index)
        }
        return runs
    }

    /**
     * Folds the operator glyphs a recognizer can emit onto one ASCII spelling each, and lifts
     * superscript digits into `^` exponents before anything else looks at the string. Doing this first
     * means every later rule has exactly one spelling of "times" to reason about.
     */
    private fun canonicalize(text: String): String {
        val out = StringBuilder(text.length + EXPONENT_HEADROOM)
        var index = 0
        while (index < text.length) {
            val superscript = SUPERSCRIPT_DIGITS[text[index]]
            if (superscript != null) {
                out.append('^').append(superscript)
                index++
                while (index < text.length) {
                    val next = SUPERSCRIPT_DIGITS[text[index]] ?: break
                    out.append(next)
                    index++
                }
                continue
            }
            out.append(OPERATOR_ALIASES[text[index]] ?: text[index])
            index++
        }
        return out.toString().replace("**", "^")
    }

    /**
     * Spells out the symbols that have no ASCII operator: `π` becomes the constant the parser folds,
     * and `√` becomes a function call over an explicit argument. `√16+2` has to become `sqrt(16)+2`
     * rather than `sqrt 16+2`, which the grammar reads as a variable named `sqrt` multiplied by 18.
     */
    private fun expandSymbols(text: String): String {
        var working = text
        if (working.indexOf('π') >= 0) working = working.replace("π", "pi")
        if (working.indexOf('√') >= 0) {
            working = RADICAL_BEFORE_GROUP.replace(working, "sqrt(")
            working = RADICAL_BEFORE_OPERAND.replace(working) { "sqrt(${it.groupValues[1]})" }
            working = working.replace("√", "sqrt")
        }
        return BARE_ROOT.replace(working) { "sqrt(${it.groupValues[1]})" }
    }

    /**
     * Walks the string token by token, where a token is a run of letters, digits and `|`.
     *
     * The token is the right unit because the decisive evidence is usually inside it: `va1ue` contains
     * letters that cannot be digits, `1O0` contains a digit that cannot be a letter, and either fact
     * settles every ambiguous character in the run. Only a token with no internal evidence at all
     * falls back to looking outside itself.
     */
    private fun repairConfusions(text: String): String {
        val chars = text.toCharArray()
        var index = 0
        while (index < chars.size) {
            if (!isTokenChar(chars[index])) {
                index++
                continue
            }
            var end = index
            while (end < chars.size && isTokenChar(chars[end])) end++
            repairToken(chars, index, end)
            index = end
        }
        return String(chars)
    }

    private fun repairToken(chars: CharArray, start: Int, end: Int) {
        var letterAnchor = false
        var digitAnchor = false
        var digit = false
        var confusableLetter = false
        for (index in start until end) {
            val c = chars[index]
            when {
                c.isDigit() -> {
                    digit = true
                    if (c in UNAMBIGUOUS_DIGITS) digitAnchor = true
                }
                LETTER_TO_DIGIT.containsKey(c) -> confusableLetter = true
                c.isLetter() && c != TIMES_LETTER && c != TIMES_LETTER.uppercaseChar() -> letterAnchor = true
            }
        }
        when {
            // Both kinds of unambiguous character in one run: a deliberate identifier like `a3`, and
            // nothing here is a better guess than what the recognizer already read.
            letterAnchor && digitAnchor -> Unit
            letterAnchor -> applyWordMode(chars, start, end)
            digitAnchor || digit -> applyNumberMode(chars, start, end)
            // A lone confusable glyph with no internal evidence — the `l` in `1+l`. Two or more
            // letters with no evidence is far more likely a short word ("Is", "So") than a misread
            // number, and "Is" turning into "15" is a worse failure than "lO" staying "lO".
            confusableLetter && end - start == 1 && hasDigitNeighbour(chars, start, end) ->
                applyNumberMode(chars, start, end)
            else -> Unit
        }
    }

    /** Inside a word, a digit is a misread letter. */
    private fun applyWordMode(chars: CharArray, start: Int, end: Int) {
        for (index in start until end) {
            val letter = DIGIT_TO_LETTER[chars[index]] ?: continue
            val before = letterRunBefore(chars, start, index)
            val after = letterRunAfter(chars, index, end)
            // Two letters on one side is the evidence: it separates `va1ue` from `x2y`, where the
            // digit is an index rather than a misread `z`.
            val interior = before > 0 && after > 0 && (before >= WORD_EVIDENCE_LETTERS || after >= WORD_EVIDENCE_LETTERS)
            // A leading digit needs a longer run before it is touched, or every ordinal — `2nd`,
            // `9pm` — is rewritten into nonsense.
            val leading = index == start &&
                before == 0 &&
                end - start >= MIN_LEADING_REPAIR_TOKEN &&
                after >= WORD_EVIDENCE_LETTERS
            if (!interior && !leading) continue
            val neighbour = if (before > 0) chars[index - 1] else chars[index + 1]
            chars[index] = if (neighbour.isUpperCase()) letter.uppercaseChar() else letter
        }
    }

    /** Inside a number, a letter is a misread digit. */
    private fun applyNumberMode(chars: CharArray, start: Int, end: Int) {
        for (index in start until end) {
            chars[index] = LETTER_TO_DIGIT[chars[index]] ?: chars[index]
        }
    }

    /**
     * Looks for a digit beside the token, crossing spaces and a bounded number of operators but never
     * a letter or a line break.
     *
     * Operators are transparent because the digit that licenses the repair is often one operator away
     * — the `1` that makes the `l` in `1+l` a `1`. A letter is opaque because it means the neighbour
     * is a word, and words next to ambiguous letters are the case this must not touch.
     */
    private fun hasDigitNeighbour(chars: CharArray, start: Int, end: Int): Boolean =
        scanForDigit(chars, start - 1, -1) || scanForDigit(chars, end, 1)

    private fun scanForDigit(chars: CharArray, from: Int, step: Int): Boolean {
        var index = from
        var crossed = 0
        while (index >= 0 && index < chars.size) {
            val c = chars[index]
            when {
                c == '\n' -> return false
                c.isWhitespace() -> Unit
                c.isDigit() -> return true
                c.isLetter() -> return false
                c in TRANSPARENT_OPERATORS -> {
                    crossed++
                    if (crossed > MAX_CROSSED_OPERATORS) return false
                }
                else -> return false
            }
            index += step
        }
        return false
    }

    /**
     * Decides, per occurrence, whether a handwritten `x` is a times sign or a variable.
     *
     * A value on the left and a value on the right makes it an operator (`12 x 47`); anything else
     * leaves it a variable. The parenthesised form is only taken as multiplication when the group
     * itself starts with a number: rewriting `2x(y+1)` to `2*(y+1)` would delete a variable from the
     * user's expression, which is a far worse outcome than leaving an implicit product to the grammar.
     */
    private fun resolveTimesSign(text: String): String {
        val chars = text.toCharArray()
        for (index in chars.indices) {
            val c = chars[index]
            if (c != TIMES_LETTER && c != TIMES_LETTER.uppercaseChar()) continue
            if (adjoinsLetterInToken(chars, index)) continue
            val leftIndex = significantIndex(chars, index - 1, -1)
            val rightIndex = significantIndex(chars, index + 1, 1)
            if (leftIndex < 0 || rightIndex < 0) continue
            val left = chars[leftIndex]
            val right = chars[rightIndex]
            val leftIsValue = left.isDigit() || left == ')'
            val rightIsValue = right.isDigit() || (right == '(' && groupStartsWithValue(chars, rightIndex))
            if (leftIsValue && rightIsValue) chars[index] = '*'
        }
        return String(chars)
    }

    /** True when the `x` is part of a longer word or identifier, as in `max` or `xy`. */
    private fun adjoinsLetterInToken(chars: CharArray, index: Int): Boolean {
        val before = chars.getOrNull(index - 1)
        val after = chars.getOrNull(index + 1)
        return (before != null && before.isLetter()) || (after != null && after.isLetter())
    }

    private fun groupStartsWithValue(chars: CharArray, parenIndex: Int): Boolean {
        val firstIndex = significantIndex(chars, parenIndex + 1, 1)
        if (firstIndex < 0) return false
        val first = chars[firstIndex]
        return first.isDigit() || first == '(' || first == '-' || first == '+'
    }

    private fun significantIndex(chars: CharArray, from: Int, step: Int): Int {
        var index = from
        while (index >= 0 && index < chars.size) {
            val c = chars[index]
            if (c == '\n') return -1
            if (!c.isWhitespace()) return index
            index += step
        }
        return -1
    }

    /**
     * Resolves the comma, which is a thousands separator in `1,000` and a decimal point in `1,5`.
     * Exactly three following digits is the grouping convention; one or two is a fraction, because no
     * convention groups digits in pairs.
     */
    private fun normalizeSeparators(text: String): String {
        var working = THOUSANDS_COMMA.replace(text, "")
        working = DECIMAL_COMMA.replace(working, ".")
        return working
    }

    /**
     * Only spells out products between characters the user wrote with no space between them, and only
     * when the string holds no ordinary words.
     *
     * Both restrictions are there to keep `5th` from becoming `5*th`: adjacency alone cannot tell a
     * coefficient from an ordinal suffix, so the presence of any multi-letter word that is not itself
     * math notation disqualifies the whole string. The grammar already handles implicit products, so
     * skipping this costs nothing but the explicitness the solver's prompt prefers.
     */
    private fun allowsImplicitProduct(text: String): Boolean =
        letterRuns(text).none { it.length >= 2 && it.lowercase() !in MATH_IDENTIFIERS }

    private fun insertImplicitProducts(text: String): String {
        val out = StringBuilder(text.length + PRODUCT_HEADROOM)
        for (index in text.indices) {
            val c = text[index]
            if (index > 0 && needsProduct(text[index - 1], c)) out.append('*')
            out.append(c)
        }
        return out.toString()
    }

    /**
     * A letter after `(` is left alone deliberately: `sin(x)` is a call, not a product, and inserting
     * a `*` there would turn every function in the expression into an unbound variable.
     */
    private fun needsProduct(previous: Char, current: Char): Boolean = when {
        previous.isDigit() && current.isLetter() -> true
        previous.isDigit() && current == '(' -> true
        previous == ')' && (current.isDigit() || current.isLetter() || current == '(') -> true
        else -> false
    }

    private fun collapseSpaces(text: String): String =
        text.split('\n').joinToString("\n") { HORIZONTAL_SPACE.replace(it, " ").trim() }.trim()

    /**
     * Restores the glyphs a person writes. Division is the one operator that cannot be replaced on
     * sight: the `/` in `d/dx` is part of the notation for a derivative, and `d÷dx` is not a thing
     * anyone has ever written.
     */
    private fun prettify(text: String): String {
        val chars = text.toCharArray()
        for (index in chars.indices) {
            when (chars[index]) {
                '*' -> chars[index] = '×'
                '-' -> chars[index] = '−'
                '/' -> if (dividesValues(chars, index)) chars[index] = '÷'
            }
        }
        return String(chars)
            .replace("sqrt(", "√(")
            .replace(PI_WORD, "π")
    }

    private fun dividesValues(chars: CharArray, index: Int): Boolean {
        val leftIndex = significantIndex(chars, index - 1, -1)
        val rightIndex = significantIndex(chars, index + 1, 1)
        if (leftIndex < 0 || rightIndex < 0) return false
        val left = chars[leftIndex]
        val right = chars[rightIndex]
        return (left.isDigit() || left == ')') && (right.isDigit() || right == '(')
    }

    private fun isTokenChar(c: Char): Boolean = c.isLetterOrDigit() || c == '|'

    private fun letterRunBefore(chars: CharArray, start: Int, index: Int): Int {
        var count = 0
        var cursor = index - 1
        while (cursor >= start && chars[cursor].isLetter()) {
            count++
            cursor--
        }
        return count
    }

    private fun letterRunAfter(chars: CharArray, index: Int, end: Int): Int {
        var count = 0
        var cursor = index + 1
        while (cursor < end && chars[cursor].isLetter()) {
            count++
            cursor++
        }
        return count
    }

    /** The glyph that is both the commonest variable and the commonest multiplication sign. */
    private const val TIMES_LETTER = 'x'

    /** Digits with no letter that is drawn like them, and therefore the anchors of a number. */
    private const val UNAMBIGUOUS_DIGITS = "347"

    /** Characters a digit hunt may cross: operators and separators, never letters. */
    private const val TRANSPARENT_OPERATORS = "+-*/^%=().,"

    private const val MAX_CROSSED_OPERATORS = 2

    private const val WORD_EVIDENCE_LETTERS = 2

    /** `2nd` and `9pm` are three characters long; `5olve` is five. */
    private const val MIN_LEADING_REPAIR_TOKEN = 4

    private const val EXPONENT_HEADROOM = 8
    private const val PRODUCT_HEADROOM = 8

    private val LETTER_TO_DIGIT: Map<Char, Char> = mapOf(
        'l' to '1', 'I' to '1', '|' to '1',
        'O' to '0', 'o' to '0',
        'S' to '5', 's' to '5',
        'z' to '2', 'Z' to '2',
        'G' to '6',
        'g' to '9', 'q' to '9',
        'B' to '8',
    )

    /**
     * The reverse reading, used inside words. Lower case because a misread digit in the middle of a
     * word is overwhelmingly a lower-case letter; [applyWordMode] restores the case from the
     * neighbour when the surrounding word is capitalised.
     */
    private val DIGIT_TO_LETTER: Map<Char, Char> = mapOf(
        '1' to 'l', '0' to 'o', '5' to 's', '2' to 'z', '6' to 'g', '9' to 'g', '8' to 'b',
    )

    private val SUPERSCRIPT_DIGITS: Map<Char, Char> = mapOf(
        '⁰' to '0', '¹' to '1', '²' to '2', '³' to '3', '⁴' to '4',
        '⁵' to '5', '⁶' to '6', '⁷' to '7', '⁸' to '8', '⁹' to '9',
    )

    private val OPERATOR_ALIASES: Map<Char, Char> = mapOf(
        '×' to '*', '✕' to '*', '⋅' to '*', '·' to '*', '∗' to '*', '∙' to '*',
        '÷' to '/', '⁄' to '/', '∕' to '/',
        '−' to '-', '–' to '-', '—' to '-', '‐' to '-', '‑' to '-', '‒' to '-',
        '⁼' to '=', '＝' to '=',
    )

    private val RADICAL_BEFORE_GROUP = Regex("""√\s*\(""")
    private val RADICAL_BEFORE_OPERAND = Regex("""√\s*(\d+(?:\.\d+)?|[A-Za-z]+)""")
    private val BARE_ROOT = Regex("""\bsqrt\s*(\d+(?:\.\d+)?)""", RegexOption.IGNORE_CASE)
    private val THOUSANDS_COMMA = Regex("""(?<=\d),(?=\d{3}(?!\d))""")
    private val DECIMAL_COMMA = Regex("""(?<=\d),(?=\d{1,2}(?!\d))""")
    private val HORIZONTAL_SPACE = Regex("""[ \t\r]+""")
    private val PI_WORD = Regex("""\bpi\b""")
}
