package com.debayan.ainotebook.domain.math

/** Parsed expression tree produced by [ExpressionParser]. */
sealed interface Expr {
    data class Num(val value: Double) : Expr
    data class Var(val name: String) : Expr
    data class Unary(val op: Char, val operand: Expr) : Expr
    data class Binary(val op: Char, val left: Expr, val right: Expr) : Expr
    data class Func(val name: String, val arg: Expr) : Expr
}
