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
    val coverStyle: String = "DEFAULT",
    val folderName: String = "General",
    val lastViewedPageIndex: Int = 0,
    val pagesJson: String // serialized JSON list of page UUIDs
)
