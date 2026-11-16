package com.vudev.pet.core

/** Toàn bộ trạng thái bền vững của một thú cưng. */
data class PetState(
    val name: String = "Bông",
    val personality: Personality = Personality.FRIENDLY,
    val needs: Needs = Needs(),
    val behavior: Behavior = Behavior.IDLE,
    val behaviorStartedAt: Long = 0L,
    val behaviorEndsAt: Long = 0L,
    val mood: Mood = Mood.CALM,
    val memory: InteractionMemory = InteractionMemory(),
    /** Mốc wall-clock lần cuối trạng thái được lưu/ái cập, dùng cho offline catch-up. */
    val lastSeenWallMs: Long = 0L,
)

data class CatchUpResult(
    val state: PetState,
    val awayMinutes: Double,
    val wasAwayHours: Double,
)

data class TickResult(
    val state: PetState,
    val behaviorChanged: Boolean,
    val wokeUp: Boolean,
)

data class InteractionResult(
    val state: PetState,
    val speech: SpeechContext,
    val behavior: Behavior,
)
