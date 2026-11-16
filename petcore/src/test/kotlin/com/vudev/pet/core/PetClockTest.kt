package com.vudev.pet.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class PetClockTest {

    private fun wallAt(hour: Int, minute: Int): Long {
        val c = Calendar.getInstance()
        c.set(Calendar.HOUR_OF_DAY, hour)
        c.set(Calendar.MINUTE, minute)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    @Test
    fun elapsedIsZeroForMissingOrBackwardsClock() {
        assertEquals(0L, PetClock.safeElapsedMs(0L, 1000L))
        assertEquals(0L, PetClock.safeElapsedMs(5000L, 4000L))
    }

    @Test
    fun elapsedCapsAtOneDay() {
        val now = 1_700_000_000_000L
        val week = 7L * 24L * 60L * 60L * 1000L
        assertEquals(PetClock.MAX_CATCH_UP_MS, PetClock.safeElapsedMs(now - week, now))
    }

    @Test
    fun partOfDayFollowsTheLocalHour() {
        assertEquals(PartOfDay.MORNING, PetClock.partOfDay(wallAt(8, 0)))
        assertEquals(PartOfDay.NOON, PetClock.partOfDay(wallAt(12, 0)))
        assertEquals(PartOfDay.AFTERNOON, PetClock.partOfDay(wallAt(15, 0)))
        assertEquals(PartOfDay.EVENING, PetClock.partOfDay(wallAt(19, 0)))
        assertEquals(PartOfDay.NIGHT, PetClock.partOfDay(wallAt(1, 0)))
    }

    @Test
    fun minuteOfDayIsZeroAtMidnight() {
        assertEquals(0, PetClock.minuteOfDay(wallAt(0, 0)))
        assertTrue(PetClock.minuteOfDay(wallAt(23, 59)) >= 23 * 60)
    }
}
