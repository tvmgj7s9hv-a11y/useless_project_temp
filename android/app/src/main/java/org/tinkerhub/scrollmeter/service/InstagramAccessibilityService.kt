package org.tinkerhub.scrollmeter.service

import android.accessibilityservice.AccessibilityService
import android.util.DisplayMetrics
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.tinkerhub.scrollmeter.ScrollMeterApplication
import org.tinkerhub.scrollmeter.core.MilestoneManager
import org.tinkerhub.scrollmeter.core.ScrollDistanceEstimator
import org.tinkerhub.scrollmeter.data.DailyStat
import org.tinkerhub.scrollmeter.data.ScrollSession
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * InstagramAccessibilityService
 *
 * Runs as a background AccessibilityService. Legally monitors when the Instagram application
 * (com.instagram.android) is in the foreground and estimates vertical scroll displacement.
 *
 * Privacy & Security Guarantees:
 * - canRetrieveWindowContent is set to FALSE.
 * - Does NOT inspect text messages, media, photos, comments, user credentials, or direct messages.
 * - Only listens to window state transitions and scroll gesture events.
 * - 100% on-device processing. No external network data transmission.
 */
class InstagramAccessibilityService : AccessibilityService() {

    companion object {
        const val INSTAGRAM_PACKAGE = "com.instagram.android"

        // State flows for UI observation when ScrollMeter is open
        private val _isServiceConnected = MutableStateFlow(false)
        val isServiceConnected: StateFlow<Boolean> = _isServiceConnected.asStateFlow()

        private val _isInstagramActive = MutableStateFlow(false)
        val isInstagramActive: StateFlow<Boolean> = _isInstagramActive.asStateFlow()

        private val _currentSessionDistanceMeters = MutableStateFlow(0.0)
        val currentSessionDistanceMeters: StateFlow<Double> = _currentSessionDistanceMeters.asStateFlow()

        private val _currentSessionDurationSeconds = MutableStateFlow(0L)
        val currentSessionDurationSeconds: StateFlow<Long> = _currentSessionDurationSeconds.asStateFlow()
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var estimator: ScrollDistanceEstimator

    private var isSessionActive = false
    private var sessionStartTime = 0L
    private var sessionDistanceMeters = 0.0
    private var sessionScrollEvents = 0

    private var todayTotalMeters = 0.0

    override fun onServiceConnected() {
        super.onServiceConnected()
        _isServiceConnected.value = true

        // Calibrate estimator with device's physical screen Y-DPI
        val metrics = resources.displayMetrics
        val ydpi = metrics.ydpi
        estimator = ScrollDistanceEstimator(ydpi)

        // Preload today's total distance from database
        serviceScope.launch {
            val todayStr = getTodayDateString()
            val stat = ScrollMeterApplication.instance.database.scrollDao().getDailyStat(todayStr)
            todayTotalMeters = stat?.totalDistanceMeters ?: 0.0
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val prefs = ScrollMeterApplication.instance.preferencesRepository
        if (!prefs.isTrackingEnabled) {
            if (isSessionActive) {
                endInstagramSession()
            }
            return
        }

        val packageName = event.packageName?.toString() ?: ""

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOWS_CHANGED -> {
                handleWindowStateChanged(packageName)
            }

            AccessibilityEvent.TYPE_VIEW_SCROLLED -> {
                if (packageName == INSTAGRAM_PACKAGE) {
                    handleScrollEvent(event)
                }
            }
        }
    }

    private fun handleWindowStateChanged(packageName: String) {
        if (packageName == INSTAGRAM_PACKAGE) {
            if (!isSessionActive) {
                startInstagramSession()
            }
        } else if (isSessionActive && !isSystemDialog(packageName)) {
            // User left Instagram for another app or home launcher
            endInstagramSession()
        }
    }

    private fun isSystemDialog(packageName: String): Boolean {
        return packageName == "android" ||
               packageName.contains("systemui") ||
               packageName.contains("inputmethod")
    }

    private fun startInstagramSession() {
        isSessionActive = true
        sessionStartTime = System.currentTimeMillis()
        sessionDistanceMeters = 0.0
        sessionScrollEvents = 0

        _isInstagramActive.value = true
        _currentSessionDistanceMeters.value = 0.0
        _currentSessionDurationSeconds.value = 0L
    }

    private fun handleScrollEvent(event: AccessibilityEvent) {
        if (!isSessionActive) {
            startInstagramSession()
        }

        // On Android 7.0+ (API 24+), scrollDeltaY provides vertical scroll distance in pixels
        var deltaY = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            event.scrollDeltaY
        } else {
            0
        }

        // Fallback for feeds reporting index or unspecified delta
        if (deltaY == 0) {
            val itemCount = event.itemCount
            val fromIndex = event.fromIndex
            val toIndex = event.toIndex

            deltaY = if (toIndex > fromIndex) {
                // Scrolled down approximately 350 pixels (typical card item displacement)
                350
            } else if (fromIndex > toIndex) {
                // Scrolled up
                -350
            } else {
                // Default minimum gesture displacement
                250
            }
        }

        val estimatedMeters = estimator.estimateMetersFromPixels(deltaY)
        if (estimatedMeters <= 0.0) return

        val prevToday = todayTotalMeters
        sessionDistanceMeters += estimatedMeters
        sessionScrollEvents++
        todayTotalMeters += estimatedMeters

        val currentDuration = (System.currentTimeMillis() - sessionStartTime) / 1000
        _currentSessionDistanceMeters.value = sessionDistanceMeters
        _currentSessionDurationSeconds.value = currentDuration

        // Check for newly unlocked milestones
        val prefs = ScrollMeterApplication.instance.preferencesRepository
        if (prefs.isNotificationEnabled) {
            val newMilestones = MilestoneManager.getNewlyCrossedMilestones(prevToday, todayTotalMeters)
            for (milestone in newMilestones) {
                ScrollMeterApplication.instance.notificationHelper.showMilestoneNotification(milestone)
            }

            // Periodic usage reminders (e.g. every 500m)
            val step = prefs.notificationFrequencyMeters
            if (step > 0 && (todayTotalMeters - prefs.lastNotifiedMetersToday) >= step) {
                prefs.lastNotifiedMetersToday = todayTotalMeters
                val formatted = estimator.formatDistance(todayTotalMeters, prefs.distanceUnit.name == "METRIC")
                val comment = MilestoneManager.getWittyComparison(todayTotalMeters)
                ScrollMeterApplication.instance.notificationHelper.showUsageReminder(formatted, comment)
            }
        }
    }

    private fun endInstagramSession() {
        if (!isSessionActive) return

        val endTime = System.currentTimeMillis()
        val durationSeconds = ((endTime - sessionStartTime) / 1000).coerceAtLeast(1)
        val distance = sessionDistanceMeters
        val eventCount = sessionScrollEvents
        val dateString = getTodayDateString()

        isSessionActive = false
        _isInstagramActive.value = false

        if (distance > 0.0 || durationSeconds > 5) {
            serviceScope.launch {
                val db = ScrollMeterApplication.instance.database.scrollDao()

                // Insert individual session
                val session = ScrollSession(
                    startTime = sessionStartTime,
                    endTime = endTime,
                    durationSeconds = durationSeconds,
                    distanceMeters = distance,
                    scrollEventCount = eventCount,
                    dateString = dateString
                )
                db.insertSession(session)

                // Update aggregated daily stat
                val existing = db.getDailyStat(dateString)
                val updatedStat = DailyStat(
                    dateString = dateString,
                    totalDistanceMeters = (existing?.totalDistanceMeters ?: 0.0) + distance,
                    totalDurationSeconds = (existing?.totalDurationSeconds ?: 0L) + durationSeconds,
                    sessionCount = (existing?.sessionCount ?: 0) + 1,
                    scrollEventCount = (existing?.scrollEventCount ?: 0) + eventCount,
                    highestSessionDistanceMeters = maxOf(existing?.highestSessionDistanceMeters ?: 0.0, distance),
                    lastUpdated = System.currentTimeMillis()
                )
                db.insertOrUpdateDailyStat(updatedStat)
            }
        }
    }

    override fun onInterrupt() {
        endInstagramSession()
        _isServiceConnected.value = false
    }

    override fun onDestroy() {
        endInstagramSession()
        _isServiceConnected.value = false
        super.onDestroy()
    }

    private fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }
}
