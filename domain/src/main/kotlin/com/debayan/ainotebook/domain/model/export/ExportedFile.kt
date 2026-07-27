package com.debayan.ainotebook.domain.model.export

/** A file produced by an export, ready to be shared or opened. */
data class ExportedFile(
    val path: String,
    val mimeType: String,
)
