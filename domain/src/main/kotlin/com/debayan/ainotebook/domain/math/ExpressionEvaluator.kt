package com.debayan.ainotebook.domain.math

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * Numerically evaluates an [Expr].
 *
 * This is the fallback path: [ExactEvaluator] runs first and only hands over for genuinely
 * irrational results, so everything here is expected to be rounded for display afterwards.
 */
object ExpressionEvaluator {

    private const val DEGREES_PER_RADIAN = 180.0 / PI

    /** @throws MathParseException if a variable is unbound or a function/operator is unsupported. */
    fun eval(
        expr: Expr,
        variables: Map<String, Double> = emptyMap(),
        angleMode: AngleMode = AngleMode.RADIANS,
    ): Double = when (expr) {
        is Expr.Num -> expr.value
        is Expr.Const -> expr.value
        is Expr.Var -> variables[expr.name]
            ?: throw MathParseException("Unknown variable: ${expr.name}")
        is Expr.Unary -> if (expr.op == '-') {
            -eval(expr.operand, variables, angleMode)
        } else {
            eval(expr.operand, variables, angleMode)
        }
        is Expr.Binary -> {
            val l = eval(expr.left, variables, angleMode)
            val r = eval(expr.right, variables, angleMode)
            when (expr.op) {
                '+' -> l + r
                '-' -> l - r
                '*' -> l * r
                '/' -> l / r
                '%' -> l % r
                '^' -> power(l, r)
                else -> throw MathParseException("Unsupported operator: ${expr.op}")
            }
        }
        is Expr.Func -> evalFunction(expr, variables, angleMode)
    }

    private fun evalFunction(expr: Expr.Func, variables: Map<String, Double>, angleMode: AngleMode): Double {
        val args = expr.args.map { eval(it, variables, angleMode) }
        if (args.size == 2) {
            return when (expr.name) {
                "nthroot" -> nthRoot(args[0], args[1])
                // log(base, value): written base-first, the way it is said out loud.
                "log" -> ln(args[1]) / ln(args[0])
                else -> throw MathParseException("${expr.name} takes one argument")
            }
        }
        if (args.size != 1) throw MathParseException("Wrong argument count for ${expr.name}")
        val x = args[0]
        return when (expr.name) {
            "sqrt" -> sqrt(x)
            "cbrt" -> Math.cbrt(x)
            "abs" -> abs(x)
            "sin" -> sin(toRadians(x, angleMode))
            "cos" -> cos(toRadians(x, angleMode))
            "tan" -> tan(toRadians(x, angleMode))
            "sec" -> 1.0 / cos(toRadians(x, angleMode))
            "csc" -> 1.0 / sin(toRadians(x, angleMode))
            "cot" -> 1.0 / tan(toRadians(x, angleMode))
            "asin" -> fromRadians(asin(x), angleMode)
            "acos" -> fromRadians(acos(x), angleMode)
            "atan" -> fromRadians(atan(x), angleMode)
            "ln" -> ln(x)
            "log" -> log10(x)
            "log2" -> ln(x) / ln(2.0)
            "exp" -> exp(x)
            "floor" -> floor(x)
            "ceil" -> ceil(x)
            "round" -> round(x)
            else -> throw MathParseException("Unknown function: ${expr.name}")
        }
    }

    /**
     * `(-8)^(1/3)` is NaN through [pow] even though the real cube root is -2, and a student writing
     * a cube root of a negative number deserves the real answer rather than "undefined".
     */
    private fun power(base: Double, exponent: Double): Double {
        if (base >= 0.0 || exponent == round(exponent)) return base.pow(exponent)
        val reciprocal = 1.0 / exponent
        if (abs(reciprocal - round(reciprocal)) > 1e-12) return base.pow(exponent)
        val degree = round(reciprocal).toInt()
        return if (degree % 2 == 0) Double.NaN else -(-base).pow(exponent)
    }

    private fun nthRoot(value: Double, degree: Double): Double {
        if (degree == 0.0) return Double.NaN
        if (value < 0.0) {
            val whole = round(degree)
            if (abs(degree - whole) > 1e-12 || whole.toInt() % 2 == 0) return Double.NaN
            return -(-value).pow(1.0 / degree)
        }
        return value.pow(1.0 / degree)
    }

    private fun toRadians(value: Double, angleMode: AngleMode): Double =
        if (angleMode == AngleMode.DEGREES) value / DEGREES_PER_RADIAN else value

    private fun fromRadians(value: Double, angleMode: AngleMode): Double =
        if (angleMode == AngleMode.DEGREES) value * DEGREES_PER_RADIAN else value
}
