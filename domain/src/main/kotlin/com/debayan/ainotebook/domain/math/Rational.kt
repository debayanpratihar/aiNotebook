package com.debayan.ainotebook.domain.math

import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext

/**
 * An exact rational number, always stored reduced with a positive denominator.
 *
 * The engine computes in rationals wherever it can and drops to [Double] only for genuinely
 * irrational results. This is a credibility feature rather than a numerical nicety: a student who
 * sees `0.30000000000000004` for `0.1 + 0.2`, or `0.5000000000000001` for `1/3 + 1/6`, concludes the
 * app cannot add — and then stops trusting every other answer it gives, including the correct ones.
 *
 * Backed by [BigInteger] rather than [Long] because intermediate values grow fast once fractions are
 * combined (`1/7 + 1/11 + 1/13` already needs a four-digit denominator) and a silent overflow would
 * produce exactly the confidently-wrong answer this engine exists to avoid.
 */
class Rational private constructor(
    val numerator: BigInteger,
    val denominator: BigInteger,
) : Comparable<Rational> {

    val isZero: Boolean get() = numerator.signum() == 0

    val isInteger: Boolean get() = denominator == BigInteger.ONE

    val isOne: Boolean get() = isInteger && numerator == BigInteger.ONE

    val signum: Int get() = numerator.signum()

    /**
     * Whether this is worth showing as a fraction. Doubles converted to rationals are exact but
     * carry power-of-two denominators in the quadrillions, and nobody wants to read
     * `884279719003555/281474976710656` where `3.141593` was meant.
     */
    val isSimpleFraction: Boolean
        get() = numerator.abs() <= DISPLAY_LIMIT && denominator <= DISPLAY_LIMIT

    /**
     * Guard against runaway growth. Repeatedly squaring a double-derived rational doubles its bit
     * length each time, so the exact paths abandon a computation that exceeds this and let the
     * floating-point path answer instead — a slightly rounded answer beats a frozen canvas.
     */
    internal val isWithinBudget: Boolean
        get() = numerator.bitLength() <= MAX_BITS && denominator.bitLength() <= MAX_BITS

    operator fun plus(other: Rational): Rational = of(
        numerator * other.denominator + other.numerator * denominator,
        denominator * other.denominator,
    )

    operator fun minus(other: Rational): Rational = of(
        numerator * other.denominator - other.numerator * denominator,
        denominator * other.denominator,
    )

    operator fun times(other: Rational): Rational =
        of(numerator * other.numerator, denominator * other.denominator)

    /** @throws ArithmeticException on division by zero; use [divideOrNull] where that is reachable. */
    operator fun div(other: Rational): Rational {
        if (other.isZero) throw ArithmeticException("Division by zero")
        return of(numerator * other.denominator, denominator * other.numerator)
    }

    fun divideOrNull(other: Rational): Rational? = if (other.isZero) null else div(other)

    operator fun unaryMinus(): Rational = Rational(numerator.negate(), denominator)

    /** Truncated remainder, matching the `%` operator of the floating-point path. */
    fun remainderOrNull(other: Rational): Rational? {
        if (other.isZero) return null
        val quotient = div(other)
        return this - other * of(quotient.truncated())
    }

    fun abs(): Rational = if (signum < 0) -this else this

    fun reciprocalOrNull(): Rational? = if (isZero) null else of(denominator, numerator)

    /** @throws ArithmeticException when raising zero to a negative power. */
    fun pow(exponent: Int): Rational = when {
        exponent >= 0 -> of(numerator.pow(exponent), denominator.pow(exponent))
        isZero -> throw ArithmeticException("Zero to a negative power")
        else -> of(denominator.pow(-exponent), numerator.pow(-exponent))
    }

    /** Exact nth root, or null when it is irrational so callers can fall back to doubles. */
    fun nthRootOrNull(degree: Int): Rational? {
        if (degree <= 0) return null
        if (signum < 0 && degree % 2 == 0) return null
        val rootNumerator = NumberTheory.integerNthRoot(numerator.abs(), degree) ?: return null
        val rootDenominator = NumberTheory.integerNthRoot(denominator, degree) ?: return null
        return of(if (signum < 0) rootNumerator.negate() else rootNumerator, rootDenominator)
    }

    /** Rounds toward zero. */
    fun truncated(): BigInteger = numerator / denominator

    fun floor(): BigInteger {
        val parts = numerator.divideAndRemainder(denominator)
        return if (parts[1].signum() < 0) parts[0].subtract(BigInteger.ONE) else parts[0]
    }

    fun ceiling(): BigInteger = (-this).floor().negate()

    /** Half-up rounding, which is what handwritten working expects. */
    fun rounded(): BigInteger = (this + HALF).floor()

    fun toBigIntegerOrNull(): BigInteger? = if (isInteger) numerator else null

    fun toDouble(): Double =
        BigDecimal(numerator).divide(BigDecimal(denominator), MathContext.DECIMAL64).toDouble()

    /**
     * The exact decimal expansion when there is one, else null. Only denominators of the form
     * `2^a · 5^b` terminate, and the [maxDecimals] cap keeps `1/2^80` from printing 80 digits.
     */
    fun terminatingDecimalOrNull(maxDecimals: Int = MAX_DECIMALS): String? {
        if (isInteger) return numerator.toString()
        var residue = denominator
        while (residue.mod(BIG_TWO).signum() == 0) residue = residue.divide(BIG_TWO)
        while (residue.mod(BIG_FIVE).signum() == 0) residue = residue.divide(BIG_FIVE)
        if (residue != BigInteger.ONE) return null
        val decimal = BigDecimal(numerator).divide(BigDecimal(denominator))
        if (decimal.scale() > maxDecimals) return null
        return decimal.stripTrailingZeros().toPlainString()
    }

    override fun compareTo(other: Rational): Int =
        (numerator * other.denominator).compareTo(other.numerator * denominator)

    override fun equals(other: Any?): Boolean =
        other is Rational && numerator == other.numerator && denominator == other.denominator

    override fun hashCode(): Int = 31 * numerator.hashCode() + denominator.hashCode()

    override fun toString(): String = if (isInteger) "$numerator" else "$numerator/$denominator"

    companion object {
        val ZERO: Rational = Rational(BigInteger.ZERO, BigInteger.ONE)
        val ONE: Rational = Rational(BigInteger.ONE, BigInteger.ONE)
        val HUNDRED: Rational = Rational(BigInteger.valueOf(100L), BigInteger.ONE)

        fun of(value: Long): Rational = of(BigInteger.valueOf(value))

        fun of(value: BigInteger): Rational = Rational(value, BigInteger.ONE)

        fun of(numerator: Long, denominator: Long): Rational =
            of(BigInteger.valueOf(numerator), BigInteger.valueOf(denominator))

        /** @throws IllegalArgumentException when [denominator] is zero. */
        fun of(numerator: BigInteger, denominator: BigInteger): Rational {
            require(denominator.signum() != 0) { "Zero denominator" }
            val sign = denominator.signum()
            val signedNumerator = if (sign < 0) numerator.negate() else numerator
            val signedDenominator = denominator.abs()
            val divisor = signedNumerator.gcd(signedDenominator)
            if (divisor == BigInteger.ONE || divisor.signum() == 0) {
                return Rational(signedNumerator, signedDenominator)
            }
            return Rational(signedNumerator.divide(divisor), signedDenominator.divide(divisor))
        }

        /**
         * Reads a decimal literal exactly: `"0.1"` becomes one tenth, not the double nearest to it.
         * Returns null for anything that is not a plain unsigned-or-signed decimal.
         */
        fun parse(literal: String): Rational? {
            val text = literal.trim()
            if (text.isEmpty()) return null
            val negative = text.startsWith('-')
            val body = text.removePrefix("-").removePrefix("+")
            if (body.isEmpty() || body.count { it == '.' } > 1) return null
            if (body.any { !it.isDigit() && it != '.' }) return null
            val dot = body.indexOf('.')
            val digits = if (dot < 0) body else body.removeRange(dot, dot + 1)
            if (digits.isEmpty()) return null
            val scale = if (dot < 0) 0 else body.length - dot - 1
            val value = of(BigInteger(digits), BigInteger.TEN.pow(scale))
            return if (negative) -value else value
        }

        /**
         * The exact rational a [Double] represents. Every finite double *is* a dyadic rational, so
         * this loses nothing — but the denominators are large, which is why display paths check
         * [isSimpleFraction] before printing a fraction.
         */
        fun fromDouble(value: Double): Rational? {
            if (value.isNaN() || value.isInfinite()) return null
            val decimal = BigDecimal(value)
            if (decimal.scale() <= 0) return of(decimal.toBigInteger())
            return of(decimal.unscaledValue(), BigInteger.TEN.pow(decimal.scale()))
        }

        private val HALF = Rational(BigInteger.ONE, BigInteger.valueOf(2L))
        private val BIG_TWO = BigInteger.valueOf(2L)
        private val BIG_FIVE = BigInteger.valueOf(5L)
        private val DISPLAY_LIMIT = BigInteger.valueOf(1_000_000_000L)
        private const val MAX_BITS = 1024
        private const val MAX_DECIMALS = 12
    }
}
