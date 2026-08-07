package io.github.easyhooon.ding.core

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DingAppleCaptureTest {
    @Test
    fun tokenChangesOnlyAffectSubsequentSnapshots() = runTest {
        val capture = DingAppleCapture(InMemoryDingCaptureStore())

        capture.updateFcmToken(" fcm-token-v1 ")
        capture.captureAppleUserInfo(
            userInfo = payload("First"),
            transport = PushTransport.FCM_APNS,
            capturePoint = CapturePoint.FOREGROUND,
            receivedAtMillis = 100L,
        )
        capture.updateFcmToken("fcm-token-v2")
        capture.captureAppleUserInfo(
            userInfo = payload("Second"),
            transport = PushTransport.FCM_APNS,
            capturePoint = CapturePoint.NOTIFICATION_RESPONSE,
            receivedAtMillis = 200L,
        )

        val snapshots = capture.snapshots().map(Json::parseToJsonElement)
        assertEquals("fcm-token-v1", snapshots[0].jsonObject.tokenValue())
        assertEquals("fcm-token-v2", snapshots[1].jsonObject.tokenValue())
    }

    @Test
    fun fcmAndApnsTokensRemainDistinct() = runTest {
        val capture = DingAppleCapture(InMemoryDingCaptureStore())
        capture.updateFcmToken("fcm-token")
        capture.updateApnsToken("apns-token")

        val fcmSnapshot = capture.captureAppleUserInfo(
            userInfo = payload("FCM"),
            transport = PushTransport.FCM_APNS,
            capturePoint = CapturePoint.FOREGROUND,
            receivedAtMillis = 100L,
        )
        val apnsSnapshot = capture.captureAppleUserInfo(
            userInfo = payload("APNs"),
            transport = PushTransport.APNS,
            capturePoint = CapturePoint.BACKGROUND_CALLBACK,
            receivedAtMillis = 200L,
        )

        assertEquals("fcm", Json.parseToJsonElement(fcmSnapshot).jsonObject.tokenKind())
        assertEquals("fcm-token", Json.parseToJsonElement(fcmSnapshot).jsonObject.tokenValue())
        assertEquals("apns", Json.parseToJsonElement(apnsSnapshot).jsonObject.tokenKind())
        assertEquals("apns-token", Json.parseToJsonElement(apnsSnapshot).jsonObject.tokenValue())
    }

    @Test
    fun blankTokenDoesNotReplaceTheLatestToken() = runTest {
        val capture = DingAppleCapture(InMemoryDingCaptureStore())
        capture.updateFcmToken("fcm-token")
        capture.updateFcmToken("  ")

        val snapshot = capture.captureAppleUserInfo(
            userInfo = payload("Message"),
            transport = PushTransport.FCM_APNS,
            capturePoint = CapturePoint.FOREGROUND,
            receivedAtMillis = 100L,
        )

        assertEquals("fcm-token", Json.parseToJsonElement(snapshot).jsonObject.tokenValue())
    }

    @Test
    fun storeRetainsOnlyTheNewestSnapshots() = runTest {
        val store = InMemoryDingCaptureStore(maxSnapshots = 2)
        store.append("first")
        store.append("second")
        store.append("third")

        assertEquals(listOf("second", "third"), store.snapshots())
    }

    @Test
    fun clearingSnapshotsKeepsRegistrationTokens() = runTest {
        val store = InMemoryDingCaptureStore()
        store.updateRegistrationToken(RegistrationTokenKind.FCM, "fcm-token")
        store.append("snapshot")

        store.clearSnapshots()

        assertEquals(emptyList(), store.snapshots())
        assertEquals("fcm-token", store.registrationToken(RegistrationTokenKind.FCM))
        assertNull(store.registrationToken(RegistrationTokenKind.APNS))
    }

    private fun payload(title: String): Map<String, Any> =
        mapOf("aps" to mapOf("alert" to mapOf("title" to title)))

    private fun JsonObject.tokenKind(): String =
        getValue("registrationToken").jsonObject.getValue("kind").jsonPrimitive.content

    private fun JsonObject.tokenValue(): String =
        getValue("registrationToken").jsonObject.getValue("value").jsonPrimitive.content
}
