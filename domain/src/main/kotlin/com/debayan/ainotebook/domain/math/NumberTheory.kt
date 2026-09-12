package com.debayan.ainotebook.domain.math

import java.math.BigInteger

/**
 * Integer machinery the exact paths lean on: roots, factorization, divisors, and the square
 * extraction that turns `sqrt(8)` into `2√2`.
 *
 * Every search here is bounded, because this runs synchronously on the solve path of a phone that
 * may have 2 GB of RAM: factoring a forty-digit semiprime is not worth a frozen canvas. Each entry
 * point either returns null or leaves the stubborn cofactor intact, so callers degrade to a decimal
 * answer rather than hanging or guessing.
 */
internal object NumberTheory {

    fun gcd(first: BigInteger, second: BigInteger): BigInteger = first.gcd(second)

    fun lcm(first: BigInteger, second: BigInteger): BigInteger {
        if (first.signum() == 0 || second.signum() == 0) return BigInteger.ZERO
        return first.multiply(second).abs().divide(first.gcd(second))
    }

    /**
     * Exact integer nth root of [value], or null when [value] is not a perfect nth power.
     *
     * Newton's method from an upper bound rather than `BigInteger.sqrt`, which needs API 31 and only
     * covers the square case anyway.
     */
    fun integerNthRoot(value: BigInteger, degree: Int): BigInteger? {
        if (degree <= 0 || value.signum() < 0) return null
        if (value.signum() == 0) return BigInteger.ZERO
        if (degree == 1) return value
        val degreeBig = BigInteger.valueOf(degree.toLong())
        val degreeMinusOne = BigInteger.valueOf((degree - 1).toLong())
        var estimate = BigInteger.ONE.shiftLeft((value.bitLength() + degree - 1) / degree)
        while (true) {
            val next = degreeMinusOne.multiply(estimate)
                .add(value.divide(estimate.pow(degree - 1)))
                .divide(degreeBig)
            if (next >= estimate) break
            estimate = next
        }
        return if (estimate.pow(degree) == value) estimate else null
    }

    /**
     * Splits a positive [value] into `outside² × inside`, pulling out every square factor the trial
     * bound can reach and testing the remaining cofactor for being a perfect square itself, so
     * `72 = 6² × 2` and the caller can print `6√2`.
     */
    fun extractSquare(value: BigInteger): Pair<BigInteger, BigInteger> {
        if (value.signum() <= 0) return BigInteger.ONE to value
        var remaining = value
        var outside = BigInteger.ONE
        var inside = BigInteger.ONE
        var candidate = TWO
        while (candidate.multiply(candidate) <= remaining && candidate <= TRIAL_LIMIT) {
            var exponent = 0
            while (remaining.mod(candidate).signum() == 0) {
                remaining = remaining.divide(candidate)
                exponent++
            }
            if (exponent > 0) {
                outside = outside.multiply(candidate.pow(exponent / 2))
                if (exponent % 2 == 1) inside = inside.multiply(candidate)
            }
            candidate = if (candidate == TWO) THREE else candidate.add(TWO)
        }
        if (remaining > BigInteger.ONE) {
            val root = integerNthRoot(remaining, 2)
            if (root != null) outside = outside.multiply(root) else inside = inside.multiply(remaining)
        }
        return outside to inside
    }

    /**
     * Prime factors with multiplicity, ascending, or null when [value] could not be factored within
     * the trial bound. Null rather than a partial answer: "360 = 2^3 × 3^2 × 5" is useful and
     * "360 = 2^3 × 45" is wrong.
     */
    fun factorize(value: BigInteger): List<Pair<BigInteger, Int>>? {
        if (value.signum() <= 0) return null
        if (value == BigInteger.ONE) return emptyList()
        var remaining = value
        val factors = mutableListOf<Pair<BigInteger, Int>>()
        var candidate = TWO
        while (candidate.multiply(candidate) <= remaining && candidate <= TRIAL_LIMIT) {
            var exponent = 0
            while (remaining.mod(candidate).signum() == 0) {
                remaining = remaining.divide(candidate)
                exponent++
            }
            if (exponent > 0) factors += candidate to exponent
            candidate = if (candidate == TWO) THREE else candidate.add(TWO)
        }
        if (remaining > BigInteger.ONE) {
            if (!remaining.isProbablePrime(PRIME_CERTAINTY)) return null
            factors += remaining to 1
        }
        return factors
    }

    fun isPrime(value: BigInteger): Boolean =
        value >= TWO && value.isProbablePrime(PRIME_CERTAINTY)

    /**
     * Every positive divisor of [value], ascending, or null when [value] is too large to enumerate
     * cheaply. Used by the rational root search, which needs the divisors of the constant and
     * leading coefficients.
     */
    fun divisors(value: BigInteger): List<BigInteger>? {
        val magnitude = value.abs()
        if (magnitude.signum() == 0 || magnitude > DIVISOR_LIMIT) return null
        val target = magnitude.toLong()
        val ascending = mutableListOf<Long>()
        val descending = mutableListOf<Long>()
        var candidate = 1L
        while (candidate * candidate <= target) {
            if (target % candidate == 0L) {
                ascending += candidate
                val paired = target / candidate
                if (paired != candidate) descending += paired
            }
            candidate++
        }
        descending.reverse()
        return (ascending + descending).map { BigInteger.valueOf(it) }
    }

    private val TWO = BigInteger.valueOf(2L)
    private val THREE = BigInteger.valueOf(3L)
    private val TRIAL_LIMIT = BigInteger.valueOf(100_000L)
    private val DIVISOR_LIMIT = BigInteger.valueOf(1_000_000_000L)
    private const val PRIME_CERTAINTY = 40
}
