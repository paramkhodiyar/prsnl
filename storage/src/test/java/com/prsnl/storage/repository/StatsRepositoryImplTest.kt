package com.prsnl.storage.repository

import com.prsnl.storage.dao.AggregateStatResult
import com.prsnl.storage.dao.DailyStatRow
import com.prsnl.storage.dao.FolderDao
import com.prsnl.storage.dao.FolderStatRow
import com.prsnl.storage.dao.MonthlyStatRow
import com.prsnl.storage.dao.NotebookDao
import com.prsnl.storage.dao.NotebookStatRow
import com.prsnl.storage.dao.PageDao
import com.prsnl.storage.dao.WritingStatDao
import com.prsnl.storage.entity.FolderEntity
import com.prsnl.storage.entity.NotebookEntity
import com.prsnl.storage.entity.PageEntity
import com.prsnl.storage.entity.WritingStatEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class StatsRepositoryImplTest {

    @Test
    fun testRecordStrokeAndGetOverallStats() = runBlocking {
        val statsMap = mutableMapOf<String, WritingStatEntity>()

        val fakeWritingStatDao = object : WritingStatDao {
            override suspend fun upsertStat(
                date: String,
                notebookId: String,
                folderName: String,
                strokeCount: Int,
                lengthUnits: Float
            ) {
                val key = "$date-$notebookId"
                val existing = statsMap[key]
                val updated = if (existing != null) {
                    existing.copy(
                        strokeCount = existing.strokeCount + strokeCount,
                        inkLengthUnits = existing.inkLengthUnits + lengthUnits
                    )
                } else {
                    WritingStatEntity(
                        date = date,
                        notebookId = notebookId,
                        folderName = folderName,
                        strokeCount = strokeCount,
                        inkLengthUnits = lengthUnits
                    )
                }
                statsMap[key] = updated
            }

            override suspend fun insertOrReplace(entity: WritingStatEntity) {
                statsMap["${entity.date}-${entity.notebookId}"] = entity
            }

            override fun getTotalStats(): Flow<AggregateStatResult?> {
                val totalUnits = statsMap.values.sumOf { it.inkLengthUnits.toDouble() }.toFloat()
                val totalStrokes = statsMap.values.sumOf { it.strokeCount }
                return flowOf(AggregateStatResult(totalUnits, totalStrokes))
            }

            override suspend fun getTotalStatsSync(): AggregateStatResult? {
                val totalUnits = statsMap.values.sumOf { it.inkLengthUnits.toDouble() }.toFloat()
                val totalStrokes = statsMap.values.sumOf { it.strokeCount }
                return AggregateStatResult(totalUnits, totalStrokes)
            }

            override suspend fun getYearlyStatsSync(yearPrefix: String): AggregateStatResult? {
                val filtered = statsMap.values.filter { it.date.startsWith(yearPrefix) }
                return AggregateStatResult(
                    filtered.sumOf { it.inkLengthUnits.toDouble() }.toFloat(),
                    filtered.sumOf { it.strokeCount }
                )
            }

            override suspend fun getMonthlyStatsSync(monthPrefix: String): AggregateStatResult? {
                val filtered = statsMap.values.filter { it.date.startsWith(monthPrefix) }
                return AggregateStatResult(
                    filtered.sumOf { it.inkLengthUnits.toDouble() }.toFloat(),
                    filtered.sumOf { it.strokeCount }
                )
            }

            override fun getTopNotebooks(limit: Int): Flow<List<NotebookStatRow>> = flowOf(emptyList())

            override suspend fun getTopNotebooksForYearSync(yearPrefix: String, limit: Int): List<NotebookStatRow> = emptyList()

            override fun getTopFolders(limit: Int): Flow<List<FolderStatRow>> = flowOf(emptyList())

            override suspend fun getTopFoldersForYearSync(yearPrefix: String, limit: Int): List<FolderStatRow> = emptyList()

            override suspend fun getAllActiveDatesSync(): List<String> = statsMap.values.map { it.date }.distinct().sorted()

            override fun getRecentDailyStats(startDate: String): Flow<List<DailyStatRow>> = flowOf(emptyList())

            override suspend fun getDailyActivityForYearSync(yearPrefix: String): List<DailyStatRow> = emptyList()

            override suspend fun getBusiestDaySync(): DailyStatRow? = null

            override suspend fun getBusiestMonthForYearSync(yearPrefix: String): MonthlyStatRow? = null

            override suspend fun getBusiestMonthAllTimeSync(): MonthlyStatRow? = null
        }

        val fakeNotebookDao = object : NotebookDao {
            override fun getAllNotebooks(): Flow<List<NotebookEntity>> = flowOf(emptyList())
            override suspend fun getAllNotebooksSync(): List<NotebookEntity> = emptyList()
            override suspend fun getNotebookById(id: String): NotebookEntity? = null
            override suspend fun insertNotebook(notebook: NotebookEntity) {}
            override suspend fun updateNotebook(notebook: NotebookEntity) {}
            override suspend fun updateNotebookFolderName(oldName: String, newName: String) {}
            override suspend fun deleteNotebook(notebook: NotebookEntity) {}
            override suspend fun deleteNotebookById(id: String) {}
            override suspend fun deleteNotebooksByFolder(folderName: String) {}
        }

        val fakeFolderDao = object : FolderDao {
            override fun getAllFolders(): Flow<List<FolderEntity>> = flowOf(emptyList())
            override suspend fun getAllFoldersSync(): List<FolderEntity> = emptyList()
            override suspend fun insertFolder(folder: FolderEntity) {}
            override suspend fun deleteFolder(folderId: String) {}
        }

        val fakePageDao = object : PageDao {
            override fun getPagesForNotebook(notebookId: String): Flow<List<PageEntity>> = flowOf(emptyList())
            override suspend fun getPagesForNotebookSync(notebookId: String): List<PageEntity> = emptyList()
            override suspend fun getAllPagesSync(): List<PageEntity> = emptyList()
            override suspend fun getPageById(id: String): PageEntity? = null
            override suspend fun insertPage(page: PageEntity) {}
            override suspend fun updatePage(page: PageEntity) {}
            override suspend fun deletePage(page: PageEntity) {}
        }

        val repository = StatsRepositoryImpl(
            fakeWritingStatDao,
            fakeNotebookDao,
            fakeFolderDao,
            fakePageDao
        )

        // Record 1200 units (which equals 0.21 meters for standard 1200 width page)
        repository.recordStroke("nb_1", "Math", 1200f)
        repository.recordStroke("nb_1", "Math", 1200f)

        val stats = repository.getOverallStatsSync()
        assertEquals(2, stats.totalStrokes)
        assertEquals(0.42f, stats.totalMetersAllTime, 0.001f)
        assertEquals(1, stats.currentStreakDays)
        assertEquals(1, stats.longestStreakDays)
    }
}
