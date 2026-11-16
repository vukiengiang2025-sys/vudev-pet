package com.vudev.pet.core

import org.junit.Assert.assertEquals
import org.junit.Test

class EmotionEngineTest {

    private val now = 1_700_000_000_000L

    private fun state(
        needs: Needs = Needs(),
        behavior: Behavior = Behavior.IDLE,
        memory: InteractionMemory = InteractionMemory(lastInteractionAt = now - 60_000L),
    ) = PetState(needs = needs, behavior = behavior, memory = memory)

    @Test
    fun behaviorOverridesNeeds() {
        assertEquals(Mood.SLEEPY, EmotionEngine.derive(state(behavior = Behavior.SLEEP), now))
        assertEquals(Mood.HAPPY, EmotionEngine.derive(state(behavior = Behavior.EAT), now))
        assertEquals(Mood.EXCITED, EmotionEngine.derive(state(behavior = Behavior.PLAY), now))
        assertEquals(Mood.SULK, EmotionEngine.derive(state(behavior = Behavior.SULK), now))
    }

    @Test
    fun starvingAndUnhappyIsAngry() {
        val mood = EmotionEngine.derive(state(needs = Needs(hunger = 95.0, happiness = 10.0)), now)
        assertEquals(Mood.ANGRY, mood)
    }

    @Test
    fun exhaustedIsSleepy() {
        val mood = EmotionEngine.derive(state(needs = Needs(energy = 5.0)), now)
        assertEquals(Mood.SLEEPY, mood)
    }

    @Test
    fun neglectedPetSulks() {
        val memory = InteractionMemory(lastInteractionAt = now - 30L * 60L * 60L * 1000L)
        val mood = EmotionEngine.derive(state(needs = Needs(happiness = 40.0), memory = memory), now)
        assertEquals(Mood.SULK, mood)
    }

    @Test
    fun happyWellLovedPetLooksHappy() {
        val mood = EmotionEngine.derive(
            state(needs = Needs(happiness = 90.0, affection = 80.0, hunger = 10.0, energy = 80.0)),
            now,
        )
        assertEquals(Mood.HAPPY, mood)
    }
}
