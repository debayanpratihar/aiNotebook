package com.debayan.ainotebook.domain.repository

import com.debayan.ainotebook.core.result.AppResult
import com.debayan.ainotebook.domain.model.handwriting.GlyphLibrary
import com.debayan.ainotebook.domain.model.handwriting.GlyphSample
import kotlinx.coroutines.flow.Flow

/**
 * Persistence for the user's recorded handwriting.
 *
 * This is the most personal data the app holds and it never leaves the device — it is excluded from
 * cloud requests entirely, since a handwriting model is a biometric-adjacent identifier and solving
 * a problem never requires it.
 */
interface GlyphRepository {

    /** The library, re-emitted whenever training changes it. */
    fun observeLibrary(): Flow<GlyphLibrary>

    /** A snapshot for synthesis, avoiding a Flow collection on the render path. */
    suspend fun library(): GlyphLibrary

    /** Samples recorded for one character, oldest first. */
    suspend fun samplesFor(glyph: String): List<GlyphSample>

    /**
     * Stores a sample. When the character already has [GlyphSample.TARGET_VARIANTS] samples, the
     * oldest is evicted so re-recording a glyph improves it rather than growing the library forever.
     */
    suspend fun save(sample: GlyphSample): AppResult<Unit>

    /** Deletes every sample for one character, so the user can re-record it from scratch. */
    suspend fun clearGlyph(glyph: String): AppResult<Unit>

    /** Deletes all recorded handwriting. Surfaced in Settings as a privacy control. */
    suspend fun clearAll(): AppResult<Unit>

    /** Measured forward lean of the user's hand, in degrees, derived from their samples. */
    suspend fun estimatedSlantDegrees(): Float
}
