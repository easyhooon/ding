package io.github.easyhooon.ding

internal enum class NotificationFilterTag(
    val label: String,
    val jsonValue: String,
) {
    ALL(label = "All", jsonValue = "all"),
    FCM(label = "FCM", jsonValue = "fcm"),
    LOCAL(label = "Local", jsonValue = "local"),
}
