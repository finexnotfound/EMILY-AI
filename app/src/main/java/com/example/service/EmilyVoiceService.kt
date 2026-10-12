package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.gemini.GeminiBrain
import com.example.data.gemini.GeminiResult
import com.example.data.model.ActionExecutionDetail
import com.example.data.model.AssistantSettings
import com.example.data.model.AssistantState
import com.example.data.model.ChatMessage
import com.example.data.model.MessageSender
import com.example.phonecontrol.PhoneActionExecutor
import com.example.voice.EmilySpeaker
import com.example.voice.WakeWordRecognizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EmilyVoiceService : Service() {

    companion object {
        private const val TAG = "EmilyVoiceService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "emily_voice_assistant_channel"

        const val ACTION_START = "com.example.action.START"
        const val ACTION_STOP = "com.example.action.STOP"
        const val ACTION_WAKE = "com.example.action.WAKE"
        const val ACTION_SLEEP = "com.example.action.SLEEP"

        var isRunning = false
            private set
    }

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

    private val _assistantState = MutableStateFlow(AssistantState.SLEEPING)
    val assistantState: StateFlow<AssistantState> = _assistantState.asStateFlow()

    private val _audioRms = MutableStateFlow(0f)
    val audioRms: StateFlow<Float> = _audioRms.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private var settings = AssistantSettings()
    private lateinit var speaker: EmilySpeaker
    private lateinit var recognizer: WakeWordRecognizer
    private lateinit var geminiBrain: GeminiBrain
    private lateinit var actionExecutor: PhoneActionExecutor

    inner class LocalBinder : Binder() {
        fun getService(): EmilyVoiceService = this@EmilyVoiceService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        createNotificationChannel()

        actionExecutor = PhoneActionExecutor(this)
        geminiBrain = GeminiBrain()

        speaker = EmilySpeaker(
            context = this,
            onSpeechStarted = {
                _assistantState.value = AssistantState.SPEAKING
            },
            onSpeechFinished = {
                onEmilyFinishedSpeaking()
            }
        )

        recognizer = WakeWordRecognizer(
            context = this,
            onWakeWordDetected = {
                onWakeWordDetected()
            },
            onCommandReceived = { command ->
                onUserCommand(command)
            },
            onRmsChanged = { rms ->
                _audioRms.value = rms
            },
            onSleepRequested = {
                goToSleep()
            }
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START

        when (action) {
            ACTION_START -> {
                startForegroundWithNotification("Standby: Say \"Hey Emily\"")
                startWakeWordStandby()
            }
            ACTION_STOP -> {
                stopListening()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_WAKE -> {
                manualWake()
            }
            ACTION_SLEEP -> {
                goToSleep()
            }
        }

        return START_STICKY
    }

    fun updateSettings(newSettings: AssistantSettings) {
        settings = newSettings
        speaker.setupVoice(newSettings.languageCode, newSettings.voicePitch, newSettings.voiceSpeed)
        recognizer.setLanguage(newSettings.languageCode)
    }

    fun manualWake() {
        vibratePhone()
        _assistantState.value = AssistantState.LISTENING_COMMAND
        speaker.stop()
        updateNotification("Listening for command...")
        recognizer.startCommandListening()
    }

    fun goToSleep() {
        _assistantState.value = AssistantState.SLEEPING
        speaker.stop()
        recognizer.stop()
        updateNotification("Emily is sleeping (Tap to wake)")
        speaker.speak("Going to sleep. Say Hey Emily to wake me.")
    }

    fun startWakeWordStandby() {
        _assistantState.value = AssistantState.LISTENING_WAKE_WORD
        speaker.stop()
        updateNotification("Standby: Say \"Hey Emily\"")
        recognizer.startStandby()
    }

    fun stopListening() {
        _assistantState.value = AssistantState.SLEEPING
        _audioRms.value = 0f
        speaker.stop()
        recognizer.stop()
    }

    private fun onWakeWordDetected() {
        vibratePhone()
        _assistantState.value = AssistantState.LISTENING_COMMAND
        speaker.stop()
        updateNotification("Listening...")
    }

    private fun onUserCommand(command: String) {
        if (command.isBlank()) {
            if (settings.continuousListening) {
                startWakeWordStandby()
            }
            return
        }

        // Add user message to log
        addMessage(ChatMessage(sender = MessageSender.USER, text = command))
        _assistantState.value = AssistantState.THINKING
        updateNotification("Thinking: \"$command\"")

        serviceScope.launch {
            val result = geminiBrain.processUserPrompt(command, settings)
            handleGeminiResult(result)
        }
    }

    private suspend fun handleGeminiResult(result: GeminiResult) {
        when (result) {
            is GeminiResult.Success -> {
                addMessage(ChatMessage(sender = MessageSender.EMILY, text = result.displayText))
                updateNotification("Speaking reply...")
                speaker.speak(result.spokenText)
            }
            is GeminiResult.ToolCall -> {
                _assistantState.value = AssistantState.EXECUTING
                updateNotification("Executing ${result.functionName}...")

                val execResult = actionExecutor.executeAction(result.functionName, result.arguments)
                addMessage(
                    ChatMessage(
                        sender = MessageSender.EMILY,
                        text = execResult.message,
                        actionDetail = execResult
                    )
                )

                // Speak real feedback confirming the action
                speaker.speak(execResult.message)
            }
            is GeminiResult.Error -> {
                _assistantState.value = AssistantState.ERROR
                val errorMsg = "Sorry, ${result.message}"
                addMessage(ChatMessage(sender = MessageSender.SYSTEM, text = errorMsg))
                speaker.speak(errorMsg)
            }
        }
    }

    private fun onEmilyFinishedSpeaking() {
        if (settings.continuousListening && _assistantState.value != AssistantState.SLEEPING) {
            // Keep conversation going: continue listening for user follow-up
            _assistantState.value = AssistantState.LISTENING_COMMAND
            updateNotification("Listening for follow-up...")
            recognizer.startCommandListening()
        } else {
            startWakeWordStandby()
        }
    }

    private fun addMessage(message: ChatMessage) {
        val current = _messages.value.toMutableList()
        current.add(message)
        // Keep last 40 messages to maintain ultra-low memory on 3GB RAM device
        if (current.size > 40) {
            current.removeAt(0)
        }
        _messages.value = current
    }

    fun clearMessages() {
        _messages.value = emptyList()
        geminiBrain.resetSession()
    }

    private fun vibratePhone() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                @Suppress("DEPRECATION")
                vibrator?.vibrate(80)
            }
        } catch (e: Exception) {
            Log.d(TAG, "Vibrate error: ${e.message}")
        }
    }

    private fun startForegroundWithNotification(statusText: String) {
        val notification = buildNotification(statusText)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification(statusText: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(statusText))
    }

    private fun buildNotification(statusText: String): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, EmilyVoiceService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val wakeIntent = Intent(this, EmilyVoiceService::class.java).apply {
            action = ACTION_WAKE
        }
        val wakePendingIntent = PendingIntent.getService(
            this, 2, wakeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Emily AI — Active")
            .setContentText(statusText)
            .setSmallIcon(R.drawable.ic_emily_logo)
            .setContentIntent(openAppPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(0, "Wake Up", wakePendingIntent)
            .addAction(0, "Stop", stopPendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_desc)
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        stopListening()
        speaker.shutdown()
    }
}
