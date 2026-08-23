package com.prsnl.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prsnl.document.model.Background
import com.prsnl.document.model.Command
import com.prsnl.document.model.Notebook
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

    private val _activeNotebook = MutableStateFlow<Notebook?>(null)
    val activeNotebook: StateFlow<Notebook?> = _activeNotebook.asStateFlow()

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
                _activeNotebook.value = notebook
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

            val targetIndex = (notebook?.lastViewedPageIndex ?: 0).coerceIn(0, loadedPages.size - 1)
            _activePageIndex.value = targetIndex
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
                val newIndex = updatedList.size - 1
                val updatedNb = notebook.copy(
                    pages = updatedList.map { it.id },
                    lastViewedPageIndex = newIndex,
                    updatedAt = System.currentTimeMillis()
                )
                repository.saveNotebook(updatedNb)
                _activeNotebook.value = updatedNb
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
        setActivePageIndex(pageIndex)
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

    fun setActivePageIndex(index: Int) {
        if (index !in _pagesList.value.indices) return
        _activePageIndex.value = index
        updateUndoRedoStates()

        val currentNb = _activeNotebook.value
        if (currentNb != null && currentNb.lastViewedPageIndex != index) {
            viewModelScope.launch {
                val updatedNb = currentNb.copy(
                    lastViewedPageIndex = index,
                    updatedAt = System.currentTimeMillis()
                )
                repository.saveNotebook(updatedNb)
                _activeNotebook.value = updatedNb
            }
        }
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

    fun changeLineWeight(newWeight: Float) {
        updateActiveBackground { background ->
            background.copy(lineWeight = newWeight.coerceIn(0.25f, 8f))
        }
    }

    fun changeLineOpacity(newOpacity: Float) {
        updateActiveBackground { background ->
            background.copy(lineOpacity = newOpacity.coerceIn(0f, 1f))
        }
    }

    fun changeLineColor(newColor: Int) {
        updateActiveBackground { background ->
            background.copy(lineColor = newColor)
        }
    }

    fun changeMarginWeight(newWeight: Float) {
        updateActiveBackground { background ->
            background.copy(marginWeight = newWeight.coerceIn(0.25f, 10f))
        }
    }

    fun changePaperColor(colorLight: Int) {
        updateActiveBackground { background ->
            background.copy(colorLight = colorLight)
        }
    }

    private fun updateActiveBackground(transform: (Background) -> Background) {
        val pageIndex = _activePageIndex.value
        val currentPages = _pagesList.value.toMutableList()
        if (pageIndex !in currentPages.indices) return

        val currentPage = currentPages[pageIndex]
        val updatedPage = currentPage.copy(
            background = transform(currentPage.background)
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
