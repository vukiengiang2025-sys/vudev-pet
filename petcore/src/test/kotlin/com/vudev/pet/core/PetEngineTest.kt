package com.vudev.pet.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class PetEngineTest {

    private val now = 1_700_000_000_000L

    @Test
    fun offlineCatchUpAppliesElapsedNeeds() {
        val saved = PetState(lastSeenWallMs = now - 2L * 60L * 60L * 1000L)
        val engine = PetEngine(saved)
        val result = engine.restore(saved, now)
        assertEquals(120.0, result.awayMinutes, 0.5)
        assertEquals(35.0, result.state.needs.hunger, 0.5)
    }

    @Test
    fun offlineCatchUpIsCappedAtOneDay() {
        val saved = PetState(lastSeenWallMs = now - 72L * 60L * 60L * 1000L)
        val engine = PetEngine(saved)
        val result = engine.restore(saved, now)
        assertEquals(24.0 * 60.0, result.awayMinutes, 0.5)
    }

    @Test
    fun clockGoingBackwardsDoesNotPunishThePet() {
        val saved = PetState(lastSeenWallMs = now + 60_000L)
        val engine = PetEngine(saved)
        val result = engine.restore(saved, now)
        assertEquals(0.0, result.awayMinutes, 0.001)
    }

    @Test
    fun sleepingRestoresEnergyOnTick() {
        val start = PetState(behavior = Behavior.SLEEP, needs = Needs(energy = 50.0), lastSeenWallMs = now - 1_000L)
        val engine = PetEngine(start)
        engine.restore(start, now)
        val tick = engine.tick(now + 4L * 60L * 1000L)
        assertTrue(tick.state.needs.energy > 50.0)
    }

    @Test
    fun aRestedPetWakesUp() {
        val start = PetState(behavior = Behavior.SLEEP, needs = Needs(energy = 96.0), lastSeenWallMs = now - 1_000L)
        val engine = PetEngine(start)
        engine.restore(start, now)
        val tick = engine.tick(now + 2_000L)
        assertTrue(tick.wokeUp)
        assertNotEquals(Behavior.SLEEP, tick.state.behavior)
    }

    @Test
    fun feedingAppliesNeedsMemoryAndReaction() {
        val engine = PetEngine(PetState(needs = Needs(hunger = 90.0)))
        engine.restore(engine.state, now)
        val result = engine.interact(Interaction.FEED, now + 1_000L)
        assertTrue(result.state.needs.hunger < 90.0)
        assertEquals(Behavior.EAT, result.behavior)
        assertEquals(SpeechContext.FED, result.speech)
        assertEquals(now + 1_000L, result.state.memory.lastFeedAt)
        assertEquals(1, result.state.memory.totalInteractions)
    }

    @Test
    fun anyInteractionWakesASleepingPet() {
        val start = PetState(behavior = Behavior.SLEEP, lastSeenWallMs = now - 1_000L)
        val engine = PetEngine(start)
        engine.restore(start, now)
        val result = engine.interact(Interaction.TAP, now + 1_000L)
        assertNotEquals(Behavior.SLEEP, result.state.behavior)
    }

    @Test
    fun behaviorIsNotRepeatedAfterThreeTimes() {
        val state = PetState(
            needs = Needs(energy = 80.0, hunger = 10.0),
            behavior = Behavior.SIT,
            behaviorEndsAt = now - 1L,
            memory = InteractionMemory(recentBehaviors = listOf(Behavior.SIT, Behavior.SIT, Behavior.SIT)),
        )
        val engine = PetEngine(state, Random(5))
        engine.restore(state, now)
        val tick = engine.tick(now + 2_000L)
        assertNotEquals(Behavior.SIT, tick.state.behavior)
    }

    @Test
    fun snapshotCarriesTheSaveTimestamp() {
        val engine = PetEngine()
        engine.restore(PetState(), now)
        val snap = engine.snapshot(now + 5_000L)
        assertEquals(now + 5_000L, snap.lastSeenWallMs)
    }

    @Test
    fun nameIsTrimmedAndNeverEmpty() {
        val engine = PetEngine()
        engine.setName("   Bông   ")
        assertEquals("Bông", engine.state.name)
        engine.setName("   ")
        assertEquals("Bông", engine.state.name)
    }
}
