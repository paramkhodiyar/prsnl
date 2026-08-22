package com.prsnl.ui.notebook

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prsnl.document.model.Background
import com.prsnl.document.model.Notebook
import com.prsnl.document.model.Page
import com.prsnl.document.repository.NotebookRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class NotebookDetailViewModel(
    private val repository: NotebookRepository,
    private val notebookId: String
) : ViewModel() {

    private val _notebook = MutableStateFlow<Notebook?>(null)
    val notebook: StateFlow<Notebook?> = _notebook.asStateFlow()

    private val _pages = MutableStateFlow<List<Page>>(emptyList())
    val pages: StateFlow<List<Page>> = _pages.asStateFlow()

    init {
        loadNotebookDetails()
    }

    private fun loadNotebookDetails() {
        viewModelScope.launch {
            val nb = repository.getNotebookById(notebookId)
            _notebook.value = nb
            repository.getPagesForNotebook(notebookId).collect { pageList ->
                _pages.value = pageList
            }
        }
    }

    fun createNewPage(backgroundType: Background.Type = Background.Type.RULED) {
        viewModelScope.launch {
            val newIndex = _pages.value.size
            val newPage = Page(
                id = UUID.randomUUID().toString(),
                notebookId = notebookId,
                index = newIndex,
                width = 1200f,
                height = 1600f,
                background = Background(
                    type = backgroundType,
                    lineSpacing = 40f,
                    colorLight = 0xFFFFFFFF.toInt(),
                    colorDark = 0xFF181818.toInt()
                )
            )
            repository.savePage(newPage)
        }
    }

    fun deletePage(pageId: String) {
        viewModelScope.launch {
            repository.deletePage(pageId)
        }
    }
}
