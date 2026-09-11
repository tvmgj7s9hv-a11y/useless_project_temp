package org.tinkerhub.scrollmeter.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScrollDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ScrollSession): Long

    @Query("SELECT * FROM scroll_sessions WHERE dateString = :dateString ORDER BY startTime DESC")
    fun getSessionsForDate(dateString: String): Flow<List<ScrollSession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateDailyStat(stat: DailyStat)

    @Query("SELECT * FROM daily_stats WHERE dateString = :dateString LIMIT 1")
    suspend fun getDailyStat(dateString: String): DailyStat?

    @Query("SELECT * FROM daily_stats WHERE dateString = :dateString LIMIT 1")
    fun getDailyStatFlow(dateString: String): Flow<DailyStat?>

    @Query("SELECT * FROM daily_stats ORDER BY dateString DESC LIMIT 30")
    fun getRecentDailyStats(): Flow<List<DailyStat>>

    @Query("SELECT * FROM daily_stats ORDER BY dateString DESC")
    suspend fun getAllDailyStats(): List<DailyStat>

    @Query("SELECT SUM(totalDistanceMeters) FROM daily_stats WHERE dateString >= :startDate AND dateString <= :endDate")
    suspend fun getDistanceBetweenDates(startDate: String, endDate: String): Double?

    @Query("SELECT SUM(totalDistanceMeters) FROM daily_stats")
    fun getTotalLifetimeDistance(): Flow<Double?>

    @Query("SELECT SUM(totalDurationSeconds) FROM daily_stats")
    fun getTotalLifetimeDuration(): Flow<Long?>

    @Query("SELECT * FROM daily_stats ORDER BY totalDistanceMeters DESC LIMIT 1")
    suspend fun getHighestDistanceDay(): DailyStat?

    @Query("SELECT * FROM scroll_sessions ORDER BY durationSeconds DESC LIMIT 1")
    suspend fun getLongestSession(): ScrollSession?

    @Query("DELETE FROM scroll_sessions WHERE dateString = :dateString")
    suspend fun deleteSessionsForDate(dateString: String)

    @Query("DELETE FROM daily_stats WHERE dateString = :dateString")
    suspend fun deleteDailyStat(dateString: String)

    @Query("DELETE FROM scroll_sessions")
    suspend fun deleteAllSessions()

    @Query("DELETE FROM daily_stats")
    suspend fun deleteAllDailyStats()
}
