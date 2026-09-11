package org.tinkerhub.scrollmeter.data

import android.content.Context
import android.content.SharedPreferences

enum class DistanceUnit {
    METRIC,    // Meters & Kilometers
    IMPERIAL   // Feet & Miles
}

class UserPreferencesRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("scroll_meter_preferences", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_TRACKING_ENABLED = "tracking_enabled"
        private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
        private const val KEY_NOTIFICATION_FREQUENCY = "notification_frequency_meters"
        private const val KEY_DAILY_GOAL_METERS = "daily_goal_meters"
        private const val KEY_DISTANCE_UNIT = "distance_unit"
        private const val KEY_LAST_NOTIFIED_METERS = "last_notified_meters"
    }

    var isTrackingEnabled: Boolean
        get() = prefs.getBoolean(KEY_TRACKING_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_TRACKING_ENABLED, value).apply()

    var isNotificationEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, value).apply()

    var notificationFrequencyMeters: Double
        get() = prefs.getFloat(KEY_NOTIFICATION_FREQUENCY, 500.0f).toDouble()
        set(value) = prefs.edit().putFloat(KEY_NOTIFICATION_FREQUENCY, value.toFloat()).apply()

    var dailyGoalMeters: Double
        get() = prefs.getFloat(KEY_DAILY_GOAL_METERS, 2000.0f).toDouble()
        set(value) = prefs.edit().putFloat(KEY_DAILY_GOAL_METERS, value.toFloat()).apply()

    var distanceUnit: DistanceUnit
        get() {
            val name = prefs.getString(KEY_DISTANCE_UNIT, DistanceUnit.METRIC.name)
            return try {
                DistanceUnit.valueOf(name ?: DistanceUnit.METRIC.name)
            } catch (e: Exception) {
                DistanceUnit.METRIC
            }
        }
        set(value) = prefs.edit().putString(KEY_DISTANCE_UNIT, value.name).apply()

    var lastNotifiedMetersToday: Double
        get() = prefs.getFloat(KEY_LAST_NOTIFIED_METERS, 0.0f).toDouble()
        set(value) = prefs.edit().putFloat(KEY_LAST_NOTIFIED_METERS, value.toFloat()).apply()

    fun resetPreferences() {
        prefs.edit().clear().apply()
    }
}
