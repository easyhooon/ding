package io.github.easyhooon.ding

import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.firebase.messaging.RemoteMessage
import org.json.JSONObject

object Ding {
    private const val TAG = "Ding"

    fun capture(context: Context, remoteMessage: RemoteMessage) {
        captureRemoteMessage(context = context, remoteMessage = remoteMessage, suppliedFcmToken = null)
    }

    /**
     * Captures an FCM message together with the host-supplied registration token known at receipt time.
     * FCM does not expose or verify the destination token through [RemoteMessage].
     */
    fun capture(
        context: Context,
        remoteMessage: RemoteMessage,
        fcmToken: String,
    ) {
        captureRemoteMessage(
            context = context,
            remoteMessage = remoteMessage,
            suppliedFcmToken = fcmToken.normalizedFcmToken(),
        )
    }

    private fun captureRemoteMessage(
        context: Context,
        remoteMessage: RemoteMessage,
        suppliedFcmToken: String?,
    ) {
        val appContext = context.applicationContext
        val store = DingStore(appContext)
        val receivedAtMillis = System.currentTimeMillis()
        suppliedFcmToken?.let(store::updateFcmTokenAsync)

        store.appendWithFcmTokenAsync(
            suppliedFcmToken = suppliedFcmToken,
            snapshotProvider = { fcmToken ->
                RemoteMessageSnapshot.from(
                    remoteMessage = remoteMessage,
                    fcmToken = fcmToken,
                    receivedAtMillis = receivedAtMillis,
                ).also { snapshot ->
                    Log.d(TAG, snapshot.toString(2))
                }
            },
            onStored = {
                PersistentNotificationController.refreshIfEnabled(appContext, store)
            },
        )
    }

    /**
     * Replaces the latest registration token used by subsequent two-argument [capture] calls.
     * Call this from `FirebaseMessagingService.onNewToken` and after startup token retrieval.
     */
    fun updateFcmToken(context: Context, fcmToken: String) {
        val normalizedToken = fcmToken.normalizedFcmToken()
        if (normalizedToken == null) {
            Log.w(TAG, "Ignoring a blank FCM registration token")
            return
        }
        DingStore(context).updateFcmTokenAsync(normalizedToken)
    }

    fun captureNotification(
        context: Context,
        source: String,
        notificationId: Int,
        title: String,
        body: String,
        data: Map<String, String> = emptyMap(),
    ) {
        val snapshot = LocalNotificationSnapshot.from(
            source = source,
            notificationId = notificationId,
            title = title,
            body = body,
            data = data,
            receivedAtMillis = System.currentTimeMillis(),
        )
        store(context, snapshot)
        Log.d(TAG, snapshot.toString(2))
    }

    fun setPersistentNotificationEnabled(context: Context, enabled: Boolean) {
        PersistentNotificationController.setEnabled(context, enabled)
    }

    fun open(context: Context) {
        val intent = Intent(context, DingActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    private fun store(context: Context, snapshot: JSONObject) {
        val appContext = context.applicationContext
        val store = DingStore(appContext)
        store.appendAsync(snapshot) {
            PersistentNotificationController.refreshIfEnabled(appContext, store)
        }
    }

    private fun String.normalizedFcmToken(): String? {
        return trim().takeIf { it.isNotEmpty() }
    }
}
