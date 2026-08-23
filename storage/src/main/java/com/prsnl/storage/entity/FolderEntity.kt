package com.prsnl.storage.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey val id: String,
    val name: String,
    val createdAt: Long,
    val color: Int,
    val isLocked: Boolean = false,
    val pin: String? = null,
    val securityQuestion: String? = null,
    val securityAnswerHash: String? = null
)
