package com.prsnl.storage.repository

import kotlinx.coroutines.flow.Flow

data class OverallWritingStats(
    val totalMetersAllTime: Float = 0f,
    val totalMetersThisYear: Float = 0f,
    val totalMetersThisMonth: Float = 0f,
    val totalStrokes: Int = 0,
    val currentStreakDays: Int = 0,
    val longestStreakDays: Int = 0,
    val totalPagesCreated: Int = 0,
    val busiestDayDate: String? = null,
    val busiestDayMeters: Float = 0f,
    val busiestMonth: String? = null,
    val busiestMonthMeters: Float = 0f
)

data class TopNotebookStat(
    val notebookId: String,
    val notebookTitle: String,
    val folderName: String,
    val coverColor: Int,
    val metersWritten: Float,
    val strokeCount: Int
)

data class TopFolderStat(
    val folderName: String,
    val folderColor: Int,
    val metersWritten: Float,
    val strokeCount: Int
)

data class DailyBarStat(
    val date: String,            // yyyy-MM-dd
    val dayOfWeekLabel: String,  // e.g. "M", "T", "W", "T", "F", "S", "S"
    val metersWritten: Float,
    val strokeCount: Int
)

data class YearlyWrappedData(
    val year: String,
    val totalMetersThisYear: Float = 0f,
    val totalStrokesThisYear: Int = 0,
    val totalPagesThisYear: Int = 0,
    val longestStreakDays: Int = 0,
    val activeDaysCount: Int = 0,
    val topNotebook: TopNotebookStat? = null,
    val topFolder: TopFolderStat? = null,
    val busiestMonth: String? = null,
    val busiestMonthMeters: Float = 0f,
    val monthlyActivity: List<Pair<String, Float>> = emptyList(),
    val comparisonLandmark: String = ""
)

interface StatsRepository {
    suspend fun recordStroke(
        notebookId: String,
        folderName: String,
        lengthUnits: Float
    )

    suspend fun recordStrokeDelta(
        notebookId: String,
        folderName: String,
        strokeDelta: Int,
        lengthUnitsDelta: Float
    )

    suspend fun recalculateAllStatsFromNotebooks()

    fun getOverallStatsFlow(): Flow<OverallWritingStats>
    suspend fun getOverallStatsSync(): OverallWritingStats

    fun getTopNotebooksFlow(limit: Int = 5): Flow<List<TopNotebookStat>>
    suspend fun getTopNotebooksSync(limit: Int = 5): List<TopNotebookStat>

    fun getTopFoldersFlow(limit: Int = 5): Flow<List<TopFolderStat>>
    suspend fun getTopFoldersSync(limit: Int = 5): List<TopFolderStat>

    fun getRecentDailyActivityFlow(days: Int = 7): Flow<List<DailyBarStat>>
    suspend fun getRecentDailyActivitySync(days: Int = 7): List<DailyBarStat>

    suspend fun getYearlyWrapped(year: String): YearlyWrappedData
}
