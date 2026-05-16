package com.rehaby.app.util

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class VoiceFeedbackManager(context: Context) : TextToSpeech.OnInitListener {

    private val tts = TextToSpeech(context.applicationContext, this)
    private var isReady = false
    private var lastSpokenMessage = ""
    private var lastSpokenTime = 0L
    private val cooldownMs = 4000L

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.ENGLISH
            isReady = true
        }
    }

    fun speak(message: String) {
        if (!isReady || message.isEmpty()) return
        val now = System.currentTimeMillis()
        if (message == lastSpokenMessage && (now - lastSpokenTime) < cooldownMs) return
        lastSpokenMessage = message
        lastSpokenTime = now
        tts.speak(message, TextToSpeech.QUEUE_FLUSH, null, "rehaby_feedback")
    }

    fun shutdown() {
        tts.stop()
        tts.shutdown()
    }
}
