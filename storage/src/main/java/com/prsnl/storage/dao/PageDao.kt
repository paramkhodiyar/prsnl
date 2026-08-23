package com.prsnl.storage.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.prsnl.storage.entity.PageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PageDao {
    @Query("SELECT * FROM pages WHERE notebookId = :notebookId ORDER BY pageIndex ASC")
    fun getPagesForNotebook(notebookId: String): Flow<List<PageEntity>>

    @Query("SELECT * FROM pages WHERE notebookId = :notebookId ORDER BY pageIndex ASC")
    suspend fun getPagesForNotebookSync(notebookId: String): List<PageEntity>

    @Query("SELECT * FROM pages WHERE id = :id LIMIT 1")
    suspend fun getPageById(id: String): PageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPage(page: PageEntity)

    @Update
    suspend fun updatePage(page: PageEntity)

    @Delete
    suspend fun deletePage(page: PageEntity)
}
