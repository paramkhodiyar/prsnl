package com.prsnl.storage.repository

import com.prsnl.document.model.Folder
import com.prsnl.document.repository.FolderRepository
import com.prsnl.storage.dao.FolderDao
import com.prsnl.storage.entity.FolderEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FolderRepositoryImpl(
    private val folderDao: FolderDao
) : FolderRepository {

    override fun getAllFolders(): Flow<List<Folder>> {
        return folderDao.getAllFolders().map { entities ->
            entities.map { entity ->
                Folder(
                    id = entity.id,
                    name = entity.name,
                    createdAt = entity.createdAt,
                    color = entity.color
                )
            }
        }
    }

    override suspend fun saveFolder(folder: Folder) {
        folderDao.insertFolder(
            FolderEntity(
                id = folder.id,
                name = folder.name,
                createdAt = folder.createdAt,
                color = folder.color
            )
        )
    }

    override suspend fun deleteFolder(folderId: String) {
        folderDao.deleteFolder(folderId)
    }
}
