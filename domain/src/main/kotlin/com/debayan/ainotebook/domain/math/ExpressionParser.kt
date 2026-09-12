package com.debayan.ainotebook.domain.math

/** Thrown when a string cannot be parsed as a math expression. */
class MathParseException(message: String) : Exception(message)

/**
 * A small recursive-descent parser for arithmetic/algebraic expressions.
 *
 * Grammar (lowest to highest precedence):
 * ```
 * expression := term (('+' | '-') term)*
 * term       := unary (('*' | '/' | '%') unary | implicit-unary)*
 * unary      := ('+' | '-') unary | power
 * power      := primary ('^' unary)?          // right-associative
 * primary    := number | constant | func '(' arguments ')' | '(' expression ')' | variable
 * arguments  := expression (',' expression)*
 * ```
 * Implicit multiplication (`2x`, `3(4)`, `2sqrt(9)`) is supported. Constants `pi`/`e` become
 * [Expr.Const] so they survive differentiation and printing as names. Unknown identifiers become
 * variables (used by the polynomial/equation paths).
 *
 * Numeric literals keep the rational they were written as alongside their double value: dropping
 * that is what makes `0.1 + 0.2` answer `0.30000000000000004`.
 */
class ExpressionParser(input: String) {

    private sealed interface Token {
        data class Number(val value: Double, val text: String) : Token
        data class Ident(val name: String) : Token
        data class Op(val ch: Char) : Token
        data object LParen : Token
        data object RParen : Token
        data object Comma : Token
    }

    private val tokens: List<Token> = tokenize(input)
    private var pos = 0

    fun parse(): Expr {
        if (tokens.isEmpty()) throw MathParseException("Empty expression")
        val expr = parseExpression()
        if (pos != tokens.size) throw MathParseException("Unexpected trailing input")
        return expr
    }

    private fun peek(): Token? = tokens.getOrNull(pos)
    private fun next(): Token = tokens.getOrNull(pos++) ?: throw MathParseException("Unexpected end")

    private fun parseExpression(): Expr {
        var left = parseTerm()
        while (true) {
            val t = peek()
            if (t is Token.Op && (t.ch == '+' || t.ch == '-')) {
                next()
                left = Expr.Binary(t.ch, left, parseTerm())
            } else {
                break
            }
        }
        return left
    }

    private fun parseTerm(): Expr {
        var left = parseUnary()
        while (true) {
            when (val t = peek()) {
                is Token.Op -> if (t.ch == '*' || t.ch == '/' || t.ch == '%') {
                    next()
                    left = Expr.Binary(t.ch, left, parseUnary())
                } else {
                    return left
                }
                // Implicit multiplication: a factor immediately followed by another factor.
                is Token.Number, is Token.Ident, Token.LParen -> left = Expr.Binary('*', left, parseUnary())
                else -> return left
            }
        }
    }

    private fun parseUnary(): Expr {
        val t = peek()
        if (t is Token.Op && (t.ch == '+' || t.ch == '-')) {
            next()
            return Expr.Unary(t.ch, parseUnary())
        }
        return parsePower()
    }

    private fun parsePower(): Expr {
        val base = parsePrimary()
        val t = peek()
        if (t is Token.Op && t.ch == '^') {
            next()
            return Expr.Binary('^', base, parseUnary()) // right-associative, signed exponent allowed
        }
        return base
    }

    private fun parsePrimary(): Expr = when (val t = next()) {
        is Token.Number -> Expr.Num(t.value, Rational.parse(t.text))
        Token.LParen -> parseExpression().also { expect(Token.RParen) }
        is Token.Ident -> if (peek() == Token.LParen) {
            next()
            Expr.Func(t.name, parseArguments())
        } else {
            when (t.name) {
                "pi" -> Expr.Const("pi", kotlin.math.PI)
                "e" -> Expr.Const("e", kotlin.math.E)
                else -> Expr.Var(t.name)
            }
        }
        else -> throw MathParseException("Unexpected token: $t")
    }

    private fun parseArguments(): List<Expr> {
        val args = mutableListOf(parseExpression())
        while (peek() == Token.Comma) {
            next()
            args += parseExpression()
        }
        expect(Token.RParen)
        return args
    }

    private fun expect(token: Token) {
        if (next() != token) throw MathParseException("Expected $token")
    }

    private fun tokenize(input: String): List<Token> {
        val result = mutableListOf<Token>()
        var i = 0
        while (i < input.length) {
            val c = input[i]
            when {
                c.isWhitespace() -> i++
                c.isDigit() || c == '.' -> {
                    val start = i
                    while (i < input.length && (input[i].isDigit() || input[i] == '.')) i++
                    val text = input.substring(start, i)
                    val value = text.toDoubleOrNull() ?: throw MathParseException("Bad number: $text")
                    result += Token.Number(value, text)
                }
                c.isLetter() -> {
                    val start = i
                    while (i < input.length && input[i].isLetter()) i++
                    result += Token.Ident(input.substring(start, i))
                }
                c == '+' || c == '-' || c == '*' || c == '/' || c == '%' || c == '^' -> {
                    result += Token.Op(c)
                    i++
                }
                c == '(' || c == '[' || c == '{' -> {
                    result += Token.LParen
                    i++
                }
                c == ')' || c == ']' || c == '}' -> {
                    result += Token.RParen
                    i++
                }
                c == ',' -> {
                    result += Token.Comma
                    i++
                }
                else -> throw MathParseException("Unexpected character: '$c'")
            }
        }
        return result
    }
}
