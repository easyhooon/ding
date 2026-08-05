@file:Suppress("UnusedParameter")

package io.github.easyhooon.notificationinspector

import android.content.Context
import com.google.firebase.messaging.RemoteMessage

object NotificationInspector {
    fun capture(context: Context, remoteMessage: RemoteMessage) = Unit

    fun captureNotification(
        context: Context,
        source: String,
        notificationId: Int,
        title: String,
        body: String,
        data: Map<String, String> = emptyMap(),
    ) = Unit

    fun open(context: Context) = Unit
}
