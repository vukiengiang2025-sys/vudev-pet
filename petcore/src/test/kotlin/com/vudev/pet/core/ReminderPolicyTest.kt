package com.vudev.pet.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReminderPolicyTest {

    private val now = 1_700_000_000_000L
    private val hour = PetClock.HOUR

    private fun state(hunger: Double = 20.0, happiness: Double = 70.0, lastInteractionAt: Long = now - 60_000L) =
        PetState(
            needs = Needs(hunger = hunger, happiness = happiness),
            memory = InteractionMemory(lastInteractionAt = lastInteractionAt),
        )

    @Test
    fun hungryPetIsRemindedOncePerCooldown() {
        val policy = ReminderPolicy()
        assertEquals(ReminderKind.HUNGRY, policy.poll(state(hunger = 90.0), now, false))
        assertNull(policy.poll(state(hunger = 90.0), now + hour, false))
        assertEquals(ReminderKind.HUNGRY, policy.poll(state(hunger = 90.0), now + 7 * hour, false))
    }

    @Test
    fun quietHoursSilenceEverything() {
        val policy = ReminderPolicy()
        assertNull(policy.poll(state(hunger = 95.0, happiness = 5.0), now, true))
    }

    @Test
    fun neglectedPetGetsANeglectReminder() {
        val policy = ReminderPolicy()
        val lonely = state(happiness = 10.0, lastInteractionAt = now - 30 * hour)
        assertEquals(ReminderKind.NEGLECTED, policy.poll(lonely, now, false))
    }

    @Test
    fun neglectedPetDoesNotAlsoGetTheSadReminderImmediately() {
        val policy = ReminderPolicy()
        val lonely = state(happiness = 10.0, lastInteractionAt = now - 30 * hour)
        assertEquals(ReminderKind.NEGLECTED, policy.poll(lonely, now, false))
        assertNull(policy.poll(lonely, now + 1_000L, false))
    }

    @Test
    fun sadPetGetsASadReminder() {
        val policy = ReminderPolicy()
        val sad = state(happiness = 20.0, lastInteractionAt = now - 2 * hour)
        assertEquals(ReminderKind.SAD, policy.poll(sad, now, false))
        assertNull(policy.poll(sad, now + hour, false))
    }

    @Test
    fun healthyPetIsNotReminded() {
        val policy = ReminderPolicy()
        assertNull(policy.poll(state(hunger = 20.0, happiness = 80.0), now, false))
    }

    @Test
    fun resetClearsCooldowns() {
        val policy = ReminderPolicy()
        policy.poll(state(hunger = 90.0), now, false)
        policy.reset()
        assertEquals(ReminderKind.HUNGRY, policy.poll(state(hunger = 90.0), now + 1_000L, false))
    }
}
