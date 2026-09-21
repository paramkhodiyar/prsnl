package com.prsnl.storage.repository

import com.prsnl.core.util.DistanceUtils
import com.prsnl.storage.dao.FolderDao
import com.prsnl.storage.dao.NotebookDao
import com.prsnl.storage.dao.PageDao
import com.prsnl.storage.dao.WritingStatDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

class StatsRepositoryImpl(
    private val writingStatDao: WritingStatDao,
    private val notebookDao: NotebookDao,
    private val folderDao: FolderDao,
    private val pageDao: PageDao,
    private val pageFileStorage: com.prsnl.storage.PageFileStorage? = null
) : StatsRepository {

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US)

    constructor(
        writingStatDao: WritingStatDao,
        notebookDao: NotebookDao,
        folderDao: FolderDao,
        pageDao: PageDao
    ) : this(writingStatDao, notebookDao, folderDao, pageDao, null)

    override suspend fun recordStroke(
        notebookId: String,
        folderName: String,
        lengthUnits: Float
    ) = recordStrokeDelta(
        notebookId = notebookId,
        folderName = folderName,
        strokeDelta = 1,
        lengthUnitsDelta = lengthUnits
    )

    override suspend fun recordStrokeDelta(
        notebookId: String,
        folderName: String,
        strokeDelta: Int,
        lengthUnitsDelta: Float
    ) = withContext(Dispatchers.IO) {
        if (notebookId.isBlank() || (strokeDelta == 0 && lengthUnitsDelta == 0f)) return@withContext
        val today = LocalDate.now().format(dateFormatter)
        val safeFolder = if (folderName.isBlank()) "General" else folderName
        writingStatDao.upsertStat(
            date = today,
            notebookId = notebookId,
            folderName = safeFolder,
            strokeCountDelta = strokeDelta,
            lengthUnitsDelta = lengthUnitsDelta
        )
    }

    override suspend fun recalculateAllStatsFromNotebooks() = withContext(Dispatchers.IO) {
        val storage = pageFileStorage ?: return@withContext
        try {
            val allNotebooks = notebookDao.getAllNotebooksSync()
            val today = LocalDate.now().format(dateFormatter)
            writingStatDao.clearAllStats()

            for (nb in allNotebooks) {
                val pages = pageDao.getPagesForNotebookSync(nb.id)
                var totalUnits = 0f
                var totalStrokes = 0

                for (p in pages) {
                    val elements = try {
                        storage.loadPageElements(p.elementFilePath)
                    } catch (_: Exception) {
                        emptyList()
                    }
                    for (el in elements) {
                        if (el is com.prsnl.document.model.Stroke) {
                            totalStrokes++
                            totalUnits += DistanceUtils.calculatePathLength(el.points.map { Pair(it.x, it.y) })
                        }
                    }
                }

                if (totalStrokes > 0 && totalUnits > 0f) {
                    writingStatDao.upsertStat(
                        date = today,
                        notebookId = nb.id,
                        folderName = nb.folderName.ifBlank { "General" },
                        strokeCountDelta = totalStrokes,
                        lengthUnitsDelta = totalUnits
                    )
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("StatsRepo", "Error recalculating stats", e)
        }
    }

    override fun getOverallStatsFlow(): Flow<OverallWritingStats> {
        return writingStatDao.getTotalStats()
            .map {
                getOverallStatsSync()
            }
            .flowOn(Dispatchers.IO)
    }

    override suspend fun getOverallStatsSync(): OverallWritingStats = withContext(Dispatchers.IO) {
        val today = LocalDate.now()
        val yearPrefix = today.year.toString()
        val monthPrefix = today.format(DateTimeFormatter.ofPattern("yyyy-MM", Locale.US))

        val allTimeAgg = writingStatDao.getTotalStatsSync()
        val yearAgg = writingStatDao.getYearlyStatsSync(yearPrefix)
        val monthAgg = writingStatDao.getMonthlyStatsSync(monthPrefix)

        val totalMetersAllTime = DistanceUtils.unitsToMeters(allTimeAgg?.totalUnits ?: 0f)
        val totalMetersThisYear = DistanceUtils.unitsToMeters(yearAgg?.totalUnits ?: 0f)
        val totalMetersThisMonth = DistanceUtils.unitsToMeters(monthAgg?.totalUnits ?: 0f)
        val totalStrokes = allTimeAgg?.totalStrokes ?: 0

        val (currentStreak, longestStreak) = calculateStreaks(writingStatDao.getAllActiveDatesSync())

        val totalPages = pageDao.getAllPagesSync().size

        val busiestDayRow = writingStatDao.getBusiestDaySync()
        val busiestDayDate = busiestDayRow?.date
        val busiestDayMeters = DistanceUtils.unitsToMeters(busiestDayRow?.totalUnits ?: 0f)

        val busiestMonthRow = writingStatDao.getBusiestMonthAllTimeSync()
        val busiestMonth = busiestMonthRow?.month
        val busiestMonthMeters = DistanceUtils.unitsToMeters(busiestMonthRow?.totalUnits ?: 0f)

        OverallWritingStats(
            totalMetersAllTime = totalMetersAllTime,
            totalMetersThisYear = totalMetersThisYear,
            totalMetersThisMonth = totalMetersThisMonth,
            totalStrokes = totalStrokes,
            currentStreakDays = currentStreak,
            longestStreakDays = longestStreak,
            totalPagesCreated = totalPages,
            busiestDayDate = busiestDayDate,
            busiestDayMeters = busiestDayMeters,
            busiestMonth = busiestMonth,
            busiestMonthMeters = busiestMonthMeters
        )
    }

    override fun getTopNotebooksFlow(limit: Int): Flow<List<TopNotebookStat>> {
        return writingStatDao.getTopNotebooks(limit)
            .map { rows ->
                val allNotebooks = notebookDao.getAllNotebooksSync().associateBy { it.id }
                rows.map { row ->
                    val nb = allNotebooks[row.notebookId]
                    TopNotebookStat(
                        notebookId = row.notebookId,
                        notebookTitle = nb?.title ?: "Notebook",
                        folderName = nb?.folderName ?: row.folderName,
                        coverColor = nb?.coverColor ?: 0xFF4C6EF5.toInt(),
                        metersWritten = DistanceUtils.unitsToMeters(row.totalUnits),
                        strokeCount = row.totalStrokes
                    )
                }
            }
            .flowOn(Dispatchers.IO)
    }

    override suspend fun getTopNotebooksSync(limit: Int): List<TopNotebookStat> = withContext(Dispatchers.IO) {
        val rows = writingStatDao.getTopNotebooksForYearSync(LocalDate.now().year.toString(), limit)
        val allNotebooks = notebookDao.getAllNotebooksSync().associateBy { it.id }
        rows.map { row ->
            val nb = allNotebooks[row.notebookId]
            TopNotebookStat(
                notebookId = row.notebookId,
                notebookTitle = nb?.title ?: "Notebook",
                folderName = nb?.folderName ?: row.folderName,
                coverColor = nb?.coverColor ?: 0xFF4C6EF5.toInt(),
                metersWritten = DistanceUtils.unitsToMeters(row.totalUnits),
                strokeCount = row.totalStrokes
            )
        }
    }

    override fun getTopFoldersFlow(limit: Int): Flow<List<TopFolderStat>> {
        return writingStatDao.getTopFolders(limit)
            .map { rows ->
                val allFolders = folderDao.getAllFoldersSync().associateBy { it.name }
                rows.map { row ->
                    val folder = allFolders[row.folderName]
                    TopFolderStat(
                        folderName = row.folderName,
                        folderColor = folder?.color ?: 0xFF3B82F6.toInt(),
                        metersWritten = DistanceUtils.unitsToMeters(row.totalUnits),
                        strokeCount = row.totalStrokes
                    )
                }
            }
            .flowOn(Dispatchers.IO)
    }

    override suspend fun getTopFoldersSync(limit: Int): List<TopFolderStat> = withContext(Dispatchers.IO) {
        val rows = writingStatDao.getTopFoldersForYearSync(LocalDate.now().year.toString(), limit)
        val allFolders = folderDao.getAllFoldersSync().associateBy { it.name }
        rows.map { row ->
            val folder = allFolders[row.folderName]
            TopFolderStat(
                folderName = row.folderName,
                folderColor = folder?.color ?: 0xFF3B82F6.toInt(),
                metersWritten = DistanceUtils.unitsToMeters(row.totalUnits),
                strokeCount = row.totalStrokes
            )
        }
    }

    override fun getRecentDailyActivityFlow(days: Int): Flow<List<DailyBarStat>> {
        val startDate = LocalDate.now().minusDays((days - 1).toLong()).format(dateFormatter)
        return writingStatDao.getRecentDailyStats(startDate)
            .map { rows ->
                buildDailyBarList(rows, days)
            }
            .flowOn(Dispatchers.IO)
    }

    override suspend fun getRecentDailyActivitySync(days: Int): List<DailyBarStat> = withContext(Dispatchers.IO) {
        val startDate = LocalDate.now().minusDays((days - 1).toLong()).format(dateFormatter)
        val dailyMap = writingStatDao.getDailyActivityForYearSync(LocalDate.now().year.toString()).associateBy { it.date }
        val result = mutableListOf<DailyBarStat>()
        for (i in (days - 1) downTo 0) {
            val d = LocalDate.now().minusDays(i.toLong())
            val dateStr = d.format(dateFormatter)
            val row = dailyMap[dateStr]
            val label = d.dayOfWeek.name.take(1)
            result.add(
                DailyBarStat(
                    date = dateStr,
                    dayOfWeekLabel = label,
                    metersWritten = DistanceUtils.unitsToMeters(row?.totalUnits ?: 0f),
                    strokeCount = row?.totalStrokes ?: 0
                )
            )
        }
        result
    }

    private fun buildDailyBarList(rows: List<com.prsnl.storage.dao.DailyStatRow>, days: Int): List<DailyBarStat> {
        val rowMap = rows.associateBy { it.date }
        val result = mutableListOf<DailyBarStat>()
        val today = LocalDate.now()
        for (i in (days - 1) downTo 0) {
            val d = today.minusDays(i.toLong())
            val dateStr = d.format(dateFormatter)
            val row = rowMap[dateStr]
            val label = d.dayOfWeek.name.take(1)
            result.add(
                DailyBarStat(
                    date = dateStr,
                    dayOfWeekLabel = label,
                    metersWritten = DistanceUtils.unitsToMeters(row?.totalUnits ?: 0f),
                    strokeCount = row?.totalStrokes ?: 0
                )
            )
        }
        return result
    }

    override suspend fun getYearlyWrapped(year: String): YearlyWrappedData = withContext(Dispatchers.IO) {
        val yearAgg = writingStatDao.getYearlyStatsSync(year)
        val totalMeters = DistanceUtils.unitsToMeters(yearAgg?.totalUnits ?: 0f)
        val totalStrokes = yearAgg?.totalStrokes ?: 0

        val topNbs = writingStatDao.getTopNotebooksForYearSync(year, 1)
        val topNotebookStat = topNbs.firstOrNull()?.let { row ->
            val nb = notebookDao.getNotebookById(row.notebookId)
            TopNotebookStat(
                notebookId = row.notebookId,
                notebookTitle = nb?.title ?: "Top Notebook",
                folderName = nb?.folderName ?: row.folderName,
                coverColor = nb?.coverColor ?: 0xFF4C6EF5.toInt(),
                metersWritten = DistanceUtils.unitsToMeters(row.totalUnits),
                strokeCount = row.totalStrokes
            )
        }

        val topFls = writingStatDao.getTopFoldersForYearSync(year, 1)
        val topFolderStat = topFls.firstOrNull()?.let { row ->
            val allFolders = folderDao.getAllFoldersSync().associateBy { it.name }
            val f = allFolders[row.folderName]
            TopFolderStat(
                folderName = row.folderName,
                folderColor = f?.color ?: 0xFF3B82F6.toInt(),
                metersWritten = DistanceUtils.unitsToMeters(row.totalUnits),
                strokeCount = row.totalStrokes
            )
        }

        val activeDatesInYear = writingStatDao.getAllActiveDatesSync().filter { it.startsWith(year) }
        val (_, longestStreak) = calculateStreaks(activeDatesInYear)

        val busiestMonthRow = writingStatDao.getBusiestMonthForYearSync(year)
        val busiestMonth = busiestMonthRow?.month
        val busiestMonthMeters = DistanceUtils.unitsToMeters(busiestMonthRow?.totalUnits ?: 0f)

        val totalPages = pageDao.getAllPagesSync().size

        YearlyWrappedData(
            year = year,
            totalMetersThisYear = totalMeters,
            totalStrokesThisYear = totalStrokes,
            totalPagesThisYear = totalPages,
            longestStreakDays = longestStreak,
            activeDaysCount = activeDatesInYear.size,
            topNotebook = topNotebookStat,
            topFolder = topFolderStat,
            busiestMonth = busiestMonth,
            busiestMonthMeters = busiestMonthMeters,
            comparisonLandmark = DistanceUtils.getLandmarkComparison(totalMeters)
        )
    }

    private fun calculateStreaks(activeDatesSorted: List<String>): Pair<Int, Int> {
        if (activeDatesSorted.isEmpty()) return Pair(0, 0)

        val parsedDates = activeDatesSorted.mapNotNull {
            try {
                LocalDate.parse(it, dateFormatter)
            } catch (e: Exception) {
                null
            }
        }.distinct().sorted()

        if (parsedDates.isEmpty()) return Pair(0, 0)

        var maxStreak = 1
        var tempStreak = 1

        for (i in 1 until parsedDates.size) {
            val diff = ChronoUnit.DAYS.between(parsedDates[i - 1], parsedDates[i])
            if (diff == 1L) {
                tempStreak++
                if (tempStreak > maxStreak) {
                    maxStreak = tempStreak
                }
            } else if (diff > 1L) {
                tempStreak = 1
            }
        }

        // Current streak calculation
        val today = LocalDate.now()
        val lastDate = parsedDates.last()
        val daysFromLast = ChronoUnit.DAYS.between(lastDate, today)

        val currentStreak = if (daysFromLast == 0L || daysFromLast == 1L) {
            var curr = 1
            var prev = lastDate
            for (i in (parsedDates.size - 2) downTo 0) {
                val candidate = parsedDates[i]
                if (ChronoUnit.DAYS.between(candidate, prev) == 1L) {
                    curr++
                    prev = candidate
                } else {
                    break
                }
            }
            curr
        } else {
            0
        }

        return Pair(currentStreak, maxStreak)
    }
}
