package com.prsnl.storage.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.prsnl.storage.entity.WritingStatEntity
import kotlinx.coroutines.flow.Flow

data class AggregateStatResult(
    val totalUnits: Float,
    val totalStrokes: Int
)

data class NotebookStatRow(
    val notebookId: String,
    val folderName: String,
    val totalUnits: Float,
    val totalStrokes: Int
)

data class FolderStatRow(
    val folderName: String,
    val totalUnits: Float,
    val totalStrokes: Int
)

data class DailyStatRow(
    val date: String,
    val totalUnits: Float,
    val totalStrokes: Int
)

data class MonthlyStatRow(
    val month: String,
    val totalUnits: Float,
    val totalStrokes: Int
)

@Dao
interface WritingStatDao {

    @Query(
        """
        INSERT INTO writing_stats (date, notebookId, folderName, strokeCount, inkLengthUnits)
        VALUES (:date, :notebookId, :folderName, MAX(0, :strokeCountDelta), MAX(0.0, :lengthUnitsDelta))
        ON CONFLICT(date, notebookId) DO UPDATE SET
            strokeCount = MAX(0, strokeCount + :strokeCountDelta),
            inkLengthUnits = MAX(0.0, inkLengthUnits + :lengthUnitsDelta),
            folderName = :folderName
        """
    )
    suspend fun upsertStat(
        date: String,
        notebookId: String,
        folderName: String,
        strokeCountDelta: Int,
        lengthUnitsDelta: Float
    )

    @Query("DELETE FROM writing_stats WHERE notebookId = :notebookId")
    suspend fun deleteStatsForNotebook(notebookId: String)

    @Query("DELETE FROM writing_stats")
    suspend fun clearAllStats()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(entity: WritingStatEntity)

    @Query("SELECT COALESCE(SUM(inkLengthUnits), 0.0) as totalUnits, COALESCE(SUM(strokeCount), 0) as totalStrokes FROM writing_stats")
    fun getTotalStats(): Flow<AggregateStatResult?>

    @Query("SELECT COALESCE(SUM(inkLengthUnits), 0.0) as totalUnits, COALESCE(SUM(strokeCount), 0) as totalStrokes FROM writing_stats")
    suspend fun getTotalStatsSync(): AggregateStatResult?

    @Query("SELECT COALESCE(SUM(inkLengthUnits), 0.0) as totalUnits, COALESCE(SUM(strokeCount), 0) as totalStrokes FROM writing_stats WHERE date LIKE :yearPrefix || '%'")
    suspend fun getYearlyStatsSync(yearPrefix: String): AggregateStatResult?

    @Query("SELECT COALESCE(SUM(inkLengthUnits), 0.0) as totalUnits, COALESCE(SUM(strokeCount), 0) as totalStrokes FROM writing_stats WHERE date LIKE :monthPrefix || '%'")
    suspend fun getMonthlyStatsSync(monthPrefix: String): AggregateStatResult?

    @Query("""
        SELECT notebookId, folderName, SUM(inkLengthUnits) as totalUnits, SUM(strokeCount) as totalStrokes
        FROM writing_stats
        GROUP BY notebookId
        ORDER BY totalUnits DESC
        LIMIT :limit
    """)
    fun getTopNotebooks(limit: Int = 5): Flow<List<NotebookStatRow>>

    @Query("""
        SELECT notebookId, folderName, SUM(inkLengthUnits) as totalUnits, SUM(strokeCount) as totalStrokes
        FROM writing_stats
        WHERE date LIKE :yearPrefix || '%'
        GROUP BY notebookId
        ORDER BY totalUnits DESC
        LIMIT :limit
    """)
    suspend fun getTopNotebooksForYearSync(yearPrefix: String, limit: Int = 5): List<NotebookStatRow>

    @Query("""
        SELECT folderName, SUM(inkLengthUnits) as totalUnits, SUM(strokeCount) as totalStrokes
        FROM writing_stats
        GROUP BY folderName
        ORDER BY totalUnits DESC
        LIMIT :limit
    """)
    fun getTopFolders(limit: Int = 5): Flow<List<FolderStatRow>>

    @Query("""
        SELECT folderName, SUM(inkLengthUnits) as totalUnits, SUM(strokeCount) as totalStrokes
        FROM writing_stats
        WHERE date LIKE :yearPrefix || '%'
        GROUP BY folderName
        ORDER BY totalUnits DESC
        LIMIT :limit
    """)
    suspend fun getTopFoldersForYearSync(yearPrefix: String, limit: Int = 5): List<FolderStatRow>

    @Query("SELECT DISTINCT date FROM writing_stats WHERE inkLengthUnits > 0 ORDER BY date ASC")
    suspend fun getAllActiveDatesSync(): List<String>

    @Query("""
        SELECT date, SUM(inkLengthUnits) as totalUnits, SUM(strokeCount) as totalStrokes
        FROM writing_stats
        WHERE date >= :startDate
        GROUP BY date
        ORDER BY date ASC
    """)
    fun getRecentDailyStats(startDate: String): Flow<List<DailyStatRow>>

    @Query("""
        SELECT date, SUM(inkLengthUnits) as totalUnits, SUM(strokeCount) as totalStrokes
        FROM writing_stats
        WHERE date LIKE :yearPrefix || '%'
        GROUP BY date
        ORDER BY date ASC
    """)
    suspend fun getDailyActivityForYearSync(yearPrefix: String): List<DailyStatRow>

    @Query("""
        SELECT date, SUM(inkLengthUnits) as totalUnits, SUM(strokeCount) as totalStrokes
        FROM writing_stats
        GROUP BY date
        ORDER BY totalUnits DESC
        LIMIT 1
    """)
    suspend fun getBusiestDaySync(): DailyStatRow?

    @Query("""
        SELECT SUBSTR(date, 1, 7) as month, SUM(inkLengthUnits) as totalUnits, SUM(strokeCount) as totalStrokes
        FROM writing_stats
        WHERE date LIKE :yearPrefix || '%'
        GROUP BY month
        ORDER BY totalUnits DESC
        LIMIT 1
    """)
    suspend fun getBusiestMonthForYearSync(yearPrefix: String): MonthlyStatRow?

    @Query("""
        SELECT SUBSTR(date, 1, 7) as month, SUM(inkLengthUnits) as totalUnits, SUM(strokeCount) as totalStrokes
        FROM writing_stats
        GROUP BY month
        ORDER BY totalUnits DESC
        LIMIT 1
    """)
    suspend fun getBusiestMonthAllTimeSync(): MonthlyStatRow?
}
