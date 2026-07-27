package com.debayan.ainotebook.domain.math

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

/** Numerically evaluates an [Expr]. Trigonometric functions use radians. */
object ExpressionEvaluator {

    /** @throws MathParseException if a variable is unbound or a function/operator is unsupported. */
    fun eval(expr: Expr, variables: Map<String, Double> = emptyMap()): Double = when (expr) {
        is Expr.Num -> expr.value
        is Expr.Var -> variables[expr.name]
            ?: throw MathParseException("Unknown variable: ${expr.name}")
        is Expr.Unary -> if (expr.op == '-') -eval(expr.operand, variables) else eval(expr.operand, variables)
        is Expr.Binary -> {
            val l = eval(expr.left, variables)
            val r = eval(expr.right, variables)
            when (expr.op) {
                '+' -> l + r
                '-' -> l - r
                '*' -> l * r
                '/' -> l / r
                '%' -> l % r
                '^' -> l.pow(r)
                else -> throw MathParseException("Unsupported operator: ${expr.op}")
            }
        }
        is Expr.Func -> {
            val x = eval(expr.arg, variables)
            when (expr.name) {
                "sqrt" -> sqrt(x)
                "abs" -> abs(x)
                "sin" -> sin(x)
                "cos" -> cos(x)
                "tan" -> tan(x)
                "asin" -> asin(x)
                "acos" -> acos(x)
                "atan" -> atan(x)
                "ln" -> ln(x)
                "log" -> log10(x)
                "exp" -> exp(x)
                "floor" -> floor(x)
                "ceil" -> ceil(x)
                "round" -> round(x)
                else -> throw MathParseException("Unknown function: ${expr.name}")
            }
        }
    }
}
