package com.debayan.ainotebook.domain.math

import java.math.BigInteger

/**
 * Evaluates an [Expr] exactly, in the field of numbers `a + b√r`, or returns null when the value is
 * not expressible there.
 *
 * Null is the important part of the contract: it means "this one needs floating point", not "this is
 * not maths". Returning an approximation from here would defeat the whole purpose, since the caller
 * would have no way to tell an exact `1/2` from a rounded `0.5000000000000001`.
 */
internal object ExactEvaluator {

    fun eval(expr: Expr, variables: Map<String, Rational> = emptyMap()): SurdNumber? {
        val value = evaluate(expr, variables) ?: return null
        return if (value.isWithinBudget) value else null
    }

    /** The exact value when it is rational — the form the polynomial and equation paths need. */
    fun rationalOf(expr: Expr, variables: Map<String, Rational> = emptyMap()): Rational? =
        eval(expr, variables)?.takeIf { it.isRational }?.rational

    private fun evaluate(expr: Expr, variables: Map<String, Rational>): SurdNumber? = when (expr) {
        is Expr.Num -> expr.exact?.let { SurdNumber.of(it) }
        is Expr.Const -> null // pi and e are irrational by definition; the double path owns them.
        is Expr.Var -> variables[expr.name]?.let { SurdNumber.of(it) }
        is Expr.Unary -> evaluate(expr.operand, variables)?.let { if (expr.op == '-') -it else it }
        is Expr.Binary -> evaluateBinary(expr, variables)
        is Expr.Func -> evaluateFunction(expr, variables)
    }

    private fun evaluateBinary(expr: Expr.Binary, variables: Map<String, Rational>): SurdNumber? {
        val left = evaluate(expr.left, variables) ?: return null
        if (expr.op == '^') return evaluatePower(left, expr.right, variables)
        val right = evaluate(expr.right, variables) ?: return null
        if (!left.isWithinBudget || !right.isWithinBudget) return null
        return when (expr.op) {
            '+' -> left + right
            '-' -> left - right
            '*' -> left * right
            '/' -> left / right
            '%' -> remainder(left, right)
            else -> null
        }
    }

    private fun remainder(left: SurdNumber, right: SurdNumber): SurdNumber? {
        if (!left.isRational || !right.isRational) return null
        return left.rational.remainderOrNull(right.rational)?.let { SurdNumber.of(it) }
    }

    /**
     * Exact powers cover the two cases that matter: a whole exponent, and a reciprocal one that
     * happens to land on a perfect root (`27^(1/3)` is 3, `2^(1/3)` is not expressible and defers).
     */
    private fun evaluatePower(
        base: SurdNumber,
        exponentExpr: Expr,
        variables: Map<String, Rational>,
    ): SurdNumber? {
        val exponent = evaluate(exponentExpr, variables)?.takeIf { it.isRational }?.rational ?: return null
        val whole = exponent.toBigIntegerOrNull()
        if (whole != null) {
            if (whole.abs() > MAX_WHOLE_EXPONENT) return null
            if (base.isRational && base.rational.isZero && whole.signum() < 0) return null
            return base.pow(whole.toInt())
        }
        if (!base.isRational) return null
        val degree = exponent.denominator
        if (degree > MAX_ROOT_DEGREE) return null
        val root = base.rational.nthRootOrNull(degree.toInt()) ?: return sqrtFallback(base.rational, exponent)
        val numerator = exponent.numerator
        if (numerator.abs() > MAX_WHOLE_EXPONENT) return null
        if (root.isZero && numerator.signum() < 0) return null
        return SurdNumber.of(root.pow(numerator.toInt()))
    }

    /** `x^(1/2)` with no rational root is still exact as a surd, e.g. `8^(1/2)` is `2√2`. */
    private fun sqrtFallback(base: Rational, exponent: Rational): SurdNumber? {
        if (exponent != HALF) return null
        return SurdNumber.sqrtOf(base)
    }

    private fun evaluateFunction(expr: Expr.Func, variables: Map<String, Rational>): SurdNumber? {
        val args = expr.args.map { evaluate(it, variables) ?: return null }
        if (args.size == 2) {
            val value = args[1].takeIf { it.isRational }?.rational ?: return null
            val first = args[0].takeIf { it.isRational }?.rational ?: return null
            return when (expr.name) {
                "nthroot" -> exactNthRoot(first, value)
                "log" -> exactLog(first, value)?.let { SurdNumber.of(it) }
                else -> null
            }
        }
        if (args.size != 1) return null
        val argument = args[0]
        return when (expr.name) {
            "sqrt" -> argument.sqrtOrNull()
            "abs" -> if (argument.signum < 0) -argument else argument
            "cbrt" -> argument.takeIf { it.isRational }
                ?.rational?.nthRootOrNull(3)?.let { SurdNumber.of(it) }
            "floor" -> wholeOf(argument) { Rational.of(it.floor()) }
            "ceil" -> wholeOf(argument) { Rational.of(it.ceiling()) }
            "round" -> wholeOf(argument) { Rational.of(it.rounded()) }
            "ln" -> argument.takeIf { it.isRational && it.rational.isOne }?.let { SurdNumber.of(Rational.ZERO) }
            "log" -> argument.takeIf { it.isRational }
                ?.rational?.let { exactLog(Rational.of(10L), it) }?.let { SurdNumber.of(it) }
            "log2" -> argument.takeIf { it.isRational }
                ?.rational?.let { exactLog(Rational.of(2L), it) }?.let { SurdNumber.of(it) }
            "exp" -> argument.takeIf { it.isRational && it.rational.isZero }?.let { SurdNumber.of(Rational.ONE) }
            else -> null
        }
    }

    private fun wholeOf(value: SurdNumber, transform: (Rational) -> Rational): SurdNumber? =
        value.takeIf { it.isRational }?.let { SurdNumber.of(transform(it.rational)) }

    private fun exactNthRoot(value: Rational, degree: Rational): SurdNumber? {
        val whole = degree.toBigIntegerOrNull() ?: return null
        if (whole.signum() <= 0 || whole > MAX_ROOT_DEGREE) return null
        val root = value.nthRootOrNull(whole.toInt())
        if (root != null) return SurdNumber.of(root)
        return if (whole.toInt() == 2) SurdNumber.sqrtOf(value) else null
    }

    /**
     * Exact logarithms only when the argument is a whole power of the base, which is the case that
     * shows up in practice (`log 1000`, `log2 64`) and the case floating point gets visibly wrong —
     * `log10(1000)` is 2.9999999999999996 on some inputs.
     */
    private fun exactLog(base: Rational, value: Rational): Rational? {
        if (base.signum <= 0 || base.isOne || value.signum <= 0) return null
        if (value.isOne) return Rational.ZERO
        var power = base
        for (exponent in 1..MAX_LOG_SEARCH) {
            if (power == value) return Rational.of(exponent.toLong())
            power *= base
            if (!power.isWithinBudget) return null
        }
        val reciprocal = value.reciprocalOrNull() ?: return null
        var negativePower = base
        for (exponent in 1..MAX_LOG_SEARCH) {
            if (negativePower == reciprocal) return Rational.of(-exponent.toLong())
            negativePower *= base
            if (!negativePower.isWithinBudget) return null
        }
        return null
    }

    private val HALF = Rational.of(1L, 2L)
    private val MAX_WHOLE_EXPONENT = BigInteger.valueOf(64L)
    private val MAX_ROOT_DEGREE = BigInteger.valueOf(64L)
    private const val MAX_LOG_SEARCH = 64
}
