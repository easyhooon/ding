package io.github.easyhooon.notificationinspector

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject

internal object PersistentNotificationController {
    private const val TAG = "NotificationInspector"
    private const val CHANNEL_ID = "notification_inspector_debug"
    private const val NOTIFICATION_ID = 0x4E49
    private const val OPEN_REQUEST_CODE = 0x4E49
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val stateMutex = Mutex()

    fun setEnabled(context: Context, enabled: Boolean) {
        val appContext = context.applicationContext
        val store = NotificationInspectorStore(appContext)
        scope.launch {
            stateMutex.withLock {
                store.setPersistentNotificationEnabled(enabled)
                if (enabled) {
                    show(appContext, store.readAll())
                } else {
                    notificationManager(appContext).cancel(NOTIFICATION_ID)
                }
            }
        }
    }

    fun refreshAsync(context: Context, store: NotificationInspectorStore) {
        val appContext = context.applicationContext
        scope.launch {
            refreshIfEnabled(appContext, store)
        }
    }

    suspend fun refreshIfEnabled(context: Context, store: NotificationInspectorStore) {
        stateMutex.withLock {
            if (store.isPersistentNotificationEnabled()) {
                show(context.applicationContext, store.readAll())
            }
        }
    }

    private fun show(context: Context, snapshots: List<JSONObject>) {
        val notificationManager = notificationManager(context)
        createChannel(context, notificationManager)

        if (!notificationManager.areNotificationsEnabled() || !hasPostNotificationsPermission(context)) {
            Log.w(TAG, "Persistent notification enabled, but notification permission is unavailable")
            return
        }

        val openIntent = Intent(context, NotificationInspectorActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            OPEN_REQUEST_CODE,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_inspector)
            .setContentTitle(context.getString(R.string.notification_inspector_persistent_title))
            .setContentText(summary(context, snapshots))
            .setContentIntent(openPendingIntent)
            .setCategory(Notification.CATEGORY_STATUS)
            .setVisibility(Notification.VISIBILITY_PRIVATE)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createChannel(context: Context, notificationManager: NotificationManager) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_inspector_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.notification_inspector_channel_description)
            setShowBadge(false)
            enableVibration(false)
            setSound(null, null)
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun summary(context: Context, snapshots: List<JSONObject>): String {
        if (snapshots.isEmpty()) {
            return context.getString(R.string.notification_inspector_persistent_empty)
        }

        val latestTag = snapshots.last().optString("tag")
        val latestLabel = NotificationFilterTag.entries
            .firstOrNull { it.jsonValue == latestTag }
            ?.label
            ?: context.getString(R.string.notification_inspector_other_category)
        return context.resources.getQuantityString(
            R.plurals.notification_inspector_persistent_summary,
            snapshots.size,
            snapshots.size,
            latestLabel,
        )
    }

    private fun hasPostNotificationsPermission(context: Context): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }

    private fun notificationManager(context: Context): NotificationManager {
        return context.getSystemService(NotificationManager::class.java)
    }
}
