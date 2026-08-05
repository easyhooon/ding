package io.github.easyhooon.notificationinspector

import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

internal data class NotificationSnapshotUiModel(
    val title: String,
    val body: String?,
    val source: String,
    val tag: NotificationFilterTag,
    val status: NotificationSnapshotStatus,
    val receivedAt: String,
    val overview: List<Pair<String, String>>,
    val dataJson: String,
    val notificationJson: String,
    val rawJson: String,
) {
    fun matches(query: String): Boolean {
        return query.isBlank() || rawJson.contains(query.trim(), ignoreCase = true)
    }

    companion object {
        fun from(snapshot: JSONObject): NotificationSnapshotUiModel {
            val tag = NotificationFilterTag.entries.firstOrNull {
                it.jsonValue == snapshot.optString("tag")
            } ?: NotificationFilterTag.ALL
            val notification = snapshot.optJSONObject("notification")
            val receivedAtMillis = snapshot.optLong("receivedAtMillis")
            val receivedAt = receivedAtMillis
                .takeIf { it > 0L }
                ?.let(::formatTimestamp)
                ?: "Unknown time"
            val status = NotificationSnapshotStatus.from(snapshot, tag)
            val type = snapshot.stringValue("type") ?: "unknown"
            val source = snapshot.stringValue("source") ?: "unknown"

            return NotificationSnapshotUiModel(
                title = firstNonBlank(
                    snapshot.stringValue("title"),
                    notification?.stringValue("title"),
                ) ?: when (tag) {
                    NotificationFilterTag.FCM -> "Remote message"
                    NotificationFilterTag.LOCAL -> "Local notification"
                    NotificationFilterTag.ALL -> "Notification event"
                },
                body = firstNonBlank(
                    snapshot.stringValue("body"),
                    notification?.stringValue("body"),
                ),
                source = source,
                tag = tag,
                status = status,
                receivedAt = receivedAt,
                overview = buildList {
                    add("Type" to type)
                    add("Source" to source)
                    add("Category" to tag.label)
                    add("Status" to status.label)
                    add("Received" to receivedAt)
                    snapshot.stringValue("messageId")?.let { add("Message ID" to it) }
                    snapshot.stringValue("notificationId")?.let { add("Notification ID" to it) }
                    snapshot.stringValue("collapseKey")?.let { add("Collapse key" to it) }
                },
                dataJson = snapshot.optJSONObject("data")?.toString(2) ?: "{}",
                notificationJson = notification?.toString(2) ?: "No notification payload",
                rawJson = snapshot.toString(2),
            )
        }

        private fun firstNonBlank(vararg values: String?): String? {
            return values.firstOrNull { !it.isNullOrBlank() }
        }

        private fun formatTimestamp(timestampMillis: Long): String {
            return DateTimeFormatter
                .ofPattern("MMM d, HH:mm:ss", Locale.getDefault())
                .withZone(ZoneId.systemDefault())
                .format(Instant.ofEpochMilli(timestampMillis))
        }
    }
}

internal enum class NotificationSnapshotStatus(
    val label: String,
) {
    SUCCESS("Success"),
    IN_PROGRESS("In progress"),
    ERROR("Error"),
    INFORMATIONAL("Info"),
    ;

    companion object {
        fun from(snapshot: JSONObject, tag: NotificationFilterTag): NotificationSnapshotStatus {
            return when (snapshot.optString("status").lowercase()) {
                "success" -> SUCCESS
                "in-progress", "in_progress", "progress" -> IN_PROGRESS
                "error", "failed", "failure" -> ERROR
                "info", "informational", "local" -> INFORMATIONAL
                else -> if (tag == NotificationFilterTag.LOCAL) INFORMATIONAL else SUCCESS
            }
        }
    }
}

private fun JSONObject.stringValue(name: String): String? {
    val value = opt(name)
    return value
        ?.takeUnless { it == JSONObject.NULL }
        ?.toString()
        ?.takeIf { it.isNotBlank() }
}
