package com.vudev.pet.data

import android.content.Context
import com.vudev.pet.core.Personality
import com.vudev.pet.render.PetLooks

/** Cấu hình người dùng. SharedPreferences là đủ cho dữ liệu nhỏ, không cần Room. */
class PetSettings(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var petName: String
        get() = prefs.getString(KEY_NAME, "Bông").orEmpty().ifBlank { "Bông" }
        set(value) = prefs.edit().putString(KEY_NAME, value.trim().take(24).ifBlank { "Bông" }).apply()

    var personality: Personality
        get() = Personality.fromId(prefs.getString(KEY_PERSONALITY, Personality.FRIENDLY.id))
        set(value) = prefs.edit().putString(KEY_PERSONALITY, value.id).apply()

    var lookId: String
        get() = prefs.getString(KEY_LOOK, PetLooks.BONG.id).orEmpty().ifBlank { PetLooks.BONG.id }
        set(value) = prefs.edit().putString(KEY_LOOK, value).apply()

    /** Kích thước thú trên màn hình, dp. */
    var sizeDp: Int
        get() = prefs.getInt(KEY_SIZE, 150).coerceIn(90, 260)
        set(value) = prefs.edit().putInt(KEY_SIZE, value.coerceIn(90, 260)).apply()

    /** 0.35..1.0 */
    var opacity: Float
        get() = prefs.getFloat(KEY_OPACITY, 1f).coerceIn(0.35f, 1f)
        set(value) = prefs.edit().putFloat(KEY_OPACITY, value.coerceIn(0.35f, 1f)).apply()

    var soundOn: Boolean
        get() = prefs.getBoolean(KEY_SOUND, true)
        set(value) = prefs.edit().putBoolean(KEY_SOUND, value).apply()

    var bubbleOn: Boolean
        get() = prefs.getBoolean(KEY_BUBBLE, true)
        set(value) = prefs.edit().putBoolean(KEY_BUBBLE, value).apply()

    /** Giờ yên tĩnh: không đọc TTS và không hiện bong bóng tự động. */
    var quietEnabled: Boolean
        get() = prefs.getBoolean(KEY_QUIET, true)
        set(value) = prefs.edit().putBoolean(KEY_QUIET, value).apply()

    var quietStartHour: Int
        get() = prefs.getInt(KEY_QUIET_START, 22).coerceIn(0, 23)
        set(value) = prefs.edit().putInt(KEY_QUIET_START, value.coerceIn(0, 23)).apply()

    var quietEndHour: Int
        get() = prefs.getInt(KEY_QUIET_END, 7).coerceIn(0, 23)
        set(value) = prefs.edit().putInt(KEY_QUIET_END, value.coerceIn(0, 23)).apply()

    /** Người dùng đã đồng ý cho thú chạy trên màn hình. */
    var overlayEnabled: Boolean
        get() = prefs.getBoolean(KEY_OVERLAY, false)
        set(value) = prefs.edit().putBoolean(KEY_OVERLAY, value).apply()

    private var overlayX: Int
        get() = prefs.getInt(KEY_X, -1)
        set(value) = prefs.edit().putInt(KEY_X, value).apply()

    private var overlayY: Int
        get() = prefs.getInt(KEY_Y, -1)
        set(value) = prefs.edit().putInt(KEY_Y, value).apply()

    fun savedPosition(): Pair<Int, Int> = Pair(overlayX, overlayY)

    fun savePosition(x: Int, y: Int) {
        overlayX = x
        overlayY = y
    }

    fun isQuietNow(hourOfDay: Int): Boolean {
        if (!quietEnabled) return false
        val start = quietStartHour
        val end = quietEndHour
        return if (start == end) false
        else if (start < end) hourOfDay in start until end
        else hourOfDay >= start || hourOfDay < end
    }

    companion object {
        private const val PREFS = "pet_settings"
        private const val KEY_NAME = "name"
        private const val KEY_PERSONALITY = "personality"
        private const val KEY_LOOK = "look"
        private const val KEY_SIZE = "size_dp"
        private const val KEY_OPACITY = "opacity"
        private const val KEY_SOUND = "sound"
        private const val KEY_BUBBLE = "bubble"
        private const val KEY_QUIET = "quiet"
        private const val KEY_QUIET_START = "quiet_start"
        private const val KEY_QUIET_END = "quiet_end"
        private const val KEY_OVERLAY = "overlay_enabled"
        private const val KEY_X = "overlay_x"
        private const val KEY_Y = "overlay_y"
    }
}
