package com.prsnl.storage

import androidx.room.Database
import androidx.room.RoomDatabase
import com.prsnl.storage.dao.FolderDao
import com.prsnl.storage.dao.NotebookDao
import com.prsnl.storage.dao.PageDao
import com.prsnl.storage.dao.WritingStatDao
import com.prsnl.storage.entity.FolderEntity
import com.prsnl.storage.entity.NotebookEntity
import com.prsnl.storage.entity.PageEntity
import com.prsnl.storage.entity.WritingStatEntity

@Database(
    entities = [FolderEntity::class, NotebookEntity::class, PageEntity::class, WritingStatEntity::class],
    version = 7,
    exportSchema = false
)
abstract class PrsnlDatabase : RoomDatabase() {
    abstract fun folderDao(): FolderDao
    abstract fun notebookDao(): NotebookDao
    abstract fun pageDao(): PageDao
    abstract fun writingStatDao(): WritingStatDao
}
