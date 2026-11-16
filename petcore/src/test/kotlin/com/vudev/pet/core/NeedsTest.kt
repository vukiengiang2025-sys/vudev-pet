package com.vudev.pet.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NeedsTest {

    private fun around(value: Double, expected: Double, tolerance: Double = 0.01) {
        assertTrue("expected " + expected + " but was " + value, kotlin.math.abs(value - expected) <= tolerance)
    }

    @Test
    fun clampsEveryValueIntoRange() {
        val n = Needs(hunger = -10.0, happiness = 130.0, energy = 200.0, cleanliness = -5.0, affection = 101.0)
        around(n.clamped().hunger, 0.0)
        around(n.clamped().happiness, 100.0)
        around(n.clamped().energy, 100.0)
        around(n.clamped().cleanliness, 0.0)
        around(n.clamped().affection, 100.0)
    }

    @Test
    fun awakeDecayMovesInTheExpectedDirection() {
        val d = Needs().decayed(60.0, Personality.FRIENDLY, sleeping = false)
        around(d.hunger, 27.5)       // 20 + 0.125 * 60
        around(d.happiness, 64.6)    // 70 - 0.1 * 0.9 (FRIENDLY) * 60
        around(d.energy, 70.0)       // 80 - 0.1667 * 60 = 70
        assertTrue(d.cleanliness < 85.0)
        assertTrue(d.affection < 50.0)
    }

    @Test
    fun sleepingSlowHungerAndRestoresEnergy() {
        val awake = Needs(energy = 40.0).decayed(30.0, Personality.LAZY, sleeping = false)
        val asleep = Needs(energy = 40.0).decayed(30.0, Personality.LAZY, sleeping = true)
        assertTrue(asleep.hunger < awake.hunger)
        assertTrue(asleep.energy > awake.energy)
        assertTrue(asleep.energy > 40.0)
    }

    @Test
    fun playfulBurnsMoreEnergyThanLazy() {
        val playful = Needs(energy = 80.0).decayed(60.0, Personality.PLAYFUL, sleeping = false)
        val lazy = Needs(energy = 80.0).decayed(60.0, Personality.LAZY, sleeping = false)
        assertTrue(playful.energy < lazy.energy)
    }

    @Test
    fun feedingReducesHungerAndRaisesHappiness() {
        val before = Needs(hunger = 90.0, happiness = 30.0)
        val after = before.apply(Interaction.FEED)
        assertTrue(after.hunger < before.hunger)
        assertTrue(after.happiness > before.happiness)
    }

    @Test
    fun playingCostsEnergyAndRaisesHappiness() {
        val before = Needs(energy = 50.0, happiness = 40.0)
        val after = before.apply(Interaction.PLAY)
        assertTrue(after.energy < before.energy)
        assertTrue(after.happiness > before.happiness)
    }

    @Test
    fun interactionsNeverPushNeedsOutOfRange() {
        var n = Needs()
        repeat(100) { n = n.apply(Interaction.FEED).apply(Interaction.PLAY).apply(Interaction.BATHE) }
        assertTrue(n.hunger in 0.0..100.0)
        assertTrue(n.happiness in 0.0..100.0)
        assertTrue(n.energy in 0.0..100.0)
        assertTrue(n.cleanliness in 0.0..100.0)
        assertTrue(n.affection in 0.0..100.0)
    }

    @Test
    fun mostUrgentPicksTheWorstNeed() {
        assertEquals(NeedKind.HUNGER, Needs(hunger = 90.0).mostUrgent())
        assertEquals(NeedKind.ENERGY, Needs(energy = 10.0).mostUrgent())
        assertEquals(NeedKind.CLEANLINESS, Needs(cleanliness = 10.0).mostUrgent())
        assertEquals(NeedKind.HAPPINESS, Needs(happiness = 10.0).mostUrgent())
        assertEquals(NeedKind.NONE, Needs().mostUrgent())
    }
}
