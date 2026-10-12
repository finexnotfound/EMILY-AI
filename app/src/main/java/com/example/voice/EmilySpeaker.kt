package com.example.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

class EmilySpeaker(
    private val context: Context,
    private val onSpeechStarted: () -> Unit,
    private val onSpeechFinished: () -> Unit
) : TextToSpeech.OnInitListener {

    companion object {
        private const val TAG = "EmilySpeaker"
    }

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            setupVoice("en-IN")
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    onSpeechStarted()
                }

                override fun onDone(utteranceId: String?) {
                    onSpeechFinished()
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    onSpeechFinished()
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    onSpeechFinished()
                }
            })
        } else {
            Log.e(TAG, "Failed to initialize TextToSpeech ($status)")
        }
    }

    fun setupVoice(languageCode: String, pitch: Float = 1.05f, speed: Float = 1.0f) {
        val tts = tts ?: return
        val locale = when {
            languageCode.contains("IN") -> Locale.forLanguageTag("en-IN")
            languageCode.contains("GB") -> Locale.UK
            else -> Locale.US
        }

        val result = tts.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            tts.language = Locale.US
        }

        // Set higher pitch for a clear, natural female vocal tone
        tts.setPitch(pitch)
        tts.setSpeechRate(speed)

        // Try to pick a natural female voice if provided by the device engine
        try {
            val voices = tts.voices
            val femaleVoice = voices?.firstOrNull { voice ->
                voice.name.lowercase().contains("female") || voice.name.lowercase().contains("sfg")
            }
            if (femaleVoice != null) {
                tts.voice = femaleVoice
            }
        } catch (e: Exception) {
            Log.d(TAG, "Voice selection fallback: ${e.message}")
        }
    }

    fun speak(text: String, utteranceId: String = "EMILY_SPEECH_${System.currentTimeMillis()}") {
        if (!isInitialized || text.isBlank()) {
            onSpeechFinished()
            return
        }
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
