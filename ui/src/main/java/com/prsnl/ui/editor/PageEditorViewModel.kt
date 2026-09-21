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
import com.prsnl.document.model.Stroke
import com.prsnl.document.model.StrokePoint
import com.prsnl.storage.repository.StatsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(FlowPreview::class)
class PageEditorViewModel(
    private val repository: NotebookRepository,
    private val initialPageId: String,
    private val statsRepository: StatsRepository? = null
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

    private val _penWidth = MutableStateFlow(3f)
    val penWidth: StateFlow<Float> = _penWidth.asStateFlow()

    private val _pencilWidth = MutableStateFlow(2f)
    val pencilWidth: StateFlow<Float> = _pencilWidth.asStateFlow()

    private val _highlighterWidth = MutableStateFlow(18f)
    val highlighterWidth: StateFlow<Float> = _highlighterWidth.asStateFlow()

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
            val loadedPages = mutableListOf<Page>()
            var notebook = repository.getNotebookById(initialPageId)

            if (notebook != null) {
                _activeNotebook.value = notebook
                _notebookTitle.value = notebook.title

                val pageFlowList = repository.getPagesForNotebook(notebook.id).first()
                if (pageFlowList.isNotEmpty()) {
                    loadedPages.addAll(pageFlowList)
                } else {
                    for (pId in notebook.pages) {
                        val p = repository.getPageById(pId)
                        if (p != null) loadedPages.add(p)
                    }
                }
            }

            if (loadedPages.isEmpty()) {
                val firstPage = repository.ensureNotebookAndPage(initialPageId)
                notebook = repository.getNotebookById(firstPage.notebookId)
                if (notebook != null) {
                    _activeNotebook.value = notebook
                    _notebookTitle.value = notebook.title
                }
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

        val (strokeDelta, lengthDelta) = calculateCommandStatDelta(command)
        recordCommandStats(page.notebookId, strokeDelta, lengthDelta)
    }

    private fun recordCommandStats(notebookId: String, strokeDelta: Int, lengthDelta: Float) {
        if (strokeDelta == 0 && lengthDelta == 0f) return
        val repo = statsRepository ?: return
        val folderName = _activeNotebook.value?.folderName ?: "General"
        viewModelScope.launch(Dispatchers.IO) {
            repo.recordStrokeDelta(notebookId, folderName, strokeDelta, lengthDelta)
        }
    }

    private fun calculateCommandStatDelta(command: Command): Pair<Int, Float> {
        return when (command) {
            is Command.AddElement -> {
                val elem = command.element
                if (elem is Stroke) {
                    Pair(1, calculateStrokeLength(elem.points))
                } else {
                    Pair(0, 0f)
                }
            }
            is Command.DeleteElement -> {
                val elem = command.element
                if (elem is Stroke) {
                    Pair(-1, -calculateStrokeLength(elem.points))
                } else {
                    Pair(0, 0f)
                }
            }
            is Command.CompoundCommand -> {
                val deletedStrokes = command.commands.filterIsInstance<Command.DeleteElement>().mapNotNull { it.element as? Stroke }
                val addedStrokes = command.commands.filterIsInstance<Command.AddElement>().mapNotNull { it.element as? Stroke }

                if (deletedStrokes.isNotEmpty() && addedStrokes.isNotEmpty()) {
                    // Stroke split / pixel eraser operation
                    val deletedLen = deletedStrokes.sumOf { calculateStrokeLength(it.points).toDouble() }.toFloat()
                    val addedLen = addedStrokes.sumOf { calculateStrokeLength(it.points).toDouble() }.toFloat()
                    val lengthDelta = addedLen - deletedLen // negative because erased
                    val strokeDelta = if (addedStrokes.isEmpty()) -deletedStrokes.size else 0
                    Pair(strokeDelta, lengthDelta)
                } else {
                    var totalStrokes = 0
                    var totalLen = 0f
                    for (sub in command.commands) {
                        val (s, l) = calculateCommandStatDelta(sub)
                        totalStrokes += s
                        totalLen += l
                    }
                    Pair(totalStrokes, totalLen)
                }
            }
            else -> Pair(0, 0f)
        }
    }

    private fun calculateStrokeLength(points: List<StrokePoint>): Float {
        if (points.size < 2) return 0f
        var total = 0f
        for (i in 1 until points.size) {
            val dx = points[i].x - points[i - 1].x
            val dy = points[i].y - points[i - 1].y
            total += kotlin.math.hypot(dx, dy)
        }
        return total
    }

    fun undo() {
        val pageIndex = _activePageIndex.value
        val currentPages = _pagesList.value.toMutableList()
        if (pageIndex !in currentPages.indices) return

        val page = currentPages[pageIndex]
        val manager = undoRedoManagers[page.id] ?: return
        val result = manager.undoWithCommand()

        if (result != null) {
            val (updatedPage, command) = result
            currentPages[pageIndex] = updatedPage
            _pagesList.value = currentPages
            updateUndoRedoStates()
            triggerAutosave()

            val (strokeDelta, lengthDelta) = calculateCommandStatDelta(command)
            // Undo inverts the executed action's delta
            recordCommandStats(page.notebookId, -strokeDelta, -lengthDelta)
        }
    }

    fun redo() {
        val pageIndex = _activePageIndex.value
        val currentPages = _pagesList.value.toMutableList()
        if (pageIndex !in currentPages.indices) return

        val page = currentPages[pageIndex]
        val manager = undoRedoManagers[page.id] ?: return
        val result = manager.redoWithCommand()

        if (result != null) {
            val (updatedPage, command) = result
            currentPages[pageIndex] = updatedPage
            _pagesList.value = currentPages
            updateUndoRedoStates()
            triggerAutosave()

            val (strokeDelta, lengthDelta) = calculateCommandStatDelta(command)
            // Redo reapplies the action's delta
            recordCommandStats(page.notebookId, strokeDelta, lengthDelta)
        }
    }

    fun setToolMode(mode: CanvasToolMode) {
        _toolMode.value = mode
    }

    fun setWidthForTool(mode: CanvasToolMode, width: Float) {
        when (mode) {
            CanvasToolMode.PEN -> _penWidth.value = width.coerceIn(1f, 12f)
            CanvasToolMode.PENCIL -> _pencilWidth.value = width.coerceIn(1f, 10f)
            CanvasToolMode.HIGHLIGHTER -> _highlighterWidth.value = width.coerceIn(8f, 40f)
            else -> {}
        }
    }

    fun getWidthForTool(mode: CanvasToolMode): Float {
        return when (mode) {
            CanvasToolMode.PEN -> _penWidth.value
            CanvasToolMode.PENCIL -> _pencilWidth.value
            CanvasToolMode.HIGHLIGHTER -> _highlighterWidth.value
            else -> _penWidth.value
        }
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

    fun relinkPdfSource(pdfFile: java.io.File) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentPages = _pagesList.value
            if (currentPages.isEmpty()) return@launch
            val updatedPages = currentPages.map { page ->
                val updatedBg = page.background.copy(
                    type = Background.Type.PDF,
                    pdfSourceRef = pdfFile.absolutePath
                )
                page.copy(background = updatedBg)
            }
            _pagesList.value = updatedPages
            updatedPages.forEach { page ->
                repository.savePage(page)
                undoRedoManagers[page.id]?.updateCurrentPage(page)
            }
        }
    }
}
