package com.debayan.ainotebook.domain.model.ai

/** Capability/size tier of a local model, used for automatic device-based recommendation. */
enum class ModelTier {
    COMPACT,
    BALANCED,
    HIGH_QUALITY,
    ;

    /** Higher rank = more capable/heavier. */
    val rank: Int get() = ordinal
}
