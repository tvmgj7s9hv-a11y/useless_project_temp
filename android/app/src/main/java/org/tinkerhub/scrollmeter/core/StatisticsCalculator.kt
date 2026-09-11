package org.tinkerhub.scrollmeter.core

import org.tinkerhub.scrollmeter.data.DailyStat
import org.tinkerhub.scrollmeter.data.ScrollDao
import org.tinkerhub.scrollmeter.data.ScrollSession
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class SummaryStatistics(
    val todayDistanceMeters: Double,
    val todayDurationSeconds: Long,
    val todaySessionCount: Int,
    val thisWeekDistanceMeters: Double,
    val thisMonthDistanceMeters: Double,
    val averageDailyDistanceMeters: Double,
    val longestSessionDurationSeconds: Long,
    val longestSessionDistanceMeters: Double,
    val highestDayDistanceMeters: Double,
    val highestDayDate: String,
    val totalLifetimeDistanceMeters: Double,
    val totalLifetimeDurationSeconds: Long
)

class StatisticsCalculator(private val dao: ScrollDao) {

    suspend fun calculateFullStatistics(): SummaryStatistics {
        val todayStr = getTodayDateString()
        val todayStat = dao.getDailyStat(todayStr)

        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        // Calculate this week start date (last 7 days)
        calendar.time = Date()
        calendar.add(Calendar.DAY_OF_YEAR, -6)
        val weekStartStr = dateFormat.format(calendar.time)

        // Calculate this month start date (first day of current month)
        calendar.time = Date()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val monthStartStr = dateFormat.format(calendar.time)

        val weekDistance = dao.getDistanceBetweenDates(weekStartStr, todayStr) ?: 0.0
        val monthDistance = dao.getDistanceBetweenDates(monthStartStr, todayStr) ?: 0.0

        val allStats = dao.getAllDailyStats()
        val avgDistance = if (allStats.isNotEmpty()) {
            allStats.map { it.totalDistanceMeters }.average()
        } else {
            0.0
        }

        val highestDay = dao.getHighestDistanceDay()
        val longestSession = dao.getLongestSession()

        val lifetimeDistance = allStats.sumOf { it.totalDistanceMeters }
        val lifetimeDuration = allStats.sumOf { it.totalDurationSeconds }

        return SummaryStatistics(
            todayDistanceMeters = todayStat?.totalDistanceMeters ?: 0.0,
            todayDurationSeconds = todayStat?.totalDurationSeconds ?: 0L,
            todaySessionCount = todayStat?.sessionCount ?: 0,
            thisWeekDistanceMeters = weekDistance,
            thisMonthDistanceMeters = monthDistance,
            averageDailyDistanceMeters = avgDistance,
            longestSessionDurationSeconds = longestSession?.durationSeconds ?: 0L,
            longestSessionDistanceMeters = longestSession?.distanceMeters ?: 0.0,
            highestDayDistanceMeters = highestDay?.totalDistanceMeters ?: 0.0,
            highestDayDate = highestDay?.dateString ?: "None",
            totalLifetimeDistanceMeters = lifetimeDistance,
            totalLifetimeDurationSeconds = lifetimeDuration
        )
    }

    private fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }
}
