package org.tinkerhub.scrollmeter.core

import kotlin.math.abs

/**
 * ScrollDistanceEstimator
 *
 * Converts raw screen pixel scroll gestures received from Instagram into physical
 * estimated distance (meters, kilometers, miles).
 *
 * Platform & Technical Considerations:
 * 1. Android's Accessibility API does not expose the internal virtual viewport height of Instagram's
 *    complex RecyclerView/Feed components.
 * 2. However, AccessibilityRecord emits TYPE_VIEW_SCROLLED events containing vertical delta pixels
 *    (getScrollDeltaY) or index changes.
 * 3. We map the vertical gesture displacement on the physical screen to real-world units using
 *    the device's hardware DPI (dots per inch).
 *    Formula:
 *       Physical Distance (inches) = |scrollDeltaPixels| / screenYdpi
 *       Physical Distance (meters) = Physical Distance (inches) * 0.0254
 * 4. Noise filtering:
 *    - Taps with zero or sub-threshold displacement (< 8px) are discarded.
 *    - Extreme fling momentum spikes are clamped to prevent unrealistic jumps.
 */
class ScrollDistanceEstimator(
    private var screenYdpi: Float = DEFAULT_DPI
) {

    companion object {
        const val DEFAULT_DPI = 420.0f
        const val METERS_PER_INCH = 0.0254
        const val FEET_PER_METER = 3.28084
        const val MILES_PER_METER = 0.000621371

        // Discard accidental micro-jitters / tremors
        const val MIN_PIXEL_THRESHOLD = 8

        // Max single event displacement clamp (~1 full screen height on high-res device)
        const val MAX_PIXEL_PER_EVENT = 2800
    }

    fun updateScreenDpi(ydpi: Float) {
        if (ydpi > 100f && ydpi < 1000f) {
            this.screenYdpi = ydpi
        }
    }

    /**
     * Estimates physical distance in meters from vertical pixel displacement.
     * Returns 0.0 if the gesture is considered accidental or noise.
     */
    fun estimateMetersFromPixels(deltaY: Int): Double {
        val absDelta = abs(deltaY)
        if (absDelta < MIN_PIXEL_THRESHOLD) {
            return 0.0
        }

        // Clamp extreme flings
        val clampedDelta = absDelta.coerceAtMost(MAX_PIXEL_PER_EVENT)

        // Convert pixels to physical inches, then to meters
        val inches = clampedDelta.toDouble() / screenYdpi.toDouble()
        return inches * METERS_PER_INCH
    }

    /**
     * Converts meters into human-readable string formatted according to user unit preference.
     * Example: 1850m -> "1.85 km" (Metric) or "1.15 mi" (Imperial)
     */
    fun formatDistance(meters: Double, isMetric: Boolean = true): String {
        return if (isMetric) {
            if (meters < 1000.0) {
                "${meters.toInt()} m"
            } else {
                val km = meters / 1000.0
                String.format(java.util.Locale.US, "%.2f km", km)
            }
        } else {
            val miles = meters * MILES_PER_METER
            if (miles < 0.1) {
                val feet = (meters * FEET_PER_METER).toInt()
                "$feet ft"
            } else {
                String.format(java.util.Locale.US, "%.2f mi", miles)
            }
        }
    }

    /**
     * Formats duration in seconds to human-readable string like "1h 42m" or "25m".
     */
    fun formatDuration(totalSeconds: Long): String {
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m"
            else -> "${totalSeconds}s"
        }
    }
}
