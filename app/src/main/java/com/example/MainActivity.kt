package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.service.EmilyVoiceService
import com.example.ui.MainViewModel
import com.example.ui.components.NavTab
import com.example.ui.components.NothingTopBar
import com.example.ui.screens.AssistantScreen
import com.example.ui.screens.PermissionsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.EmilyAiTheme
import com.example.ui.theme.NothingBlack

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            EmilyAiTheme {
                MainContent(viewModel = viewModel)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.bindService(this)
    }

    override fun onStop() {
        super.onStop()
        viewModel.unbindService(this)
    }
}

@Composable
fun MainContent(viewModel: MainViewModel) {
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf(NavTab.ASSISTANT) }

    val assistantState by viewModel.assistantState.collectAsStateWithLifecycle()
    val audioRms by viewModel.audioRms.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val isBound by viewModel.isServiceBound.collectAsStateWithLifecycle()

    // Handle back button on sub-screens to return to ASSISTANT
    if (currentTab != NavTab.ASSISTANT) {
        BackHandler {
            currentTab = NavTab.ASSISTANT
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(NothingBlack),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            NothingTopBar(
                selectedTab = currentTab,
                onTabSelected = { currentTab = it },
                assistantState = assistantState,
                isServiceActive = EmilyVoiceService.isRunning || isBound
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding()
        ) {
            when (currentTab) {
                NavTab.ASSISTANT -> {
                    AssistantScreen(
                        state = assistantState,
                        audioRms = audioRms,
                        messages = messages,
                        onManualWake = { viewModel.manualWake(context) },
                        onSleepToggle = { viewModel.toggleSleep(context) },
                        onSendCommand = { command -> viewModel.sendCommand(context, command) },
                        onClearChat = { viewModel.clearChat() }
                    )
                }

                NavTab.PERMISSIONS -> {
                    PermissionsScreen(
                        onStartService = { viewModel.startService(context) }
                    )
                }

                NavTab.SETTINGS -> {
                    SettingsScreen(
                        currentSettings = settings,
                        onSettingsChanged = { updated -> viewModel.updateSettings(updated) }
                    )
                }
            }
        }
    }
}
