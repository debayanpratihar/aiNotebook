package com.debayan.ainotebook.domain.math

/** Parsed expression tree produced by [ExpressionParser]. */
sealed interface Expr {

    /**
     * A numeric literal. [exact] carries the rational the source text actually denoted when there
     * was one, because `0.1` has to stay one tenth: the nearest double is 0.1000000000000000055…,
     * and once that has been rounded there is no way to recover the tenth the user wrote.
     */
    data class Num(val value: Double, val exact: Rational? = null) : Expr {
        companion object {
            fun of(value: Rational): Num = Num(value.toDouble(), value)

            fun of(value: Long): Num = of(Rational.of(value))
        }
    }

    /**
     * A named irrational constant. Kept symbolic rather than folded into a literal so a derivative
     * reads `2cos(2x)` instead of `2.718282^x`, and so `pi` prints back as `pi`.
     */
    data class Const(val name: String, val value: Double) : Expr

    data class Var(val name: String) : Expr

    data class Unary(val op: Char, val operand: Expr) : Expr

    data class Binary(val op: Char, val left: Expr, val right: Expr) : Expr

    /**
     * A function application. Multi-argument so `nthroot(32, 5)` and `log(2, 8)` are expressible;
     * the single-argument constructor keeps the common case readable.
     */
    data class Func(val name: String, val args: List<Expr>) : Expr {
        constructor(name: String, arg: Expr) : this(name, listOf(arg))

        val arg: Expr get() = args.first()
    }
}

/** Free variables in this tree. Named constants such as `pi` are not variables. */
fun Expr.variables(): Set<String> = when (this) {
    is Expr.Num, is Expr.Const -> emptySet()
    is Expr.Var -> setOf(name)
    is Expr.Unary -> operand.variables()
    is Expr.Binary -> left.variables() + right.variables()
    is Expr.Func -> args.flatMapTo(mutableSetOf()) { it.variables() }
}

/** Whether [name] occurs anywhere in this tree. */
fun Expr.dependsOn(name: String): Boolean = name in variables()

/** How many times [name] occurs, which is how the solver tells `2x + 3x` from `2x`. */
fun Expr.occurrencesOf(name: String): Int = when (this) {
    is Expr.Num, is Expr.Const -> 0
    is Expr.Var -> if (this.name == name) 1 else 0
    is Expr.Unary -> operand.occurrencesOf(name)
    is Expr.Binary -> left.occurrencesOf(name) + right.occurrencesOf(name)
    is Expr.Func -> args.sumOf { it.occurrencesOf(name) }
}

/**
 * Whether this tree actually asks for a calculation. A lone literal does not: echoing `5 = 5` back
 * at someone who wrote a page number is worse than staying quiet.
 */
fun Expr.hasOperation(): Boolean = when (this) {
    is Expr.Num, is Expr.Const, is Expr.Var -> false
    is Expr.Unary -> operand.hasOperation()
    is Expr.Binary, is Expr.Func -> true
}
