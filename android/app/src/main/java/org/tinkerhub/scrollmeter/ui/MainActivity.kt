package org.tinkerhub.scrollmeter.ui

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.accessibility.AccessibilityManager
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.tinkerhub.scrollmeter.R
import org.tinkerhub.scrollmeter.ScrollMeterApplication
import org.tinkerhub.scrollmeter.core.MilestoneManager
import org.tinkerhub.scrollmeter.core.ScrollDistanceEstimator
import org.tinkerhub.scrollmeter.core.StatisticsCalculator
import org.tinkerhub.scrollmeter.data.DailyStat
import org.tinkerhub.scrollmeter.data.DistanceUnit
import org.tinkerhub.scrollmeter.databinding.ActivityMainBinding
import org.tinkerhub.scrollmeter.databinding.ItemHistoryBinding
import org.tinkerhub.scrollmeter.databinding.ItemMilestoneBinding
import org.tinkerhub.scrollmeter.service.InstagramAccessibilityService
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val app get() = ScrollMeterApplication.instance
    private val estimator by lazy { ScrollDistanceEstimator(resources.displayMetrics.ydpi) }
    private val statsCalculator by lazy { StatisticsCalculator(app.database.scrollDao()) }

    private var currentTab = 0 // 0: Today, 1: History, 2: Stats, 3: Settings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavigation()
        setupSettings()
        observeTrackingState()
        observeTodayData()
    }

    override fun onResume() {
        super.onResume()
        checkAccessibilityServiceState()
        refreshAllScreens()
    }

    private fun setupNavigation() {
        binding.btnNavToday.setOnClickListener { switchTab(0) }
        binding.btnNavHistory.setOnClickListener { switchTab(1) }
        binding.btnNavStats.setOnClickListener { switchTab(2) }
        binding.btnNavSettings.setOnClickListener { switchTab(3) }

        binding.btnEnableAccessibility.setOnClickListener {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        }

        binding.btnResetMeter.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Reset ScrollMeter?")
                .setMessage("Reset today's Instagram scrolling distance back to 0?")
                .setPositiveButton("Reset") { _, _ ->
                    val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                    lifecycleScope.launch {
                        app.database.scrollDao().deleteDailyStat(todayStr)
                        app.database.scrollDao().deleteSessionsForDate(todayStr)
                        InstagramAccessibilityService.resetRunningMeters()
                        updateTodayUI(null)
                        refreshAllScreens()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun switchTab(tabIndex: Int) {
        currentTab = tabIndex
        binding.tabViewToday.visibility = if (tabIndex == 0) View.VISIBLE else View.GONE
        binding.tabViewHistory.visibility = if (tabIndex == 1) View.VISIBLE else View.GONE
        binding.tabViewStats.visibility = if (tabIndex == 2) View.VISIBLE else View.GONE
        binding.tabViewSettings.visibility = if (tabIndex == 3) View.VISIBLE else View.GONE

        // Color active navigation item
        val activeColor = getColor(R.color.accent_primary)
        val mutedColor = getColor(R.color.text_muted)

        binding.tvNavTodayLabel.setTextColor(if (tabIndex == 0) activeColor else mutedColor)
        binding.tvNavHistoryLabel.setTextColor(if (tabIndex == 1) activeColor else mutedColor)
        binding.tvNavStatsLabel.setTextColor(if (tabIndex == 2) activeColor else mutedColor)
        binding.tvNavSettingsLabel.setTextColor(if (tabIndex == 3) activeColor else mutedColor)

        if (tabIndex == 1) populateHistory()
        if (tabIndex == 2) populateStatistics()
    }

    private fun checkAccessibilityServiceState() {
        val am = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        val isServiceRunning = enabledServices.any {
            it.resolveInfo.serviceInfo.packageName == packageName
        }

        if (isServiceRunning) {
            binding.cardPermissionAlert.visibility = View.GONE
            binding.statusText.text = "TRACKING"
            binding.statusText.setTextColor(getColor(R.color.badge_active))
            binding.statusIndicatorDot.backgroundTintList = getColorStateList(R.color.badge_active)
        } else {
            binding.cardPermissionAlert.visibility = View.VISIBLE
            binding.statusText.text = "SETUP NEEDED"
            binding.statusText.setTextColor(Color.parseColor("#FBBF24"))
            binding.statusIndicatorDot.backgroundTintList = getColorStateList(android.R.color.holo_orange_light)
        }
    }

    private fun observeTrackingState() {
        lifecycleScope.launch {
            InstagramAccessibilityService.isInstagramActive.collectLatest { isActive ->
                binding.cardLiveSession.visibility = if (isActive) View.VISIBLE else View.GONE
            }
        }

        lifecycleScope.launch {
            InstagramAccessibilityService.currentSessionDistanceMeters.collectLatest { sessionMeters ->
                val seconds = InstagramAccessibilityService.currentSessionDurationSeconds.value
                val isMetric = app.preferencesRepository.distanceUnit == DistanceUnit.METRIC
                val distStr = estimator.formatDistance(sessionMeters, isMetric)
                val timeStr = estimator.formatDuration(seconds)
                binding.tvLiveSessionMetrics.text = "Live scrolling: $distStr ($timeStr)"
            }
        }
    }

    private fun observeTodayData() {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        lifecycleScope.launch {
            app.database.scrollDao().getDailyStatFlow(todayStr).collectLatest { todayStat ->
                updateTodayUI(todayStat)
            }
        }
    }

    private fun updateTodayUI(stat: DailyStat?) {
        val isMetric = app.preferencesRepository.distanceUnit == DistanceUnit.METRIC
        val distanceMeters = stat?.totalDistanceMeters ?: 0.0
        val durationSeconds = stat?.totalDurationSeconds ?: 0L
        val sessionCount = stat?.sessionCount ?: 0

        // Hero distance text
        binding.tvTodayDistance.text = estimator.formatDistance(distanceMeters, isMetric)
        binding.tvTodayDuration.text = "${estimator.formatDuration(durationSeconds)} active"

        // Goal Progress
        val goalMeters = app.preferencesRepository.dailyGoalMeters
        val percent = if (goalMeters > 0) ((distanceMeters / goalMeters) * 100).toInt().coerceAtMost(100) else 0
        binding.pbDailyGoal.progress = percent
        val goalFormatted = estimator.formatDistance(goalMeters, isMetric)
        binding.tvGoalProgress.text = "$percent% of $goalFormatted daily goal"

        // Witty quote
        binding.tvWittyQuote.text = "“${MilestoneManager.getWittyComparison(distanceMeters)}”"

        // Sessions count
        binding.tvTodaySessions.text = "$sessionCount opens"
    }

    private fun refreshAllScreens() {
        lifecycleScope.launch {
            val stats = statsCalculator.calculateFullStatistics()
            val isMetric = app.preferencesRepository.distanceUnit == DistanceUnit.METRIC

            binding.tvWeekDistance.text = estimator.formatDistance(stats.thisWeekDistanceMeters, isMetric)
            binding.tvMonthDistance.text = estimator.formatDistance(stats.thisMonthDistanceMeters, isMetric)
            binding.tvDailyAverage.text = "${estimator.formatDistance(stats.averageDailyDistanceMeters, isMetric)}/day"
        }
    }

    private fun populateHistory() {
        binding.historyListContainer.removeAllViews()
        lifecycleScope.launch {
            val stats = app.database.scrollDao().getAllDailyStats()
            val isMetric = app.preferencesRepository.distanceUnit == DistanceUnit.METRIC

            if (stats.isEmpty()) {
                val emptyTv = TextView(this@MainActivity).apply {
                    text = "No history recorded yet. Start scrolling in Instagram!"
                    setTextColor(getColor(R.color.text_muted))
                    setPadding(0, 40, 0, 40)
                }
                binding.historyListContainer.addView(emptyTv)
                return@launch
            }

            for (stat in stats) {
                val itemBinding = ItemHistoryBinding.inflate(layoutInflater, binding.historyListContainer, false)
                itemBinding.tvHistoryDate.text = stat.dateString
                itemBinding.tvHistoryDistance.text = estimator.formatDistance(stat.totalDistanceMeters, isMetric)
                itemBinding.tvHistoryDuration.text = "${estimator.formatDuration(stat.totalDurationSeconds)} active"
                itemBinding.tvHistorySessions.text = "${stat.sessionCount} sessions"
                binding.historyListContainer.addView(itemBinding.root)
            }
        }
    }

    private fun populateStatistics() {
        lifecycleScope.launch {
            val stats = statsCalculator.calculateFullStatistics()
            val isMetric = app.preferencesRepository.distanceUnit == DistanceUnit.METRIC

            binding.tvLifetimeDistance.text = estimator.formatDistance(stats.totalLifetimeDistanceMeters, isMetric)
            binding.tvLongestSession.text = "${estimator.formatDuration(stats.longestSessionDurationSeconds)} (${estimator.formatDistance(stats.longestSessionDistanceMeters, isMetric)})"
            binding.tvHighestDay.text = "${estimator.formatDistance(stats.highestDayDistanceMeters, isMetric)} (${stats.highestDayDate})"

            // Milestones list
            binding.milestonesContainer.removeAllViews()
            for (milestone in MilestoneManager.MILESTONES) {
                val itemBinding = ItemMilestoneBinding.inflate(layoutInflater, binding.milestonesContainer, false)
                val isUnlocked = stats.totalLifetimeDistanceMeters >= milestone.thresholdMeters

                itemBinding.tvMilestoneEmoji.text = milestone.emoji
                itemBinding.tvMilestoneTitle.text = milestone.title
                itemBinding.tvMilestoneDistance.text = estimator.formatDistance(milestone.thresholdMeters, isMetric)
                itemBinding.tvMilestoneDesc.text = milestone.description

                if (!isUnlocked) {
                    itemBinding.root.alpha = 0.5f
                    itemBinding.tvMilestoneTitle.text = "${milestone.title} (Locked)"
                } else {
                    itemBinding.root.alpha = 1.0f
                }
                binding.milestonesContainer.addView(itemBinding.root)
            }
        }
    }

    private fun setupSettings() {
        val prefs = app.preferencesRepository
        binding.switchTracking.isChecked = prefs.isTrackingEnabled
        binding.switchNotifications.isChecked = prefs.isNotificationEnabled

        if (prefs.distanceUnit == DistanceUnit.METRIC) {
            binding.rbMetric.isChecked = true
        } else {
            binding.rbImperial.isChecked = true
        }

        binding.switchTracking.setOnCheckedChangeListener { _, isChecked ->
            prefs.isTrackingEnabled = isChecked
        }

        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            prefs.isNotificationEnabled = isChecked
        }

        binding.rgUnits.setOnCheckedChangeListener { _, checkedId ->
            prefs.distanceUnit = if (checkedId == R.id.rbMetric) DistanceUnit.METRIC else DistanceUnit.IMPERIAL
            refreshAllScreens()
            if (currentTab == 1) populateHistory()
            if (currentTab == 2) populateStatistics()
        }

        binding.btnResetToday.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Reset Today's Distance?")
                .setMessage("Are you sure you want to reset your Instagram scroll distance for today?")
                .setPositiveButton("Reset") { _, _ ->
                    val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                    lifecycleScope.launch {
                        app.database.scrollDao().deleteDailyStat(todayStr)
                        app.database.scrollDao().deleteSessionsForDate(todayStr)
                        refreshAllScreens()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        binding.btnDeleteAll.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Delete All Stored History?")
                .setMessage("This will erase all past session and distance statistics stored on this device. This action cannot be undone.")
                .setPositiveButton("Delete All") { _, _ ->
                    lifecycleScope.launch {
                        app.database.scrollDao().deleteAllSessions()
                        app.database.scrollDao().deleteAllDailyStats()
                        refreshAllScreens()
                        populateHistory()
                        populateStatistics()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }
}
