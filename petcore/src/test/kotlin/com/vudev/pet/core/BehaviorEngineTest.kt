package com.vudev.pet.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import kotlin.random.Random

class BehaviorEngineTest {

    private fun nightWall(): Long {
        val c = Calendar.getInstance()
        c.set(Calendar.HOUR_OF_DAY, 23)
        c.set(Calendar.MINUTE, 0)
        return c.timeInMillis
    }

    private fun dayWall(): Long {
        val c = Calendar.getInstance()
        c.set(Calendar.HOUR_OF_DAY, 10)
        c.set(Calendar.MINUTE, 0)
        return c.timeInMillis
    }

    private fun state(
        needs: Needs = Needs(),
        personality: Personality = Personality.FRIENDLY,
        recent: List<Behavior> = emptyList(),
    ): PetState = PetState(needs = needs, personality = personality, memory = InteractionMemory(recentBehaviors = recent))

    @Test
    fun threeIdenticalBehaviorsInARowAreNotRepeated() {
        val s = state(recent = listOf(Behavior.SIT, Behavior.SIT, Behavior.SIT))
        repeat(40) { seed ->
            val choice = BehaviorEngine.choose(s, dayWall(), Random(seed.toLong()))
            assertNotEquals(Behavior.SIT, choice.behavior)
        }
    }

    @Test
    fun exhaustedPetChoosesSleepAtNight() {
        val s = state(needs = Needs(energy = 2.0, hunger = 0.0), personality = Personality.LAZY)
        repeat(20) { seed ->
            val choice = BehaviorEngine.choose(s, nightWall(), Random(seed.toLong()))
            assertEquals(Behavior.SLEEP, choice.behavior)
        }
    }

    @Test
    fun veryHungryPetAsksInsteadOfFeedingItself() {
        val s = state(needs = Needs(hunger = 100.0), personality = Personality.FRIENDLY)
        repeat(20) { seed ->
            val choice = BehaviorEngine.choose(s, dayWall(), Random(seed.toLong()))
            assertEquals(Behavior.TALK, choice.behavior)
        }
    }

    @Test
    fun dirtyPetAsksForABath() {
        val s = state(needs = Needs(cleanliness = 2.0), personality = Personality.FRIENDLY)
        repeat(20) { seed ->
            val choice = BehaviorEngine.choose(s, dayWall(), Random(seed.toLong()))
            assertEquals(Behavior.TALK, choice.behavior)
        }
    }

    @Test
    fun durationMatchesTheBehaviorRange() {
        val choice = BehaviorEngine.choose(state(), dayWall(), Random(2L))
        val range = BehaviorDurations.of(choice.behavior)
        assertTrue(choice.durationMs >= range.minMs)
        assertTrue(choice.durationMs <= range.maxMs)
    }

    @Test
    fun neverReturnsAUserOnlyBehavior() {
        repeat(100) { seed ->
            val choice = BehaviorEngine.choose(state(), dayWall(), Random(seed.toLong()))
            assertTrue(choice.behavior != Behavior.DRAGGED)
            assertTrue(choice.behavior != Behavior.PETTED)
        }
    }
}
