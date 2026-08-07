package io.github.easyhooon.ding.core

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplePushSnapshotJsonTest {
    @Test
    fun applePushPreservesNestedUserInfoAndExtractsApsOverview() {
        val userInfo = mapOf(
            "promotions" to listOf("summer", 20, true),
            "order" to mapOf("id" to "D-2048", "items" to 2),
            "gcm.message_id" to "message-42",
            "aps" to mapOf(
                "sound" to "default",
                "badge" to 3,
                "alert" to mapOf(
                    "body" to "Order #D-2048 will arrive tomorrow.",
                    "title" to "Order shipped",
                ),
            ),
        )

        val actual = DingSnapshotJson.applePush(
            input = ApplePushSnapshotInput(
                userInfo = userInfo,
                transport = PushTransport.FCM_APNS,
                capturePoint = CapturePoint.FOREGROUND,
                registrationToken = RegistrationTokenSnapshotInput(
                    kind = RegistrationTokenKind.FCM,
                    value = "fcm-token-v2",
                ),
            ),
            receivedAtMillis = 1_717_172_100L,
        )

        val expected = Json.parseToJsonElement(
            """
            {
              "type": "remote-notification",
              "source": "fcm-apns",
              "tag": "fcm",
              "platform": "ios",
              "transport": "fcm-apns",
              "capturePoint": "foreground",
              "receivedAtMillis": 1717172100,
              "registrationToken": {
                "kind": "fcm",
                "value": "fcm-token-v2"
              },
              "title": "Order shipped",
              "body": "Order #D-2048 will arrive tomorrow.",
              "data": {
                "gcm.message_id": "message-42",
                "order": {
                  "id": "D-2048",
                  "items": 2
                },
                "promotions": ["summer", 20, true]
              },
              "notification": {
                "title": "Order shipped",
                "body": "Order #D-2048 will arrive tomorrow."
              },
              "rawDeliveredPayload": {
                "aps": {
                  "alert": {
                    "body": "Order #D-2048 will arrive tomorrow.",
                    "title": "Order shipped"
                  },
                  "badge": 3,
                  "sound": "default"
                },
                "gcm.message_id": "message-42",
                "order": {
                  "id": "D-2048",
                  "items": 2
                },
                "promotions": ["summer", 20, true]
              }
            }
            """.trimIndent(),
        )

        assertEquals(expected, Json.parseToJsonElement(actual))
    }

    @Test
    fun applePushFallsBackToNestedDataTitleAndBody() {
        val actual = DingSnapshotJson.applePush(
            input = ApplePushSnapshotInput(
                userInfo = mapOf(
                    "aps" to mapOf("content-available" to 1),
                    "data" to mapOf(
                        "title" to "Background refresh",
                        "body" to "New account activity is available.",
                    ),
                ),
                transport = PushTransport.APNS,
                capturePoint = CapturePoint.BACKGROUND_CALLBACK,
                registrationToken = RegistrationTokenSnapshotInput(
                    kind = RegistrationTokenKind.APNS,
                    value = "apns-device-token",
                ),
            ),
            receivedAtMillis = 1_717_172_200L,
        )

        val snapshot = Json.parseToJsonElement(actual)
        val expected = Json.parseToJsonElement(
            """
            {
              "type": "remote-notification",
              "source": "apns",
              "tag": "apns",
              "platform": "ios",
              "transport": "apns",
              "capturePoint": "background-callback",
              "receivedAtMillis": 1717172200,
              "registrationToken": {
                "kind": "apns",
                "value": "apns-device-token"
              },
              "title": "Background refresh",
              "body": "New account activity is available.",
              "data": {
                "data": {
                  "body": "New account activity is available.",
                  "title": "Background refresh"
                }
              },
              "notification": {
                "title": "Background refresh",
                "body": "New account activity is available."
              },
              "rawDeliveredPayload": {
                "aps": {
                  "content-available": 1
                },
                "data": {
                  "body": "New account activity is available.",
                  "title": "Background refresh"
                }
              }
            }
            """.trimIndent(),
        )

        assertEquals(expected, snapshot)
    }

    @Test
    fun applePushUsesStringAlertAsBody() {
        val actual = DingSnapshotJson.applePush(
            input = ApplePushSnapshotInput(
                userInfo = mapOf(
                    "aps" to mapOf("alert" to "Maintenance starts at midnight."),
                ),
                transport = PushTransport.APNS,
                capturePoint = CapturePoint.NOTIFICATION_RESPONSE,
                registrationToken = null,
            ),
            receivedAtMillis = 1_717_172_300L,
        )

        val expected = Json.parseToJsonElement(
            """
            {
              "type": "remote-notification",
              "source": "apns",
              "tag": "apns",
              "platform": "ios",
              "transport": "apns",
              "capturePoint": "notification-response",
              "receivedAtMillis": 1717172300,
              "registrationToken": null,
              "title": null,
              "body": "Maintenance starts at midnight.",
              "data": {},
              "notification": {
                "title": null,
                "body": "Maintenance starts at midnight."
              },
              "rawDeliveredPayload": {
                "aps": {
                  "alert": "Maintenance starts at midnight."
                }
              }
            }
            """.trimIndent(),
        )

        assertEquals(expected, Json.parseToJsonElement(actual))
    }
}
