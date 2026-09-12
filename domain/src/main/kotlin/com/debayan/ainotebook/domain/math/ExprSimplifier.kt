package com.debayan.ainotebook.domain.math

/**
 * Tidies an [Expr] into the form a person would have written it in.
 *
 * Differentiation is mechanical and produces mechanical output: the derivative of `x^2 + 3x` comes
 * out of the rules as `2 · x^(2-1) + 3 · 1`. That is the right answer and the wrong notation, and a
 * student comparing it against their own `2x + 3` cannot tell whether they were marked right. So the
 * rules here fold constants, drop identities, and collect like terms until the expression stops
 * changing.
 *
 * Rewrites are applied bottom-up and repeated to a fixed point, with a pass cap: any rule set large
 * enough to be useful is large enough to cycle on some input, and a spinning simplifier on the solve
 * path would look exactly like a hang.
 */
internal object ExprSimplifier {

    fun simplify(expr: Expr): Expr {
        var current = expr
        repeat(MAX_PASSES) {
            val next = rewrite(current)
            if (next == current) return current
            current = next
        }
        return current
    }

    private fun rewrite(expr: Expr): Expr {
        val withSimplifiedChildren = when (expr) {
            is Expr.Num, is Expr.Const, is Expr.Var -> expr
            is Expr.Unary -> Expr.Unary(expr.op, rewrite(expr.operand))
            is Expr.Binary -> Expr.Binary(expr.op, rewrite(expr.left), rewrite(expr.right))
            is Expr.Func -> Expr.Func(expr.name, expr.args.map { rewrite(it) })
        }
        foldConstant(withSimplifiedChildren)?.let { return it }
        return when (withSimplifiedChildren) {
            is Expr.Unary -> simplifyUnary(withSimplifiedChildren)
            is Expr.Binary -> simplifyBinary(withSimplifiedChildren)
            else -> withSimplifiedChildren
        }
    }

    /**
     * Collapses a variable-free subtree to a literal, but only when its exact value is rational.
     * `sqrt(2)` and `pi` stay symbolic: replacing them with 1.414214 would turn an exact answer into
     * a rounded one at the exact moment nobody is looking.
     */
    private fun foldConstant(expr: Expr): Expr? {
        if (expr is Expr.Num || expr is Expr.Const || expr is Expr.Var) return null
        if (expr.variables().isNotEmpty()) return null
        val exact = ExactEvaluator.rationalOf(expr) ?: return null
        if (!exact.isInteger && !exact.isSimpleFraction) return null
        return Expr.Num.of(exact)
    }

    private fun simplifyUnary(expr: Expr.Unary): Expr = when {
        expr.op == '+' -> expr.operand
        expr.operand is Expr.Unary && expr.operand.op == '-' -> expr.operand.operand
        expr.operand is Expr.Num -> negate(expr.operand)
        else -> expr
    }

    private fun simplifyBinary(expr: Expr.Binary): Expr {
        val left = expr.left
        val right = expr.right
        return when (expr.op) {
            '+' -> when {
                isZero(left) -> right
                isZero(right) -> left
                else -> collectSum(left, right) ?: expr
            }
            '-' -> when {
                isZero(right) -> left
                isZero(left) -> negate(right)
                left == right -> ZERO
                else -> collectSum(left, negate(right)) ?: expr
            }
            '*' -> simplifyProduct(left, right, expr)
            '/' -> when {
                isOne(right) -> left
                isZero(left) && !isZero(right) -> ZERO
                left == right -> ONE
                else -> expr
            }
            '^' -> when {
                isZero(right) -> ONE
                isOne(right) -> left
                isOne(left) -> ONE
                isZero(left) -> ZERO
                else -> reciprocalPower(left, right) ?: foldPowerTower(left, right) ?: expr
            }
            else -> expr
        }
    }

    private fun simplifyProduct(left: Expr, right: Expr, expr: Expr.Binary): Expr = when {
        isZero(left) || isZero(right) -> ZERO
        isOne(left) -> right
        isOne(right) -> left
        isMinusOne(left) -> negate(right)
        isMinusOne(right) -> negate(left)
        // Numbers to the front: `x 2` is legal notation nobody writes, and putting the coefficient
        // first is also what lets like terms be collected.
        right is Expr.Num && left !is Expr.Num -> Expr.Binary('*', right, left)
        left is Expr.Num && right is Expr.Binary && right.op == '*' && right.left is Expr.Num ->
            Expr.Binary('*', Expr.Binary('*', left, right.left), right.right)
        else -> expr
    }

    /** `f^-2` reads better as `1/f^2`, and `f^-1` as `1/f`. */
    private fun reciprocalPower(base: Expr, exponent: Expr): Expr? {
        val value = exactOf(exponent) ?: return null
        if (value.signum >= 0) return null
        val magnitude = -value
        val denominator = if (magnitude.isOne) base else Expr.Binary('^', base, Expr.Num.of(magnitude))
        return Expr.Binary('/', ONE, denominator)
    }

    /** `(x^2)^3` is `x^6`. */
    private fun foldPowerTower(base: Expr, exponent: Expr): Expr? {
        if (base !is Expr.Binary || base.op != '^') return null
        val inner = exactOf(base.right) ?: return null
        val outer = exactOf(exponent) ?: return null
        return Expr.Binary('^', base.left, Expr.Num.of(inner * outer))
    }

    /** Collects `2x + 3x` into `5x`, which is the step a reader expects to have been done already. */
    private fun collectSum(left: Expr, right: Expr): Expr? {
        val (leftCoefficient, leftBase) = splitCoefficient(left) ?: return null
        val (rightCoefficient, rightBase) = splitCoefficient(right) ?: return null
        if (leftBase != rightBase) return null
        val total = leftCoefficient + rightCoefficient
        return when {
            total.isZero -> ZERO
            total.isOne -> leftBase
            else -> Expr.Binary('*', Expr.Num.of(total), leftBase)
        }
    }

    /**
     * Splits a term into its numeric coefficient and the rest. Pure numbers return null: they are
     * folded elsewhere, and treating `2` as `2 × 1` here would make every sum of two numbers look
     * like a pair of like terms.
     */
    private fun splitCoefficient(expr: Expr): Pair<Rational, Expr>? = when {
        expr is Expr.Num -> null
        expr is Expr.Binary && expr.op == '*' -> exactOf(expr.left)?.let { it to expr.right }
        expr is Expr.Unary && expr.op == '-' ->
            splitCoefficient(expr.operand)?.let { (coefficient, base) -> -coefficient to base }
                ?: (-Rational.ONE to expr.operand)
        else -> Rational.ONE to expr
    }

    private fun negate(expr: Expr): Expr = when {
        expr is Expr.Num -> Expr.Num(-expr.value, expr.exact?.let { -it })
        expr is Expr.Unary && expr.op == '-' -> expr.operand
        else -> Expr.Unary('-', expr)
    }

    private fun exactOf(expr: Expr): Rational? =
        (expr as? Expr.Num)?.let { it.exact ?: Rational.fromDouble(it.value) }

    private fun isZero(expr: Expr): Boolean = exactOf(expr)?.isZero == true

    private fun isOne(expr: Expr): Boolean = exactOf(expr)?.isOne == true

    private fun isMinusOne(expr: Expr): Boolean = exactOf(expr) == MINUS_ONE

    private val ZERO = Expr.Num.of(Rational.ZERO)
    private val ONE = Expr.Num.of(Rational.ONE)
    private val MINUS_ONE = -Rational.ONE
    private const val MAX_PASSES = 12
}
