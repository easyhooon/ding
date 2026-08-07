package io.github.easyhooon.ding

import io.github.easyhooon.ding.core.DingSnapshotJson
import io.github.easyhooon.ding.core.LocalNotificationSnapshotInput
import org.json.JSONObject

internal object LocalNotificationSnapshot {
    fun from(
        source: String,
        notificationId: Int,
        title: String,
        body: String,
        data: Map<String, String>,
        receivedAtMillis: Long,
    ): JSONObject {
        val input = LocalNotificationSnapshotInput(
            source = source,
            notificationId = notificationId,
            title = title,
            body = body,
            data = data,
        )
        return JSONObject(
            DingSnapshotJson.localNotification(
                input = input,
                receivedAtMillis = receivedAtMillis,
            ),
        )
    }
}
