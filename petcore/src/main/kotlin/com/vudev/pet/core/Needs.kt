package com.vudev.pet.core

/**
 * Năm nhu cầu, mỗi cái 0..100.
 *
 * Quy ước: hunger CÀNG CAO càng đói; các nhu cầu còn lại càng cao càng tốt.
 * Mọi phép tính đều trả về bản sao mới, không sửa tại chỗ, để test và persistence dễ.
 */
data class Needs(
    val hunger: Double = 20.0,
    val happiness: Double = 70.0,
    val energy: Double = 80.0,
    val cleanliness: Double = 85.0,
    val affection: Double = 50.0,
) {
    fun clamped(): Needs = Needs(
        hunger = hunger.coerceIn(0.0, 100.0),
        happiness = happiness.coerceIn(0.0, 100.0),
        energy = energy.coerceIn(0.0, 100.0),
        cleanliness = cleanliness.coerceIn(0.0, 100.0),
        affection = affection.coerceIn(0.0, 100.0),
    )

    /**
     * Suy giảm theo số phút trôi qua. [sleeping] đổi tốc độ: khi ngủ thú đói chậm hơn
     * và năng lượng hồi lại thay vì tiêu hao.
     */
    fun decayed(minutes: Double, personality: Personality, sleeping: Boolean): Needs {
        if (minutes <= 0.0) return this
        val energyRate = if (sleeping) 1.0 else -0.1667 * personality.energyDrainMultiplier
        val happinessRate = if (sleeping) -0.02 else -0.1 * personality.happinessDrainMultiplier
        val hungerRate = if (sleeping) 0.05 else 0.125
        val cleanRate = -0.0333
        val affectionRate = -0.01
        return Needs(
            hunger = hunger + hungerRate * minutes,
            happiness = happiness + happinessRate * minutes,
            energy = energy + energyRate * minutes,
            cleanliness = cleanliness + cleanRate * minutes,
            affection = affection + affectionRate * minutes,
        ).clamped()
    }

    fun apply(interaction: Interaction): Needs = when (interaction) {
        Interaction.TAP -> copy(happiness = happiness + 1.0, affection = affection + 1.0)
        Interaction.PET -> copy(happiness = happiness + 8.0, affection = affection + 6.0)
        Interaction.FEED -> copy(
            hunger = hunger - 45.0,
            happiness = happiness + 6.0,
            cleanliness = cleanliness - 3.0,
            affection = affection + 2.0,
        )
        Interaction.PLAY -> copy(
            happiness = happiness + 15.0,
            energy = energy - 8.0,
            hunger = hunger + 4.0,
            affection = affection + 4.0,
        )
        Interaction.BATHE -> copy(
            cleanliness = cleanliness + 70.0,
            happiness = happiness + 3.0,
            energy = energy - 3.0,
            affection = affection + 2.0,
        )
        Interaction.WAKE -> copy(energy = energy + 2.0)
        Interaction.DRAGGED -> copy(happiness = happiness - 1.0)
    }.clamped()

    /** Nhu cầu nào đang tệ nhất (để hành vi ưu tiên). */
    fun mostUrgent(): NeedKind = when {
        hunger >= 70.0 -> NeedKind.HUNGER
        energy <= 25.0 -> NeedKind.ENERGY
        cleanliness <= 30.0 -> NeedKind.CLEANLINESS
        happiness <= 30.0 -> NeedKind.HAPPINESS
        affection <= 20.0 -> NeedKind.AFFECTION
        else -> NeedKind.NONE
    }

    val isHungry: Boolean get() = hunger >= 70.0
    val isTired: Boolean get() = energy <= 25.0
    val isDirty: Boolean get() = cleanliness <= 30.0
    val isSad: Boolean get() = happiness <= 30.0
}

enum class NeedKind { HUNGER, ENERGY, CLEANLINESS, HAPPINESS, AFFECTION, NONE }
