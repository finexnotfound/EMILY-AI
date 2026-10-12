package com.example.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class EmilyNotificationListener : NotificationListenerService() {

    companion object {
        var instance: EmilyNotificationListener? = null
            private set

        val isEnabled: Boolean
            get() = instance != null
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        if (instance == this) {
            instance = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
    }

    fun getActiveNotificationsSummary(): String {
        return try {
            val active = activeNotifications ?: return ""
            if (active.isEmpty()) return ""

            val builder = StringBuilder()
            val filtered = active.filter { sbn ->
                // Filter out Emily's own persistent foreground notification and system silent icons
                sbn.packageName != packageName && !sbn.isOngoing
            }.takeLast(5)

            if (filtered.isEmpty()) {
                return "You have no new personal messages or alerts."
            }

            builder.append("You have ${filtered.size} recent notifications: ")
            for ((index, sbn) in filtered.withIndex()) {
                val extras = sbn.notification.extras
                val title = extras.getCharSequence("android.title")?.toString() ?: "Alert"
                val text = extras.getCharSequence("android.text")?.toString() ?: ""
                val appLabel = getAppLabel(sbn)
                builder.append("From $appLabel, $title: $text. ")
            }
            builder.toString().trim()
        } catch (e: Exception) {
            "Could not read notifications: ${e.message}"
        }
    }

    private fun getAppLabel(sbn: StatusBarNotification): String {
        return try {
            val appInfo = packageManager.getApplicationInfo(sbn.packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            sbn.packageName.substringAfterLast('.')
        }
    }
}
