package com.prsnl.document.repository

import com.prsnl.document.model.Folder
import kotlinx.coroutines.flow.Flow

interface FolderRepository {
    fun getAllFolders(): Flow<List<Folder>>
    suspend fun saveFolder(folder: Folder)
    suspend fun deleteFolder(folderId: String)
}
