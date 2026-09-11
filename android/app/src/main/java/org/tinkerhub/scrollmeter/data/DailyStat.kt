package org.tinkerhub.scrollmeter.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Aggregated statistics for a specific calendar day.
 */
@Entity(tableName = "daily_stats")
data class DailyStat(
    @PrimaryKey
    val dateString: String, // Format: YYYY-MM-DD
    val totalDistanceMeters: Double = 0.0,
    val totalDurationSeconds: Long = 0L,
    val sessionCount: Int = 0,
    val scrollEventCount: Int = 0,
    val highestSessionDistanceMeters: Double = 0.0,
    val lastUpdated: Long = System.currentTimeMillis()
)
