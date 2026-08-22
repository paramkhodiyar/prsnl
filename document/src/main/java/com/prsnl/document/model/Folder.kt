package com.prsnl.document.model

import kotlinx.serialization.Serializable

@Serializable
data class Folder(
    val id: String,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val color: Int = 0xFF4B5563.toInt()
)
