package com.debayan.ainotebook.data.export

import android.graphics.Bitmap
import android.graphics.Color
import com.debayan.ainotebook.data.ocr.StrokeBitmapRenderer
import com.debayan.ainotebook.domain.model.canvas.Stroke
import com.debayan.ainotebook.domain.model.export.ImageFormat
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

/** Renders a page's strokes to a PNG/JPEG file (content-cropped, white background). */
class ImageExporter @Inject constructor(
    private val renderer: StrokeBitmapRenderer,
) {
    fun export(strokes: List<Stroke>, format: ImageFormat, destination: File) {
        val bitmap = renderer.render(strokes) ?: blankBitmap()
        destination.parentFile?.mkdirs()
        try {
            BufferedOutputStream(FileOutputStream(destination)).use { stream ->
                val compressFormat = when (format) {
                    ImageFormat.PNG -> Bitmap.CompressFormat.PNG
                    ImageFormat.JPEG -> Bitmap.CompressFormat.JPEG
                }
                bitmap.compress(compressFormat, JPEG_QUALITY, stream)
            }
        } finally {
            bitmap.recycle()
        }
    }

    private fun blankBitmap(): Bitmap =
        Bitmap.createBitmap(BLANK_SIZE, BLANK_SIZE, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.WHITE) }

    private companion object {
        const val JPEG_QUALITY = 95
        const val BLANK_SIZE = 512
    }
}
