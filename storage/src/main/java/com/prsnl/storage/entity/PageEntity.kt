package com.prsnl.storage.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "pages",
    foreignKeys = [
        ForeignKey(
            entity = NotebookEntity::class,
            parentColumns = ["id"],
            childColumns = ["notebookId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("notebookId")]
)
data class PageEntity(
    @PrimaryKey val id: String,
    val notebookId: String,
    val pageIndex: Int,
    val width: Float,
    val height: Float,
    val backgroundType: String,
    val lineSpacing: Float?,
    val colorLight: Int,
    val colorDark: Int,
    val pdfSourceRef: String?,
    val elementFilePath: String, // Relative path to .json payload file
    val schemaVersion: Int
)
