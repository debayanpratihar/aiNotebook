package com.debayan.ainotebook.domain.math

import java.math.BigInteger
import kotlin.math.sqrt

/**
 * A single-variable polynomial as a map of exponent → coefficient (zero coefficients omitted), the
 * workhorse behind equation solving, derivatives and integrals.
 *
 * Coefficients are [Rational] rather than [Double] so that `x/3 + 1/6 = 1/2` answers `x = 1` and
 * `2x = 1/3` answers `x = 1/6` instead of `0.166667`. It also removes the epsilon comparisons the
 * double version needed: a coefficient either is zero or is not, so a degree is never misread and a
 * root is never invented at 1e-12.
 *
 * Non-polynomial input (a variable in a denominator or an exponent, a function of the variable) is
 * rejected by [fromExpr], which returns null so the caller can fall back to another strategy.
 */
class Polynomial private constructor(val terms: Map<Int, Rational>) {

    val degree: Int get() = terms.keys.maxOrNull() ?: 0

    val isZero: Boolean get() = terms.isEmpty()

    /** The smallest exponent present, i.e. the power of the variable that can be factored out. */
    val lowestExponent: Int get() = terms.keys.minOrNull() ?: 0

    fun coefficient(exponent: Int): Rational = terms[exponent] ?: Rational.ZERO

    /** Dense coefficients from the constant term upward. */
    fun coefficients(): List<Rational> = List(degree + 1) { coefficient(it) }

    operator fun plus(other: Polynomial): Polynomial =
        of((terms.keys + other.terms.keys).associateWith { coefficient(it) + other.coefficient(it) })

    operator fun minus(other: Polynomial): Polynomial =
        of((terms.keys + other.terms.keys).associateWith { coefficient(it) - other.coefficient(it) })

    operator fun times(other: Polynomial): Polynomial {
        val out = HashMap<Int, Rational>()
        for ((leftExponent, leftCoefficient) in terms) {
            for ((rightExponent, rightCoefficient) in other.terms) {
                val exponent = leftExponent + rightExponent
                out[exponent] = (out[exponent] ?: Rational.ZERO) + leftCoefficient * rightCoefficient
            }
        }
        return of(out)
    }

    fun pow(exponent: Int): Polynomial {
        require(exponent >= 0) { "Negative polynomial power" }
        var result = constant(Rational.ONE)
        repeat(exponent) { result *= this }
        return result
    }

    fun scaled(factor: Rational): Polynomial = of(terms.mapValues { it.value * factor })

    fun derivative(): Polynomial = of(
        terms.filterKeys { it >= 1 }
            .map { (exponent, coefficient) -> (exponent - 1) to coefficient * Rational.of(exponent.toLong()) }
            .toMap(),
    )

    fun integral(): Polynomial = of(
        terms.map { (exponent, coefficient) ->
            (exponent + 1) to coefficient / Rational.of((exponent + 1).toLong())
        }.toMap(),
    )

    fun evaluate(at: Rational): Rational =
        terms.entries.fold(Rational.ZERO) { sum, (exponent, coefficient) ->
            sum + coefficient * at.pow(exponent)
        }

    /** Divides out `x^[count]`; the caller has already recorded that zero is a root. */
    fun withoutLowestPower(count: Int): Polynomial =
        of(terms.mapKeys { it.key - count })

    /**
     * Synthetic division by `(x - root)`. The caller must have verified that [root] really is a
     * root; otherwise the remainder is silently discarded.
     */
    fun divideByRoot(root: Rational): Polynomial {
        val dense = coefficients()
        if (dense.size <= 1) return constant(Rational.ZERO)
        val quotient = MutableList(dense.size - 1) { Rational.ZERO }
        var carry = Rational.ZERO
        for (index in dense.indices.reversed()) {
            if (index == 0) break
            carry = dense[index] + carry * root
            quotient[index - 1] = carry
        }
        return of(quotient.withIndex().associate { (exponent, value) -> exponent to value })
    }

    /**
     * Exact rational roots via the rational root theorem, or null when the coefficients are too
     * large to enumerate divisors for. Requires a non-zero constant term — factor out `x` first, or
     * every candidate denominator search is wasted on a root the caller already knows about.
     */
    fun rationalRoots(): List<Rational>? {
        if (degree < 1 || coefficient(0).isZero) return null
        val scale = terms.values.fold(BigInteger.ONE) { acc, value -> NumberTheory.lcm(acc, value.denominator) }
        val whole = scaled(Rational.of(scale))
        val constantTerm = whole.coefficient(0).numerator
        val leading = whole.coefficient(degree).numerator
        val numerators = NumberTheory.divisors(constantTerm) ?: return null
        val denominators = NumberTheory.divisors(leading) ?: return null
        if (numerators.size * denominators.size > MAX_ROOT_CANDIDATES) return null
        val roots = mutableListOf<Rational>()
        for (numerator in numerators) {
            for (denominator in denominators) {
                for (sign in SIGNS) {
                    val candidate = Rational.of(numerator.multiply(sign), denominator)
                    if (evaluate(candidate).isZero && candidate !in roots) roots += candidate
                }
            }
        }
        return roots
    }

    /** Real roots of `this = 0` for degree ≤ 2, or null if the degree is unsupported. */
    fun realRoots(): List<Double>? = when (degree) {
        0 -> if (isZero) null else emptyList() // 0=0 (all) vs c=0 (none) → treat as none
        1 -> listOf(-(coefficient(0) / coefficient(1)).toDouble())
        2 -> {
            val a = coefficient(2).toDouble()
            val b = coefficient(1).toDouble()
            val c = coefficient(0).toDouble()
            val discriminant = b * b - 4 * a * c
            when {
                discriminant < 0.0 -> emptyList()
                discriminant == 0.0 -> listOf(-b / (2 * a))
                else -> {
                    val spread = sqrt(discriminant)
                    listOf((-b + spread) / (2 * a), (-b - spread) / (2 * a))
                }
            }
        }
        else -> null
    }

    /** Human-readable form like `2x^2 - 3x + 1` (constant `0` when empty). */
    fun format(variable: String): String {
        val ordered = terms.entries
            .filterNot { it.value.isZero }
            .sortedByDescending { it.key }
        if (ordered.isEmpty()) return "0"
        val sb = StringBuilder()
        for ((index, entry) in ordered.withIndex()) {
            val (exponent, coefficient) = entry
            when {
                index == 0 -> if (coefficient.signum < 0) sb.append("-")
                coefficient.signum < 0 -> sb.append(" - ")
                else -> sb.append(" + ")
            }
            sb.append(monomial(coefficient.abs(), variable, exponent))
        }
        return sb.toString()
    }

    companion object {
        fun constant(value: Rational): Polynomial = of(mapOf(0 to value))

        /** `x^[exponent]` scaled by [coefficient]. */
        fun term(coefficient: Rational, exponent: Int): Polynomial = of(mapOf(exponent to coefficient))

        /**
         * One term's magnitude as text: `3x^2`, `x`, `(1/2)x`, `7`. The sign belongs to the caller,
         * which is what lets both [format] and the equation steps read as `... - 3x` rather than
         * `... + -3x`.
         */
        internal fun monomial(magnitude: Rational, variable: String, exponent: Int): String {
            if (exponent == 0) return MathFormat.rational(magnitude)
            val power = if (exponent == 1) variable else "$variable^$exponent"
            if (magnitude.isOne) return power
            val coefficient = MathFormat.rational(magnitude)
            return if (magnitude.isInteger) "$coefficient$power" else "($coefficient)$power"
        }

        /** Converts an [Expr] to a polynomial in [variable], or null if it is not polynomial. */
        fun fromExpr(
            expr: Expr,
            variable: String,
            angleMode: AngleMode = AngleMode.RADIANS,
        ): Polynomial? = try {
            build(expr, variable, angleMode)
        } catch (_: MathParseException) {
            null
        } catch (_: ArithmeticException) {
            null
        }

        private fun of(raw: Map<Int, Rational>): Polynomial =
            Polynomial(raw.filterValues { !it.isZero })

        private fun build(expr: Expr, variable: String, angleMode: AngleMode): Polynomial = when (expr) {
            is Expr.Num -> constant(expr.exact ?: rationalOf(expr.value))
            is Expr.Const -> constant(rationalOf(expr.value))
            is Expr.Var -> if (expr.name == variable) {
                term(Rational.ONE, 1)
            } else {
                throw MathParseException("Second variable: ${expr.name}")
            }
            is Expr.Unary -> build(expr.operand, variable, angleMode).let {
                if (expr.op == '-') it.scaled(-Rational.ONE) else it
            }
            is Expr.Binary -> {
                val left = build(expr.left, variable, angleMode)
                when (expr.op) {
                    '+' -> left + build(expr.right, variable, angleMode)
                    '-' -> left - build(expr.right, variable, angleMode)
                    '*' -> left * build(expr.right, variable, angleMode)
                    '/' -> {
                        val divisor = build(expr.right, variable, angleMode)
                        if (divisor.degree != 0) throw MathParseException("Division by a variable")
                        val value = divisor.coefficient(0)
                        if (value.isZero) throw MathParseException("Division by zero")
                        left.scaled(Rational.ONE / value)
                    }
                    '^' -> {
                        val exponent = build(expr.right, variable, angleMode)
                        if (exponent.degree != 0) throw MathParseException("Variable exponent")
                        val whole = exponent.coefficient(0).toBigIntegerOrNull()
                            ?: throw MathParseException("Non-integer power")
                        if (whole.signum() < 0 || whole > MAX_POWER) {
                            throw MathParseException("Unsupported power")
                        }
                        left.pow(whole.toInt())
                    }
                    else -> throw MathParseException("Unsupported operator ${expr.op}")
                }
            }
            // A function of the variable is not polynomial, but a function of constants is just a
            // coefficient: `sqrt(2)x = 4` is a perfectly ordinary linear equation.
            is Expr.Func -> if (expr.dependsOn(variable)) {
                throw MathParseException("Function of the unknown")
            } else {
                constant(constantValueOf(expr, angleMode))
            }
        }

        private fun constantValueOf(expr: Expr, angleMode: AngleMode): Rational {
            ExactEvaluator.rationalOf(expr)?.let { return it }
            return rationalOf(ExpressionEvaluator.eval(expr, emptyMap(), angleMode))
        }

        private fun rationalOf(value: Double): Rational =
            Rational.fromDouble(value) ?: throw MathParseException("Not a finite number")

        private val SIGNS = listOf(BigInteger.ONE, BigInteger.ONE.negate())
        private val MAX_POWER = BigInteger.valueOf(32L)
        private const val MAX_ROOT_CANDIDATES = 4096
    }
}
