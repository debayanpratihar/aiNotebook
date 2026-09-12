package com.debayan.ainotebook.domain.model.handwriting

/**
 * One character to collect during handwriting training, plus the guidance shown with it.
 *
 * [example] exists because several required symbols are ones users cannot name on sight; showing
 * "sum" next to `Σ` removes the guesswork that otherwise produces garbage samples.
 */
data class TrainingGlyph(
    val glyph: String,
    val label: String,
    val example: String? = null,
)

/** A named run of characters, so training can be done in short sittings instead of one long slog. */
data class TrainingStage(
    val id: String,
    val title: String,
    val subtitle: String,
    val glyphs: List<TrainingGlyph>,
) {
    val size: Int get() = glyphs.size
}

/**
 * The full set of characters the app asks the user to write, split into stages.
 *
 * Ordering is deliberate: digits and operators come first because they unlock the offline math
 * engine — the one path that answers instantly on any device — so a user who abandons training
 * after two minutes still gets a working app. Letters follow, and the rarely-written symbols come
 * last where skipping them costs the least.
 */
object TrainingPlan {

    val digits = TrainingStage(
        id = "digits",
        title = "Numbers",
        subtitle = "These unlock instant offline answers for calculations.",
        glyphs = ('0'..'9').map { TrainingGlyph(it.toString(), it.toString()) },
    )

    val operators = TrainingStage(
        id = "operators",
        title = "Math operators",
        subtitle = "Write these the way you normally would in a calculation.",
        glyphs = listOf(
            TrainingGlyph("+", "plus"),
            TrainingGlyph("-", "minus"),
            TrainingGlyph("×", "times", "3 × 4"),
            TrainingGlyph("÷", "divided by", "8 ÷ 2"),
            TrainingGlyph("=", "equals"),
            TrainingGlyph("<", "less than"),
            TrainingGlyph(">", "greater than"),
            TrainingGlyph("(", "open bracket"),
            TrainingGlyph(")", "close bracket"),
            TrainingGlyph("[", "open square bracket"),
            TrainingGlyph("]", "close square bracket"),
            TrainingGlyph("/", "slash or fraction bar", "3/4"),
            TrainingGlyph(".", "decimal point", "1.5"),
            TrainingGlyph(",", "comma"),
            TrainingGlyph("^", "to the power of", "x^2"),
            TrainingGlyph("_", "subscript", "a_1"),
            TrainingGlyph("%", "percent"),
        ),
    )

    val lowercase = TrainingStage(
        id = "lowercase",
        title = "Small letters",
        subtitle = "Write each letter as you would inside a word.",
        glyphs = ('a'..'z').map { TrainingGlyph(it.toString(), it.toString()) },
    )

    val uppercase = TrainingStage(
        id = "uppercase",
        title = "Capital letters",
        subtitle = "Almost done — these are used to start sentences.",
        glyphs = ('A'..'Z').map { TrainingGlyph(it.toString(), it.toString()) },
    )

    val advancedSymbols = TrainingStage(
        id = "advanced",
        title = "Advanced symbols",
        subtitle = "Optional. Skip any you never write; answers will use typed text instead.",
        glyphs = listOf(
            TrainingGlyph("√", "square root", "√9"),
            TrainingGlyph("π", "pi", "3.14159"),
            TrainingGlyph("Σ", "sum", "Σ x"),
            TrainingGlyph("∫", "integral", "∫ x dx"),
            TrainingGlyph("α", "alpha"),
            TrainingGlyph("β", "beta"),
            TrainingGlyph("γ", "gamma"),
            TrainingGlyph("θ", "theta", "sin θ"),
            TrainingGlyph("→", "arrow / gives", "x → 0"),
            TrainingGlyph("≤", "less than or equal"),
            TrainingGlyph("≥", "greater than or equal"),
            TrainingGlyph("≠", "not equal"),
            TrainingGlyph("∞", "infinity"),
            TrainingGlyph("°", "degrees", "90°"),
        ),
    )

    /** Stages in the order the training flow presents them. */
    val stages: List<TrainingStage> = listOf(digits, operators, lowercase, uppercase, advancedSymbols)

    /**
     * Characters required before handwritten replies are offered at all. Below this the output would
     * fall back to typed text so often that offering it would only disappoint.
     */
    val essentialGlyphs: Set<String> =
        (digits.glyphs + operators.glyphs + lowercase.glyphs).map { it.glyph }.toSet()

    val allGlyphs: List<TrainingGlyph> = stages.flatMap { it.glyphs }

    val totalGlyphCount: Int = allGlyphs.size

    /** The space character is synthesized from metrics, never drawn, so it is never collected. */
    const val SPACE: String = " "
}
