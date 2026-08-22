package com.prsnl.document.model

import kotlinx.serialization.Serializable

@Serializable
data class Page(
    val id: String,
    val notebookId: String,
    val index: Int,
    val width: Float,
    val height: Float,
    val background: Background,
    val elements: List<Element> = emptyList(),
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 1
    }
}
