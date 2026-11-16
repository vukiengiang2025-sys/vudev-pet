package com.vudev.pet.core

/** Loại nhắc nhở chăm sóc; app ánh xạ sang tiêu đề/nội dung thông báo. */
enum class ReminderKind { HUNGRY, SAD, NEGLECTED }

/**
 * Quyết định có gửi thông báo nhắc chăm sóc hay không, thuần logic và có cooldown.
 *
 * - Đói: đói >= 85, nhắc tối đa mỗi 6 giờ.
 * - Bỏ bê: vui <= 25 và đã 24 giờ không tương tác, nhắc tối đa mỗi 12 giờ.
 * - Buồn: vui <= 30, nhắc tối đa mỗi 12 giờ.
 * - Giờ yên tĩnh: không nhắc gì.
 *
 * Thú không chết và không gây áp lực: mỗi loại có cooldown riêng, tối đa vài thông báo/ngày.
 */
class ReminderPolicy(
    private val hungryCooldownMs: Long = 6L * PetClock.HOUR,
    private val sadCooldownMs: Long = 12L * PetClock.HOUR,
    private val neglectedCooldownMs: Long = 12L * PetClock.HOUR,
) {
    private var lastHungryAt = 0L
    private var lastSadAt = 0L
    private var lastNeglectedAt = 0L

    fun poll(state: PetState, nowWallMs: Long, quietNow: Boolean): ReminderKind? {
        if (quietNow) return null
        val needs = state.needs
        val neglectHours = state.memory.hoursSinceLastInteraction(nowWallMs)
        return when {
            needs.hunger >= 85.0 && nowWallMs - lastHungryAt >= hungryCooldownMs -> {
                lastHungryAt = nowWallMs
                ReminderKind.HUNGRY
            }
            needs.happiness <= 25.0 && neglectHours >= 24.0 &&
                nowWallMs - lastNeglectedAt >= neglectedCooldownMs -> {
                lastNeglectedAt = nowWallMs
                // Cùng một tình trạng bỏ bê: không gửi thêm thông báo buồn ngay sau đó.
                lastSadAt = nowWallMs
                ReminderKind.NEGLECTED
            }
            needs.happiness <= 30.0 && nowWallMs - lastSadAt >= sadCooldownMs -> {
                lastSadAt = nowWallMs
                ReminderKind.SAD
            }
            else -> null
        }
    }

    fun reset() {
        lastHungryAt = 0L
        lastSadAt = 0L
        lastNeglectedAt = 0L
    }
}
