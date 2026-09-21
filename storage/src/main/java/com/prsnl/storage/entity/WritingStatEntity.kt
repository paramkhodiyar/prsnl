package com.prsnl.storage.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "writing_stats",
    primaryKeys = ["date", "notebookId"],
    indices = [
        Index(value = ["date"]),
        Index(value = ["notebookId"]),
        Index(value = ["folderName"])
    ]
)
data class WritingStatEntity(
    val date: String,            // yyyy-MM-dd
    val notebookId: String,
    val folderName: String,
    val strokeCount: Int,
    val inkLengthUnits: Float
)
