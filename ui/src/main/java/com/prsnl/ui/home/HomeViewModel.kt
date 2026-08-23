package com.prsnl.ui.home

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prsnl.document.model.Background
import com.prsnl.document.model.Folder
import com.prsnl.document.model.Notebook
import com.prsnl.document.model.Page
import com.prsnl.document.repository.FolderRepository
import com.prsnl.document.repository.NotebookRepository
import com.prsnl.pdf.PdfImporter
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class HomeViewModel(
    private val notebookRepository: NotebookRepository,
    private val folderRepository: FolderRepository
) : ViewModel() {

    val notebooks: StateFlow<List<Notebook>> = notebookRepository.getAllNotebooks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    val folders: StateFlow<List<Folder>> = folderRepository.getAllFolders()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    init {
        viewModelScope.launch {
            val existingFolders = folderRepository.getAllFolders().first()
            if (existingFolders.isEmpty()) {
                val defaultFolder = Folder(
                    id = UUID.randomUUID().toString(),
                    name = "Personal",
                    createdAt = System.currentTimeMillis(),
                    color = 0xFFC85A32.toInt(),
                    iconName = "PERSONAL"
                )
                folderRepository.saveFolder(defaultFolder)

                val existingNotebooks = notebookRepository.getAllNotebooks().first()
                if (existingNotebooks.isEmpty()) {
                    createNotebook(
                        title = "Personal",
                        folderName = "Personal",
                        coverColor = 0xFFC85A32.toInt(),
                        coverStyle = "PERSONAL",
                        backgroundType = Background.Type.MARGIN_RULED,
                        paperColor = 0xFFFAF8F5.toInt()
                    )
                }
            }
        }
    }

    fun createFolder(name: String, color: Int = 0xFF8B5E3C.toInt(), iconName: String = "FOLDER"): Folder {
        val trimmed = name.trim().ifBlank { "Untitled Folder" }
        val existing = folders.value.find { it.name.equals(trimmed, ignoreCase = true) }
        if (existing != null) return existing

        val newFolder = Folder(
            id = UUID.randomUUID().toString(),
            name = trimmed,
            createdAt = System.currentTimeMillis(),
            color = color,
            iconName = iconName
        )
        viewModelScope.launch {
            folderRepository.saveFolder(newFolder)
        }
        return newFolder
    }

    fun updateFolder(folderId: String, oldName: String, newName: String, newColor: Int, newIconName: String = "FOLDER") {
        if (newName.isBlank()) return
        val existing = folders.value.find { it.id == folderId || it.name.equals(oldName, ignoreCase = true) } ?: return
        val updated = existing.copy(
            name = newName.trim(),
            color = newColor,
            iconName = newIconName
        )
        viewModelScope.launch {
            folderRepository.saveFolder(updated)
            val allNbs = notebooks.value
            allNbs.filter { it.folderName.equals(oldName, ignoreCase = true) }.forEach { nb ->
                notebookRepository.saveNotebook(nb.copy(folderName = newName.trim()))
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
        val existing = folders.value.find { it.id == folderId } ?: return
        val updated = existing.copy(
            isLocked = isLocked,
            pin = if (isLocked) pin else null,
            securityQuestion = if (isLocked) securityQuestion else null,
            securityAnswerHash = if (isLocked) securityAnswerHash else null
        )
        viewModelScope.launch {
            folderRepository.saveFolder(updated)
        }
    }

    fun deleteFolder(folderId: String, folderName: String) {
        viewModelScope.launch {
            folderRepository.deleteFolder(folderId)
            val allNbs = notebooks.value
            allNbs.filter { it.folderName.equals(folderName, ignoreCase = true) }.forEach { nb ->
                notebookRepository.deleteNotebook(nb.id)
            }
        }
    }

    fun createNotebook(
        title: String,
        folderName: String = "Personal",
        coverColor: Int = 0xFF8B5E3C.toInt(),
        coverStyle: String = "DEFAULT",
        backgroundType: Background.Type = Background.Type.RULED,
        paperColor: Int = 0xFFFAF8F5.toInt(),
        onCreated: ((notebookId: String, firstPageId: String) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val notebookId = UUID.randomUUID().toString()
            val pageId = UUID.randomUUID().toString()
            val now = System.currentTimeMillis()

            val safeFolderName = if (folderName.isBlank()) "Personal" else folderName.trim()
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
                coverStyle = coverStyle,
                folderName = safeFolderName,
                pages = listOf(pageId)
            )
            notebookRepository.saveNotebook(newNotebook)

            onCreated?.invoke(notebookId, pageId)
        }
    }

    fun importPdf(
        context: Context,
        uri: Uri,
        folderName: String,
        onImported: (notebookId: String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val tempPdfFile = File(context.cacheDir, "import_${UUID.randomUUID()}.pdf")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(tempPdfFile).use { output -> input.copyTo(output) }
                }

                val pdfImporter = PdfImporter(context)
                val result = pdfImporter.importPdfToNotebook(tempPdfFile)
                if (result != null) {
                    val (notebook, pages) = result
                    val safeFolder = if (folderName.isBlank()) "PDF Annotations" else folderName.trim()
                    createFolder(safeFolder)

                    val updatedNotebook = notebook.copy(
                        folderName = safeFolder,
                        coverStyle = "PDF",
                        coverColor = 0xFF4C6EF5.toInt()
                    )

                    // 1. Save all PDF pages to PageFileStorage and Room SQLite DB
                    for (page in pages) {
                        notebookRepository.savePage(page)
                    }
                    // 2. Save parent notebook model to Room SQLite DB
                    notebookRepository.saveNotebook(updatedNotebook)

                    // 3. Open the newly imported PDF notebook immediately!
                    onImported(updatedNotebook.id)
                } else {
                    android.util.Log.e("HomeViewModel", "PdfImporter returned null for file ${tempPdfFile.name}")
                }
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Failed to import PDF file", e)
            }
        }
    }

    fun updateNotebook(notebook: Notebook, newTitle: String, newCoverColor: Int, newFolderName: String, newCoverStyle: String = "DEFAULT") {
        viewModelScope.launch {
            val updated = notebook.copy(
                title = newTitle.ifBlank { notebook.title },
                coverColor = newCoverColor,
                coverStyle = newCoverStyle,
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
