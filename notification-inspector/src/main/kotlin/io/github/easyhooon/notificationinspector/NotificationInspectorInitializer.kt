package io.github.easyhooon.notificationinspector

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.util.Log
import androidx.startup.Initializer

internal class NotificationInspectorInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        LauncherShortcutController.register(context)
        PersistentNotificationController.initialize(context)
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}

private object LauncherShortcutController {
    private const val TAG = "NotificationInspector"
    private const val SHORTCUT_ID = "open_notification_inspector"

    fun register(context: Context) {
        val shortcutManager = context.getSystemService(ShortcutManager::class.java) ?: return
        val openIntent = Intent(context, NotificationInspectorActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val shortcut = ShortcutInfo.Builder(context, SHORTCUT_ID)
            .setShortLabel(context.getString(R.string.notification_inspector_shortcut_label))
            .setLongLabel(context.getString(R.string.notification_inspector_shortcut_long_label))
            .setIcon(Icon.createWithResource(context, R.drawable.ic_notification_inspector))
            .setIntent(openIntent)
            .build()

        runCatching {
            shortcutManager.addDynamicShortcuts(listOf(shortcut))
        }.onFailure { error ->
            Log.w(TAG, "Failed to register launcher shortcut", error)
        }
    }
}
