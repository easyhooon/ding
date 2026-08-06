package io.github.easyhooon.ding.sample

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
import android.widget.Toast
import com.google.firebase.messaging.RemoteMessage
import io.github.easyhooon.ding.Ding

class MainActivity : Activity() {
    private var sampleFcmTokenVersion = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sampleFcmTokenVersion = savedInstanceState?.getInt(SAMPLE_FCM_TOKEN_VERSION_KEY, 1) ?: 1
        requestNotificationPermissionIfNeeded()
        updateSampleFcmToken()
        setContentView(createContentView())
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(SAMPLE_FCM_TOKEN_VERSION_KEY, sampleFcmTokenVersion)
        super.onSaveInstanceState(outState)
    }

    private fun createContentView(): LinearLayout {
        val padding = dp(20)
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding, padding, padding)

            addView(
                TextView(this@MainActivity).apply {
                    text = "Ding Sample"
                    textSize = 22f
                    typeface = Typeface.DEFAULT_BOLD
                },
            )

            addView(
                TextView(this@MainActivity).apply {
                    text = "Long-press the app icon or tap the Ding notification to open captured payloads."
                    textSize = 14f
                    setPadding(0, dp(8), 0, dp(16))
                },
            )

            addView(actionButton("Send Local Notification") { sendLocalNotification() })
            addView(actionButton("Add Demo FCM Notifications") { addDemoFcmNotifications() })
            addView(actionButton("Rotate Sample FCM Token") { rotateSampleFcmToken() })
            addView(
                actionButton("Enable Ding notification") {
                    Ding.setPersistentNotificationEnabled(this@MainActivity, true)
                },
            )
            addView(
                actionButton("Disable Ding notification") {
                    Ding.setPersistentNotificationEnabled(this@MainActivity, false)
                },
            )
            addView(actionButton("Open Ding") { Ding.open(this@MainActivity) })
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
        val body = "Captured by Ding"
        val notification = Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(Notification.BigTextStyle().bigText(body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(notificationId, notification)
        Ding.captureNotification(
            context = this,
            source = "sample",
            notificationId = notificationId,
            title = title,
            body = body,
            data = mapOf("thread-id" to "sample-thread"),
        )
    }

    private fun addDemoFcmNotifications() {
        val batchId = System.currentTimeMillis()
        DEMO_FCM_MESSAGES.forEachIndexed { index, message ->
            val messageId = "demo-${batchId + index}"
            val remoteMessage = RemoteMessage.Builder("app-instance@fcm.googleapis.com")
                .setMessageId(messageId)
                .addData("title", message.title)
                .addData("body", message.body)
                .addData("category", message.category)
                .addData("event", message.event)
                .addData("message-id", messageId)
                .apply {
                    message.metadata.forEach { (key, value) -> addData(key, value) }
                }
                .build()

            Ding.capture(this, remoteMessage)
        }
        Toast.makeText(this, "Demo FCM notifications added", Toast.LENGTH_SHORT).show()
    }

    private fun rotateSampleFcmToken() {
        sampleFcmTokenVersion++
        updateSampleFcmToken()
        Toast.makeText(this, "Sample FCM token rotated", Toast.LENGTH_SHORT).show()
    }

    private fun updateSampleFcmToken() {
        Ding.updateFcmToken(
            context = this,
            fcmToken = "$SAMPLE_FCM_TOKEN_PREFIX-v$sampleFcmTokenVersion",
        )
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
        private const val SAMPLE_FCM_TOKEN_VERSION_KEY = "sample_fcm_token_version"
        private const val SAMPLE_FCM_TOKEN_PREFIX =
            "sample-fcm-registration-token:APA91bG_ding_debug_only"
        private val DEMO_FCM_MESSAGES = listOf(
            DemoFcmMessage(
                title = "Weekly activity summary",
                body = "You completed 12 tasks and stayed focused for 6h 20m.",
                category = "insights",
                event = "weekly_summary_ready",
                metadata = mapOf("week" to "2026-W32"),
            ),
            DemoFcmMessage(
                title = "Payment completed",
                body = "Your payment of \$24.90 was processed successfully.",
                category = "billing",
                event = "payment_completed",
                metadata = mapOf("order-id" to "D-2048"),
            ),
            DemoFcmMessage(
                title = "Reservation confirmed",
                body = "Your table for two is booked for Friday at 7:30 PM.",
                category = "reservations",
                event = "reservation_confirmed",
                metadata = mapOf("reservation-id" to "RSV-7319"),
            ),
            DemoFcmMessage(
                title = "New message from Mina",
                body = "Are we still on for dinner at 7?",
                category = "messages",
                event = "message_received",
                metadata = mapOf("conversation-id" to "C-1842"),
            ),
            DemoFcmMessage(
                title = "Order shipped",
                body = "Order #D-2048 is on the way and will arrive tomorrow.",
                category = "delivery",
                event = "order_shipped",
                metadata = mapOf("tracking-id" to "TRK-583104"),
            ),
        )
    }
}

private data class DemoFcmMessage(
    val title: String,
    val body: String,
    val category: String,
    val event: String,
    val metadata: Map<String, String>,
)
