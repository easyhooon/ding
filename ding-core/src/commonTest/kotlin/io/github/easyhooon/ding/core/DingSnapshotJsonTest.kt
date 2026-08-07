package io.github.easyhooon.ding.core

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class DingSnapshotJsonTest {
    @Test
    fun remoteMessagePreservesPayloadAndAddsDeliveryContext() {
        val input = RemoteMessageSnapshotInput(
            messageId = "message-42",
            messageType = "chat",
            from = "/topics/inbox",
            collapseKey = "inbox",
            sentTime = 1_717_171_717L,
            ttl = 3_600,
            priority = 2,
            originalPriority = 1,
            data = mapOf("z-last" to "last", "a-first" to "first"),
            notification = RemoteNotificationSnapshotInput(
                title = "New message",
                body = "Are we still on for dinner?",
                imageUrl = "https://example.com/image.png",
                channelId = "messages",
                clickAction = "OPEN_CHAT",
                color = "#336699",
                icon = "ic_message",
                link = "ding://chat/42",
                sound = "default",
                tag = "chat-42",
            ),
        )

        val actual = DingSnapshotJson.remoteMessage(
            input = input,
            fcmToken = "token-v1",
            receivedAtMillis = 1_717_171_999L,
        )

        val expected = Json.parseToJsonElement(
            """
            {
              "type": "remote-message",
              "source": "fcm",
              "tag": "fcm",
              "platform": "android",
              "transport": "fcm",
              "capturePoint": "host-callback",
              "receivedAtMillis": 1717171999,
              "fcmToken": "token-v1",
              "messageId": "message-42",
              "messageType": "chat",
              "from": "/topics/inbox",
              "collapseKey": "inbox",
              "sentTime": 1717171717,
              "ttl": 3600,
              "priority": 2,
              "originalPriority": 1,
              "data": {
                "a-first": "first",
                "z-last": "last"
              },
              "notification": {
                "title": "New message",
                "body": "Are we still on for dinner?",
                "imageUrl": "https://example.com/image.png",
                "channelId": "messages",
                "clickAction": "OPEN_CHAT",
                "color": "#336699",
                "icon": "ic_message",
                "link": "ding://chat/42",
                "sound": "default",
                "tag": "chat-42"
              },
              "rawDeliveredPayload": {
                "messageId": "message-42",
                "messageType": "chat",
                "from": "/topics/inbox",
                "collapseKey": "inbox",
                "sentTime": 1717171717,
                "ttl": 3600,
                "priority": 2,
                "originalPriority": 1,
                "data": {
                  "a-first": "first",
                  "z-last": "last"
                },
                "notification": {
                  "title": "New message",
                  "body": "Are we still on for dinner?",
                  "imageUrl": "https://example.com/image.png",
                  "channelId": "messages",
                  "clickAction": "OPEN_CHAT",
                  "color": "#336699",
                  "icon": "ic_message",
                  "link": "ding://chat/42",
                  "sound": "default",
                  "tag": "chat-42"
                }
              }
            }
            """.trimIndent(),
        )

        assertEquals(expected, Json.parseToJsonElement(actual))
    }

    @Test
    fun localNotificationPreservesLegacyShapeAndAddsDeliveryContext() {
        val actual = DingSnapshotJson.localNotification(
            input = LocalNotificationSnapshotInput(
                source = "checkout",
                notificationId = 2048,
                title = "Payment completed",
                body = "Your payment was processed successfully.",
                data = mapOf("order-id" to "D-2048"),
            ),
            receivedAtMillis = 1_717_172_000L,
        )

        val expected = Json.parseToJsonElement(
            """
            {
              "type": "local-notification",
              "source": "checkout",
              "tag": "local",
              "platform": "android",
              "transport": "local",
              "capturePoint": "host-api",
              "receivedAtMillis": 1717172000,
              "notificationId": 2048,
              "title": "Payment completed",
              "body": "Your payment was processed successfully.",
              "data": {
                "order-id": "D-2048"
              },
              "notification": {
                "notificationId": 2048,
                "title": "Payment completed",
                "body": "Your payment was processed successfully."
              },
              "rawDeliveredPayload": {
                "notificationId": 2048,
                "title": "Payment completed",
                "body": "Your payment was processed successfully.",
                "data": {
                  "order-id": "D-2048"
                }
              }
            }
            """.trimIndent(),
        )

        assertEquals(expected, Json.parseToJsonElement(actual))
    }
}
