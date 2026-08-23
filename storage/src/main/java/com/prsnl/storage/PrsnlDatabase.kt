package com.prsnl.storage

import androidx.room.Database
import androidx.room.RoomDatabase
import com.prsnl.storage.dao.FolderDao
import com.prsnl.storage.dao.NotebookDao
import com.prsnl.storage.dao.PageDao
import com.prsnl.storage.entity.FolderEntity
import com.prsnl.storage.entity.NotebookEntity
import com.prsnl.storage.entity.PageEntity

@Database(
    entities = [FolderEntity::class, NotebookEntity::class, PageEntity::class],
    version = 5,
    exportSchema = false
)
abstract class PrsnlDatabase : RoomDatabase() {
    abstract fun folderDao(): FolderDao
    abstract fun notebookDao(): NotebookDao
    abstract fun pageDao(): PageDao
}
