package com.debayan.ainotebook.data.remote.config

import com.debayan.ainotebook.core.AppConstants
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import javax.inject.Inject

/**
 * Fetches and parses the remote configuration files over HTTPS. Calls are blocking (synchronous
 * OkHttp) and must be invoked from an IO context by the repository.
 */
class ConfigService @Inject constructor(
    private val client: OkHttpClient,
    private val json: Json,
) {
    fun fetchConfig(): RemoteConfigDto = json.decodeFromString(body(CONFIG_FILE))

    fun fetchModels(): List<RemoteModelDto> =
        json.decodeFromString<ModelsResponseDto>(body(MODELS_FILE)).models

    fun fetchAnnouncements(): List<AnnouncementDto> =
        listOf(json.decodeFromString<AnnouncementDto>(body(ANNOUNCEMENTS_FILE)))

    fun fetchChangelog(): ChangelogResponseDto = json.decodeFromString(body(CHANGELOG_FILE))

    private fun body(fileName: String): String {
        val request = Request.Builder()
            .url(AppConstants.CONFIG_BASE_URL + fileName)
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Config request failed (HTTP ${response.code}) for $fileName")
            }
            return response.body?.string() ?: throw IOException("Empty config response for $fileName")
        }
    }

    private companion object {
        const val CONFIG_FILE = "config.json"
        const val MODELS_FILE = "models.json"
        const val ANNOUNCEMENTS_FILE = "announcements.json"
        const val CHANGELOG_FILE = "changelog.json"
    }
}
