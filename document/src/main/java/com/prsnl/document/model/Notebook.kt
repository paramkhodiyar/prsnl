package com.prsnl.document.model

import kotlinx.serialization.Serializable

@Serializable
data class Notebook(
    val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val coverColor: Int,
    val coverStyle: String = "DEFAULT",
    val folderName: String = "General",
    val lastViewedPageIndex: Int = 0,
    val pages: List<String> = emptyList() // ordered page UUIDs
)
