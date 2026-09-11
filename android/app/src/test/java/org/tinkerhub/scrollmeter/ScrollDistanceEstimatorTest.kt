package org.tinkerhub.scrollmeter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tinkerhub.scrollmeter.core.MilestoneManager
import org.tinkerhub.scrollmeter.core.ScrollDistanceEstimator

class ScrollDistanceEstimatorTest {

    @Test
    fun testPhysicalDistanceCalculation() {
        // At 420 DPI:
        // 420 pixels = 1.0 inch = 0.0254 meters
        val estimator = ScrollDistanceEstimator(420.0f)
        val meters = estimator.estimateMetersFromPixels(420)
        assertEquals(0.0254, meters, 0.0001)
    }

    @Test
    fun testNoiseRejectionForMicroJitter() {
        val estimator = ScrollDistanceEstimator(420.0f)
        // Values below 8px threshold should be discarded as accidental tremor/jitter
        val zeroMeters = estimator.estimateMetersFromPixels(4)
        assertEquals(0.0, zeroMeters, 0.0)
    }

    @Test
    fun testFlingClampPreventsExtremeSpikes() {
        val estimator = ScrollDistanceEstimator(420.0f)
        // An enormous delta of 100,000 pixels should be clamped to MAX_PIXEL_PER_EVENT (2800)
        val clampedMeters = estimator.estimateMetersFromPixels(100000)
        val maxExpectedMeters = (2800.0 / 420.0) * 0.0254
        assertEquals(maxExpectedMeters, clampedMeters, 0.0001)
    }

    @Test
    fun testFormattingMetricAndImperial() {
        val estimator = ScrollDistanceEstimator()
        assertEquals("450 m", estimator.formatDistance(450.0, isMetric = true))
        assertEquals("2.47 km", estimator.formatDistance(2470.0, isMetric = true))
        assertEquals("1.53 mi", estimator.formatDistance(2470.0, isMetric = false))
    }

    @Test
    fun testMilestoneCrossingDetection() {
        // Crossing 100m landmark
        val crossed = MilestoneManager.getNewlyCrossedMilestones(90.0, 110.0)
        assertEquals(1, crossed.size)
        assertEquals("Crossed The Street", crossed[0].title)

        // Crossing 1000m landmark
        val crossedKm = MilestoneManager.getNewlyCrossedMilestones(950.0, 1050.0)
        assertEquals(1, crossedKm.size)
        assertEquals("1 Kilometer Club", crossedKm[0].title)
    }

    @Test
    fun testWittyComparisons() {
        val coffee = MilestoneManager.getWittyComparison(2500.0)
        assertTrue(coffee.contains("coffee shop", ignoreCase = true))

        val grass = MilestoneManager.getWittyComparison(12000.0)
        assertTrue(grass.contains("grass", ignoreCase = true))
    }
}
