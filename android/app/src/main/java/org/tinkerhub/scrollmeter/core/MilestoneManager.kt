package org.tinkerhub.scrollmeter.core

data class Milestone(
    val id: String,
    val thresholdMeters: Double,
    val title: String,
    val description: String,
    val emoji: String
)

/**
 * MilestoneManager
 *
 * Compares accumulated Instagram scrolling distance with famous real-world landmarks,
 * athletic feats, and humorous everyday journeys.
 */
object MilestoneManager {

    val MILESTONES = listOf(
        Milestone("m_25", 25.0, "Olympic Pool", "You swam an Olympic swimming pool with your thumb.", "🏊"),
        Milestone("m_100", 100.0, "Crossed The Street", "You crossed a busy city street without getting hit.", "🚶"),
        Milestone("m_300", 300.0, "Eiffel Tower", "You scaled the vertical height of the Eiffel Tower.", "🗼"),
        Milestone("m_828", 828.0, "Burj Khalifa", "You reached the pinnacle of the world's tallest building.", "🏙️"),
        Milestone("m_1000", 1000.0, "1 Kilometer Club", "You walked an entire kilometer without moving a muscle.", "🦥"),
        Milestone("m_2500", 2500.0, "Coffee Run", "Roughly the distance from your home to the nearest specialty coffee shop.", "☕"),
        Milestone("m_5000", 5000.0, "5K Park Run", "That's a pretty long walk... your thumb is in great cardio shape.", "🏃"),
        Milestone("m_8848", 8848.0, "Mount Everest", "You literally summitted Mount Everest vertically from bed.", "🏔️"),
        Milestone("m_10000", 10000.0, "Touch Some Grass", "You've scrolled 10 km. Maybe put the phone down and touch some grass.", "🌱"),
        Milestone("m_21097", 21097.5, "Half Marathon", "You completed a Half Marathon on social media.", "🎽"),
        Milestone("m_42195", 42195.0, "Full Marathon", "You just scrolled 42.195 km... A full Olympic Marathon!", "🏅")
    )

    /**
     * Returns a witty comparison based on current distance in meters.
     */
    fun getWittyComparison(meters: Double): String {
        return when {
            meters < 50 -> "Just warming up your scrolling thumb..."
            meters < 150 -> "That's roughly the distance to cross the street."
            meters < 500 -> "You've scrolled higher than the Eiffel Tower."
            meters < 1200 -> "You walked an entire kilometer without moving your legs."
            meters < 3000 -> "That's roughly the distance from your home to the nearest coffee shop."
            meters < 6000 -> "That's a solid 5K park jog... but on your couch."
            meters < 9500 -> "You've scrolled higher than Mount Everest's death zone."
            meters < 15000 -> "10+ kilometers! Seriously, go outside and touch some grass 🌱"
            meters < 30000 -> "Half marathon territory. Your thumb has six-pack abs."
            else -> "You've scrolled farther than the length of a marathon... eventually."
        }
    }

    /**
     * Checks if any new milestones have been crossed between previous and current distance.
     */
    fun getNewlyCrossedMilestones(previousMeters: Double, currentMeters: Double): List<Milestone> {
        return MILESTONES.filter { milestone ->
            previousMeters < milestone.thresholdMeters && currentMeters >= milestone.thresholdMeters
        }
    }
}
