package io.github.easyhooon.notificationinspector

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
        return JSONObject().apply {
            put("type", "local-notification")
            put("source", source)
            put("tag", NotificationFilterTag.LOCAL.jsonValue)
            put("receivedAtMillis", receivedAtMillis)
            put("notificationId", notificationId)
            put("title", title)
            put("body", body)
            put("data", dataJson(data))
            put(
                "notification",
                JSONObject().apply {
                    put("notificationId", notificationId)
                    put("title", title)
                    put("body", body)
                },
            )
        }
    }

    private fun dataJson(data: Map<String, String>): JSONObject {
        return JSONObject().apply {
            data.toSortedMap().forEach { (key, value) ->
                put(key, value)
            }
        }
    }
}
