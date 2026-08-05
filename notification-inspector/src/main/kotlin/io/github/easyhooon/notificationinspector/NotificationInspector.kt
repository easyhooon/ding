package io.github.easyhooon.notificationinspector

import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.firebase.messaging.RemoteMessage

object NotificationInspector {
    private const val TAG = "NotificationInspector"

    fun capture(context: Context, remoteMessage: RemoteMessage) {
        val snapshot = RemoteMessageSnapshot.from(
            remoteMessage = remoteMessage,
            receivedAtMillis = System.currentTimeMillis(),
        )
        NotificationInspectorStore(context).appendAsync(snapshot)
        Log.d(TAG, snapshot.toString(2))
    }

    fun captureNotification(
        context: Context,
        source: String,
        notificationId: Int,
        title: String,
        body: String,
        data: Map<String, String> = emptyMap(),
    ) {
        val snapshot = LocalNotificationSnapshot.from(
            source = source,
            notificationId = notificationId,
            title = title,
            body = body,
            data = data,
            receivedAtMillis = System.currentTimeMillis(),
        )
        NotificationInspectorStore(context).appendAsync(snapshot)
        Log.d(TAG, snapshot.toString(2))
    }

    fun open(context: Context) {
        val intent = Intent(context, NotificationInspectorActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
