package com.debayan.ainotebook.domain.math

import kotlin.math.abs
import kotlin.math.sqrt

/**
 * A single-variable polynomial as a map of exponent → coefficient (zero coefficients omitted). Used
 * for the "little bit of calculus" the app supports: derivatives, integrals, and solving linear and
 * quadratic equations. Non-polynomial input (trig, division by a variable, non-integer powers) is
 * rejected by [fromExpr], which returns null so the caller can fall back to another strategy.
 */
class Polynomial private constructor(val terms: Map<Int, Double>) {

    val degree: Int get() = terms.keys.maxOrNull() ?: 0

    private fun coeff(exp: Int): Double = terms[exp] ?: 0.0

    operator fun plus(other: Polynomial): Polynomial =
        of((terms.keys + other.terms.keys).associateWith { coeff(it) + other.coeff(it) })

    operator fun minus(other: Polynomial): Polynomial =
        of((terms.keys + other.terms.keys).associateWith { coeff(it) - other.coeff(it) })

    operator fun times(other: Polynomial): Polynomial {
        val out = HashMap<Int, Double>()
        for ((e1, c1) in terms) for ((e2, c2) in other.terms) {
            out[e1 + e2] = (out[e1 + e2] ?: 0.0) + c1 * c2
        }
        return of(out)
    }

    fun pow(n: Int): Polynomial {
        require(n >= 0) { "Negative polynomial power" }
        var result = of(mapOf(0 to 1.0))
        repeat(n) { result *= this }
        return result
    }

    fun derivative(): Polynomial =
        of(terms.filterKeys { it >= 1 }.map { (e, c) -> (e - 1) to c * e }.toMap())

    fun integral(): Polynomial =
        of(terms.map { (e, c) -> (e + 1) to c / (e + 1) }.toMap())

    /** Real roots of `this = 0` for degree ≤ 2, or null if the degree is unsupported. */
    fun realRoots(): List<Double>? = when (degree) {
        0 -> if (abs(coeff(0)) < EPS) null else emptyList() // 0=0 (all) vs c=0 (none) → treat as none
        1 -> listOf(-coeff(0) / coeff(1))
        2 -> {
            val a = coeff(2); val b = coeff(1); val c = coeff(0)
            val disc = b * b - 4 * a * c
            when {
                disc < -EPS -> emptyList()
                disc < EPS -> listOf(-b / (2 * a))
                else -> {
                    val s = sqrt(disc)
                    listOf((-b + s) / (2 * a), (-b - s) / (2 * a))
                }
            }
        }
        else -> null
    }

    /** Human-readable form like `2x^2 - 3x + 1` (constant `0` when empty). */
    fun format(variable: String): String {
        val ordered = terms.entries
            .filter { abs(it.value) >= EPS }
            .sortedByDescending { it.key }
        if (ordered.isEmpty()) return "0"
        val sb = StringBuilder()
        for ((index, entry) in ordered.withIndex()) {
            val (exp, coef) = entry
            val magnitude = abs(coef)
            when {
                index == 0 -> if (coef < 0) sb.append("-")
                coef < 0 -> sb.append(" - ")
                else -> sb.append(" + ")
            }
            val showCoeff = abs(magnitude - 1.0) >= EPS || exp == 0
            if (showCoeff) sb.append(MathFormat.number(magnitude))
            when {
                exp == 0 -> Unit
                exp == 1 -> sb.append(variable)
                else -> sb.append("$variable^$exp")
            }
        }
        return sb.toString()
    }

    companion object {
        private const val EPS = 1e-9

        private fun of(raw: Map<Int, Double>): Polynomial =
            Polynomial(raw.filterValues { abs(it) >= EPS })

        /** Converts an [Expr] to a polynomial in [variable], or null if it is not polynomial. */
        fun fromExpr(expr: Expr, variable: String): Polynomial? = try {
            build(expr, variable)
        } catch (_: MathParseException) {
            null
        }

        private fun build(expr: Expr, variable: String): Polynomial = when (expr) {
            is Expr.Num -> of(mapOf(0 to expr.value))
            is Expr.Var -> if (expr.name == variable) {
                of(mapOf(1 to 1.0))
            } else {
                throw MathParseException("Second variable: ${expr.name}")
            }
            is Expr.Unary -> build(expr.operand, variable).let { if (expr.op == '-') it * of(mapOf(0 to -1.0)) else it }
            is Expr.Binary -> {
                val l = build(expr.left, variable)
                when (expr.op) {
                    '+' -> l + build(expr.right, variable)
                    '-' -> l - build(expr.right, variable)
                    '*' -> l * build(expr.right, variable)
                    '/' -> {
                        val divisor = build(expr.right, variable)
                        if (divisor.degree != 0) throw MathParseException("Division by a variable")
                        val d = divisor.coeff(0)
                        if (abs(d) < EPS) throw MathParseException("Division by zero")
                        of(l.terms.mapValues { it.value / d })
                    }
                    '^' -> {
                        val exponent = build(expr.right, variable)
                        if (exponent.degree != 0) throw MathParseException("Variable exponent")
                        val n = exponent.coeff(0)
                        if (n < 0 || abs(n - n.toInt()) > EPS) throw MathParseException("Non-natural power")
                        l.pow(n.toInt())
                    }
                    else -> throw MathParseException("Unsupported operator ${expr.op}")
                }
            }
            is Expr.Func -> throw MathParseException("Functions are not polynomial")
        }
    }
}
