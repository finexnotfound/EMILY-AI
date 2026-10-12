package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AssistantSettings
import com.example.ui.theme.NothingBlack
import com.example.ui.theme.NothingCardBg
import com.example.ui.theme.NothingCardBorder
import com.example.ui.theme.NothingGreyDark
import com.example.ui.theme.NothingGreyLight
import com.example.ui.theme.NothingGreyMuted
import com.example.ui.theme.NothingRed
import com.example.ui.theme.NothingWhite

@Composable
fun SettingsScreen(
    currentSettings: AssistantSettings,
    onSettingsChanged: (AssistantSettings) -> Unit,
    modifier: Modifier = Modifier
) {
    var pitch by remember { mutableFloatStateOf(currentSettings.voicePitch) }
    var speed by remember { mutableFloatStateOf(currentSettings.voiceSpeed) }
    var continuous by remember { mutableStateOf(currentSettings.continuousListening) }
    var selectedLanguage by remember { mutableStateOf(currentSettings.languageCode) }
    var selectedModel by remember { mutableStateOf(currentSettings.selectedModel) }
    var apiKey by remember { mutableStateOf(currentSettings.customApiKey) }
    var lowMemory by remember { mutableStateOf(currentSettings.lowMemoryMode) }

    val languages = listOf(
        "en-IN" to "English / Hinglish (India)",
        "en-US" to "English (US)",
        "en-GB" to "English (UK)"
    )

    val models = listOf(
        "gemini-3.5-flash" to "Gemini 3.5 Flash (Recommended)",
        "gemini-3.1-pro-preview" to "Gemini 3.1 Pro (High Thinking)",
        "gemini-3.1-flash-lite-preview" to "Gemini 3.1 Flash-Lite (Speed)"
    )

    fun notifyUpdate() {
        onSettingsChanged(
            currentSettings.copy(
                voicePitch = pitch,
                voiceSpeed = speed,
                continuousListening = continuous,
                languageCode = selectedLanguage,
                selectedModel = selectedModel,
                customApiKey = apiKey,
                lowMemoryMode = lowMemory
            )
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(NothingBlack)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "CONFIG // VOICE & AI ENGINE",
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = NothingWhite,
                letterSpacing = 1.2.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Tailored for Samsung Galaxy SM-A042F (Android 14, 3GB RAM).",
                fontSize = 12.sp,
                fontFamily = FontFamily.SansSerif,
                color = NothingGreyMuted
            )
        }

        // Continuous Conversation
        item {
            SettingsCard(title = "CONTINUOUS CONVERSATION") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auto-listen after speaking",
                            fontSize = 13.sp,
                            color = NothingWhite
                        )
                        Text(
                            text = "Keeps listening for follow-ups without pressing a button.",
                            fontSize = 11.sp,
                            color = NothingGreyMuted
                        )
                    }
                    Switch(
                        checked = continuous,
                        onCheckedChange = {
                            continuous = it
                            notifyUpdate()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NothingBlack,
                            checkedTrackColor = NothingWhite,
                            uncheckedThumbColor = NothingGreyLight,
                            uncheckedTrackColor = NothingCardBg
                        )
                    )
                }
            }
        }

        // Voice Language & Locale
        item {
            SettingsCard(title = "LANGUAGE & ACCENT") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    languages.forEach { (code, label) ->
                        val isSelected = selectedLanguage == code
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) NothingGreyDark else NothingBlack)
                                .border(1.dp, if (isSelected) NothingWhite else NothingCardBorder, RoundedCornerShape(6.dp))
                                .clickable {
                                    selectedLanguage = code
                                    notifyUpdate()
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    color = if (isSelected) NothingWhite else NothingGreyLight,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(NothingRed)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Voice Pitch & Speed
        item {
            SettingsCard(title = "VOICE PITCH & SPEED") {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Pitch (Natural Female):", fontSize = 12.sp, color = NothingGreyLight)
                        Text(String.format("%.2fx", pitch), fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = NothingWhite)
                    }
                    Slider(
                        value = pitch,
                        onValueChange = {
                            pitch = it
                            notifyUpdate()
                        },
                        valueRange = 0.8f..1.4f,
                        colors = SliderDefaults.colors(
                            thumbColor = NothingWhite,
                            activeTrackColor = NothingWhite,
                            inactiveTrackColor = NothingGreyDark
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Speech Rate:", fontSize = 12.sp, color = NothingGreyLight)
                        Text(String.format("%.2fx", speed), fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = NothingWhite)
                    }
                    Slider(
                        value = speed,
                        onValueChange = {
                            speed = it
                            notifyUpdate()
                        },
                        valueRange = 0.75f..1.4f,
                        colors = SliderDefaults.colors(
                            thumbColor = NothingWhite,
                            activeTrackColor = NothingWhite,
                            inactiveTrackColor = NothingGreyDark
                        )
                    )
                }
            }
        }

        // Gemini AI Model Selector
        item {
            SettingsCard(title = "GEMINI AI MODEL") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    models.forEach { (modelId, desc) ->
                        val isSelected = selectedModel == modelId
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) NothingGreyDark else NothingBlack)
                                .border(1.dp, if (isSelected) NothingWhite else NothingCardBorder, RoundedCornerShape(6.dp))
                                .clickable {
                                    selectedModel = modelId
                                    notifyUpdate()
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = desc,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.SansSerif,
                                color = if (isSelected) NothingWhite else NothingGreyLight,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }

        // Custom Gemini API Key override
        item {
            SettingsCard(title = "GEMINI API KEY") {
                Column {
                    Text(
                        text = "Configured via BuildConfig / AI Studio Secrets by default. You can enter an override key below if needed:",
                        fontSize = 11.sp,
                        color = NothingGreyMuted
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = {
                            apiKey = it
                            notifyUpdate()
                        },
                        placeholder = { Text("AIzaSy...", fontSize = 12.sp, color = NothingGreyDark) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_api_key_input"),
                        shape = RoundedCornerShape(6.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = NothingBlack,
                            unfocusedContainerColor = NothingBlack,
                            focusedBorderColor = NothingWhite,
                            unfocusedBorderColor = NothingCardBorder,
                            focusedTextColor = NothingWhite,
                            unfocusedTextColor = NothingWhite
                        ),
                        singleLine = true
                    )
                }
            }
        }

        // Samsung Galaxy A042F Low RAM Mode & Architectural Guide
        item {
            SettingsCard(title = "SAMSUNG SM-A042F SPECIFICATIONS") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "• RAM: 3 GB (Optimized with low footprint, no heavy local models, stream audio duty cycling).",
                        fontSize = 12.sp,
                        color = NothingGreyLight
                    )
                    Text(
                        text = "• Wake Word: Local speech recognition looking for \"Hey Emily\" in standby mode.",
                        fontSize = 12.sp,
                        color = NothingGreyLight
                    )
                    Text(
                        text = "• Android 14 Policy: Microphone access in background is protected by Foreground Service with persistent notification. Battery optimization MUST be exempted to prevent One UI from killing the listener.",
                        fontSize = 12.sp,
                        color = NothingGreyLight
                    )
                    Text(
                        text = "• Offline Mode: Core phone controls (flashlight, volume, timer, alarm, app launching, navigation) execute instantly without internet.",
                        fontSize = 12.sp,
                        color = NothingGreyLight
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun SettingsCard(
    title: String,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(NothingCardBg)
            .border(1.dp, NothingCardBorder, RoundedCornerShape(8.dp))
            .padding(14.dp)
    ) {
        Column {
            Text(
                text = title,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = NothingWhite,
                letterSpacing = 0.8.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}
