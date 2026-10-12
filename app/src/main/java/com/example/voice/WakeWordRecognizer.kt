package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log

enum class ListeningMode {
    WAKE_WORD_STANDBY,
    ACTIVE_COMMAND
}

class WakeWordRecognizer(
    private val context: Context,
    private val onWakeWordDetected: () -> Unit,
    private val onCommandReceived: (String) -> Unit,
    private val onRmsChanged: (Float) -> Unit,
    private val onSleepRequested: () -> Unit
) {
    companion object {
        private const val TAG = "WakeWordRecognizer"
        private val WAKE_PHRASES = listOf("hey emily", "emily", "ok emily", "okay emily", "wake up emily", "hello emily")
        private val SLEEP_PHRASES = listOf("stop listening", "go to sleep", "sleep emily", "quiet emily")
    }

    private var speechRecognizer: SpeechRecognizer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var isListening = false
    private var currentMode = ListeningMode.WAKE_WORD_STANDBY
    private var languageLocale = "en-IN"

    fun setLanguage(lang: String) {
        languageLocale = lang
    }

    fun startStandby() {
        currentMode = ListeningMode.WAKE_WORD_STANDBY
        startListeningInternal()
    }

    fun startCommandListening() {
        currentMode = ListeningMode.ACTIVE_COMMAND
        startListeningInternal()
    }

    fun stop() {
        isListening = false
        handler.removeCallbacksAndMessages(null)
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recognizer: ${e.message}")
        }
        speechRecognizer = null
    }

    private fun startListeningInternal() {
        handler.post {
            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                Log.e(TAG, "Speech recognition not available on device.")
                return@post
            }

            try {
                speechRecognizer?.destroy()
            } catch (e: Exception) {
                // Ignore
            }

            try {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(createListener())
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageLocale)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                    // Prefer offline processing if device model exists to save battery & data
                    putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                }

                speechRecognizer?.startListening(intent)
                isListening = true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start listening: ${e.message}")
                scheduleRestart(1500)
            }
        }
    }

    private fun scheduleRestart(delayMs: Long) {
        if (!isListening) return
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({
            if (isListening) {
                startListeningInternal()
            }
        }, delayMs)
    }

    private fun createListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}

        override fun onBeginningOfSpeech() {}

        override fun onRmsChanged(rmsdB: Float) {
            // Normalize RMS dB (typical range -2 to 10 dB) to 0.0 .. 1.0
            val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
            onRmsChanged(normalized)
        }

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {}

        override fun onError(error: Int) {
            // SpeechRecognizer errors (e.g. timeout / no match in standby) are normal in continuous loops
            onRmsChanged(0f)
            if (isListening) {
                // Quick restart for continuous listening loop
                scheduleRestart(500)
            }
        }

        override fun onResults(results: Bundle?) {
            onRmsChanged(0f)
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val recognizedText = matches?.firstOrNull()?.trim() ?: ""

            handleRecognizedSpeech(recognizedText)
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val partial = matches?.firstOrNull()?.lowercase()?.trim() ?: ""

            // Quick trigger if user said wake word during standby
            if (currentMode == ListeningMode.WAKE_WORD_STANDBY && partial.isNotBlank()) {
                if (containsWakePhrase(partial)) {
                    val remainingCommand = extractCommandAfterWake(partial)
                    speechRecognizer?.stopListening()
                    onWakeWordDetected()
                    if (remainingCommand.isNotBlank()) {
                        onCommandReceived(remainingCommand)
                    } else {
                        startCommandListening()
                    }
                }
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    private fun handleRecognizedSpeech(text: String) {
        if (text.isBlank()) {
            if (currentMode == ListeningMode.WAKE_WORD_STANDBY) {
                scheduleRestart(300)
            }
            return
        }

        val lower = text.lowercase()

        // Check for sleep command
        if (SLEEP_PHRASES.any { lower.contains(it) }) {
            onSleepRequested()
            return
        }

        when (currentMode) {
            ListeningMode.WAKE_WORD_STANDBY -> {
                if (containsWakePhrase(lower)) {
                    val command = extractCommandAfterWake(lower)
                    onWakeWordDetected()
                    if (command.isNotBlank()) {
                        onCommandReceived(command)
                    } else {
                        startCommandListening()
                    }
                } else {
                    scheduleRestart(300)
                }
            }
            ListeningMode.ACTIVE_COMMAND -> {
                // If user said wake phrase again inside command, clean it
                val cleanCommand = if (containsWakePhrase(lower)) {
                    extractCommandAfterWake(lower)
                } else {
                    text
                }
                if (cleanCommand.isNotBlank()) {
                    onCommandReceived(cleanCommand)
                } else {
                    scheduleRestart(300)
                }
            }
        }
    }

    private fun containsWakePhrase(text: String): Boolean {
        return WAKE_PHRASES.any { text.contains(it) }
    }

    private fun extractCommandAfterWake(text: String): String {
        for (phrase in WAKE_PHRASES) {
            if (text.contains(phrase)) {
                return text.substringAfter(phrase).trim()
            }
        }
        return text
    }
}
