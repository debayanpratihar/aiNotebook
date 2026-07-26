package com.debayan.ainotebook.domain.model

/** How the app resolves light vs. dark appearance. */
enum class ThemeMode {
    /** Follow the Android system setting. */
    SYSTEM,

    /** Always light. */
    LIGHT,

    /** Always dark. */
    DARK,
}
