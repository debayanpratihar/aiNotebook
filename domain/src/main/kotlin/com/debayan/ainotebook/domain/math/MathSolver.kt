package com.debayan.ainotebook.domain.math

import javax.inject.Inject
import kotlin.math.abs

/**
 * Offline math solver: recognizes and solves arithmetic, linear/quadratic equations, and polynomial
 * derivatives/integrals directly on-device — instantly, with no model download and no network. Runs
 * on any device (including 1–2 GB RAM) because it is pure arithmetic, not an LLM.
 *
 * [solve] returns null when the input is not something this engine handles (e.g. a word problem), so
 * callers can fall back to the language model.
 */
class MathSolver @Inject constructor() {

    fun solve(raw: String): MathSolution? {
        val normalized = MathNormalizer.normalize(raw)
        if (normalized.isBlank() || normalized.none { it.isDigit() || it == 'x' }) return null

        return runCatching {
            when {
                isIntegral(normalized) -> solveIntegral(normalized)
                isDerivative(normalized) -> solveDerivative(normalized)
                normalized.contains('=') -> solveEquation(normalized)
                else -> solveArithmetic(normalized)
            }
        }.getOrNull()
    }

    // --- Arithmetic ---------------------------------------------------------------------------

    private fun solveArithmetic(input: String): MathSolution? {
        val expr = ExpressionParser(input).parse()
        if (collectVars(expr).isNotEmpty()) return null // has an unknown → not a plain calculation
        val value = ExpressionEvaluator.eval(expr)
        if (value.isNaN() || value.isInfinite()) return null
        return MathSolution(
            type = MathProblemType.ARITHMETIC,
            normalizedInput = prettify(input),
            answer = MathFormat.number(value),
        )
    }

    // --- Equations ----------------------------------------------------------------------------

    private fun solveEquation(input: String): MathSolution? {
        val sides = input.split('=')
        if (sides.size != 2) return null
        val lhs = ExpressionParser(sides[0]).parse()
        val rhs = ExpressionParser(sides[1]).parse()
        val variables = collectVars(lhs) + collectVars(rhs)

        if (variables.isEmpty()) {
            val equal = abs(ExpressionEvaluator.eval(lhs) - ExpressionEvaluator.eval(rhs)) < 1e-9
            return MathSolution(
                type = MathProblemType.EQUATION,
                normalizedInput = prettify(input),
                answer = if (equal) "True" else "False",
            )
        }
        if (variables.size > 1) return null
        val v = variables.first()
        val poly = (Polynomial.fromExpr(lhs, v) ?: return null) - (Polynomial.fromExpr(rhs, v) ?: return null)
        val roots = poly.realRoots() ?: return null

        val answer = when {
            roots.isEmpty() -> "No real solution"
            else -> roots.distinctBy { MathFormat.number(it) }
                .joinToString(" or ") { "$v = ${MathFormat.number(it)}" }
        }
        val steps = buildList {
            add("${poly.format(v)} = 0")
            if (poly.degree == 2) {
                val a = polyCoeff(poly, 2); val b = polyCoeff(poly, 1); val c = polyCoeff(poly, 0)
                add("Discriminant = ${MathFormat.number(b * b - 4 * a * c)}")
            }
        }
        return MathSolution(MathProblemType.EQUATION, prettify(input), answer, steps)
    }

    // --- Calculus (polynomial) ----------------------------------------------------------------

    private fun solveDerivative(input: String): MathSolution? {
        val v = Regex("d\\s*/\\s*d([a-z])").find(input)?.groupValues?.get(1) ?: "x"
        val body = input
            .replace(Regex("d\\s*/\\s*d[a-z]"), " ")
            .replace("differentiate", " ")
            .replace("derivative", " ")
            .replace(Regex("\\bof\\b"), " ")
            .trim()
        if (body.isBlank()) return null
        val poly = Polynomial.fromExpr(ExpressionParser(body).parse(), v) ?: return null
        return MathSolution(
            type = MathProblemType.DERIVATIVE,
            normalizedInput = body,
            answer = "d/d$v (${poly.format(v)}) = ${poly.derivative().format(v)}",
        )
    }

    private fun solveIntegral(input: String): MathSolution? {
        val v = Regex("\\bd([a-z])\\b").find(input)?.groupValues?.get(1) ?: "x"
        val body = input
            .replace("integrate", " ")
            .replace("integral", " ")
            .replace(Regex("\\bd[a-z]\\b"), " ")
            .replace(Regex("\\bof\\b"), " ")
            .trim()
        if (body.isBlank()) return null
        val poly = Polynomial.fromExpr(ExpressionParser(body).parse(), v) ?: return null
        return MathSolution(
            type = MathProblemType.INTEGRAL,
            normalizedInput = body,
            answer = "∫ (${poly.format(v)}) d$v = ${poly.integral().format(v)} + C",
        )
    }

    // --- Helpers ------------------------------------------------------------------------------

    private fun isDerivative(s: String): Boolean =
        s.contains(Regex("d\\s*/\\s*d[a-z]")) || s.contains("derivative") || s.contains("differentiate")

    private fun isIntegral(s: String): Boolean =
        s.contains("integral") || s.contains("integrate")

    private fun collectVars(expr: Expr): Set<String> = when (expr) {
        is Expr.Num -> emptySet()
        is Expr.Var -> setOf(expr.name)
        is Expr.Unary -> collectVars(expr.operand)
        is Expr.Binary -> collectVars(expr.left) + collectVars(expr.right)
        is Expr.Func -> collectVars(expr.arg)
    }

    private fun polyCoeff(poly: Polynomial, exp: Int): Double = poly.terms[exp] ?: 0.0

    private fun prettify(ascii: String): String =
        ascii
            .replace("*", " × ")
            .replace("/", " ÷ ")
            .replace("+", " + ")
            .replace("=", " = ")
            .replace(Regex("(?<=[\\d)])\\s*-"), " - ")
            .replace(Regex("\\s+"), " ")
            .trim()
}
