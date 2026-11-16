package com.vudev.pet.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class MemoryAndSpeechTest {

    private val now = 1_700_000_000_000L

    @Test
    fun hoursSinceLastInteractionIsZeroWhenUnknown() {
        assertEquals(0.0, InteractionMemory().hoursSinceLastInteraction(now), 0.001)
        assertEquals(0.0, InteractionMemory(lastInteractionAt = now + 1_000L).hoursSinceLastInteraction(now), 0.001)
    }

    @Test
    fun hoursSinceLastInteractionCountsRealHours() {
        val memory = InteractionMemory(lastInteractionAt = now - 3L * 60L * 60L * 1000L)
        assertEquals(3.0, memory.hoursSinceLastInteraction(now), 0.01)
    }

    @Test
    fun recentBehaviorsAreCapped() {
        var memory = InteractionMemory()
        repeat(40) { i ->
            memory = memory.withBehavior(if (i % 2 == 0) Behavior.IDLE else Behavior.WANDER)
        }
        assertTrue(memory.recentBehaviors.size <= InteractionMemory.MAX_RECENT)
    }

    @Test
    fun everySpeechContextHasVietnameseLinesForEveryPersonality() {
        for (personality in Personality.entries) {
            for (context in SpeechContext.entries) {
                val phrases = SpeechBank.phrases(context, personality)
                assertTrue("missing phrases for " + context + " / " + personality, phrases.isNotEmpty())
                assertTrue(phrases.all { it.isNotBlank() })
            }
        }
    }

    @Test
    fun selectorRespectsItsCooldown() {
        val selector = SpeechSelector(minIntervalMs = 60_000L)
        val first = selector.pick(SpeechContext.HAPPY, Personality.FRIENDLY, now, Random(1))
        assertNotNull(first)
        val second = selector.pick(SpeechContext.HAPPY, Personality.FRIENDLY, now + 1_000L, Random(1))
        assertNull(second)
        val later = selector.pick(SpeechContext.HAPPY, Personality.FRIENDLY, now + 61_000L, Random(2))
        assertNotNull(later)
    }

    @Test
    fun selectorNeverRepeatsThePreviousPhraseImmediately() {
        val selector = SpeechSelector(minIntervalMs = 0L)
        var previous: String? = null
        repeat(30) { i ->
            val phrase = selector.pick(SpeechContext.IDLE_CHAT, Personality.PLAYFUL, now + i * 1_000L, Random(i.toLong()))
            if (phrase != null && previous != null) assertNotEquals(previous, phrase)
            if (phrase != null) previous = phrase
        }
    }

    @Test
    fun resetForgetsTheLastPhrase() {
        val selector = SpeechSelector(minIntervalMs = 60_000L)
        val first = selector.pick(SpeechContext.HAPPY, Personality.SHY, now, Random(1))
        selector.reset()
        val again = selector.pick(SpeechContext.HAPPY, Personality.SHY, now + 1_000L, Random(9))
        assertNotNull(first)
        assertNotNull(again)
    }
}
