package com.debayan.ainotebook.domain.math

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MathSolverTest {

    private val solver = MathSolver()

    private fun answer(input: String): String? = solver.solve(input)?.answer

    @Test
    fun arithmetic_respectsPrecedenceAndParentheses() {
        assertEquals("14", answer("2 + 3 * 4"))
        assertEquals("20", answer("(2 + 3) * 4"))
        assertEquals("8", answer("2 ^ 3"))
        assertEquals("3", answer("sqrt(9)"))
    }

    @Test
    fun arithmetic_handlesOcrNoise() {
        assertEquals("36", answer("12 × 3")) // unicode multiply
        assertEquals("36", answer("12 x 3")) // handwritten x as multiply
        assertEquals("4", answer("what is 8 ÷ 2?"))
        assertEquals("1000", answer("1,000"))
        assertEquals("0.5", answer("50%"))
    }

    @Test
    fun arithmetic_superscriptExponent() {
        assertEquals("25", answer("5²"))
    }

    @Test
    fun equation_linear() {
        assertEquals("x = 5", answer("2x + 3 = 13"))
    }

    @Test
    fun equation_quadratic_twoRoots() {
        val result = answer("x^2 - 5x + 6 = 0")
        assertTrue(result == "x = 3 or x = 2" || result == "x = 2 or x = 3")
    }

    @Test
    fun equation_noRealSolution() {
        assertEquals("No real solution", answer("x^2 + 1 = 0"))
    }

    @Test
    fun derivative_polynomial() {
        assertEquals("d/dx (x^2 + 3x) = 2x + 3", answer("d/dx x^2 + 3x"))
    }

    @Test
    fun integral_polynomial() {
        assertEquals("∫ (2x) dx = x^2 + C", answer("integral 2x dx"))
    }

    @Test
    fun nonMath_returnsNull() {
        assertNull(solver.solve("explain photosynthesis"))
        assertNull(solver.solve(""))
        assertNull(solver.solve("hello world"))
    }
}
