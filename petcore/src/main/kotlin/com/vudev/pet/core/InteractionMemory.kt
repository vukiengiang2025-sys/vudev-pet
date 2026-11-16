package com.vudev.pet.core

/** Ký ức tương tác gần đây. Thuần dữ liệu, không phụ thuộc Android. */
data class InteractionMemory(
    val lastInteractionAt: Long = 0L,
    val lastPetAt: Long = 0L,
    val lastFeedAt: Long = 0L,
    val lastPlayAt: Long = 0L,
    val lastBatheAt: Long = 0L,
    val lastTalkAt: Long = 0L,
    val totalInteractions: Int = 0,
    val recentBehaviors: List<Behavior> = emptyList(),
) {
    fun hoursSinceLastInteraction(nowWallMs: Long): Double {
        if (lastInteractionAt <= 0L || nowWallMs <= lastInteractionAt) return 0.0
        return (nowWallMs - lastInteractionAt).toDouble() / PetClock.HOUR
    }

    fun withBehavior(behavior: Behavior): InteractionMemory {
        val next = (recentBehaviors + behavior).takeLast(MAX_RECENT)
        return copy(recentBehaviors = next)
    }

    companion object {
        const val MAX_RECENT = 12
    }
}
