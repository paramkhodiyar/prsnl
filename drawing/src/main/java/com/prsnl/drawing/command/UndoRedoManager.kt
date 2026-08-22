package com.prsnl.drawing.command

import com.prsnl.document.model.Command
import com.prsnl.document.model.Page
import java.util.ArrayDeque

class UndoRedoManager(
    private var currentPage: Page
) {
    private val undoStack = ArrayDeque<Command>()
    private val redoStack = ArrayDeque<Command>()

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    fun getPage(): Page = currentPage

    fun execute(command: Command): Page {
        currentPage = command.apply(currentPage)
        undoStack.push(command)
        redoStack.clear()
        return currentPage
    }

    fun undo(): Page? {
        if (undoStack.isEmpty()) return null
        val command = undoStack.pop()
        currentPage = command.invert(currentPage)
        redoStack.push(command)
        return currentPage
    }

    fun redo(): Page? {
        if (redoStack.isEmpty()) return null
        val command = redoStack.pop()
        currentPage = command.apply(currentPage)
        undoStack.push(command)
        return currentPage
    }

    fun updateCurrentPage(page: Page) {
        currentPage = page
    }

    fun reset(page: Page) {
        currentPage = page
        undoStack.clear()
        redoStack.clear()
    }
}
