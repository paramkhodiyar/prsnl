package com.prsnl.storage.repository

import com.prsnl.document.model.Background
import com.prsnl.document.model.Page
import com.prsnl.storage.PageFileStorage
import com.prsnl.storage.dao.NotebookDao
import com.prsnl.storage.dao.PageDao
import com.prsnl.storage.entity.NotebookEntity
import com.prsnl.storage.entity.PageEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class NotebookRepositoryImplTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testSavePageAutoInsertsParentNotebookIfMissing() {
        runBlocking {
            val fakeNotebooks = mutableMapOf<String, NotebookEntity>()
            val fakePages = mutableMapOf<String, PageEntity>()

            val fakeNotebookDao = object : NotebookDao {
                override fun getAllNotebooks(): Flow<List<NotebookEntity>> = flowOf(fakeNotebooks.values.toList())
                override suspend fun getNotebookById(id: String): NotebookEntity? = fakeNotebooks[id]
                override suspend fun insertNotebook(notebook: NotebookEntity) { fakeNotebooks[notebook.id] = notebook }
                override suspend fun updateNotebook(notebook: NotebookEntity) { fakeNotebooks[notebook.id] = notebook }
                override suspend fun updateNotebookFolderName(oldName: String, newName: String) {}
                override suspend fun deleteNotebook(notebook: NotebookEntity) { fakeNotebooks.remove(notebook.id) }
                override suspend fun deleteNotebookById(id: String) { fakeNotebooks.remove(id) }
                override suspend fun deleteNotebooksByFolder(folderName: String) {}
            }

            val fakePageDao = object : PageDao {
                override fun getPagesForNotebook(notebookId: String): Flow<List<PageEntity>> =
                    flowOf(fakePages.values.filter { it.notebookId == notebookId })
                override suspend fun getPageById(id: String): PageEntity? = fakePages[id]
                override suspend fun insertPage(page: PageEntity) {
                    check(fakeNotebooks.containsKey(page.notebookId)) {
                        "SQLiteConstraintException: FOREIGN KEY constraint failed for notebookId ${page.notebookId}"
                    }
                    fakePages[page.id] = page
                }
                override suspend fun updatePage(page: PageEntity) { fakePages[page.id] = page }
                override suspend fun deletePage(page: PageEntity) { fakePages.remove(page.id) }
            }

            val fileStorage = PageFileStorage(tempFolder.root)
            val repository = NotebookRepositoryImpl(fakeNotebookDao, fakePageDao, fileStorage)

            val pageWithUnknownNotebook = Page(
                id = "page-uuid-100",
                notebookId = "non-existent-notebook-id",
                index = 0,
                width = 1200f,
                height = 1600f,
                background = Background(Background.Type.RULED, 40f, 0xFFFFFFFF.toInt(), 0xFF000000.toInt())
            )

            repository.savePage(pageWithUnknownNotebook)

            val autoCreatedNotebook = repository.getNotebookById("non-existent-notebook-id")
            assertNotNull(autoCreatedNotebook)
            assertEquals("non-existent-notebook-id", autoCreatedNotebook?.id)

            val savedPage = repository.getPageById("page-uuid-100")
            assertNotNull(savedPage)
            assertEquals("page-uuid-100", savedPage?.id)
            assertEquals("non-existent-notebook-id", savedPage?.notebookId)
        }
    }
}
