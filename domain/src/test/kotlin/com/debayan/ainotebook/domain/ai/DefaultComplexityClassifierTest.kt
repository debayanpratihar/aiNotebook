package com.debayan.ainotebook.domain.ai

import com.debayan.ainotebook.domain.math.MathSolver
import com.debayan.ainotebook.domain.model.ai.DeviceCapabilities
import com.debayan.ainotebook.domain.model.ai.ProblemAssessment
import com.debayan.ainotebook.domain.model.ai.ProblemKind
import com.debayan.ainotebook.domain.model.ai.SolveRoute
import com.debayan.ainotebook.domain.model.ai.SolveRoute.Unavailable.FixAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultComplexityClassifierTest {

    private val solver = MathSolver()

    // Typed as the interface so the pageContext default declared there applies.
    private val classifier: ComplexityClassifier = DefaultComplexityClassifier(solver)

    @Test
    fun assess_arithmetic_isTrivialAndAcceptedByTheOfflineEngine() {
        val result = classifier.assess("12 × 47")

        assertEquals(ProblemKind.ARITHMETIC, result.kind)
        assertNotNull(result.normalizedExpression)
        assertTrue(result.isTrivial)
        assertFalse(result.requiresSymbolicSolving)
    }

    @Test
    fun assess_linearEquation_isSymbolicAndAcceptedByTheOfflineEngine() {
        val result = classifier.assess("2x + 3 = 13")

        assertEquals(ProblemKind.ALGEBRA, result.kind)
        assertTrue(result.requiresSymbolicSolving)
        assertNotNull(result.normalizedExpression)
        assertFalse(result.isTrivial)
    }

    @Test
    fun assess_acceptedExpression_isTheStringTheEngineCanSolveAgain() {
        // Handwritten "x" as a multiplication sign: the expression that comes back has to be the one
        // the engine accepted, or the solve path would re-derive a different reading.
        val expression = classifier.assess("12 x 3").normalizedExpression

        assertNotNull(expression)
        assertEquals("36", solver.solve(expression!!)?.answer)
    }

    @Test
    fun assess_mathTheOfflineEngineCannotDo_carriesNoExpression() {
        // The engine only knows single-variable polynomials, so this reads as maths and is still
        // not something it can answer.
        val result = classifier.assess("∫ sin(x) dx")

        assertTrue(result.kind.isMathematical)
        assertNull(result.normalizedExpression)
    }

    @Test
    fun assess_prose_isNeverHandedToTheOfflineEngine() {
        val result = classifier.assess("Notes from the lecture on photosynthesis and chlorophyll.")

        assertNull(result.normalizedExpression)
        assertFalse(result.requiresSymbolicSolving)
    }

    @Test
    fun assess_blankReading_hasNothingToSolve() {
        val result = classifier.assess("   ")

        assertEquals(0f, result.complexity, 0f)
        assertEquals(0, result.estimatedPromptTokens)
        assertNull(result.normalizedExpression)
        assertTrue(result.rationale.isNotBlank())
    }

    @Test
    fun assess_alwaysExplainsItselfInOneSentence() {
        val readings = listOf(
            "12 × 47",
            "2x + 3 = 13",
            "d/dx x^2",
            "integrate sin(x) dx",
            "A train leaves Delhi at 3 pm travelling at 60 km/h. When does it arrive?",
            "Why is the sky blue?",
            "Notes from the lecture on photosynthesis.",
        )

        for (reading in readings) {
            val rationale = classifier.assess(reading).rationale
            assertTrue(reading, rationale.isNotBlank())
            assertTrue(reading, rationale.endsWith("."))
        }
    }

    @Test
    fun assess_nestingAndLengthPushComplexityUp() {
        val flat = classifier.assess("2 + 3 * 4")
        val nested = classifier.assess("((2 + 3) * (4 + 5)) * ((6 + 7) * (8 + 9))")

        assertTrue(nested.complexity > flat.complexity)
    }

    @Test
    fun assess_complexityStaysWithinRangeForPathologicalInput() {
        val monster = "(".repeat(200) + "x + 1 " + "2 + 3 * ".repeat(200)

        val complexity = classifier.assess(monster).complexity

        assertTrue(complexity in 0f..1f)
    }

    @Test
    fun assess_countsThePageContextItWillActuallySend() {
        val short = classifier.assess("Why?", "one line of notes")
        val long = classifier.assess("Why?", "notes ".repeat(200))

        assertTrue(long.estimatedPromptTokens > short.estimatedPromptTokens)
    }

    @Test
    fun assess_promptEstimateSaturatesAtTheContextCap() {
        val atCap = classifier.assess("Why?", "n".repeat(PromptBuilder.CLOUD_CONTEXT_CHARS))
        val wellPast = classifier.assess("Why?", "n".repeat(PromptBuilder.CLOUD_CONTEXT_CHARS * 10))

        assertEquals(atCap.estimatedPromptTokens, wellPast.estimatedPromptTokens)
    }

    @Test
    fun route_solvableMath_goesOfflineEvenWhenEverythingElseIsAvailable() {
        val route = classifier.route(
            assessment = assessment(ProblemKind.ARITHMETIC, expression = "12*47"),
            cloudAllowed = true,
            cloudProvidersAvailable = true,
            localModelAvailable = true,
            device = device(totalRamMb = 12_000),
        )

        assertEquals(SolveRoute.OfflineMath, route)
    }

    @Test
    fun route_mathTheOfflineEngineRejected_prefersTheCloud() {
        val route = classifier.route(
            assessment = assessment(ProblemKind.CALCULUS, expression = null),
            cloudAllowed = true,
            cloudProvidersAvailable = true,
            localModelAvailable = true,
            device = device(totalRamMb = 12_000),
        )

        assertTrue(route is SolveRoute.Cloud)
    }

    @Test
    fun route_expressionOnANonMathKind_neverReachesTheArithmeticEngine() {
        val route = classifier.route(
            assessment = assessment(ProblemKind.WORD_PROBLEM, expression = "40*3"),
            cloudAllowed = true,
            cloudProvidersAvailable = true,
            localModelAvailable = true,
            device = device(totalRamMb = 12_000),
        )

        assertTrue(route is SolveRoute.Cloud)
    }

    @Test
    fun route_withoutCloud_fallsBackToTheLocalModel() {
        val route = classifier.route(
            assessment = assessment(ProblemKind.FREEFORM_QUESTION),
            cloudAllowed = false,
            cloudProvidersAvailable = false,
            localModelAvailable = true,
            device = device(totalRamMb = 8_000),
        )

        assertEquals(SolveRoute.Local(DefaultComplexityClassifier.DEFAULT_LOCAL_BACKEND), route)
    }

    @Test
    fun route_heavyProblemOnALowMemoryDevice_refusesLocallyAndAsksForAKey() {
        val route = classifier.route(
            assessment = assessment(ProblemKind.WORD_PROBLEM, complexity = 0.8f),
            cloudAllowed = true,
            cloudProvidersAvailable = false,
            localModelAvailable = true,
            device = device(totalRamMb = LOW_MEMORY_DEVICE_RAM_MB),
        )

        val unavailable = route as SolveRoute.Unavailable
        assertEquals(FixAction.ADD_API_KEY, unavailable.fixAction)
        assertTrue(unavailable.reason.isNotBlank())
    }

    @Test
    fun route_lightProblemOnALowMemoryDevice_stillRunsLocally() {
        val route = classifier.route(
            assessment = assessment(ProblemKind.FREEFORM_QUESTION, complexity = 0.3f),
            cloudAllowed = true,
            cloudProvidersAvailable = false,
            localModelAvailable = true,
            device = device(totalRamMb = LOW_MEMORY_DEVICE_RAM_MB),
        )

        assertTrue(route is SolveRoute.Local)
    }

    @Test
    fun route_heavyProblemOnACapableDevice_runsLocally() {
        val route = classifier.route(
            assessment = assessment(ProblemKind.WORD_PROBLEM, complexity = 0.8f),
            cloudAllowed = true,
            cloudProvidersAvailable = false,
            localModelAvailable = true,
            device = device(totalRamMb = 8_000),
        )

        assertTrue(route is SolveRoute.Local)
    }

    @Test
    fun route_cloudConfiguredButSwitchedOff_andNoLocalModel_pointsAtTheKeySettings() {
        val route = classifier.route(
            assessment = assessment(ProblemKind.FREEFORM_QUESTION),
            cloudAllowed = false,
            cloudProvidersAvailable = true,
            localModelAvailable = false,
            device = device(totalRamMb = 8_000),
        )

        assertEquals(FixAction.ADD_API_KEY, (route as SolveRoute.Unavailable).fixAction)
    }

    @Test
    fun route_cloudAllowedButUnconfigured_andNoLocalModel_asksForAKey() {
        val route = classifier.route(
            assessment = assessment(ProblemKind.FREEFORM_QUESTION),
            cloudAllowed = true,
            cloudProvidersAvailable = false,
            localModelAvailable = false,
            device = device(totalRamMb = 8_000),
        )

        assertEquals(FixAction.ADD_API_KEY, (route as SolveRoute.Unavailable).fixAction)
    }

    @Test
    fun route_cloudDeclinedAndNothingInstalled_offersTheModelDownload() {
        val route = classifier.route(
            assessment = assessment(ProblemKind.FREEFORM_QUESTION),
            cloudAllowed = false,
            cloudProvidersAvailable = false,
            localModelAvailable = false,
            device = device(totalRamMb = 8_000),
        )

        val unavailable = route as SolveRoute.Unavailable
        assertEquals(FixAction.DOWNLOAD_MODEL, unavailable.fixAction)
        assertTrue(unavailable.reason.isNotBlank())
    }

    @Test
    fun route_everyDeadEndCarriesAWayOut() {
        val combinations = listOf(false, true).flatMap { cloudAllowed ->
            listOf(false, true).flatMap { providers ->
                listOf(false, true).map { local -> Triple(cloudAllowed, providers, local) }
            }
        }

        for ((cloudAllowed, providers, local) in combinations) {
            val route = classifier.route(
                assessment = assessment(ProblemKind.WORD_PROBLEM, complexity = 0.9f),
                cloudAllowed = cloudAllowed,
                cloudProvidersAvailable = providers,
                localModelAvailable = local,
                device = device(totalRamMb = LOW_MEMORY_DEVICE_RAM_MB),
            )
            if (route is SolveRoute.Unavailable) {
                assertTrue(route.reason.isNotBlank())
                assertFalse(route.fixAction == FixAction.NONE)
            }
        }
    }

    private fun assessment(
        kind: ProblemKind,
        complexity: Float = 0.4f,
        expression: String? = null,
    ) = ProblemAssessment(
        kind = kind,
        complexity = complexity,
        requiresSymbolicSolving = kind == ProblemKind.ALGEBRA || kind == ProblemKind.CALCULUS,
        estimatedPromptTokens = 240,
        normalizedExpression = expression,
        rationale = "Test assessment.",
    )

    private fun device(totalRamMb: Long) = DeviceCapabilities(
        totalRamMb = totalRamMb,
        availableRamMb = totalRamMb / 2,
        freeStorageBytes = 8_000_000_000,
        supportedAbis = listOf("arm64-v8a"),
        sdkInt = 34,
        cpuCores = 8,
    )

    private companion object {
        /** Just under [com.debayan.ainotebook.domain.model.ai.InferenceConfig.LOW_MEMORY_RAM_MB]. */
        const val LOW_MEMORY_DEVICE_RAM_MB = 4_000L
    }
}
