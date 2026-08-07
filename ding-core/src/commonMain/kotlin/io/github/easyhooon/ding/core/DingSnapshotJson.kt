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
            "source" to JsonPrimitive(PushTransport.FCM.jsonValue),
            "tag" to JsonPrimitive(PushTransport.FCM.jsonValue),
            "platform" to JsonPrimitive(PushPlatform.ANDROID.jsonValue),
            "transport" to JsonPrimitive(PushTransport.FCM.jsonValue),
            "capturePoint" to JsonPrimitive(CapturePoint.HOST_CALLBACK.jsonValue),
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
                "tag" to JsonPrimitive(PushTransport.LOCAL.jsonValue),
                "platform" to JsonPrimitive(PushPlatform.ANDROID.jsonValue),
                "transport" to JsonPrimitive(PushTransport.LOCAL.jsonValue),
                "capturePoint" to JsonPrimitive(CapturePoint.HOST_API.jsonValue),
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

    public fun applePush(
        input: ApplePushSnapshotInput,
        receivedAtMillis: Long,
    ): String {
        val rawPayload = DingJsonValueNormalizer.normalizeObject(input.userInfo)
        val aps = rawPayload["aps"] as? JsonObject
        val alert = aps?.get("alert")
        val alertObject = alert as? JsonObject
        val nestedData = rawPayload["data"] as? JsonObject
        val title = firstNonBlank(
            alertObject.stringValue("title"),
            rawPayload.stringValue("title"),
            nestedData.stringValue("title"),
        )
        val body = firstNonBlank(
            alertObject.stringValue("body"),
            alert.stringValue(),
            rawPayload.stringValue("body"),
            nestedData.stringValue("body"),
        )
        val data = JsonObject(rawPayload.filterKeys { it != "aps" })
        val notification = JsonObject(
            linkedMapOf(
                "title" to title.asJsonElement(),
                "body" to body.asJsonElement(),
            ),
        )
        val tag = when (input.transport) {
            PushTransport.FCM, PushTransport.FCM_APNS -> PushTransport.FCM.jsonValue
            else -> input.transport.jsonValue
        }

        return JsonObject(
            linkedMapOf(
                "type" to JsonPrimitive("remote-notification"),
                "source" to JsonPrimitive(input.transport.jsonValue),
                "tag" to JsonPrimitive(tag),
                "platform" to JsonPrimitive(PushPlatform.IOS.jsonValue),
                "transport" to JsonPrimitive(input.transport.jsonValue),
                "capturePoint" to JsonPrimitive(input.capturePoint.jsonValue),
                "receivedAtMillis" to JsonPrimitive(receivedAtMillis),
                "registrationToken" to input.registrationToken.asJsonElement(),
                "title" to title.asJsonElement(),
                "body" to body.asJsonElement(),
                "data" to data,
                "notification" to notification,
                "rawDeliveredPayload" to rawPayload,
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

    private fun RegistrationTokenSnapshotInput?.asJsonElement(): JsonElement {
        this ?: return JsonNull
        return JsonObject(
            linkedMapOf(
                "kind" to JsonPrimitive(kind.jsonValue),
                "value" to JsonPrimitive(value),
            ),
        )
    }

    private fun JsonObject?.stringValue(key: String): String? =
        this?.get(key).stringValue()

    private fun JsonElement?.stringValue(): String? {
        val primitive = this as? JsonPrimitive ?: return null
        return primitive
            .takeIf { it.isString }
            ?.content
            ?.takeIf { it.isNotBlank() }
    }

    private fun firstNonBlank(vararg values: String?): String? =
        values.firstOrNull { !it.isNullOrBlank() }

    private fun Map<String, String>.toSortedJsonObject(): JsonObject =
        JsonObject(
            linkedMapOf<String, JsonElement>().apply {
                this@toSortedJsonObject.entries
                    .sortedBy { it.key }
                    .forEach { (key, value) -> put(key, JsonPrimitive(value)) }
            },
        )
}
