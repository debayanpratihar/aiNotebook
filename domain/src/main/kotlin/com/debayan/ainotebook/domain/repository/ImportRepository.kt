package com.debayan.ainotebook.domain.repository

import com.debayan.ainotebook.core.result.AppResult

/** Imports notebooks from files. Imported files are validated before any data is created. */
interface ImportRepository {

    /**
     * Imports a native `.ainb` package from [sourcePath] as a **new** notebook (never overwriting an
     * existing one) and returns its id. The source is validated; malformed archives fail cleanly.
     */
    suspend fun importNativePackage(sourcePath: String): AppResult<String>
}
