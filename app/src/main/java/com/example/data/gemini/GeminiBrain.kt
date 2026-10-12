package com.example.data.gemini

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.AssistantSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

sealed class GeminiResult {
    data class Success(val spokenText: String, val displayText: String) : GeminiResult()
    data class ToolCall(val functionName: String, val arguments: JSONObject, val rationale: String) : GeminiResult()
    data class Error(val message: String) : GeminiResult()
}

class GeminiBrain(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
) {
    companion object {
        private const val TAG = "GeminiBrain"
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/"
    }

    // Session conversation history for multi-turn context
    private val conversationHistory = mutableListOf<JSONObject>()

    fun resetSession() {
        conversationHistory.clear()
    }

    suspend fun processUserPrompt(
        prompt: String,
        settings: AssistantSettings
    ): GeminiResult = withContext(Dispatchers.IO) {
        val apiKey = if (settings.customApiKey.isNotBlank()) {
            settings.customApiKey
        } else {
            BuildConfig.GEMINI_API_KEY
        }

        // If offline or placeholder key without internet, use intelligent local heuristic fallback
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            val localHeuristic = tryOfflineHeuristics(prompt)
            if (localHeuristic != null) {
                return@withContext localHeuristic
            }
            return@withContext GeminiResult.Success(
                spokenText = "I heard you, but I need an active Gemini API key in Settings to answer general questions.",
                displayText = "Offline / No API key configured. You can still use phone control commands like 'Open YouTube', 'Flashlight on', 'Volume up', 'Set timer for 5 minutes', etc., or enter your Gemini API key in Settings."
            )
        }

        try {
            val modelName = settings.selectedModel
            val url = "$BASE_URL$modelName:generateContent?key=$apiKey"

            // Construct payload with system instructions, tools, and conversation history
            val payload = buildRequestPayload(prompt, modelName)

            val request = Request.Builder()
                .url(url)
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.e(TAG, "Gemini API error ${response.code}: $responseBody")
                // Check if local heuristic can fulfill the command despite API failure
                val fallback = tryOfflineHeuristics(prompt)
                if (fallback != null) return@withContext fallback

                return@withContext GeminiResult.Error("Gemini error (${response.code}). Please check your connection or API key.")
            }

            parseGeminiResponse(responseBody, prompt)
        } catch (e: Exception) {
            Log.e(TAG, "Network exception: ${e.message}", e)
            val fallback = tryOfflineHeuristics(prompt)
            if (fallback != null) return@withContext fallback
            GeminiResult.Error("Network error: ${e.localizedMessage ?: "Unable to connect."}")
        }
    }

    private fun buildRequestPayload(prompt: String, model: String): JSONObject {
        val root = JSONObject()

        // 1. System Instruction
        val systemInstruction = JSONObject().apply {
            val parts = JSONArray().apply {
                put(JSONObject().apply {
                    put("text", """
                        You are Emily, a friendly, ultra-fast personal AI assistant on Android (similar to Jarvis).
                        Guidelines:
                        1. Be extremely concise in speech. Keep responses under 2-3 sentences unless the user explicitly asks for deep explanation.
                        2. You have direct control of the user's phone via functions. If the user asks to open an app, adjust volume, set an alarm or timer, toggle flashlight, navigate, search the web, read screen, scroll, or go back/home, ALWAYS call the corresponding function rather than telling them how to do it.
                        3. You understand normal English and Hinglish seamlessly.
                        4. Maintain friendly, warm, intelligent persona.
                    """.trimIndent())
                })
            }
            put("parts", parts)
        }
        root.put("systemInstruction", systemInstruction)

        // 2. Tools definition
        val tools = JSONArray().apply {
            val toolObj = JSONObject().apply {
                put("functionDeclarations", getFunctionDeclarations())
            }
            put(toolObj)
        }
        root.put("tools", tools)

        // 3. Contents (History + User Prompt)
        val contents = JSONArray()
        for (hist in conversationHistory.takeLast(10)) {
            contents.put(hist)
        }
        val userTurn = JSONObject().apply {
            put("role", "user")
            put("parts", JSONArray().apply {
                put(JSONObject().apply { put("text", prompt) })
            })
        }
        contents.put(userTurn)
        root.put("contents", contents)

        // 4. Generation Config
        val genConfig = JSONObject().apply {
            put("temperature", 0.7)
            if (model.contains("pro")) {
                val thinkingConfig = JSONObject().apply {
                    put("thinkingLevel", "HIGH")
                }
                put("thinkingConfig", thinkingConfig)
            }
        }
        root.put("generationConfig", genConfig)

        return root
    }

    private fun getFunctionDeclarations(): JSONArray {
        val list = JSONArray()

        // open_app
        list.put(JSONObject().apply {
            put("name", "open_app")
            put("description", "Launch any installed Android application on the phone (e.g. YouTube, WhatsApp, Chrome, Camera, Spotify, Settings, Maps).")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("app_name", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Name of the application to launch.")
                    })
                })
                put("required", JSONArray().apply { put("app_name") })
            })
        })

        // set_alarm
        list.put(JSONObject().apply {
            put("name", "set_alarm")
            put("description", "Set an alarm at a specific hour and minute.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("hour", JSONObject().apply { put("type", "INTEGER"); put("description", "Hour (0-23)") })
                    put("minute", JSONObject().apply { put("type", "INTEGER"); put("description", "Minute (0-59)") })
                    put("label", JSONObject().apply { put("type", "STRING"); put("description", "Alarm label or reason") })
                })
                put("required", JSONArray().apply { put("hour"); put("minute") })
            })
        })

        // set_timer
        list.put(JSONObject().apply {
            put("name", "set_timer")
            put("description", "Set a countdown timer in seconds.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("seconds", JSONObject().apply { put("type", "INTEGER"); put("description", "Duration in seconds") })
                    put("label", JSONObject().apply { put("type", "STRING"); put("description", "Timer label") })
                })
                put("required", JSONArray().apply { put("seconds") })
            })
        })

        // toggle_flashlight
        list.put(JSONObject().apply {
            put("name", "toggle_flashlight")
            put("description", "Turn the device torch / flashlight on or off.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("enable", JSONObject().apply { put("type", "BOOLEAN"); put("description", "true to turn on, false to turn off") })
                })
                put("required", JSONArray().apply { put("enable") })
            })
        })

        // adjust_volume
        list.put(JSONObject().apply {
            put("name", "adjust_volume")
            put("description", "Raise, lower, or mute phone media volume.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("direction", JSONObject().apply { put("type", "STRING"); put("description", "raise, lower, or mute") })
                })
                put("required", JSONArray().apply { put("direction") })
            })
        })

        // start_navigation
        list.put(JSONObject().apply {
            put("name", "start_navigation")
            put("description", "Start turn-by-turn navigation in Maps to a destination.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("destination", JSONObject().apply { put("type", "STRING"); put("description", "Target address, city, or place name") })
                })
                put("required", JSONArray().apply { put("destination") })
            })
        })

        // search_web
        list.put(JSONObject().apply {
            put("name", "search_web")
            put("description", "Search Google or the web for a query.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("query", JSONObject().apply { put("type", "STRING"); put("description", "Search query terms") })
                })
                put("required", JSONArray().apply { put("query") })
            })
        })

        // open_settings
        list.put(JSONObject().apply {
            put("name", "open_settings")
            put("description", "Open Android Settings page (wifi, bluetooth, display, sound, apps, battery, etc.).")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("setting_type", JSONObject().apply { put("type", "STRING"); put("description", "wifi, bluetooth, sound, display, battery, apps, or general") })
                })
            })
        })

        // system_ui_action
        list.put(JSONObject().apply {
            put("name", "system_ui_action")
            put("description", "Perform global Android system action: back, home, recents, or notifications.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("action", JSONObject().apply { put("type", "STRING"); put("description", "back, home, recents, or notifications") })
                })
                put("required", JSONArray().apply { put("action") })
            })
        })

        // read_screen
        list.put(JSONObject().apply {
            put("name", "read_screen")
            put("description", "Read and inspect all visible text currently displayed on the device screen.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
            })
        })

        // scroll_screen
        list.put(JSONObject().apply {
            put("name", "scroll_screen")
            put("description", "Scroll current screen up or down.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("direction", JSONObject().apply { put("type", "STRING"); put("description", "up or down") })
                })
            })
        })

        // read_notifications
        list.put(JSONObject().apply {
            put("name", "read_notifications")
            put("description", "Read aloud active incoming notifications and messages.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
            })
        })

        return list
    }

    private fun parseGeminiResponse(jsonString: String, prompt: String): GeminiResult {
        val root = JSONObject(jsonString)
        val candidates = root.optJSONArray("candidates") ?: return GeminiResult.Error("No response candidates returned.")
        val firstCand = candidates.optJSONObject(0) ?: return GeminiResult.Error("Empty candidate response.")
        val content = firstCand.optJSONObject("content") ?: return GeminiResult.Error("No content in candidate.")
        val parts = content.optJSONArray("parts") ?: return GeminiResult.Error("No parts in content.")

        for (i in 0 until parts.length()) {
            val part = parts.optJSONObject(i) ?: continue

            // Check if function call
            if (part.has("functionCall")) {
                val funcCall = part.getJSONObject("functionCall")
                val name = funcCall.optString("name")
                val args = funcCall.optJSONObject("args") ?: JSONObject()
                return GeminiResult.ToolCall(
                    functionName = name,
                    arguments = args,
                    rationale = "Executing $name"
                )
            }

            // Text response
            val text = part.optString("text")
            if (text.isNotBlank()) {
                // Add to multi-turn session history
                val userTurn = JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply { put(JSONObject().apply { put("text", prompt) }) })
                }
                val modelTurn = JSONObject().apply {
                    put("role", "model")
                    put("parts", JSONArray().apply { put(JSONObject().apply { put("text", text) }) })
                }
                conversationHistory.add(userTurn)
                conversationHistory.add(modelTurn)

                return GeminiResult.Success(
                    spokenText = cleanForSpeech(text),
                    displayText = text
                )
            }
        }

        return GeminiResult.Error("No intelligible text or action found in model response.")
    }

    private fun cleanForSpeech(text: String): String {
        return text
            .replace(Regex("[*#_`~>|]"), "")
            .replace(Regex("\\[(.*?)\\]\\(.*?\\)"), "$1")
            .lines()
            .filter { it.isNotBlank() }
            .take(3)
            .joinToString(" ")
    }

    // Local heuristic parser for offline or instant command recognition
    private fun tryOfflineHeuristics(prompt: String): GeminiResult? {
        val p = prompt.lowercase().trim()

        if (p.contains("open ") || p.contains("launch ") || p.contains("start ")) {
            val app = p.substringAfter("open ")
                .substringAfter("launch ")
                .substringAfter("start ")
                .removeSuffix(" app")
                .removeSuffix(" please")
                .trim()
            if (app.isNotBlank()) {
                val args = JSONObject().apply { put("app_name", app) }
                return GeminiResult.ToolCall("open_app", args, "Opening $app")
            }
        }

        if (p.contains("flashlight") || p.contains("torch")) {
            val enable = !p.contains("off") && !p.contains("disable")
            val args = JSONObject().apply { put("enable", enable) }
            return GeminiResult.ToolCall("toggle_flashlight", args, if (enable) "Turning on flashlight" else "Turning off flashlight")
        }

        if (p.contains("volume")) {
            val direction = if (p.contains("down") || p.contains("lower") || p.contains("decrease")) "lower"
            else if (p.contains("mute")) "mute"
            else "raise"
            val args = JSONObject().apply { put("direction", direction) }
            return GeminiResult.ToolCall("adjust_volume", args, "Adjusting volume")
        }

        if (p.contains("alarm")) {
            // E.g. "set alarm for 7:30" or "set alarm 8 am"
            val regex = Regex("(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?")
            val match = regex.find(p)
            if (match != null) {
                var hour = match.groupValues[1].toIntOrNull() ?: 7
                val minute = match.groupValues[2].toIntOrNull() ?: 0
                val ampm = match.groupValues[3].lowercase()
                if (ampm == "pm" && hour < 12) hour += 12
                if (ampm == "am" && hour == 12) hour = 0
                val args = JSONObject().apply {
                    put("hour", hour)
                    put("minute", minute)
                    put("label", "Alarm")
                }
                return GeminiResult.ToolCall("set_alarm", args, "Setting alarm for $hour:$minute")
            }
        }

        if (p.contains("timer")) {
            val regex = Regex("(\\d+)\\s*(second|minute|min|sec|hour)")
            val match = regex.find(p)
            var seconds = 300
            if (match != null) {
                val num = match.groupValues[1].toIntOrNull() ?: 5
                val unit = match.groupValues[2].lowercase()
                seconds = when {
                    unit.startsWith("sec") -> num
                    unit.startsWith("min") -> num * 60
                    unit.startsWith("hour") -> num * 3600
                    else -> num * 60
                }
            }
            val args = JSONObject().apply {
                put("seconds", seconds)
                put("label", "Timer")
            }
            return GeminiResult.ToolCall("set_timer", args, "Setting timer for $seconds seconds")
        }

        if (p.contains("navigate to ") || p.contains("directions to ")) {
            val dest = p.substringAfter("navigate to ").substringAfter("directions to ").trim()
            val args = JSONObject().apply { put("destination", dest) }
            return GeminiResult.ToolCall("start_navigation", args, "Navigating to $dest")
        }

        if (p.contains("search for ") || p.contains("google ")) {
            val q = p.substringAfter("search for ").substringAfter("google ").trim()
            val args = JSONObject().apply { put("query", q) }
            return GeminiResult.ToolCall("search_web", args, "Searching for $q")
        }

        if (p == "go back" || p == "back") {
            return GeminiResult.ToolCall("system_ui_action", JSONObject().apply { put("action", "back") }, "Going back")
        }

        if (p == "go home" || p == "home") {
            return GeminiResult.ToolCall("system_ui_action", JSONObject().apply { put("action", "home") }, "Going home")
        }

        if (p.contains("recent")) {
            return GeminiResult.ToolCall("system_ui_action", JSONObject().apply { put("action", "recents") }, "Opening recents")
        }

        if (p.contains("scroll down")) {
            return GeminiResult.ToolCall("scroll_screen", JSONObject().apply { put("direction", "down") }, "Scrolling down")
        }

        if (p.contains("scroll up")) {
            return GeminiResult.ToolCall("scroll_screen", JSONObject().apply { put("direction", "up") }, "Scrolling up")
        }

        if (p.contains("read screen") || p.contains("read this screen") || p.contains("what's on my screen") || p.contains("explain screen")) {
            return GeminiResult.ToolCall("read_screen", JSONObject(), "Reading screen")
        }

        if (p.contains("notification") || p.contains("read my messages") || p.contains("read alerts")) {
            return GeminiResult.ToolCall("read_notifications", JSONObject(), "Checking notifications")
        }

        if (p.contains("who are you") || p.contains("what is your name")) {
            return GeminiResult.Success(
                spokenText = "I'm Emily, your personal Android AI assistant.",
                displayText = "I'm Emily, your personal Android Jarvis assistant, designed for hands-free phone control, intelligent answers, and low power usage."
            )
        }

        return null
    }
}
