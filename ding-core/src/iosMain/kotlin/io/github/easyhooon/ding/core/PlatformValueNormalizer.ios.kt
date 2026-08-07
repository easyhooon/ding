@file:OptIn(ExperimentalForeignApi::class)

package io.github.easyhooon.ding.core

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import platform.Foundation.NSNull
import platform.Foundation.NSNumber

internal actual fun normalizePlatformValue(value: Any): JsonElement? =
    when (value) {
        is NSNull -> JsonNull
        is NSNumber -> when (value.objCType?.toKString()) {
            "B", "c" -> JsonPrimitive(value.boolValue)
            "d", "f" -> JsonPrimitive(value.doubleValue)
            else -> JsonPrimitive(value.longLongValue)
        }
        else -> null
    }
