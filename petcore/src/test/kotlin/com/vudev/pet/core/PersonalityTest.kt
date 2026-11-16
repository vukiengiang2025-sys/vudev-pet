package com.vudev.pet.core

import org.junit.Assert.assertTrue
import org.junit.Test

class PersonalityTest {

    @Test
    fun playfulValuesPlayMoreThanLazyDoes() {
        assertTrue(
            Personality.PLAYFUL.behaviorBias(Behavior.PLAY) >
                Personality.LAZY.behaviorBias(Behavior.PLAY),
        )
    }

    @Test
    fun lazySleepsMoreThanPlayful() {
        assertTrue(
            Personality.LAZY.behaviorBias(Behavior.SLEEP) >
                Personality.PLAYFUL.behaviorBias(Behavior.SLEEP),
        )
    }

    @Test
    fun energyDrainOrderingIsSensible() {
        assertTrue(Personality.PLAYFUL.energyDrainMultiplier > Personality.FRIENDLY.energyDrainMultiplier)
        assertTrue(Personality.FRIENDLY.energyDrainMultiplier > Personality.LAZY.energyDrainMultiplier)
    }

    @Test
    fun unknownIdFallsBackToFriendly() {
        assertTrue(Personality.fromId("khong-ton-tai") == Personality.FRIENDLY)
        assertTrue(Personality.fromId("lazy") == Personality.LAZY)
    }
}
