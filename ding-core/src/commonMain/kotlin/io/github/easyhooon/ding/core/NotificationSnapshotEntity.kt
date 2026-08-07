package io.github.easyhooon.ding.core

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.longOrNull

@Entity(
    tableName = "notification_snapshots",
    indices = [
        Index(value = ["receivedAtMillis"]),
        Index(value = ["source"]),
        Index(value = ["transport"]),
    ],
)
internal data class NotificationSnapshotEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val receivedAtMillis: Long,
    val type: String?,
    val source: String?,
    val platform: String?,
    val transport: String?,
    val capturePoint: String?,
    val title: String?,
    val body: String?,
    val payloadJson: String,
) {
    companion object {
        fun from(payloadJson: String): NotificationSnapshotEntity {
            val payload = runCatching {
                Json.parseToJsonElement(payloadJson).jsonObject
            }.getOrDefault(JsonObject(emptyMap()))
            return NotificationSnapshotEntity(
                receivedAtMillis = payload.longValue("receivedAtMillis") ?: currentTimeMillis(),
                type = payload.stringValue("type"),
                source = payload.stringValue("source"),
                platform = payload.stringValue("platform"),
                transport = payload.stringValue("transport"),
                capturePoint = payload.stringValue("capturePoint"),
                title = payload.stringValue("title"),
                body = payload.stringValue("body"),
                payloadJson = payloadJson,
            )
        }

        private fun JsonObject.stringValue(key: String): String? =
            (get(key) as? JsonPrimitive)?.contentOrNull

        private fun JsonObject.longValue(key: String): Long? =
            (get(key) as? JsonPrimitive)?.longOrNull
    }
}
