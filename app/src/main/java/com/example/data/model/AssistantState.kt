package com.example.data.model

enum class AssistantState(val label: String) {
    SLEEPING("SLEEPING"),
    LISTENING_WAKE_WORD("STANDBY // HEY EMILY"),
    LISTENING_COMMAND("LISTENING TO YOU..."),
    THINKING("PROCESSING (GEMINI)..."),
    SPEAKING("SPEAKING..."),
    EXECUTING("EXECUTING ACTION..."),
    ERROR("SYSTEM NOTICE")
}

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: MessageSender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val actionDetail: ActionExecutionDetail? = null
)

enum class MessageSender {
    USER,
    EMILY,
    SYSTEM
}

data class ActionExecutionDetail(
    val actionName: String,
    val parameters: String,
    val success: Boolean,
    val message: String
)

data class AssistantSettings(
    val voicePitch: Float = 1.05f,
    val voiceSpeed: Float = 1.0f,
    val languageCode: String = "en-IN", // Supports English and Hinglish (India) or "en-US"
    val continuousListening: Boolean = true,
    val selectedModel: String = "gemini-3.5-flash",
    val customApiKey: String = "",
    val wakeWordSensitivity: Float = 0.5f,
    val lowMemoryMode: Boolean = true // Optimizations for 3GB RAM device
)
