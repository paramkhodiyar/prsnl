package com.prsnl.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prsnl.document.model.Background
import com.prsnl.document.model.Folder
import com.prsnl.document.model.Notebook
import com.prsnl.document.model.Page
import com.prsnl.document.repository.NotebookRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class HomeViewModel(
    private val notebookRepository: NotebookRepository
) : ViewModel() {

    val notebooks: StateFlow<List<Notebook>> = notebookRepository.getAllNotebooks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    private val _folders = MutableStateFlow<List<Folder>>(
        listOf(
            Folder("f1", "Finance", color = 0xFF4C6EF5.toInt()),
            Folder("f2", "Personal", color = 0xFFC85A32.toInt()),
            Folder("f3", "Work", color = 0xFF4A7C59.toInt())
        )
    )
    val folders: StateFlow<List<Folder>> = _folders.asStateFlow()

    fun createFolder(name: String, color: Int = 0xFF4B5563.toInt()) {
        if (name.isBlank()) return
        val current = _folders.value
        if (current.any { it.name.equals(name.trim(), ignoreCase = true) }) return

        val newFolder = Folder(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            createdAt = System.currentTimeMillis(),
            color = color
        )
        _folders.value = current + newFolder
    }

    fun updateFolder(folderId: String, oldName: String, newName: String, newColor: Int) {
        if (newName.isBlank()) return
        val current = _folders.value.toMutableList()
        val index = current.indexOfFirst { it.id == folderId || it.name == oldName }
        if (index != -1) {
            val updatedFolder = current[index].copy(name = newName.trim(), color = newColor)
            current[index] = updatedFolder
            _folders.value = current

            // Also update any child notebooks
            viewModelScope.launch {
                val allNbs = notebooks.value
                allNbs.filter { it.folderName.equals(oldName, ignoreCase = true) }.forEach { nb ->
                    notebookRepository.saveNotebook(nb.copy(folderName = newName.trim()))
                }
            }
        }
    }

    fun updateFolderLock(
        folderId: String,
        isLocked: Boolean,
        pin: String? = null,
        securityQuestion: String? = null,
        securityAnswerHash: String? = null
    ) {
        val current = _folders.value.toMutableList()
        val index = current.indexOfFirst { it.id == folderId }
        if (index != -1) {
            val updatedFolder = current[index].copy(
                isLocked = isLocked,
                pin = if (isLocked) pin else null,
                securityQuestion = if (isLocked) securityQuestion else null,
                securityAnswerHash = if (isLocked) securityAnswerHash else null
            )
            current[index] = updatedFolder
            _folders.value = current
        }
    }

    fun deleteFolder(folderId: String, folderName: String) {
        _folders.value = _folders.value.filterNot { it.id == folderId || it.name.equals(folderName, ignoreCase = true) }
        viewModelScope.launch {
            val allNbs = notebooks.value
            allNbs.filter { it.folderName.equals(folderName, ignoreCase = true) }.forEach { nb ->
                notebookRepository.deleteNotebook(nb.id)
            }
        }
    }

    fun createNotebook(
        title: String,
        folderName: String = "General",
        coverColor: Int = 0xFF4B5563.toInt(),
        backgroundType: Background.Type = Background.Type.RULED,
        paperColor: Int = 0xFFFAF8F5.toInt(),
        onCreated: ((notebookId: String, firstPageId: String) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val notebookId = UUID.randomUUID().toString()
            val pageId = UUID.randomUUID().toString()
            val now = System.currentTimeMillis()

            val safeFolderName = if (folderName.isBlank()) "General" else folderName.trim()
            createFolder(safeFolderName)

            val firstPage = Page(
                id = pageId,
                notebookId = notebookId,
                index = 0,
                width = 1200f,
                height = 1697f,
                background = Background(
                    type = backgroundType,
                    lineSpacing = 40f,
                    colorLight = paperColor,
                    colorDark = 0xFF1C1C1E.toInt()
                )
            )
            notebookRepository.savePage(firstPage)

            val newNotebook = Notebook(
                id = notebookId,
                title = if (title.isBlank()) "Untitled Notebook" else title,
                createdAt = now,
                updatedAt = now,
                coverColor = coverColor,
                folderName = safeFolderName,
                pages = listOf(pageId)
            )
            notebookRepository.saveNotebook(newNotebook)

            onCreated?.invoke(notebookId, pageId)
        }
    }

    fun updateNotebook(notebook: Notebook, newTitle: String, newCoverColor: Int, newFolderName: String) {
        viewModelScope.launch {
            val updated = notebook.copy(
                title = newTitle.ifBlank { notebook.title },
                coverColor = newCoverColor,
                folderName = newFolderName.ifBlank { notebook.folderName },
                updatedAt = System.currentTimeMillis()
            )
            notebookRepository.saveNotebook(updated)
        }
    }

    fun deleteNotebook(notebookId: String) {
        viewModelScope.launch {
            notebookRepository.deleteNotebook(notebookId)
        }
    }
}
