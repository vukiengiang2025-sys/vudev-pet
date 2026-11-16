package com.vudev.pet.speech

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * Bọc Android TextToSpeech. Máy thiếu giọng tiếng Việt vẫn chạy app: [ready] false thì
 * app chỉ hiện bong bóng chữ, không báo lỗi.
 */
class PetSpeech(context: Context) : TextToSpeech.OnInitListener {

    private val appContext = context.applicationContext
    private var tts: TextToSpeech? = null
    var ready: Boolean = false
        private set
    var vietnameseVoice: Boolean = false
        private set

    fun init() {
        if (tts != null) return
        tts = TextToSpeech(appContext, this)
    }

    override fun onInit(status: Int) {
        val engine = tts ?: return
        if (status != TextToSpeech.SUCCESS) {
            ready = false
            return
        }
        val result = runCatching { engine.setLanguage(Locale("vi", "VN")) }.getOrDefault(TextToSpeech.LANG_NOT_SUPPORTED)
        vietnameseVoice = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
        if (vietnameseVoice) {
            runCatching {
                engine.setPitch(1.05f)
                engine.setSpeechRate(0.96f)
            }
            ready = true
        } else {
            ready = false
        }
    }

    fun speak(text: String) {
        if (!ready || text.isBlank()) return
        runCatching { tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "vudev-pet") }
    }

    fun stop() {
        runCatching { tts?.stop() }
    }

    fun shutdown() {
        runCatching { tts?.stop() }
        runCatching { tts?.shutdown() }
        tts = null
        ready = false
    }
}
