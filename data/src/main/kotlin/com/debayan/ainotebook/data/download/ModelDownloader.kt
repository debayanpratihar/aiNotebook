package com.debayan.ainotebook.data.download

import com.debayan.ainotebook.core.dispatcher.DispatcherProvider
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest
import javax.inject.Inject

/**
 * Downloads a model file over HTTPS with **resume support** (HTTP Range against a `.part` file) and
 * verifies its SHA-256. The download is atomic: bytes accumulate in `<file>.part` and are renamed to
 * the final path only after the stream completes, so a crash never leaves a truncated model in place.
 */
class ModelDownloader @Inject constructor(
    private val client: OkHttpClient,
    private val dispatchers: DispatcherProvider,
) {

    /**
     * @param onProgress invoked (at most once per whole-percent change) with bytes downloaded so far
     * and the total. Suspends, so it doubles as a cooperative cancellation point.
     */
    suspend fun download(
        url: String,
        destination: File,
        expectedBytes: Long,
        onProgress: suspend (downloaded: Long, total: Long) -> Unit,
    ): File = withContext(dispatchers.io) {
        destination.parentFile?.mkdirs()
        val partFile = File(destination.parentFile, destination.name + PART_SUFFIX)
        var existing = if (partFile.exists()) partFile.length() else 0L

        val requestBuilder = Request.Builder().url(url)
        if (existing > 0) requestBuilder.header("Range", "bytes=$existing-")

        client.newCall(requestBuilder.build()).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Download failed (HTTP ${response.code})")
            }
            val partial = response.code == HTTP_PARTIAL
            if (!partial && existing > 0) {
                // Server ignored the Range header; start over.
                partFile.delete()
                existing = 0L
            }
            val body = response.body ?: throw IOException("Empty download body")
            val reportedLength = body.contentLength()
            val total = when {
                expectedBytes > 0 -> expectedBytes
                reportedLength >= 0 -> existing + reportedLength
                else -> -1L
            }

            var downloaded = existing
            var lastPercent = -1
            body.byteStream().use { input ->
                FileOutputStream(partFile, existing > 0).use { output ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        downloaded += read
                        if (total > 0) {
                            val percent = ((downloaded * 100) / total).toInt()
                            if (percent != lastPercent) {
                                lastPercent = percent
                                onProgress(downloaded, total)
                            }
                        }
                    }
                    output.flush()
                }
            }
        }

        if (destination.exists()) destination.delete()
        if (!partFile.renameTo(destination)) {
            throw IOException("Failed to finalize downloaded file")
        }
        destination
    }

    /** Returns true if [file]'s SHA-256 equals [expectedSha256]. A blank expectation is treated as
     *  "unverifiable but permitted" (the config controls what hash, if any, is published). */
    suspend fun verify(file: File, expectedSha256: String): Boolean = withContext(dispatchers.io) {
        if (expectedSha256.isBlank()) return@withContext true
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(BUFFER_SIZE)
            while (true) {
                currentCoroutineContext().ensureActive()
                val read = input.read(buffer)
                if (read == -1) break
                digest.update(buffer, 0, read)
            }
        }
        val hex = digest.digest().joinToString("") { byte -> "%02x".format(byte) }
        hex.equals(expectedSha256, ignoreCase = true)
    }

    private companion object {
        const val BUFFER_SIZE = 64 * 1024
        const val PART_SUFFIX = ".part"
        const val HTTP_PARTIAL = 206
    }
}
