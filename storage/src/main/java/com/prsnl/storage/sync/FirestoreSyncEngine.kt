package com.prsnl.storage.sync

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.prsnl.storage.PageFileStorage
import com.prsnl.storage.dao.FolderDao
import com.prsnl.storage.dao.NotebookDao
import com.prsnl.storage.dao.PageDao
import com.prsnl.storage.entity.FolderEntity
import com.prsnl.storage.entity.NotebookEntity
import com.prsnl.storage.entity.PageEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

enum class SyncState {
    IDLE, SYNCING, SUCCESS, ERROR
}

@Singleton
class FirestoreSyncEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authRepository: AuthRepository,
    private val folderDao: FolderDao,
    private val notebookDao: NotebookDao,
    private val pageDao: PageDao
) {

    private val db = FirebaseFirestore.getInstance()
    private val pageFileStorage = PageFileStorage(context.filesDir)

    private val _syncState = MutableStateFlow(SyncState.IDLE)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val _lastSyncTimestamp = MutableStateFlow<Long?>(null)
    val lastSyncTimestamp: StateFlow<Long?> = _lastSyncTimestamp.asStateFlow()

    private val _syncErrorMessage = MutableStateFlow<String?>(null)
    val syncErrorMessage: StateFlow<String?> = _syncErrorMessage.asStateFlow()

    suspend fun syncAll(): Result<Unit> = withContext(Dispatchers.IO) {
        val userId = authRepository.getUserId()
            ?: return@withContext Result.failure(Exception("Please sign in with Google to sync."))

        _syncState.value = SyncState.SYNCING
        _syncErrorMessage.value = null

        try {
            val userDocRef = db.collection("users").document(userId)

            // -------------------------------------------------------------
            // 1. SYNC FOLDERS
            // -------------------------------------------------------------
            val localFolders = folderDao.getAllFoldersSync()
            val remoteFoldersSnapshot = userDocRef.collection("folders").get().await()
            val remoteFolders = remoteFoldersSnapshot.documents.mapNotNull { doc ->
                val id = doc.id
                val name = doc.getString("name") ?: return@mapNotNull null
                val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                val color = doc.getLong("color")?.toInt() ?: 0xFFC88A4B.toInt()
                val iconName = doc.getString("iconName") ?: "FOLDER"
                val isLocked = doc.getBoolean("isLocked") ?: false
                FolderEntity(id, name, createdAt, color, iconName, isLocked)
            }

            // Push local folders to Firestore
            for (folder in localFolders) {
                val data = mapOf(
                    "id" to folder.id,
                    "name" to folder.name,
                    "createdAt" to folder.createdAt,
                    "color" to folder.color,
                    "iconName" to folder.iconName,
                    "isLocked" to folder.isLocked
                )
                userDocRef.collection("folders").document(folder.id).set(data, SetOptions.merge()).await()
            }

            // Pull remote folders into Room DB
            for (remoteFolder in remoteFolders) {
                folderDao.insertFolder(remoteFolder)
            }

            // -------------------------------------------------------------
            // 2. SYNC NOTEBOOKS
            // -------------------------------------------------------------
            val localNotebooks = notebookDao.getAllNotebooksSync()
            val remoteNotebooksSnapshot = userDocRef.collection("notebooks").get().await()

            for (notebook in localNotebooks) {
                val data = mapOf(
                    "id" to notebook.id,
                    "title" to notebook.title,
                    "createdAt" to notebook.createdAt,
                    "updatedAt" to notebook.updatedAt,
                    "coverColor" to notebook.coverColor,
                    "coverStyle" to notebook.coverStyle,
                    "folderName" to notebook.folderName,
                    "lastViewedPageIndex" to notebook.lastViewedPageIndex,
                    "pagesJson" to notebook.pagesJson
                )
                userDocRef.collection("notebooks").document(notebook.id).set(data, SetOptions.merge()).await()
            }

            for (doc in remoteNotebooksSnapshot.documents) {
                val id = doc.id
                val title = doc.getString("title") ?: "Untitled"
                val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                val updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
                val coverColor = doc.getLong("coverColor")?.toInt() ?: 0xFFC88A4B.toInt()
                val coverStyle = doc.getString("coverStyle") ?: "DEFAULT"
                val folderName = doc.getString("folderName") ?: "General"
                val lastViewedPageIndex = doc.getLong("lastViewedPageIndex")?.toInt() ?: 0
                val pagesJson = doc.getString("pagesJson") ?: "[]"

                val remoteNotebook = NotebookEntity(
                    id = id,
                    title = title,
                    createdAt = createdAt,
                    updatedAt = updatedAt,
                    coverColor = coverColor,
                    coverStyle = coverStyle,
                    folderName = folderName,
                    lastViewedPageIndex = lastViewedPageIndex,
                    pagesJson = pagesJson
                )

                val existingLocal = notebookDao.getNotebookById(id)
                if (existingLocal == null || remoteNotebook.updatedAt > existingLocal.updatedAt) {
                    notebookDao.insertNotebook(remoteNotebook)
                }
            }

            // -------------------------------------------------------------
            // 3. SYNC PAGES & VECTOR STROKE JSON
            // -------------------------------------------------------------
            val localPages = pageDao.getAllPagesSync()
            val remotePagesSnapshot = userDocRef.collection("pages").get().await()

            for (page in localPages) {
                val rawFile = java.io.File(context.filesDir, page.elementFilePath)
                val jsonText = if (rawFile.exists()) rawFile.readText() else "[]"

                val pageData = mapOf(
                    "id" to page.id,
                    "notebookId" to page.notebookId,
                    "pageIndex" to page.pageIndex,
                    "width" to page.width,
                    "height" to page.height,
                    "backgroundType" to page.backgroundType,
                    "lineSpacing" to (page.lineSpacing ?: 40f),
                    "lineWeight" to page.lineWeight,
                    "lineOpacity" to page.lineOpacity,
                    "lineColor" to page.lineColor,
                    "marginWeight" to page.marginWeight,
                    "marginColor" to page.marginColor,
                    "colorLight" to page.colorLight,
                    "colorDark" to page.colorDark,
                    "pdfSourceRef" to (page.pdfSourceRef ?: ""),
                    "elementFilePath" to page.elementFilePath,
                    "schemaVersion" to page.schemaVersion,
                    "elementsJson" to jsonText,
                    "updatedAt" to System.currentTimeMillis()
                )

                userDocRef.collection("pages").document(page.id).set(pageData, SetOptions.merge()).await()
            }

            for (doc in remotePagesSnapshot.documents) {
                val id = doc.id
                val notebookId = doc.getString("notebookId") ?: continue
                val pageIndex = doc.getLong("pageIndex")?.toInt() ?: 0
                val width = doc.getDouble("width")?.toFloat() ?: 1200f
                val height = doc.getDouble("height")?.toFloat() ?: 1600f
                val backgroundType = doc.getString("backgroundType") ?: "RULED"
                val lineSpacing = doc.getDouble("lineSpacing")?.toFloat() ?: 40f
                val lineWeight = doc.getDouble("lineWeight")?.toFloat() ?: 1f
                val lineOpacity = doc.getDouble("lineOpacity")?.toFloat() ?: 0.3f
                val lineColor = doc.getLong("lineColor")?.toInt() ?: 0xFF9E9E9E.toInt()
                val marginWeight = doc.getDouble("marginWeight")?.toFloat() ?: 2f
                val marginColor = doc.getLong("marginColor")?.toInt() ?: 0xFFE57373.toInt()
                val colorLight = doc.getLong("colorLight")?.toInt() ?: 0xFFFBF9F4.toInt()
                val colorDark = doc.getLong("colorDark")?.toInt() ?: 0xFF1E1E1E.toInt()
                val pdfSourceRef = doc.getString("pdfSourceRef")
                val elementFilePath = doc.getString("elementFilePath") ?: "pages/$id.json"
                val schemaVersion = doc.getLong("schemaVersion")?.toInt() ?: 1
                val elementsJson = doc.getString("elementsJson") ?: "[]"

                // Save page entity to Room
                val remotePageEntity = PageEntity(
                    id = id,
                    notebookId = notebookId,
                    pageIndex = pageIndex,
                    width = width,
                    height = height,
                    backgroundType = backgroundType,
                    lineSpacing = lineSpacing,
                    lineWeight = lineWeight,
                    lineOpacity = lineOpacity,
                    lineColor = lineColor,
                    marginWeight = marginWeight,
                    marginColor = marginColor,
                    colorLight = colorLight,
                    colorDark = colorDark,
                    pdfSourceRef = pdfSourceRef,
                    elementFilePath = elementFilePath,
                    schemaVersion = schemaVersion
                )
                pageDao.insertPage(remotePageEntity)

                // Save elements JSON to local file storage
                val localFile = java.io.File(context.filesDir, elementFilePath)
                localFile.parentFile?.apply { if (!exists()) mkdirs() }
                localFile.writeText(elementsJson)
            }

            val now = System.currentTimeMillis()
            _lastSyncTimestamp.value = now
            _syncState.value = SyncState.SUCCESS
            Result.success(Unit)
        } catch (e: Exception) {
            _syncErrorMessage.value = e.localizedMessage ?: "Sync failed"
            _syncState.value = SyncState.ERROR
            Result.failure(e)
        }
    }
}
