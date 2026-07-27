package com.debayan.ainotebook.domain.model.export

/** User-selectable export format. */
enum class ExportFormat {
    /** Lossless native `.ainb` package (full notebook). */
    NATIVE_PACKAGE,

    /** Multi-page PDF of the whole notebook. */
    PDF,

    /** PNG image of the notebook's first page. */
    PNG,

    /** JPEG image of the notebook's first page. */
    JPEG,
}

/** Raster image format for image exports. */
enum class ImageFormat {
    PNG,
    JPEG,
}
