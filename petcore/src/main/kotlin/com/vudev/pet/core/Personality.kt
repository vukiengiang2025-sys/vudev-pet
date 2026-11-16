package com.vudev.pet.core

/**
 * Tính cách của thú cưng. Không chỉ đổi lời thoại: mỗi tính cách làm lệch xác suất
 * hành vi và tốc độ tiêu hao năng lượng, nên hai con cùng nhu cầu vẫn cư xử khác nhau.
 */
enum class Personality(val id: String, val labelVi: String) {
    PLAYFUL("playful", "Tinh nghịch"),
    SHY("shy", "Nhút nhát"),
    LAZY("lazy", "Lười biếng"),
    FRIENDLY("friendly", "Thân thiện"),
    CURIOUS("curious", "Tò mò");

    /** Hệ số nhân cho điểm hành vi cơ bản. 1.0 là trung tính. */
    fun behaviorBias(behavior: Behavior): Double = when (this) {
        PLAYFUL -> when (behavior) {
            Behavior.PLAY -> 2.2
            Behavior.WANDER -> 1.3
            Behavior.SIT -> 0.6
            Behavior.SLEEP -> 0.7
            Behavior.SULK -> 0.8
            Behavior.IDLE -> 0.8
            else -> 1.0
        }
        SHY -> when (behavior) {
            Behavior.PETTED -> 1.6
            Behavior.SIT -> 1.4
            Behavior.WANDER -> 0.7
            Behavior.PLAY -> 0.8
            Behavior.SULK -> 1.2
            else -> 1.0
        }
        LAZY -> when (behavior) {
            Behavior.SLEEP -> 2.4
            Behavior.SIT -> 1.6
            Behavior.WANDER -> 0.6
            Behavior.PLAY -> 0.6
            Behavior.IDLE -> 1.2
            else -> 1.0
        }
        FRIENDLY -> when (behavior) {
            Behavior.PETTED -> 1.4
            Behavior.TALK -> 1.4
            Behavior.PLAY -> 1.1
            Behavior.SULK -> 0.6
            else -> 1.0
        }
        CURIOUS -> when (behavior) {
            Behavior.WANDER -> 1.8
            Behavior.TALK -> 1.3
            Behavior.PLAY -> 1.2
            Behavior.SLEEP -> 0.8
            else -> 1.0
        }
    }

    /** Nhân tố tiêu hao năng lượng mỗi phút. */
    val energyDrainMultiplier: Double
        get() = when (this) {
            PLAYFUL -> 1.25
            SHY -> 0.95
            LAZY -> 0.75
            FRIENDLY -> 1.0
            CURIOUS -> 1.15
        }

    /** Nhân tố suy giảm vui khi không được chú ý. */
    val happinessDrainMultiplier: Double
        get() = when (this) {
            PLAYFUL -> 1.2
            SHY -> 1.1
            LAZY -> 0.85
            FRIENDLY -> 0.9
            CURIOUS -> 1.0
        }

    companion object {
        fun fromId(id: String?): Personality =
            entries.firstOrNull { it.id.equals(id?.trim(), ignoreCase = true) } ?: FRIENDLY
    }
}
