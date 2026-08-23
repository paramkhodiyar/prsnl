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
                    val dx = targetBounds.left - element.boundingBox.left
                    val dy = targetBounds.top - element.boundingBox.top
                    when (element) {
                        is Stroke -> element.copy(
                            boundingBox = targetBounds,
                            points = element.points.map { point ->
                                point.copy(x = point.x + dx, y = point.y + dy)
                            }
                        )
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
                    val sourceBounds = element.boundingBox
                    val sx = if (sourceBounds.width == 0f) 1f else targetBounds.width / sourceBounds.width
                    val sy = if (sourceBounds.height == 0f) 1f else targetBounds.height / sourceBounds.height
                    when (element) {
                        is Stroke -> element.copy(
                            boundingBox = targetBounds,
                            points = element.points.map { point ->
                                point.copy(
                                    x = targetBounds.left + (point.x - sourceBounds.left) * sx,
                                    y = targetBounds.top + (point.y - sourceBounds.top) * sy
                                )
                            }
                        )
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
