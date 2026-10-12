package com.example.phonecontrol

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.provider.AlarmClock
import android.provider.Settings
import android.view.KeyEvent
import com.example.data.model.ActionExecutionDetail
import com.example.service.EmilyAccessibilityService
import com.example.service.EmilyNotificationListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class PhoneActionExecutor(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager

    suspend fun executeAction(name: String, args: JSONObject): ActionExecutionDetail = withContext(Dispatchers.Main) {
        try {
            when (name) {
                "open_app" -> {
                    val appName = args.optString("app_name", "").trim()
                    openApp(appName)
                }
                "set_alarm" -> {
                    val hour = args.optInt("hour", 7)
                    val minute = args.optInt("minute", 0)
                    val label = args.optString("label", "Emily Alarm")
                    setAlarm(hour, minute, label)
                }
                "set_timer" -> {
                    val seconds = args.optInt("seconds", 300)
                    val label = args.optString("label", "Emily Timer")
                    setTimer(seconds, label)
                }
                "search_web" -> {
                    val query = args.optString("query", "").trim()
                    searchWeb(query)
                }
                "open_url" -> {
                    val url = args.optString("url", "").trim()
                    openUrl(url)
                }
                "start_navigation" -> {
                    val destination = args.optString("destination", "").trim()
                    startNavigation(destination)
                }
                "toggle_flashlight" -> {
                    val enable = args.optBoolean("enable", true)
                    toggleFlashlight(enable)
                }
                "adjust_volume" -> {
                    val direction = args.optString("direction", "raise").lowercase()
                    adjustVolume(direction)
                }
                "open_settings" -> {
                    val settingType = args.optString("setting_type", "general").lowercase()
                    openSettings(settingType)
                }
                "media_control" -> {
                    val command = args.optString("command", "toggle").lowercase()
                    controlMedia(command)
                }
                "system_ui_action" -> {
                    val action = args.optString("action", "back").lowercase()
                    performSystemAction(action)
                }
                "read_screen" -> {
                    readScreen()
                }
                "click_element" -> {
                    val targetText = args.optString("text", "")
                    clickElement(targetText)
                }
                "scroll_screen" -> {
                    val direction = args.optString("direction", "down")
                    scrollScreen(direction)
                }
                "read_notifications" -> {
                    readNotifications()
                }
                else -> {
                    ActionExecutionDetail(
                        actionName = name,
                        parameters = args.toString(),
                        success = false,
                        message = "Unknown action: $name"
                    )
                }
            }
        } catch (e: Exception) {
            ActionExecutionDetail(
                actionName = name,
                parameters = args.toString(),
                success = false,
                message = "Failed to execute: ${e.message}"
            )
        }
    }

    private fun openApp(appName: String): ActionExecutionDetail {
        if (appName.isBlank()) {
            return ActionExecutionDetail("open_app", "", false, "No app name specified.")
        }

        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = pm.queryIntentActivities(mainIntent, 0)

        // Exact match then fuzzy match
        var targetPkg: String? = null
        var foundLabel: String = ""

        val cleanedTarget = appName.lowercase().trim()
        for (info in resolveInfos) {
            val label = info.loadLabel(pm).toString()
            val pkg = info.activityInfo.packageName
            if (label.equals(cleanedTarget, ignoreCase = true) || pkg.equals(cleanedTarget, ignoreCase = true)) {
                targetPkg = pkg
                foundLabel = label
                break
            }
        }

        if (targetPkg == null) {
            for (info in resolveInfos) {
                val label = info.loadLabel(pm).toString()
                val pkg = info.activityInfo.packageName
                if (label.lowercase().contains(cleanedTarget) || pkg.lowercase().contains(cleanedTarget)) {
                    targetPkg = pkg
                    foundLabel = label
                    break
                }
            }
        }

        // Special aliases mapping (YouTube, WhatsApp, Chrome, Camera, Settings, etc.)
        if (targetPkg == null) {
            when {
                cleanedTarget.contains("youtube") -> targetPkg = "com.google.android.youtube"
                cleanedTarget.contains("whatsapp") -> targetPkg = "com.whatsapp"
                cleanedTarget.contains("chrome") -> targetPkg = "com.android.chrome"
                cleanedTarget.contains("map") -> targetPkg = "com.google.android.apps.maps"
                cleanedTarget.contains("camera") -> {
                    val cameraIntent = Intent("android.media.action.IMAGE_CAPTURE").apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    if (cameraIntent.resolveActivity(pm) != null) {
                        context.startActivity(cameraIntent)
                        return ActionExecutionDetail("open_app", appName, true, "Opened Camera.")
                    }
                }
                cleanedTarget.contains("setting") -> {
                    return openSettings("general")
                }
            }
        }

        return if (targetPkg != null) {
            val launchIntent = pm.getLaunchIntentForPackage(targetPkg)?.apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            if (launchIntent != null) {
                context.startActivity(launchIntent)
                ActionExecutionDetail("open_app", appName, true, "Opened $foundLabel.")
            } else {
                ActionExecutionDetail("open_app", appName, false, "Could not launch package: $targetPkg")
            }
        } else {
            ActionExecutionDetail("open_app", appName, false, "App '$appName' not found on device.")
        }
    }

    private fun setAlarm(hour: Int, minute: Int, label: String): ActionExecutionDetail {
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minute)
            putExtra(AlarmClock.EXTRA_MESSAGE, label)
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return try {
            context.startActivity(intent)
            val timeStr = String.format("%02d:%02d", hour, minute)
            ActionExecutionDetail("set_alarm", "$timeStr ($label)", true, "Set alarm for $timeStr.")
        } catch (e: Exception) {
            ActionExecutionDetail("set_alarm", "$hour:$minute", false, "Alarm app not available: ${e.message}")
        }
    }

    private fun setTimer(seconds: Int, label: String): ActionExecutionDetail {
        val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
            putExtra(AlarmClock.EXTRA_LENGTH, seconds)
            putExtra(AlarmClock.EXTRA_MESSAGE, label)
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return try {
            context.startActivity(intent)
            ActionExecutionDetail("set_timer", "${seconds}s", true, "Set timer for $seconds seconds.")
        } catch (e: Exception) {
            ActionExecutionDetail("set_timer", "${seconds}s", false, "Timer app not available: ${e.message}")
        }
    }

    private fun searchWeb(query: String): ActionExecutionDetail {
        if (query.isBlank()) return ActionExecutionDetail("search_web", "", false, "No search query provided.")
        val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
            putExtra(SearchManager.QUERY, query)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return try {
            context.startActivity(intent)
            ActionExecutionDetail("search_web", query, true, "Searching web for '$query'.")
        } catch (e: Exception) {
            // Fallback to browser URL
            openUrl("https://www.google.com/search?q=" + Uri.encode(query))
        }
    }

    private fun openUrl(url: String): ActionExecutionDetail {
        val fullUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) "https://$url" else url
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(fullUrl)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return try {
            context.startActivity(intent)
            ActionExecutionDetail("open_url", fullUrl, true, "Opened $fullUrl.")
        } catch (e: Exception) {
            ActionExecutionDetail("open_url", fullUrl, false, "Could not open browser: ${e.message}")
        }
    }

    private fun startNavigation(destination: String): ActionExecutionDetail {
        val gmmIntentUri = Uri.parse("google.navigation:q=" + Uri.encode(destination))
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
            setPackage("com.google.android.apps.maps")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return try {
            context.startActivity(mapIntent)
            ActionExecutionDetail("start_navigation", destination, true, "Starting navigation to $destination.")
        } catch (e: Exception) {
            // Fallback geo intent
            val fallbackUri = Uri.parse("geo:0,0?q=" + Uri.encode(destination))
            val fallbackIntent = Intent(Intent.ACTION_VIEW, fallbackUri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            try {
                context.startActivity(fallbackIntent)
                ActionExecutionDetail("start_navigation", destination, true, "Navigating to $destination.")
            } catch (err: Exception) {
                ActionExecutionDetail("start_navigation", destination, false, "Maps app not available: ${err.message}")
            }
        }
    }

    private fun toggleFlashlight(enable: Boolean): ActionExecutionDetail {
        val cm = cameraManager ?: return ActionExecutionDetail("toggle_flashlight", "$enable", false, "Camera service unavailable.")
        return try {
            val cameraId = cm.cameraIdList.firstOrNull { id ->
                val chars = cm.getCameraCharacteristics(id)
                chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
            if (cameraId != null) {
                cm.setTorchMode(cameraId, enable)
                val status = if (enable) "ON" else "OFF"
                ActionExecutionDetail("toggle_flashlight", "$enable", true, "Turned flashlight $status.")
            } else {
                ActionExecutionDetail("toggle_flashlight", "$enable", false, "No flash hardware detected.")
            }
        } catch (e: Exception) {
            ActionExecutionDetail("toggle_flashlight", "$enable", false, "Flashlight error: ${e.message}")
        }
    }

    private fun adjustVolume(direction: String): ActionExecutionDetail {
        val am = audioManager ?: return ActionExecutionDetail("adjust_volume", direction, false, "Audio service unavailable.")
        val adjustDir = when (direction) {
            "raise", "up", "increase" -> AudioManager.ADJUST_RAISE
            "lower", "down", "decrease" -> AudioManager.ADJUST_LOWER
            "mute" -> AudioManager.ADJUST_MUTE
            "unmute" -> AudioManager.ADJUST_UNMUTE
            else -> AudioManager.ADJUST_RAISE
        }
        am.adjustStreamVolume(AudioManager.STREAM_MUSIC, adjustDir, AudioManager.FLAG_SHOW_UI)
        return ActionExecutionDetail("adjust_volume", direction, true, "Adjusted media volume ($direction).")
    }

    private fun openSettings(settingType: String): ActionExecutionDetail {
        val action = when (settingType) {
            "wifi" -> Settings.ACTION_WIFI_SETTINGS
            "bluetooth" -> Settings.ACTION_BLUETOOTH_SETTINGS
            "display", "brightness" -> Settings.ACTION_DISPLAY_SETTINGS
            "sound", "volume" -> Settings.ACTION_SOUND_SETTINGS
            "apps" -> Settings.ACTION_APPLICATION_SETTINGS
            "accessibility" -> Settings.ACTION_ACCESSIBILITY_SETTINGS
            "battery" -> Settings.ACTION_BATTERY_SAVER_SETTINGS
            else -> Settings.ACTION_SETTINGS
        }
        val intent = Intent(action).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
        return try {
            context.startActivity(intent)
            ActionExecutionDetail("open_settings", settingType, true, "Opened $settingType settings.")
        } catch (e: Exception) {
            ActionExecutionDetail("open_settings", settingType, false, "Could not open settings: ${e.message}")
        }
    }

    private fun controlMedia(command: String): ActionExecutionDetail {
        val keyCode = when (command) {
            "play" -> KeyEvent.KEYCODE_MEDIA_PLAY
            "pause" -> KeyEvent.KEYCODE_MEDIA_PAUSE
            "next", "skip" -> KeyEvent.KEYCODE_MEDIA_NEXT
            "previous", "prev" -> KeyEvent.KEYCODE_MEDIA_PREVIOUS
            else -> KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
        }
        audioManager?.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
        audioManager?.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
        return ActionExecutionDetail("media_control", command, true, "Sent media $command signal.")
    }

    private fun performSystemAction(action: String): ActionExecutionDetail {
        val service = EmilyAccessibilityService.instance
            ?: return ActionExecutionDetail("system_ui_action", action, false, "Accessibility service not active. Please enable in Settings.")
        val success = when (action) {
            "back" -> service.goBack()
            "home" -> service.goHome()
            "recents" -> service.openRecents()
            "notifications" -> service.openNotifications()
            else -> false
        }
        return ActionExecutionDetail("system_ui_action", action, success, if (success) "Executed system $action." else "Failed system $action.")
    }

    private fun readScreen(): ActionExecutionDetail {
        val service = EmilyAccessibilityService.instance
            ?: return ActionExecutionDetail("read_screen", "", false, "Accessibility service not enabled. Please enable it in Settings.")
        val content = service.dumpScreenText()
        return if (content.isNotBlank()) {
            ActionExecutionDetail("read_screen", "", true, "Screen text: $content")
        } else {
            ActionExecutionDetail("read_screen", "", false, "No text found on active screen.")
        }
    }

    private fun clickElement(targetText: String): ActionExecutionDetail {
        val service = EmilyAccessibilityService.instance
            ?: return ActionExecutionDetail("click_element", targetText, false, "Accessibility service not enabled.")
        val clicked = service.clickElementWithText(targetText)
        return ActionExecutionDetail("click_element", targetText, clicked, if (clicked) "Tapped '$targetText'." else "Could not find clickable element '$targetText'.")
    }

    private fun scrollScreen(direction: String): ActionExecutionDetail {
        val service = EmilyAccessibilityService.instance
            ?: return ActionExecutionDetail("scroll_screen", direction, false, "Accessibility service not enabled.")
        val scrolled = if (direction.contains("up")) service.scrollBackward() else service.scrollForward()
        return ActionExecutionDetail("scroll_screen", direction, scrolled, if (scrolled) "Scrolled screen $direction." else "Scroll not supported on this view.")
    }

    private fun readNotifications(): ActionExecutionDetail {
        val listener = EmilyNotificationListener.instance
            ?: return ActionExecutionDetail("read_notifications", "", false, "Notification Access not granted. Please enable in Settings.")
        val summary = listener.getActiveNotificationsSummary()
        return if (summary.isNotBlank()) {
            ActionExecutionDetail("read_notifications", "", true, summary)
        } else {
            ActionExecutionDetail("read_notifications", "", true, "You have no active unread notifications.")
        }
    }
}
