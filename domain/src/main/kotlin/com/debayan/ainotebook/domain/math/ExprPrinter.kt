package com.debayan.ainotebook.domain.math

/**
 * Renders an [Expr] back to the notation a person would have written: `2x + 3`, `2cos(2x)`,
 * `1/x`, `x^2 - 3`.
 *
 * Brackets are emitted only where precedence actually needs them, because a derivative that comes
 * back as `((2)*(x^(2-1)))+((3)*(1))` is technically correct and practically useless — the point of
 * showing working is that it can be read at a glance.
 */
internal object ExprPrinter {

    fun format(expr: Expr): String = render(expr, ADDITIVE)

    private fun render(expr: Expr, required: Int): String = when (expr) {
        is Expr.Num -> {
            val text = expr.exact?.let { MathFormat.rational(it) } ?: MathFormat.number(expr.value)
            wrap(text, if (text.startsWith("-")) UNARY else ATOM, required)
        }
        is Expr.Const -> expr.name
        is Expr.Var -> expr.name
        is Expr.Unary -> if (expr.op == '-') {
            wrap("-${render(expr.operand, POWER)}", UNARY, required)
        } else {
            render(expr.operand, required)
        }
        is Expr.Binary -> renderBinary(expr, required)
        is Expr.Func -> "${expr.name}(${expr.args.joinToString(", ") { format(it) }})"
    }

    private fun renderBinary(expr: Expr.Binary, required: Int): String = when (expr.op) {
        '+' -> {
            val negated = negationOf(expr.right)
            val text = if (negated != null) {
                "${render(expr.left, ADDITIVE)} - ${render(negated, MULTIPLICATIVE)}"
            } else {
                "${render(expr.left, ADDITIVE)} + ${render(expr.right, ADDITIVE)}"
            }
            wrap(text, ADDITIVE, required)
        }
        '-' -> wrap(
            "${render(expr.left, ADDITIVE)} - ${render(expr.right, MULTIPLICATIVE)}",
            ADDITIVE,
            required,
        )
        '*' -> wrap(product(expr.left, expr.right), MULTIPLICATIVE, required)
        '/' -> wrap(
            "${render(expr.left, MULTIPLICATIVE)}/${render(expr.right, POWER)}",
            MULTIPLICATIVE,
            required,
        )
        '%' -> wrap(
            "${render(expr.left, MULTIPLICATIVE)} mod ${render(expr.right, POWER)}",
            MULTIPLICATIVE,
            required,
        )
        '^' -> wrap("${render(expr.left, ATOM)}^${render(expr.right, UNARY)}", POWER, required)
        else -> wrap(
            "${render(expr.left, MULTIPLICATIVE)} ${expr.op} ${render(expr.right, MULTIPLICATIVE)}",
            MULTIPLICATIVE,
            required,
        )
    }

    /**
     * `2x`, `x cos(x)`, `2 × 3`. The multiplication sign is dropped wherever juxtaposition is
     * unambiguous, and kept wherever it is not — `2 3` would read as twenty-three.
     */
    private fun product(left: Expr, right: Expr): String {
        val leftText = render(left, MULTIPLICATIVE)
        val rightText = render(right, MULTIPLICATIVE)
        if (right is Expr.Num) return "$leftText × $rightText"
        return if (needsSeparator(leftText, rightText)) "$leftText $rightText" else "$leftText$rightText"
    }

    private fun needsSeparator(left: String, right: String): Boolean {
        val last = left.last()
        val first = right.first()
        if (!last.isLetterOrDigit() || !first.isLetterOrDigit()) return false
        return !(last.isDigit() && first.isLetter())
    }

    /** The positive counterpart of a negated term, so a sum can print as a difference. */
    private fun negationOf(expr: Expr): Expr? = when {
        expr is Expr.Unary && expr.op == '-' -> expr.operand
        expr is Expr.Num && expr.value < 0.0 -> Expr.Num(-expr.value, expr.exact?.let { -it })
        expr is Expr.Binary && expr.op == '*' -> negationOf(expr.left)?.let { Expr.Binary('*', it, expr.right) }
        else -> null
    }

    private fun wrap(text: String, own: Int, required: Int): String =
        if (own < required) "($text)" else text

    private const val ADDITIVE = 1
    private const val UNARY = 2
    private const val MULTIPLICATIVE = 2
    private const val POWER = 3
    private const val ATOM = 4
}
