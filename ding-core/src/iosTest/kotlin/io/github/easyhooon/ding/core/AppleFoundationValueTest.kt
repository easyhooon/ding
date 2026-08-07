@file:Suppress("CAST_NEVER_SUCCEEDS")

package io.github.easyhooon.ding.core

import kotlinx.serialization.json.Json
import platform.Foundation.NSNull
import platform.Foundation.NSNumber
import kotlin.test.Test
import kotlin.test.assertEquals

class AppleFoundationValueTest {
    @Test
    fun foundationNumbersAndNullKeepTheirJsonTypes() {
        val actual = DingSnapshotJson.applePush(
            input = ApplePushSnapshotInput(
                userInfo = mapOf(
                    "aps" to mapOf("content-available" to (1 as NSNumber)),
                    "count" to (3 as NSNumber),
                    "enabled" to (true as NSNumber),
                    "ratio" to (1.5 as NSNumber),
                    "nullable" to NSNull(),
                ),
                transport = PushTransport.APNS,
                capturePoint = CapturePoint.BACKGROUND_CALLBACK,
                registrationToken = null,
            ),
            receivedAtMillis = 1_717_172_300L,
        )

        val rawPayload = Json.parseToJsonElement(actual)
        val expected = Json.parseToJsonElement(
            """
            {
              "type": "remote-notification",
              "source": "apns",
              "tag": "apns",
              "platform": "ios",
              "transport": "apns",
              "capturePoint": "background-callback",
              "receivedAtMillis": 1717172300,
              "registrationToken": null,
              "title": null,
              "body": null,
              "data": {
                "count": 3,
                "enabled": true,
                "nullable": null,
                "ratio": 1.5
              },
              "notification": {
                "title": null,
                "body": null
              },
              "rawDeliveredPayload": {
                "aps": {
                  "content-available": 1
                },
                "count": 3,
                "enabled": true,
                "nullable": null,
                "ratio": 1.5
              }
            }
            """.trimIndent(),
        )

        assertEquals(expected, rawPayload)
    }
}
