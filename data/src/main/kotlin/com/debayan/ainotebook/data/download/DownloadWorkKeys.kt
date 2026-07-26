package com.debayan.ainotebook.data.download

/** Shared keys, tags, and names for the model-download WorkManager job. */
object DownloadWorkKeys {
    // Input data
    const val KEY_MODEL_ID = "model_id"
    const val KEY_URL = "url"
    const val KEY_FILE_NAME = "file_name"
    const val KEY_SHA256 = "sha256"
    const val KEY_SIZE = "size"
    const val KEY_NAME = "display_name"
    const val KEY_VERSION = "version"
    const val KEY_PROVIDER = "provider"
    const val KEY_TIER = "tier"
    const val KEY_MIN_RAM = "min_ram"
    const val KEY_REC_RAM = "rec_ram"

    // Progress / output data
    const val KEY_STATE = "state"
    const val KEY_PROGRESS = "progress"
    const val KEY_DOWNLOADED = "downloaded"
    const val KEY_TOTAL = "total"
    const val KEY_ERROR = "error"

    const val TAG = "model_download"
    const val MODEL_TAG_PREFIX = "modeldl:"

    fun uniqueName(modelId: String): String = "download_$modelId"
    fun modelTag(modelId: String): String = "$MODEL_TAG_PREFIX$modelId"
}
