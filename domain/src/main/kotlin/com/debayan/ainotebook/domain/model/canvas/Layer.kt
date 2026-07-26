package com.debayan.ainotebook.domain.model.canvas

/**
 * A drawing layer on a page. Strokes belong to a layer; layers can be reordered, hidden, and locked.
 * Every page has at least one layer (created with the page).
 */
data class Layer(
    val id: String,
    val pageId: String,
    val name: String,
    val orderIndex: Int,
    val visible: Boolean = true,
    val locked: Boolean = false,
    val opacity: Float = 1f,
) {
    companion object {
        const val DEFAULT_NAME: String = "Layer 1"
    }
}
