package com.vudev.pet.core

import kotlin.random.Random

/**
 * Bộ não thuần logic của thú cưng: nhu cầu theo thời gian, chọn hành vi, cảm xúc,
 * ký ức tương tác. Không phụ thuộc Android nên test được toàn bộ bằng JUnit.
 *
 * App gọi [catchUp] một lần khi mở, [tick] mỗi giây khi chạy nền, và [interact] khi
 * người dùng chạm/vuốt/kéo/cho ăn. Mọi phương thức trả về trạng thái mới; không có
 * I/O, không có thread.
 */
class PetEngine(
    initial: PetState = PetState(),
    private val random: Random = Random.Default,
) {
    var state: PetState = initial
        private set

    private var lastTickWallMs: Long = 0L
    private var lastInteractionWallMs: Long = 0L
    private var sessionStartWallMs: Long = 0L

    /**
     * Nạp trạng thái đã lưu và áp dụng thời gian vắng mặt (tối đa 24 giờ).
     * Gọi một lần khi app/service khởi động.
     */
    fun restore(saved: PetState, nowWallMs: Long): CatchUpResult {
        val elapsedMs = PetClock.safeElapsedMs(saved.lastSeenWallMs, nowWallMs)
        val minutes = elapsedMs / 60_000.0
        val sleeping = saved.behavior == Behavior.SLEEP
        val needs = saved.needs.decayed(minutes, saved.personality, sleeping)
        val revived = saved.copy(needs = needs, lastSeenWallMs = nowWallMs)
        state = refreshed(revived, nowWallMs, keepBehavior = sleeping)
        lastTickWallMs = nowWallMs
        sessionStartWallMs = nowWallMs
        return CatchUpResult(
            state = state,
            awayMinutes = minutes,
            wasAwayHours = minutes / 60.0,
        )
    }

    /** Cập nhật mỗi giây khi service đang chạy. */
    fun tick(nowWallMs: Long): TickResult {
        val previousBehavior = state.behavior
        val elapsed = if (lastTickWallMs <= 0L) 0L
        else PetClock.safeElapsedMs(lastTickWallMs, nowWallMs, MAX_TICK_GAP_MS)
        lastTickWallMs = if (lastTickWallMs <= 0L) nowWallMs else nowWallMs

        val minutes = elapsed / 60_000.0
        val sleeping = previousBehavior == Behavior.SLEEP
        var needs = state.needs.decayed(minutes, state.personality, sleeping)

        var behaviorChanged = false
        var wokeUp = false
        var behavior = previousBehavior
        var behaviorStartedAt = state.behaviorStartedAt
        var behaviorEndsAt = state.behaviorEndsAt

        // Ngủ đủ thì tự dậy, kể cả khi khung ngủ còn dài.
        if (behavior == Behavior.SLEEP && needs.energy >= 95.0) {
            behavior = Behavior.IDLE
            behaviorChanged = true
            wokeUp = true
        } else if (nowWallMs >= behaviorEndsAt && behavior != Behavior.DRAGGED) {
            val choice = BehaviorEngine.choose(state.copy(needs = needs), nowWallMs, random)
            behavior = choice.behavior
            behaviorStartedAt = nowWallMs
            behaviorEndsAt = nowWallMs + choice.durationMs
            behaviorChanged = true
        }

        // Hành vi tự chọn ngủ cần hồi năng lượng ở tick sau; bắt đầu ngủ thì không đổi nhu cầu ngay.
        state = refreshed(
            state.copy(
                needs = needs,
                behavior = behavior,
                behaviorStartedAt = behaviorStartedAt,
                behaviorEndsAt = behaviorEndsAt,
                lastSeenWallMs = nowWallMs,
                memory = if (behaviorChanged) state.memory.withBehavior(behavior) else state.memory,
            ),
            nowWallMs,
            keepBehavior = true,
        )
        return TickResult(state, behaviorChanged, wokeUp)
    }

    /** Người dùng tương tác: ngắt hành vi hiện tại, áp nhu cầu, chọn câu nói phù hợp. */
    fun interact(interaction: Interaction, nowWallMs: Long): InteractionResult {
        val reaction = interactionBehavior(interaction)
        val needs = state.needs.apply(interaction)
        val memory = state.memory.copy(
            lastInteractionAt = nowWallMs,
            totalInteractions = state.memory.totalInteractions + 1,
            lastPetAt = if (interaction == Interaction.PET) nowWallMs else state.memory.lastPetAt,
            lastFeedAt = if (interaction == Interaction.FEED) nowWallMs else state.memory.lastFeedAt,
            lastPlayAt = if (interaction == Interaction.PLAY) nowWallMs else state.memory.lastPlayAt,
            lastBatheAt = if (interaction == Interaction.BATHE) nowWallMs else state.memory.lastBatheAt,
            lastTalkAt = if (interaction == Interaction.TAP) nowWallMs else state.memory.lastTalkAt,
        ).withBehavior(reaction)
        val duration = BehaviorDurations.of(reaction).pick(random)
        val talking = reaction == Behavior.TALK || reaction == Behavior.PETTED
        val next = state.copy(
            needs = needs,
            behavior = reaction,
            behaviorStartedAt = nowWallMs,
            behaviorEndsAt = nowWallMs + duration,
            memory = memory,
            lastSeenWallMs = nowWallMs,
        )
        lastInteractionWallMs = nowWallMs
        lastTickWallMs = nowWallMs
        state = refreshed(next, nowWallMs, keepBehavior = !talking)
        return InteractionResult(
            state = state,
            speech = speechFor(interaction, nowWallMs),
            behavior = reaction,
        )
    }

    fun setPersonality(personality: Personality, nowWallMs: Long) {
        state = refreshed(state.copy(personality = personality), nowWallMs, keepBehavior = true)
    }

    fun setName(name: String) {
        val clean = name.trim().take(24).ifEmpty { state.name }
        state = state.copy(name = clean)
    }

    /** Ảnh chụp để lưu: lastSeen chính là lúc lưu. */
    fun snapshot(nowWallMs: Long): PetState = state.copy(lastSeenWallMs = nowWallMs)

    /** Nói câu phù hợp ngay lúc mở app (chào/quay lại/nhớ). */
    fun greetingContext(nowWallMs: Long, awayMinutes: Double): SpeechContext {
        val memory = state.memory
        val neglect = memory.hoursSinceLastInteraction(nowWallMs)
        return when {
            awayMinutes >= 6 * 60 && neglect >= 24 -> SpeechContext.MISSED_YOU
            awayMinutes >= 60 -> SpeechContext.BACK
            state.needs.hunger >= 80 -> SpeechContext.HUNGRY
            state.needs.energy <= 20 -> SpeechContext.SLEEPY
            else -> when (PetClock.partOfDay(nowWallMs)) {
                PartOfDay.MORNING -> SpeechContext.MORNING
                PartOfDay.NOON -> SpeechContext.NOON
                PartOfDay.AFTERNOON, PartOfDay.EVENING -> SpeechContext.EVENING
                PartOfDay.NIGHT -> SpeechContext.NIGHT
            }
        }
    }

    private fun refreshed(base: PetState, nowWallMs: Long, keepBehavior: Boolean): PetState {
        val mood = EmotionEngine.derive(base, nowWallMs)
        return if (keepBehavior) base.copy(mood = mood) else base.copy(
            behavior = Behavior.IDLE,
            behaviorStartedAt = nowWallMs,
            behaviorEndsAt = nowWallMs + 3_000L,
            mood = mood,
        )
    }

    private fun interactionBehavior(interaction: Interaction): Behavior = when (interaction) {
        Interaction.TAP -> Behavior.TALK
        Interaction.PET -> Behavior.PETTED
        Interaction.FEED -> Behavior.EAT
        Interaction.PLAY -> Behavior.PLAY
        Interaction.BATHE -> Behavior.BATHE
        Interaction.WAKE -> Behavior.IDLE
        Interaction.DRAGGED -> Behavior.DRAGGED
    }

    private fun speechFor(interaction: Interaction, nowWallMs: Long): SpeechContext {
        val needs = state.needs
        return when (interaction) {
            Interaction.FEED -> SpeechContext.FED
            Interaction.PET -> SpeechContext.PETTED
            Interaction.PLAY -> SpeechContext.PLAYED
            Interaction.BATHE -> SpeechContext.BATHED
            Interaction.DRAGGED -> SpeechContext.DRAGGED
            Interaction.WAKE -> if (needs.hunger >= 80) SpeechContext.HUNGRY else SpeechContext.BACK
            Interaction.TAP -> when {
                needs.hunger >= 80 -> SpeechContext.HUNGRY
                needs.energy <= 20 -> SpeechContext.SLEEPY
                needs.cleanliness <= 30 -> SpeechContext.DIRTY
                needs.happiness <= 30 -> SpeechContext.SAD
                else -> SpeechContext.HAPPY
            }
        }
    }

    companion object {
        private const val MAX_TICK_GAP_MS = 5 * 60_000L
    }
}
