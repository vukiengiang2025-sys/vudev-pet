package com.vudev.pet.data

import android.content.Context
import com.vudev.pet.core.Behavior
import com.vudev.pet.core.InteractionMemory
import com.vudev.pet.core.Mood
import com.vudev.pet.core.Needs
import com.vudev.pet.core.Personality
import com.vudev.pet.core.PetState
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Lưu trạng thái thú bằng JSON, ghi nguyên tử (tmp + rename). Không dùng Room vì dữ
 * liệu rất nhỏ và cần đọc/ghi nhanh trong service.
 */
class PetRepository(context: Context) {

    private val file = File(context.filesDir, "pet_state.json")

    fun load(): PetState? = runCatching {
        if (!file.exists()) return null
        val root = JSONObject(file.readText())
        val needs = root.optJSONObject("needs") ?: JSONObject()
        val memory = root.optJSONObject("memory") ?: JSONObject()
        val recent = mutableListOf<Behavior>()
        memory.optJSONArray("recent")?.let { arr ->
            for (i in 0 until arr.length()) {
                val name = arr.optString(i)
                Behavior.entries.firstOrNull { it.name == name }?.let { recent.add(it) }
            }
        }
        PetState(
            name = root.optString("name", "Bông"),
            personality = Personality.fromId(root.optString("personality")),
            needs = Needs(
                hunger = needs.optDouble("hunger", 20.0),
                happiness = needs.optDouble("happiness", 70.0),
                energy = needs.optDouble("energy", 80.0),
                cleanliness = needs.optDouble("cleanliness", 85.0),
                affection = needs.optDouble("affection", 50.0),
            ).clamped(),
            behavior = Behavior.entries.firstOrNull { it.name == root.optString("behavior") } ?: Behavior.IDLE,
            behaviorStartedAt = root.optLong("behavior_started", 0L),
            behaviorEndsAt = root.optLong("behavior_ends", 0L),
            mood = Mood.entries.firstOrNull { it.name == root.optString("mood") } ?: Mood.CALM,
            memory = InteractionMemory(
                lastInteractionAt = memory.optLong("last_interaction", 0L),
                lastPetAt = memory.optLong("last_pet", 0L),
                lastFeedAt = memory.optLong("last_feed", 0L),
                lastPlayAt = memory.optLong("last_play", 0L),
                lastBatheAt = memory.optLong("last_bathe", 0L),
                lastTalkAt = memory.optLong("last_talk", 0L),
                totalInteractions = memory.optInt("total", 0),
                recentBehaviors = recent,
            ),
            lastSeenWallMs = root.optLong("last_seen", 0L),
        )
    }.getOrNull()

    fun save(state: PetState) {
        runCatching {
            val names = JSONArray()
            state.memory.recentBehaviors.forEach { names.put(it.name) }
            val memory = JSONObject()
                .put("last_interaction", state.memory.lastInteractionAt)
                .put("last_pet", state.memory.lastPetAt)
                .put("last_feed", state.memory.lastFeedAt)
                .put("last_play", state.memory.lastPlayAt)
                .put("last_bathe", state.memory.lastBatheAt)
                .put("last_talk", state.memory.lastTalkAt)
                .put("total", state.memory.totalInteractions)
                .put("recent", names)
            val needs = JSONObject()
                .put("hunger", state.needs.hunger)
                .put("happiness", state.needs.happiness)
                .put("energy", state.needs.energy)
                .put("cleanliness", state.needs.cleanliness)
                .put("affection", state.needs.affection)
            val root = JSONObject()
                .put("version", 1)
                .put("name", state.name)
                .put("personality", state.personality.id)
                .put("needs", needs)
                .put("behavior", state.behavior.name)
                .put("behavior_started", state.behaviorStartedAt)
                .put("behavior_ends", state.behaviorEndsAt)
                .put("mood", state.mood.name)
                .put("memory", memory)
                .put("last_seen", state.lastSeenWallMs)
            val tmp = File(file.parentFile, "pet_state.json.tmp")
            tmp.writeText(root.toString())
            if (!tmp.renameTo(file)) {
                file.writeText(root.toString())
                tmp.delete()
            }
        }
    }

    fun clear() {
        file.delete()
    }
}
