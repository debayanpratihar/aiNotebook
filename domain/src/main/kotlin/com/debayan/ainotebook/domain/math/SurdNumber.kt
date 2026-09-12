package com.debayan.ainotebook.domain.math

import java.math.BigInteger
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * A number of the form `a + b√r` with integer `r` — exactly the shape a simplified root and a
 * quadratic root both take, which is why the engine carries it instead of collapsing to a double.
 * `√8` has to print as `2√2` and `x² - 2x - 1 = 0` has to give `1 + √2`; `2.828427` and `2.414214`
 * are answers a marker would take marks off for.
 *
 * Closed under +, -, ×, ÷ and integer powers *within one radicand*. Mixing radicands (`√2 + √3`)
 * leaves the field, so those operations return null and the caller falls back to decimals rather
 * than inventing a simplification that does not exist.
 */
internal class SurdNumber private constructor(
    val rational: Rational,
    val coefficient: Rational,
    val radicand: BigInteger,
) {

    val isRational: Boolean get() = coefficient.isZero

    val signum: Int
        get() {
            if (isRational) return rational.signum
            val value = toDouble()
            return if (value > 0.0) 1 else if (value < 0.0) -1 else 0
        }

    /** Whether both parts are small enough to read; large ones are better shown as a decimal. */
    val isPresentable: Boolean
        get() = rational.isSimpleFraction && coefficient.isSimpleFraction && radicand <= RADICAND_LIMIT

    operator fun plus(other: SurdNumber): SurdNumber? = when {
        isRational -> create(rational + other.rational, other.coefficient, other.radicand)
        other.isRational -> create(rational + other.rational, coefficient, radicand)
        radicand == other.radicand ->
            create(rational + other.rational, coefficient + other.coefficient, radicand)
        else -> null
    }

    operator fun minus(other: SurdNumber): SurdNumber? = plus(-other)

    operator fun unaryMinus(): SurdNumber = create(-rational, -coefficient, radicand)

    operator fun times(other: SurdNumber): SurdNumber? = when {
        isRational -> create(rational * other.rational, rational * other.coefficient, other.radicand)
        other.isRational -> create(rational * other.rational, coefficient * other.rational, radicand)
        radicand == other.radicand -> create(
            rational * other.rational + coefficient * other.coefficient * Rational.of(radicand),
            rational * other.coefficient + coefficient * other.rational,
            radicand,
        )
        else -> null
    }

    operator fun div(other: SurdNumber): SurdNumber? {
        if (other.isRational) {
            if (other.rational.isZero) return null
            return create(rational / other.rational, coefficient / other.rational, radicand)
        }
        // Rationalize the denominator: (a + b√r)/(c + d√r) × (c - d√r)/(c - d√r).
        val conjugate = create(other.rational, -other.coefficient, other.radicand)
        val denominator = (other * conjugate) ?: return null
        if (!denominator.isRational || denominator.rational.isZero) return null
        val numerator = (this * conjugate) ?: return null
        return numerator / of(denominator.rational)
    }

    fun pow(exponent: Int): SurdNumber? {
        if (exponent == 0) return of(Rational.ONE)
        if (abs(exponent) > MAX_EXPONENT) return null
        if (exponent < 0) {
            val positive = pow(-exponent) ?: return null
            return of(Rational.ONE) / positive
        }
        var result = of(Rational.ONE)
        repeat(exponent) {
            result = (result * this) ?: return null
            if (!result.isWithinBudget) return null
        }
        return result
    }

    /** Square root, defined only on the rational members — `√(1 + √2)` is outside this field. */
    fun sqrtOrNull(): SurdNumber? = if (isRational) sqrtOf(rational) else null

    fun toDouble(): Double =
        rational.toDouble() + coefficient.toDouble() * sqrt(radicand.toDouble())

    val isWithinBudget: Boolean
        get() = rational.isWithinBudget && coefficient.isWithinBudget && radicand.bitLength() <= MAX_RADICAND_BITS

    /** Linear text, safe to hand to the handwriting renderer: `2√2`, `1 + √2`, `-1/2 - √3/2`. */
    fun format(): String {
        if (isRational) return MathFormat.rational(rational)
        if (!isPresentable) return MathFormat.number(toDouble())
        val radical = radicalText(coefficient)
        if (rational.isZero) return radical
        val sign = if (coefficient.signum < 0) "-" else "+"
        return "${MathFormat.rational(rational)} $sign ${radicalText(coefficient.abs())}"
    }

    /** The magnitude wrapped so it reads unambiguously next to an `i` or another factor. */
    fun formatAsFactor(): String {
        val text = format()
        return if (text.any { it in "+-/ " }) "($text)" else text
    }

    private fun radicalText(value: Rational): String {
        val sign = if (value.signum < 0) "-" else ""
        val magnitude = value.abs()
        val scale = if (magnitude.numerator == BigInteger.ONE) "" else magnitude.numerator.toString()
        val radical = "$sign$scale√$radicand"
        return if (magnitude.isInteger) radical else "$radical/${magnitude.denominator}"
    }

    override fun toString(): String = format()

    companion object {
        fun of(value: Rational): SurdNumber = SurdNumber(value, Rational.ZERO, BigInteger.ONE)

        /**
         * `√value` as `c√r` with `r` carrying only the square-free part, or null for a negative
         * argument — imaginary results are the equation solver's business, not this type's.
         */
        fun sqrtOf(value: Rational): SurdNumber? {
            if (value.signum < 0) return null
            if (value.isZero) return of(Rational.ZERO)
            // √(n/d) = √(n·d)/d keeps the radicand a whole number, which is what simplifies.
            val (outside, inside) = NumberTheory.extractSquare(value.numerator * value.denominator)
            return create(Rational.ZERO, Rational.of(outside, value.denominator), inside)
        }

        private fun create(rational: Rational, coefficient: Rational, radicand: BigInteger): SurdNumber {
            require(radicand.signum() > 0) { "Non-positive radicand" }
            return when {
                coefficient.isZero -> SurdNumber(rational, Rational.ZERO, BigInteger.ONE)
                radicand == BigInteger.ONE -> SurdNumber(rational + coefficient, Rational.ZERO, BigInteger.ONE)
                else -> SurdNumber(rational, coefficient, radicand)
            }
        }

        private val RADICAND_LIMIT = BigInteger.valueOf(1_000_000_000L)
        private const val MAX_RADICAND_BITS = 64
        private const val MAX_EXPONENT = 64
    }
}
