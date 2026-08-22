package com.prsnl.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prsnl.document.model.Background
import com.prsnl.document.model.Command
import com.prsnl.document.model.Page
import com.prsnl.document.repository.NotebookRepository
import com.prsnl.drawing.command.UndoRedoManager
import com.prsnl.drawing.view.CanvasToolMode
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(FlowPreview::class)
class PageEditorViewModel(
    private val repository: NotebookRepository,
    private val initialPageId: String
) : ViewModel() {

    private val _pagesList = MutableStateFlow<List<Page>>(emptyList())
    val pagesList: StateFlow<List<Page>> = _pagesList.asStateFlow()

    private val _notebookTitle = MutableStateFlow("Notebook")
    val notebookTitle: StateFlow<String> = _notebookTitle.asStateFlow()

    private val _activePageIndex = MutableStateFlow(0)
    val activePageIndex: StateFlow<Int> = _activePageIndex.asStateFlow()

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    private val _toolMode = MutableStateFlow(CanvasToolMode.PEN)
    val toolMode: StateFlow<CanvasToolMode> = _toolMode.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val undoRedoManagers = mutableMapOf<String, UndoRedoManager>()
    private val saveTriggerFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    init {
        loadNotebookPages()
        setupDebouncedAutosave()
    }

    private fun loadNotebookPages() {
        viewModelScope.launch {
            val firstPage = repository.ensureNotebookAndPage(initialPageId)
            val notebook = repository.getNotebookById(firstPage.notebookId)

            if (notebook != null) {
                _notebookTitle.value = notebook.title
            }

            val loadedPages = mutableListOf<Page>()
            if (notebook != null && notebook.pages.isNotEmpty()) {
                for (pId in notebook.pages) {
                    val loaded = repository.getPageById(pId) ?: if (pId == firstPage.id) firstPage else null
                    if (loaded != null) {
                        loadedPages.add(loaded)
                    }
                }
            }

            if (loadedPages.isEmpty()) {
                loadedPages.add(firstPage)
            }

            _pagesList.value = loadedPages
            loadedPages.forEach { p ->
                undoRedoManagers[p.id] = UndoRedoManager(p)
            }
            updateUndoRedoStates()
        }
    }

    private fun setupDebouncedAutosave() {
        viewModelScope.launch {
            saveTriggerFlow
                .debounce(500L)
                .collect {
                    val pages = _pagesList.value
                    if (pages.isNotEmpty()) {
                        _isSaving.value = true
                        try {
                            for (p in pages) {
                                repository.savePage(p)
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("PageEditorViewModel", "Autosave failed for notebook pages", e)
                        } finally {
                            _isSaving.value = false
                        }
                    }
                }
        }
    }

    fun addPage() {
        viewModelScope.launch {
            val currentPages = _pagesList.value
            val previousPage = currentPages.lastOrNull()

            val newPageId = UUID.randomUUID().toString()
            val notebookId = previousPage?.notebookId ?: "nb_${System.currentTimeMillis()}"

            val inheritedBackground = previousPage?.background ?: Background(
                type = Background.Type.RULED,
                lineSpacing = 40f,
                colorLight = 0xFFFAF8F5.toInt(),
                colorDark = 0xFF1C1C1E.toInt()
            )

            val newPage = Page(
                id = newPageId,
                notebookId = notebookId,
                index = currentPages.size,
                width = previousPage?.width ?: 1200f,
                height = previousPage?.height ?: 1697f,
                background = inheritedBackground,
                elements = emptyList()
            )

            repository.savePage(newPage)
            val updatedList = currentPages + newPage
            _pagesList.value = updatedList
            undoRedoManagers[newPageId] = UndoRedoManager(newPage)

            val notebook = repository.getNotebookById(notebookId)
            if (notebook != null) {
                repository.saveNotebook(notebook.copy(pages = updatedList.map { it.id }))
            }

            _activePageIndex.value = updatedList.size - 1
            updateUndoRedoStates()
        }
    }

    fun executeCommand(pageIndex: Int, command: Command) {
        val currentPages = _pagesList.value.toMutableList()
        if (pageIndex !in currentPages.indices) return

        val page = currentPages[pageIndex]
        val manager = undoRedoManagers[page.id] ?: UndoRedoManager(page).also { undoRedoManagers[page.id] = it }
        val updatedPage = manager.execute(command)

        currentPages[pageIndex] = updatedPage
        _pagesList.value = currentPages
        _activePageIndex.value = pageIndex
        updateUndoRedoStates()
        triggerAutosave()
    }

    fun undo() {
        val pageIndex = _activePageIndex.value
        val currentPages = _pagesList.value.toMutableList()
        if (pageIndex !in currentPages.indices) return

        val page = currentPages[pageIndex]
        val manager = undoRedoManagers[page.id] ?: return
        val updatedPage = manager.undo()

        if (updatedPage != null) {
            currentPages[pageIndex] = updatedPage
            _pagesList.value = currentPages
            updateUndoRedoStates()
            triggerAutosave()
        }
    }

    fun redo() {
        val pageIndex = _activePageIndex.value
        val currentPages = _pagesList.value.toMutableList()
        if (pageIndex !in currentPages.indices) return

        val page = currentPages[pageIndex]
        val manager = undoRedoManagers[page.id] ?: return
        val updatedPage = manager.redo()

        if (updatedPage != null) {
            currentPages[pageIndex] = updatedPage
            _pagesList.value = currentPages
            updateUndoRedoStates()
            triggerAutosave()
        }
    }

    fun setToolMode(mode: CanvasToolMode) {
        _toolMode.value = mode
    }

    fun changeBackgroundType(type: Background.Type) {
        val pageIndex = _activePageIndex.value
        val currentPages = _pagesList.value.toMutableList()
        if (pageIndex !in currentPages.indices) return

        val currentPage = currentPages[pageIndex]
        val updatedPage = currentPage.copy(
            background = currentPage.background.copy(type = type)
        )
        currentPages[pageIndex] = updatedPage
        _pagesList.value = currentPages
        undoRedoManagers[currentPage.id]?.updateCurrentPage(updatedPage)
        triggerAutosave()
    }

    fun changeLineSpacing(newSpacing: Float) {
        val pageIndex = _activePageIndex.value
        val currentPages = _pagesList.value.toMutableList()
        if (pageIndex !in currentPages.indices) return

        val currentPage = currentPages[pageIndex]
        val updatedPage = currentPage.copy(
            background = currentPage.background.copy(lineSpacing = newSpacing.coerceIn(15f, 100f))
        )
        currentPages[pageIndex] = updatedPage
        _pagesList.value = currentPages
        undoRedoManagers[currentPage.id]?.updateCurrentPage(updatedPage)
        triggerAutosave()
    }

    fun changePaperColor(colorLight: Int) {
        val pageIndex = _activePageIndex.value
        val currentPages = _pagesList.value.toMutableList()
        if (pageIndex !in currentPages.indices) return

        val currentPage = currentPages[pageIndex]
        val updatedPage = currentPage.copy(
            background = currentPage.background.copy(colorLight = colorLight)
        )
        currentPages[pageIndex] = updatedPage
        _pagesList.value = currentPages
        undoRedoManagers[currentPage.id]?.updateCurrentPage(updatedPage)
        triggerAutosave()
    }

    private fun triggerAutosave() {
        saveTriggerFlow.tryEmit(Unit)
    }

    private fun updateUndoRedoStates() {
        val pageIndex = _activePageIndex.value
        val currentPages = _pagesList.value
        if (pageIndex in currentPages.indices) {
            val page = currentPages[pageIndex]
            val manager = undoRedoManagers[page.id]
            _canUndo.value = manager?.canUndo ?: false
            _canRedo.value = manager?.canRedo ?: false
        } else {
            _canUndo.value = false
            _canRedo.value = false
        }
    }
}
