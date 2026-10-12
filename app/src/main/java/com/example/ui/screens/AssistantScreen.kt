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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AssistantState
import com.example.data.model.ChatMessage
import com.example.data.model.MessageSender
import com.example.ui.components.NothingOrb
import com.example.ui.theme.NothingBlack
import com.example.ui.theme.NothingCardBg
import com.example.ui.theme.NothingCardBorder
import com.example.ui.theme.NothingDarkSurface
import com.example.ui.theme.NothingGreyDark
import com.example.ui.theme.NothingGreyLight
import com.example.ui.theme.NothingGreyMuted
import com.example.ui.theme.NothingRed
import com.example.ui.theme.NothingWhite
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusSuccess

@Composable
fun AssistantScreen(
    state: AssistantState,
    audioRms: Float,
    messages: List<ChatMessage>,
    onManualWake: () -> Unit,
    onSleepToggle: () -> Unit,
    onSendCommand: (String) -> Unit,
    onClearChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    var textInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val quickPrompts = listOf(
        "Open YouTube",
        "Set timer for 5 minutes",
        "Turn on flashlight",
        "Read my screen",
        "Read notifications",
        "Volume up",
        "Go back",
        "Who are you?"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NothingBlack)
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // State label indicator
        Box(
            modifier = Modifier
                .padding(top = 8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(NothingCardBg)
                .border(1.dp, NothingCardBorder, RoundedCornerShape(4.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(
                            when (state) {
                                AssistantState.SLEEPING -> NothingGreyDark
                                AssistantState.LISTENING_WAKE_WORD -> NothingWhite
                                AssistantState.LISTENING_COMMAND -> StatusSuccess
                                AssistantState.THINKING -> NothingGreyLight
                                AssistantState.SPEAKING -> NothingWhite
                                AssistantState.EXECUTING -> NothingRed
                                AssistantState.ERROR -> StatusError
                            }
                        )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = state.label,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = NothingWhite,
                    letterSpacing = 1.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Center Animated Orb
        NothingOrb(
            state = state,
            audioRms = audioRms,
            modifier = Modifier.clickable { onManualWake() }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Action control row (Wake / Talk & Sleep / Standby)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Talk / Wake Button
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(NothingWhite)
                    .clickable { onManualWake() }
                    .padding(vertical = 12.dp)
                    .testTag("wake_talk_button"),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Wake Emily",
                        tint = NothingBlack,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (state == AssistantState.LISTENING_COMMAND) "LISTENING..." else "TALK TO EMILY",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = NothingBlack,
                        letterSpacing = 0.8.sp
                    )
                }
            }

            // Sleep / Standby Button
            Box(
                modifier = Modifier
                    .weight(0.75f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(NothingCardBg)
                    .border(1.dp, NothingCardBorder, RoundedCornerShape(8.dp))
                    .clickable { onSleepToggle() }
                    .padding(vertical = 12.dp)
                    .testTag("sleep_standby_button"),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (state == AssistantState.SLEEPING) Icons.Default.Mic else Icons.Default.MicOff,
                        contentDescription = "Sleep Toggle",
                        tint = NothingWhite,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (state == AssistantState.SLEEPING) "WAKE UP" else "SLEEP",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = NothingWhite,
                        letterSpacing = 0.8.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Quick Command Chips
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(quickPrompts) { prompt ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(NothingCardBg)
                        .border(1.dp, NothingCardBorder, RoundedCornerShape(6.dp))
                        .clickable { onSendCommand(prompt) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = prompt,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.SansSerif,
                        color = NothingGreyLight
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Chat Timeline Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "LOG // CONVERSATION",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = NothingGreyMuted,
                letterSpacing = 1.sp
            )
            if (messages.isNotEmpty()) {
                Text(
                    text = "CLEAR",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = NothingGreyLight,
                    modifier = Modifier
                        .clickable { onClearChat() }
                        .padding(4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Conversation history
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Say \"Hey Emily\" or tap Talk above to begin.",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = NothingGreyMuted
                        )
                    }
                }
            } else {
                items(messages) { msg ->
                    MessageBubble(msg)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Text input bar (for manual command testing or quiet mode)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = textInput,
                onValueChange = { textInput = it },
                placeholder = {
                    Text(
                        "Command or question...",
                        fontSize = 12.sp,
                        color = NothingGreyMuted
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("command_text_input"),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = NothingCardBg,
                    unfocusedContainerColor = NothingCardBg,
                    focusedBorderColor = NothingWhite,
                    unfocusedBorderColor = NothingCardBorder,
                    focusedTextColor = NothingWhite,
                    unfocusedTextColor = NothingWhite
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (textInput.isNotBlank()) {
                            onSendCommand(textInput.trim())
                            textInput = ""
                        }
                    }
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = {
                    if (textInput.isNotBlank()) {
                        onSendCommand(textInput.trim())
                        textInput = ""
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(NothingWhite)
                    .testTag("send_command_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = NothingBlack,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun MessageBubble(message: ChatMessage) {
    val isUser = message.sender == MessageSender.USER
    val isSystem = message.sender == MessageSender.SYSTEM

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(if (isUser) 0.85f else 0.95f)
                .clip(RoundedCornerShape(8.dp))
                .background(if (isUser) NothingCardBg else NothingDarkSurface)
                .border(
                    width = 1.dp,
                    color = if (isUser) NothingCardBorder else NothingGreyDark,
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(12.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!isUser) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (isSystem) StatusError else NothingRed)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            text = when (message.sender) {
                                MessageSender.USER -> "YOU"
                                MessageSender.EMILY -> "EMILY"
                                MessageSender.SYSTEM -> "SYSTEM"
                            },
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isUser) NothingGreyLight else NothingWhite,
                            letterSpacing = 1.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = message.text,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.SansSerif,
                    color = NothingWhite,
                    lineHeight = 18.sp
                )

                // If message executed a phone action, show action result badge
                message.actionDetail?.let { action ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                            .background(NothingBlack)
                            .border(1.dp, NothingCardBorder, RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (action.success) StatusSuccess else StatusError)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ACTION: ${action.actionName.uppercase()}",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = if (action.success) StatusSuccess else StatusError
                            )
                        }
                    }
                }
            }
        }
    }
}
