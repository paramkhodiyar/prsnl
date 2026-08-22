package com.prsnl.document.repository

import com.prsnl.document.model.Notebook
import com.prsnl.document.model.Page
import kotlinx.coroutines.flow.Flow

interface NotebookRepository {
    fun getAllNotebooks(): Flow<List<Notebook>>
    suspend fun getNotebookById(id: String): Notebook?
    suspend fun saveNotebook(notebook: Notebook)
    suspend fun deleteNotebook(id: String)

    fun getPagesForNotebook(notebookId: String): Flow<List<Page>>
    suspend fun getPageById(id: String): Page?
    suspend fun savePage(page: Page)
    suspend fun deletePage(pageId: String)

    suspend fun ensureNotebookAndPage(targetId: String): Page
}
