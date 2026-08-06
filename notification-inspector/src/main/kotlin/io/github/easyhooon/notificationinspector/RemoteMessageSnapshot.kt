package io.github.easyhooon.notificationinspector

import com.google.firebase.messaging.RemoteMessage
import org.json.JSONObject

internal object RemoteMessageSnapshot {
    fun from(
        remoteMessage: RemoteMessage,
        fcmToken: String?,
        receivedAtMillis: Long,
    ): JSONObject {
        return JSONObject().apply {
            put("type", "remote-message")
            put("source", "fcm")
            put("tag", NotificationFilterTag.FCM.jsonValue)
            put("receivedAtMillis", receivedAtMillis)
            putNullable("fcmToken", fcmToken)
            putNullable("messageId", remoteMessage.messageId)
            putNullable("messageType", remoteMessage.messageType)
            putNullable("from", remoteMessage.from)
            putNullable("collapseKey", remoteMessage.collapseKey)
            put("sentTime", remoteMessage.sentTime)
            put("ttl", remoteMessage.ttl)
            put("priority", remoteMessage.priority)
            put("originalPriority", remoteMessage.originalPriority)
            put("data", dataJson(remoteMessage.data))
            put("notification", notificationJson(remoteMessage.notification))
        }
    }

    private fun dataJson(data: Map<String, String>): JSONObject {
        return JSONObject().apply {
            data.toSortedMap().forEach { (key, value) ->
                put(key, value)
            }
        }
    }

    private fun notificationJson(notification: RemoteMessage.Notification?): Any {
        notification ?: return JSONObject.NULL

        return JSONObject().apply {
            putNullable("title", notification.title)
            putNullable("body", notification.body)
            putNullable("imageUrl", notification.imageUrl?.toString())
            putNullable("channelId", notification.channelId)
            putNullable("clickAction", notification.clickAction)
            putNullable("color", notification.color)
            putNullable("icon", notification.icon)
            putNullable("link", notification.link?.toString())
            putNullable("sound", notification.sound)
            putNullable("tag", notification.tag)
        }
    }

    private fun JSONObject.putNullable(name: String, value: Any?) {
        put(name, value ?: JSONObject.NULL)
    }
}
