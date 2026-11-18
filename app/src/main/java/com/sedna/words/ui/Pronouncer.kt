package com.sedna.words.ui

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class Pronouncer(context: Context) {
    private var ready = false
    private val engine = TextToSpeech(context.applicationContext) { code ->
        ready = code == TextToSpeech.SUCCESS
    }

    fun speak(word: String) {
        if (!ready || word.isBlank()) return
        engine.language = Locale.US
        engine.speak(word, TextToSpeech.QUEUE_FLUSH, null, "sedna-${System.nanoTime()}")
    }

    fun close() {
        engine.stop()
        engine.shutdown()
    }
}
