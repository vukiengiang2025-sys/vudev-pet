package com.vudev.pet.core

/**
 * Hành vi mà thú có thể tự chọn. Trạng thái do con người kích hoạt (PETTED, DRAGGED,
 * EAT, BATHE) vẫn nằm trong cùng enum để renderer và persistence chỉ cần một kiểu.
 */
enum class Behavior {
    IDLE,
    WANDER,
    SIT,
    SLEEP,
    PLAY,
    EAT,
    BATHE,
    PETTED,
    DRAGGED,
    SULK,
    TALK,
}

/** Khoảng thời lượng hợp lý cho một hành vi tự chọn. */
data class BehaviorDuration(val minMs: Long, val maxMs: Long) {
    fun pick(random: kotlin.random.Random): Long =
        if (maxMs <= minMs) minMs else minMs + (random.nextDouble() * (maxMs - minMs)).toLong()
}

object BehaviorDurations {
    fun of(behavior: Behavior): BehaviorDuration = when (behavior) {
        Behavior.IDLE -> BehaviorDuration(3_000L, 12_000L)
        Behavior.WANDER -> BehaviorDuration(3_000L, 10_000L)
        Behavior.SIT -> BehaviorDuration(5_000L, 20_000L)
        Behavior.SLEEP -> BehaviorDuration(10 * 60_000L, 120 * 60_000L)
        Behavior.PLAY -> BehaviorDuration(5_000L, 15_000L)
        Behavior.EAT -> BehaviorDuration(4_000L, 4_000L)
        Behavior.BATHE -> BehaviorDuration(4_000L, 4_000L)
        Behavior.PETTED -> BehaviorDuration(2_000L, 2_000L)
        Behavior.DRAGGED -> BehaviorDuration(500L, 500L)
        Behavior.SULK -> BehaviorDuration(10_000L, 60_000L)
        Behavior.TALK -> BehaviorDuration(2_500L, 5_000L)
    }
}

/**
 * Chọn hành vi tiếp theo bằng điểm số, không random thuần.
 *
 * Quy tắc cứng: không lặp cùng một hành vi quá 3 lần liên tiếp; khi người dùng tương tác
 * thì hành vi do người dùng kích hoạt luôn thắng (xử lý ở PetEngine.interact).
 */
object BehaviorEngine {

    private const val NIGHT_START = 22 * 60
    private const val NIGHT_END = 6 * 60
    private const val MORNING_END = 11 * 60

    fun choose(
        state: PetState,
        nowWallMs: Long,
        random: kotlin.random.Random = kotlin.random.Random.Default,
    ): BehaviorChoice {
        val recent = state.memory.recentBehaviors
        val blocked: Behavior? = if (recent.size >= 3 &&
            recent[recent.size - 1] == recent[recent.size - 2] &&
            recent[recent.size - 2] == recent[recent.size - 3]
        ) recent[recent.size - 1] else null

        val minuteOfDay = PetClock.minuteOfDay(nowWallMs)
        val scores = linkedMapOf<Behavior, Double>()

        for (b in Behavior.entries) {
            if (b == Behavior.DRAGGED || b == Behavior.PETTED || b == Behavior.EAT || b == Behavior.BATHE) continue
            if (b == blocked) continue
            var score = state.personality.behaviorBias(b)
            score += needBonus(b, state.needs)
            score += timeOfDayBonus(b, minuteOfDay)
            score += neglectBonus(b, state.memory, nowWallMs)
            score *= (0.9 + random.nextDouble() * 0.2)
            scores[b] = score
        }

        val behavior = scores.maxByOrNull { it.value }?.key ?: Behavior.IDLE
        val duration = BehaviorDurations.of(behavior).pick(random)
        return BehaviorChoice(behavior, duration)
    }

    private fun needBonus(behavior: Behavior, needs: Needs): Double = when (behavior) {
        Behavior.EAT -> if (needs.hunger >= 55.0) (needs.hunger - 45.0) / 55.0 * 4.0 else -1.0
        Behavior.SLEEP -> if (needs.energy <= 45.0) (45.0 - needs.energy) / 45.0 * 4.0 else -0.5
        Behavior.PLAY -> if (needs.happiness <= 65.0) (65.0 - needs.happiness) / 65.0 * 2.5 else 0.2
        Behavior.BATHE -> if (needs.cleanliness <= 60.0) (60.0 - needs.cleanliness) / 60.0 * 3.0 else -1.0
        Behavior.SULK -> if (needs.happiness <= 35.0 || needs.affection <= 25.0) 2.0 else -0.5
        Behavior.TALK -> {
            var bonus = if (needs.happiness >= 60.0) 0.4 else 0.0
            // Đói/bẩn thì thú "xin" bằng cách nói, không tự ăn/tắm khi chưa có người cho.
            if (needs.hunger >= 70.0) bonus += 2.5
            if (needs.cleanliness <= 40.0) bonus += 1.5
            if (needs.happiness <= 50.0) bonus += 0.5
            bonus
        }
        Behavior.SIT -> if (needs.energy <= 55.0) 1.0 else 0.2
        else -> 0.0
    }

    private fun timeOfDayBonus(behavior: Behavior, minuteOfDay: Int): Double {
        val night = minuteOfDay >= NIGHT_START || minuteOfDay < NIGHT_END
        val morning = minuteOfDay in NIGHT_END until MORNING_END
        return when {
            night && behavior == Behavior.SLEEP -> 3.0
            night && behavior == Behavior.PLAY -> -1.5
            night && behavior == Behavior.WANDER -> -1.0
            morning && behavior == Behavior.WANDER -> 1.0
            morning && behavior == Behavior.TALK -> 0.5
            else -> 0.0
        }
    }

    private fun neglectBonus(behavior: Behavior, memory: InteractionMemory, nowWallMs: Long): Double {
        val neglectedHours = memory.hoursSinceLastInteraction(nowWallMs)
        return when {
            neglectedHours >= 24.0 && behavior == Behavior.SULK -> 3.0
            neglectedHours >= 24.0 && behavior == Behavior.TALK -> 1.0
            neglectedHours >= 6.0 && behavior == Behavior.PLAY -> 0.5
            else -> 0.0
        }
    }
}

data class BehaviorChoice(val behavior: Behavior, val durationMs: Long)
