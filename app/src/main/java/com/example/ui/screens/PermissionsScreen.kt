package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.service.EmilyAccessibilityService
import com.example.service.EmilyNotificationListener
import com.example.service.EmilyVoiceService
import com.example.ui.theme.NothingBlack
import com.example.ui.theme.NothingCardBg
import com.example.ui.theme.NothingCardBorder
import com.example.ui.theme.NothingGreyDark
import com.example.ui.theme.NothingGreyLight
import com.example.ui.theme.NothingGreyMuted
import com.example.ui.theme.NothingWhite
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning

@Composable
fun PermissionsScreen(
    onStartService: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    var hasNotifPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    var isAccessibilityEnabled by remember {
        mutableStateOf(EmilyAccessibilityService.isEnabled)
    }

    var isNotificationListenerEnabled by remember {
        mutableStateOf(EmilyNotificationListener.isEnabled)
    }

    var isBatteryExempt by remember {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        mutableStateOf(powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true)
    }

    val micLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
    }

    val notifLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotifPermission = granted
    }

    // Refresh state when returning to screen
    LaunchedEffect(Unit) {
        hasMicPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasNotifPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        }
        isAccessibilityEnabled = EmilyAccessibilityService.isEnabled
        isNotificationListenerEnabled = EmilyNotificationListener.isEnabled
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        isBatteryExempt = pm?.isIgnoringBatteryOptimizations(context.packageName) == true
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(NothingBlack)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "PERMISSIONS // SETUP CHECKLIST",
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = NothingWhite,
                letterSpacing = 1.2.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Required for hands-free \"Hey Emily\" wake word, Android 14 background listening, and phone automation on Samsung SM-A042F.",
                fontSize = 12.sp,
                fontFamily = FontFamily.SansSerif,
                color = NothingGreyMuted,
                lineHeight = 16.sp
            )
        }

        // 1. Microphone
        item {
            PermissionCard(
                title = "1. MICROPHONE (WAKE WORD & VOICE)",
                description = "Required to detect \"Hey Emily\" and convert your speech to commands.",
                isGranted = hasMicPermission,
                actionLabel = if (hasMicPermission) "GRANTED" else "GRANT MIC",
                onAction = {
                    micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            )
        }

        // 2. Notifications
        item {
            PermissionCard(
                title = "2. NOTIFICATIONS (ANDROID 14)",
                description = "Required for persistent foreground service notification with instant Stop button.",
                isGranted = hasNotifPermission,
                actionLabel = if (hasNotifPermission) "GRANTED" else "GRANT NOTIFS",
                onAction = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            )
        }

        // 3. Accessibility Service
        item {
            PermissionCard(
                title = "3. ACCESSIBILITY (UI AUTOMATION)",
                description = "Allows Emily to operate other apps, press Back/Home, scroll, click buttons, and read screen text.",
                isGranted = isAccessibilityEnabled,
                actionLabel = if (isAccessibilityEnabled) "ACTIVE" else "ENABLE SERVICE",
                onAction = {
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                }
            )
        }

        // 4. Notification Listener
        item {
            PermissionCard(
                title = "4. NOTIFICATION ACCESS (READ ALOUD)",
                description = "Allows Emily to read aloud incoming WhatsApp, SMS, and app notifications when requested.",
                isGranted = isNotificationListenerEnabled,
                actionLabel = if (isNotificationListenerEnabled) "GRANTED" else "ENABLE ACCESS",
                onAction = {
                    val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                }
            )
        }

        // 5. Battery Optimization Exemption
        item {
            PermissionCard(
                title = "5. BATTERY OPTIMIZATION (SAMSUNG A042F)",
                description = "Samsung One UI aggressively kills background apps. Exempting Emily ensures wake-word works when screen is locked or idle.",
                isGranted = isBatteryExempt,
                actionLabel = if (isBatteryExempt) "EXEMPTED" else "EXEMPT BATTERY",
                onAction = {
                    try {
                        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = Uri.parse("package:${context.packageName}")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        val fallback = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(fallback)
                    }
                }
            )
        }

        // 6. Default Digital Assistant Role
        item {
            PermissionCard(
                title = "6. DEFAULT DIGITAL ASSISTANT",
                description = "Set Emily as your default assistant app to trigger her from hardware home button / gesture swipe.",
                isGranted = false,
                actionLabel = "CHOOSE ASSISTANT",
                onAction = {
                    try {
                        val intent = Intent(Settings.ACTION_VOICE_INPUT_SETTINGS).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        val fallback = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(fallback)
                    }
                }
            )
        }

        // 7. Background Service Activator
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(NothingWhite)
                    .clickable { onStartService() }
                    .padding(vertical = 14.dp)
                    .testTag("activate_background_service_button"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (EmilyVoiceService.isRunning) "SERVICE RUNNING (RESTART LISTENER)" else "START BACKGROUND LISTENER",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = NothingBlack,
                    letterSpacing = 1.sp
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun PermissionCard(
    title: String,
    description: String,
    isGranted: Boolean,
    actionLabel: String,
    onAction: () -> Unit
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = NothingWhite,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.weight(1f)
                )

                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isGranted) StatusSuccess else StatusWarning)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = description,
                fontSize = 12.sp,
                fontFamily = FontFamily.SansSerif,
                color = NothingGreyLight,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isGranted) NothingGreyDark else NothingWhite)
                        .clickable { onAction() }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = actionLabel,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = if (isGranted) NothingWhite else NothingBlack,
                        letterSpacing = 0.8.sp
                    )
                }
            }
        }
    }
}
