package com.vudev.pet.core

import java.util.Calendar

object PetClock {
    private const val HOUR_MS = 60L * 60L * 1000L
    private const val MINUTE_MS = 60L * 1000L

    /** Giờ trong ngày 0..1439 theo múi giờ máy. */
    fun minuteOfDay(wallMs: Long): Int {
        val c = Calendar.getInstance()
        c.timeInMillis = wallMs
        return c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE)
    }

    fun hourOfDay(wallMs: Long): Int = minuteOfDay(wallMs) / 60

    /** Buổi trong ngày, dùng cho lời chào. */
    fun partOfDay(wallMs: Long): PartOfDay {
        val h = hourOfDay(wallMs)
        return when {
            h in 5..10 -> PartOfDay.MORNING
            h in 11..13 -> PartOfDay.NOON
            h in 14..17 -> PartOfDay.AFTERNOON
            h in 18..21 -> PartOfDay.EVENING
            else -> PartOfDay.NIGHT
        }
    }

    /**
     * Thời gian trôi qua an toàn giữa hai mốc wall-clock.
     *
     * Trả 0 khi đồng hồ bị chỉnh lùi hoặc mốc trước không hợp lệ, và cắt tối đa
     * [MAX_CATCH_UP_MS] để một tuần bỏ bê không biến thành hình phạt vô lý.
     */
    fun safeElapsedMs(lastWallMs: Long, nowWallMs: Long, maxCatchUpMs: Long = MAX_CATCH_UP_MS): Long {
        if (lastWallMs <= 0L || nowWallMs <= 0L) return 0L
        val delta = nowWallMs - lastWallMs
        if (delta <= 0L) return 0L
        return minOf(delta, maxCatchUpMs)
    }

    const val MAX_CATCH_UP_MS: Long = 24L * HOUR_MS
    const val MINUTE: Long = MINUTE_MS
    const val HOUR: Long = HOUR_MS
}

enum class PartOfDay { MORNING, NOON, AFTERNOON, EVENING, NIGHT }
