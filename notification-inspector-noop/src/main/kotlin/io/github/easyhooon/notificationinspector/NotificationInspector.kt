@file:Suppress("UnusedParameter")

package io.github.easyhooon.notificationinspector

import android.content.Context
import com.google.firebase.messaging.RemoteMessage

object NotificationInspector {
    fun capture(context: Context, remoteMessage: RemoteMessage) = Unit

    fun capture(
        context: Context,
        remoteMessage: RemoteMessage,
        fcmToken: String,
    ) = Unit

    fun updateFcmToken(context: Context, fcmToken: String) = Unit

    fun captureNotification(
        context: Context,
        source: String,
        notificationId: Int,
        title: String,
        body: String,
        data: Map<String, String> = emptyMap(),
    ) = Unit

    fun setPersistentNotificationEnabled(context: Context, enabled: Boolean) = Unit

    fun open(context: Context) = Unit
}
