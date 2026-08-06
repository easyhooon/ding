package io.github.easyhooon.notificationinspector.sample

import android.Manifest
import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.google.firebase.messaging.RemoteMessage
import io.github.easyhooon.notificationinspector.NotificationInspector

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermissionIfNeeded()
        setContentView(createContentView())
    }

    private fun createContentView(): LinearLayout {
        val padding = dp(20)
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding, padding, padding)

            addView(
                TextView(this@MainActivity).apply {
                    text = "Notification Inspector Sample"
                    textSize = 22f
                    typeface = Typeface.DEFAULT_BOLD
                },
            )

            addView(
                TextView(this@MainActivity).apply {
                    text = "Create a local notification, then open the inspector to view its captured payload."
                    textSize = 14f
                    setPadding(0, dp(8), 0, dp(16))
                },
            )

            addView(actionButton("Send Local Notification") { sendLocalNotification() })
            addView(actionButton("Capture Mock FCM Message") { captureMockRemoteMessage() })
            addView(
                actionButton("Enable Persistent Inspector") {
                    NotificationInspector.setPersistentNotificationEnabled(this@MainActivity, true)
                },
            )
            addView(
                actionButton("Disable Persistent Inspector") {
                    NotificationInspector.setPersistentNotificationEnabled(this@MainActivity, false)
                },
            )
            addView(actionButton("Open Inspector") { NotificationInspector.open(this@MainActivity) })
        }
    }

    private fun actionButton(text: String, onClick: () -> Unit): Button {
        return Button(this).apply {
            this.text = text
            setOnClickListener { onClick() }
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        }
    }

    private fun sendLocalNotification() {
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH),
        )

        val notificationId = System.currentTimeMillis().toInt()
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val title = "Sample notification"
        val body = "Captured by Notification Inspector"
        val notification = Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(Notification.BigTextStyle().bigText(body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(notificationId, notification)
        NotificationInspector.captureNotification(
            context = this,
            source = "sample",
            notificationId = notificationId,
            title = title,
            body = body,
            data = mapOf("thread-id" to "sample-thread"),
        )
    }

    private fun captureMockRemoteMessage() {
        val messageId = "sample-${System.currentTimeMillis()}"
        val remoteMessage = RemoteMessage.Builder("notification-inspector-sample@fcm.googleapis.com")
            .setMessageId(messageId)
            .setMessageType("sample")
            .addData("event", "manual-fcm-capture")
            .addData("message-id", messageId)
            .build()

        NotificationInspector.capture(this, remoteMessage)
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return
        }
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            return
        }

        requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_NOTIFICATIONS)
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private companion object {
        private const val CHANNEL_ID = "sample"
        private const val CHANNEL_NAME = "Sample Notifications"
        private const val REQUEST_NOTIFICATIONS = 100
    }
}
