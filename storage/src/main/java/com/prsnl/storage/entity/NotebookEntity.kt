package com.prsnl.storage.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notebooks")
data class NotebookEntity(
    @PrimaryKey val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val coverColor: Int,
    val coverStyle: String,
    val folderName: String = "General",
    val pageIdsJson: String // Serialized JSON list of Page IDs
)
