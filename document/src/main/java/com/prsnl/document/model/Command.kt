package com.prsnl.document.model

sealed class Command {
    abstract fun apply(page: Page): Page
    abstract fun invert(page: Page): Page

    data class AddElement(val element: Element) : Command() {
        override fun apply(page: Page): Page {
            val updated = page.elements.filterNot { it.id == element.id } + element
            return page.copy(elements = updated)
        }

        override fun invert(page: Page): Page {
            val updated = page.elements.filterNot { it.id == element.id }
            return page.copy(elements = updated)
        }
    }

    data class DeleteElement(val element: Element) : Command() {
        override fun apply(page: Page): Page {
            val updated = page.elements.filterNot { it.id == element.id }
            return page.copy(elements = updated)
        }

        override fun invert(page: Page): Page {
            val updated = page.elements.filterNot { it.id == element.id } + element
            return page.copy(elements = updated.sortedBy { it.zIndex })
        }
    }

    data class MoveElement(
        val elementId: String,
        val fromBounds: RectData,
        val toBounds: RectData
    ) : Command() {
        override fun apply(page: Page): Page = updateBounds(page, toBounds)
        override fun invert(page: Page): Page = updateBounds(page, fromBounds)

        private fun updateBounds(page: Page, targetBounds: RectData): Page {
            val updated = page.elements.map { element ->
                if (element.id == elementId) {
                    when (element) {
                        is Stroke -> element.copy(boundingBox = targetBounds)
                        is Shape -> element.copy(boundingBox = targetBounds)
                        is TextBox -> element.copy(boundingBox = targetBounds)
                        is ImageElement -> element.copy(boundingBox = targetBounds)
                        is PdfAnnotationRef -> element.copy(boundingBox = targetBounds)
                    }
                } else element
            }
            return page.copy(elements = updated)
        }
    }

    data class ResizeElement(
        val elementId: String,
        val fromBounds: RectData,
        val toBounds: RectData
    ) : Command() {
        override fun apply(page: Page): Page = updateBounds(page, toBounds)
        override fun invert(page: Page): Page = updateBounds(page, fromBounds)

        private fun updateBounds(page: Page, targetBounds: RectData): Page {
            val updated = page.elements.map { element ->
                if (element.id == elementId) {
                    when (element) {
                        is Stroke -> element.copy(boundingBox = targetBounds)
                        is Shape -> element.copy(boundingBox = targetBounds)
                        is TextBox -> element.copy(boundingBox = targetBounds)
                        is ImageElement -> element.copy(boundingBox = targetBounds)
                        is PdfAnnotationRef -> element.copy(boundingBox = targetBounds)
                    }
                } else element
            }
            return page.copy(elements = updated)
        }
    }

    data class ReplaceElement(
        val oldElement: Element,
        val newElement: Element
    ) : Command() {
        override fun apply(page: Page): Page {
            val updated = page.elements.map { if (it.id == oldElement.id) newElement else it }
            return page.copy(elements = updated)
        }

        override fun invert(page: Page): Page {
            val updated = page.elements.map { if (it.id == newElement.id) oldElement else it }
            return page.copy(elements = updated)
        }
    }

    data class CompoundCommand(val commands: List<Command>) : Command() {
        override fun apply(page: Page): Page {
            return commands.fold(page) { currentPage, cmd -> cmd.apply(currentPage) }
        }

        override fun invert(page: Page): Page {
            return commands.reversed().fold(page) { currentPage, cmd -> cmd.invert(currentPage) }
        }
    }
}
