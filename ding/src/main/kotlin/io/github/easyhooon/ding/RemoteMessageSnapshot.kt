package io.github.easyhooon.ding

import com.google.firebase.messaging.RemoteMessage
import io.github.easyhooon.ding.core.DingSnapshotJson
import io.github.easyhooon.ding.core.RemoteMessageSnapshotInput
import io.github.easyhooon.ding.core.RemoteNotificationSnapshotInput
import org.json.JSONObject

internal object RemoteMessageSnapshot {
    fun from(
        remoteMessage: RemoteMessage,
        fcmToken: String?,
        receivedAtMillis: Long,
    ): JSONObject {
        val input = RemoteMessageSnapshotInput(
            messageId = remoteMessage.messageId,
            messageType = remoteMessage.messageType,
            from = remoteMessage.from,
            collapseKey = remoteMessage.collapseKey,
            sentTime = remoteMessage.sentTime,
            ttl = remoteMessage.ttl,
            priority = remoteMessage.priority,
            originalPriority = remoteMessage.originalPriority,
            data = remoteMessage.data,
            notification = remoteMessage.notification?.toSnapshotInput(),
        )
        return JSONObject(
            DingSnapshotJson.remoteMessage(
                input = input,
                fcmToken = fcmToken,
                receivedAtMillis = receivedAtMillis,
            ),
        )
    }

    private fun RemoteMessage.Notification.toSnapshotInput(): RemoteNotificationSnapshotInput =
        RemoteNotificationSnapshotInput(
            title = title,
            body = body,
            imageUrl = imageUrl?.toString(),
            channelId = channelId,
            clickAction = clickAction,
            color = color,
            icon = icon,
            link = link?.toString(),
            sound = sound,
            tag = tag,
        )
}
