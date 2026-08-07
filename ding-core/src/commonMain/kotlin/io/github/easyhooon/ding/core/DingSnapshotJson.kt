package io.github.easyhooon.ding.core

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

public data class RemoteMessageSnapshotInput(
    public val messageId: String?,
    public val messageType: String?,
    public val from: String?,
    public val collapseKey: String?,
    public val sentTime: Long,
    public val ttl: Int,
    public val priority: Int,
    public val originalPriority: Int,
    public val data: Map<String, String>,
    public val notification: RemoteNotificationSnapshotInput?,
)

public data class RemoteNotificationSnapshotInput(
    public val title: String?,
    public val body: String?,
    public val imageUrl: String?,
    public val channelId: String?,
    public val clickAction: String?,
    public val color: String?,
    public val icon: String?,
    public val link: String?,
    public val sound: String?,
    public val tag: String?,
)

public data class LocalNotificationSnapshotInput(
    public val source: String,
    public val notificationId: Int,
    public val title: String,
    public val body: String,
    public val data: Map<String, String>,
)

public object DingSnapshotJson {
    public fun remoteMessage(
        input: RemoteMessageSnapshotInput,
        fcmToken: String?,
        receivedAtMillis: Long,
    ): String {
        val deliveredPayload = remoteMessagePayload(input)
        val snapshot = linkedMapOf<String, JsonElement>(
            "type" to JsonPrimitive("remote-message"),
            "source" to JsonPrimitive("fcm"),
            "tag" to JsonPrimitive("fcm"),
            "platform" to JsonPrimitive("android"),
            "transport" to JsonPrimitive("fcm"),
            "capturePoint" to JsonPrimitive("host-callback"),
            "receivedAtMillis" to JsonPrimitive(receivedAtMillis),
            "fcmToken" to fcmToken.asJsonElement(),
        )
        snapshot.putAll(deliveredPayload)
        snapshot["rawDeliveredPayload"] = JsonObject(deliveredPayload)
        return JsonObject(snapshot).toString()
    }

    public fun localNotification(
        input: LocalNotificationSnapshotInput,
        receivedAtMillis: Long,
    ): String {
        val data = input.data.toSortedJsonObject()
        val notification = JsonObject(
            linkedMapOf(
                "notificationId" to JsonPrimitive(input.notificationId),
                "title" to JsonPrimitive(input.title),
                "body" to JsonPrimitive(input.body),
            ),
        )
        val deliveredPayload = linkedMapOf<String, JsonElement>(
            "notificationId" to JsonPrimitive(input.notificationId),
            "title" to JsonPrimitive(input.title),
            "body" to JsonPrimitive(input.body),
            "data" to data,
        )
        return JsonObject(
            linkedMapOf(
                "type" to JsonPrimitive("local-notification"),
                "source" to JsonPrimitive(input.source),
                "tag" to JsonPrimitive("local"),
                "platform" to JsonPrimitive("android"),
                "transport" to JsonPrimitive("local"),
                "capturePoint" to JsonPrimitive("host-api"),
                "receivedAtMillis" to JsonPrimitive(receivedAtMillis),
                "notificationId" to JsonPrimitive(input.notificationId),
                "title" to JsonPrimitive(input.title),
                "body" to JsonPrimitive(input.body),
                "data" to data,
                "notification" to notification,
                "rawDeliveredPayload" to JsonObject(deliveredPayload),
            ),
        ).toString()
    }

    private fun remoteMessagePayload(input: RemoteMessageSnapshotInput): Map<String, JsonElement> =
        linkedMapOf(
            "messageId" to input.messageId.asJsonElement(),
            "messageType" to input.messageType.asJsonElement(),
            "from" to input.from.asJsonElement(),
            "collapseKey" to input.collapseKey.asJsonElement(),
            "sentTime" to JsonPrimitive(input.sentTime),
            "ttl" to JsonPrimitive(input.ttl),
            "priority" to JsonPrimitive(input.priority),
            "originalPriority" to JsonPrimitive(input.originalPriority),
            "data" to input.data.toSortedJsonObject(),
            "notification" to input.notification.asJsonElement(),
        )

    private fun RemoteNotificationSnapshotInput?.asJsonElement(): JsonElement {
        this ?: return JsonNull
        return JsonObject(
            linkedMapOf(
                "title" to title.asJsonElement(),
                "body" to body.asJsonElement(),
                "imageUrl" to imageUrl.asJsonElement(),
                "channelId" to channelId.asJsonElement(),
                "clickAction" to clickAction.asJsonElement(),
                "color" to color.asJsonElement(),
                "icon" to icon.asJsonElement(),
                "link" to link.asJsonElement(),
                "sound" to sound.asJsonElement(),
                "tag" to tag.asJsonElement(),
            ),
        )
    }

    private fun String?.asJsonElement(): JsonElement =
        this?.let(::JsonPrimitive) ?: JsonNull

    private fun Map<String, String>.toSortedJsonObject(): JsonObject =
        JsonObject(
            linkedMapOf<String, JsonElement>().apply {
                this@toSortedJsonObject.entries
                    .sortedBy { it.key }
                    .forEach { (key, value) -> put(key, JsonPrimitive(value)) }
            },
        )
}
