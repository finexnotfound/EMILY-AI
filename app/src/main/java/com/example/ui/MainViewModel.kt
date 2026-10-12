package com.example.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AssistantSettings
import com.example.data.model.AssistantState
import com.example.data.model.ChatMessage
import com.example.service.EmilyVoiceService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    private val _assistantState = MutableStateFlow(AssistantState.SLEEPING)
    val assistantState: StateFlow<AssistantState> = _assistantState.asStateFlow()

    private val _audioRms = MutableStateFlow(0f)
    val audioRms: StateFlow<Float> = _audioRms.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _settings = MutableStateFlow(AssistantSettings())
    val settings: StateFlow<AssistantSettings> = _settings.asStateFlow()

    private val _isServiceBound = MutableStateFlow(false)
    val isServiceBound: StateFlow<Boolean> = _isServiceBound.asStateFlow()

    private var voiceService: EmilyVoiceService? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as? EmilyVoiceService.LocalBinder
            val svc = binder?.getService()
            voiceService = svc
            _isServiceBound.value = true

            svc?.updateSettings(_settings.value)

            viewModelScope.launch {
                svc?.assistantState?.collect { state ->
                    _assistantState.value = state
                }
            }

            viewModelScope.launch {
                svc?.audioRms?.collect { rms ->
                    _audioRms.value = rms
                }
            }

            viewModelScope.launch {
                svc?.messages?.collect { list ->
                    _messages.value = list
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            voiceService = null
            _isServiceBound.value = false
            _assistantState.value = AssistantState.SLEEPING
        }
    }

    fun bindService(context: Context) {
        val intent = Intent(context, EmilyVoiceService::class.java)
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    fun unbindService(context: Context) {
        if (_isServiceBound.value) {
            try {
                context.unbindService(serviceConnection)
            } catch (e: Exception) {
                // Ignore
            }
            _isServiceBound.value = false
            voiceService = null
        }
    }

    fun startService(context: Context) {
        val intent = Intent(context, EmilyVoiceService::class.java).apply {
            action = EmilyVoiceService.ACTION_START
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
        bindService(context)
    }

    fun stopService(context: Context) {
        voiceService?.stopListening()
        val intent = Intent(context, EmilyVoiceService::class.java).apply {
            action = EmilyVoiceService.ACTION_STOP
        }
        context.startService(intent)
        unbindService(context)
        _assistantState.value = AssistantState.SLEEPING
    }

    fun manualWake(context: Context) {
        if (voiceService == null) {
            startService(context)
        }
        voiceService?.manualWake()
    }

    fun toggleSleep(context: Context) {
        val current = _assistantState.value
        if (current == AssistantState.SLEEPING) {
            if (voiceService == null) {
                startService(context)
            }
            voiceService?.startWakeWordStandby()
        } else {
            voiceService?.goToSleep()
        }
    }

    fun sendCommand(context: Context, command: String) {
        if (voiceService == null) {
            startService(context)
        }
        voiceService?.manualWake()
        // Wait briefly if needed or directly trigger via speech callback
        // The service's onCommandReceived logic can be called directly
        voiceService?.let { svc ->
            val reflection = svc.javaClass.getDeclaredMethod("onUserCommand", String::class.java)
            reflection.isAccessible = true
            reflection.invoke(svc, command)
        }
    }

    fun updateSettings(newSettings: AssistantSettings) {
        _settings.value = newSettings
        voiceService?.updateSettings(newSettings)
    }

    fun clearChat() {
        voiceService?.clearMessages()
        _messages.value = emptyList()
    }
}
