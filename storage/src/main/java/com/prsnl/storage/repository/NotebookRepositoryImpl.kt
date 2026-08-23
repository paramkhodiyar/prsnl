package com.prsnl.storage.repository

import com.prsnl.document.model.Background
import com.prsnl.document.model.Notebook
import com.prsnl.document.model.Page
import com.prsnl.document.repository.NotebookRepository
import com.prsnl.storage.PageFileStorage
import com.prsnl.storage.dao.NotebookDao
import com.prsnl.storage.dao.PageDao
import com.prsnl.storage.entity.NotebookEntity
import com.prsnl.storage.entity.PageEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

class NotebookRepositoryImpl(
    private val notebookDao: NotebookDao,
    private val pageDao: PageDao,
    private val fileStorage: PageFileStorage
) : NotebookRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override fun getAllNotebooks(): Flow<List<Notebook>> {
        return notebookDao.getAllNotebooks().map { list ->
            list.map { entity -> entity.toDomain() }
        }
    }

    override suspend fun getNotebookById(id: String): Notebook? {
        return notebookDao.getNotebookById(id)?.toDomain()
    }

    override suspend fun saveNotebook(notebook: Notebook) {
        notebookDao.insertNotebook(notebook.toEntity())
    }

    override suspend fun deleteNotebook(id: String) {
        val notebook = getNotebookById(id)
        if (notebook != null) {
            notebook.pages.forEach { pageId ->
                deletePage(pageId)
            }
            notebookDao.deleteNotebookById(id)
        }
    }

    override fun getPagesForNotebook(notebookId: String): Flow<List<Page>> {
        return pageDao.getPagesForNotebook(notebookId).map { entities ->
            entities.map { entity ->
                val elements = fileStorage.loadPageElements(entity.elementFilePath)
                entity.toDomain(elements)
            }
        }
    }

    override suspend fun getPageById(id: String): Page? {
        val entity = pageDao.getPageById(id) ?: return null
        val elements = fileStorage.loadPageElements(entity.elementFilePath)
        return entity.toDomain(elements)
    }

    override suspend fun savePage(page: Page) {
        val parentNotebook = notebookDao.getNotebookById(page.notebookId)
        if (parentNotebook == null) {
            val now = System.currentTimeMillis()
            val autoNotebook = NotebookEntity(
                id = page.notebookId,
                title = "Notebook",
                createdAt = now,
                updatedAt = now,
                coverColor = 0xFF4B5563.toInt(),
                coverStyle = "DEFAULT",
                folderName = "General",
                lastViewedPageIndex = 0,
                pagesJson = "[]"
            )
            notebookDao.insertNotebook(autoNotebook)
        }

        val relativePath = fileStorage.savePageElements(page.id, page.elements)
        val entity = page.toEntity(relativePath)
        pageDao.insertPage(entity)
    }

    override suspend fun deletePage(pageId: String) {
        val pageEntity = pageDao.getPageById(pageId)
        if (pageEntity != null) {
            fileStorage.deletePageElements(pageEntity.elementFilePath)
            pageDao.deletePage(pageEntity)
        }
    }

    override suspend fun ensureNotebookAndPage(targetId: String): Page {
        val existingPage = getPageById(targetId)
        if (existingPage != null) return existingPage

        val existingNotebook = getNotebookById(targetId)
        val notebookId = if (existingNotebook != null) {
            existingNotebook.id
        } else {
            val newNotebookId = if (targetId.isNotBlank()) targetId else UUID.randomUUID().toString()
            val now = System.currentTimeMillis()
            val newNotebook = Notebook(
                id = newNotebookId,
                title = "Notebook",
                createdAt = now,
                updatedAt = now,
                coverColor = 0xFF4B5563.toInt(),
                folderName = "General"
            )
            saveNotebook(newNotebook)
            newNotebookId
        }

        val existingPages = pageDao.getPagesForNotebookSync(notebookId)
        if (existingPages.isNotEmpty()) {
            val firstEntity = existingPages.first()
            val elements = fileStorage.loadPageElements(firstEntity.elementFilePath)
            return firstEntity.toDomain(elements)
        }

        val pageId = UUID.randomUUID().toString()
        val newPage = Page(
            id = pageId,
            notebookId = notebookId,
            index = 0,
            width = 1200f,
            height = 1600f,
            background = Background(
                type = Background.Type.RULED,
                lineSpacing = 40f,
                colorLight = 0xFFFAF8F5.toInt(),
                colorDark = 0xFF1C1C1E.toInt()
            )
        )

        savePage(newPage)

        val notebook = getNotebookById(notebookId)
        if (notebook != null) {
            val updatedPages = (notebook.pages + pageId).distinct()
            saveNotebook(notebook.copy(pages = updatedPages))
        }

        return newPage
    }

    private fun NotebookEntity.toDomain(): Notebook {
        val pagesList = try {
            json.decodeFromString<List<String>>(pagesJson)
        } catch (e: Exception) {
            emptyList()
        }
        return Notebook(
            id = id,
            title = title,
            createdAt = createdAt,
            updatedAt = updatedAt,
            coverColor = coverColor,
            coverStyle = coverStyle,
            folderName = folderName,
            lastViewedPageIndex = lastViewedPageIndex,
            pages = pagesList
        )
    }

    private fun Notebook.toEntity(): NotebookEntity {
        return NotebookEntity(
            id = id,
            title = title,
            createdAt = createdAt,
            updatedAt = updatedAt,
            coverColor = coverColor,
            coverStyle = coverStyle,
            folderName = folderName,
            lastViewedPageIndex = lastViewedPageIndex,
            pagesJson = json.encodeToString(pages)
        )
    }

    private fun PageEntity.toDomain(elements: List<com.prsnl.document.model.Element>): Page {
        return Page(
            id = id,
            notebookId = notebookId,
            index = pageIndex,
            width = width,
            height = height,
            background = Background(
                type = Background.Type.valueOf(backgroundType),
                lineSpacing = lineSpacing,
                lineWeight = lineWeight,
                lineOpacity = lineOpacity,
                lineColor = lineColor,
                marginWeight = marginWeight,
                marginColor = marginColor,
                colorLight = colorLight,
                colorDark = colorDark,
                pdfSourceRef = pdfSourceRef
            ),
            elements = elements,
            schemaVersion = schemaVersion
        )
    }

    private fun Page.toEntity(elementFilePath: String): PageEntity {
        return PageEntity(
            id = id,
            notebookId = notebookId,
            pageIndex = index,
            width = width,
            height = height,
            backgroundType = background.type.name,
            lineSpacing = background.lineSpacing,
            lineWeight = background.lineWeight,
            lineOpacity = background.lineOpacity,
            lineColor = background.lineColor,
            marginWeight = background.marginWeight,
            marginColor = background.marginColor,
            colorLight = background.colorLight,
            colorDark = background.colorDark,
            pdfSourceRef = background.pdfSourceRef,
            elementFilePath = elementFilePath,
            schemaVersion = schemaVersion
        )
    }
}
