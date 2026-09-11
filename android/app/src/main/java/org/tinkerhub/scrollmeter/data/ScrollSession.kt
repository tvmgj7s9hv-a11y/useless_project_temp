package org.tinkerhub.scrollmeter.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a single continuous Instagram foreground session.
 */
@Entity(tableName = "scroll_sessions")
data class ScrollSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val startTime: Long,
    val endTime: Long,
    val durationSeconds: Long,
    val distanceMeters: Double,
    val scrollEventCount: Int,
    val dateString: String // YYYY-MM-DD for fast grouping
)
