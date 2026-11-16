package com.vudev.pet.core

/** Biểu cảm mà renderer vẽ. Không phải hành vi; nhiều hành vi có thể cùng một mood. */
enum class Mood {
    CALM,
    HAPPY,
    EXCITED,
    CURIOUS,
    SAD,
    SLEEPY,
    ANGRY,
    SURPRISED,
    SULK,
}

object EmotionEngine {

    fun derive(state: PetState, nowWallMs: Long): Mood {
        val needs = state.needs
        val behavior = state.behavior
        // Hành vi đang diễn ra thắng, vì mặt phải khớp với điều đang xảy ra.
        when (behavior) {
            Behavior.SLEEP -> return Mood.SLEEPY
            Behavior.PETTED -> return if (needs.happiness >= 55.0) Mood.HAPPY else Mood.SURPRISED
            Behavior.DRAGGED -> return Mood.SURPRISED
            Behavior.EAT -> return Mood.HAPPY
            Behavior.BATHE -> return Mood.SURPRISED
            Behavior.PLAY -> return Mood.EXCITED
            Behavior.SULK -> return Mood.SULK
            else -> Unit
        }
        val neglectHours = state.memory.hoursSinceLastInteraction(nowWallMs)
        return when {
            needs.hunger >= 85.0 && needs.happiness < 40.0 -> Mood.ANGRY
            needs.hunger >= 80.0 -> Mood.SAD
            needs.energy <= 20.0 -> Mood.SLEEPY
            neglectHours >= 24.0 && needs.happiness < 50.0 -> Mood.SULK
            needs.happiness <= 30.0 -> Mood.SAD
            needs.happiness >= 80.0 && needs.affection >= 60.0 -> Mood.HAPPY
            needs.cleanliness <= 30.0 -> Mood.SAD
            neglectHours < 0.05 && needs.affection >= 50.0 -> Mood.EXCITED
            else -> Mood.CALM
        }
    }
}
