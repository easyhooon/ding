package io.github.easyhooon.ding.core

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

internal object DingJsonValueNormalizer {
    fun normalizeObject(value: Map<*, *>): JsonObject {
        val normalized = linkedMapOf<String, JsonElement>()
        value.entries
            .map { (key, item) -> key.toString() to normalize(item) }
            .sortedBy { it.first }
            .forEach { (key, item) -> normalized[key] = item }
        return JsonObject(normalized)
    }

    fun normalize(value: Any?): JsonElement =
        when (value) {
            null -> JsonNull
            is JsonElement -> value
            is String -> JsonPrimitive(value)
            is Char -> JsonPrimitive(value.toString())
            is Boolean -> JsonPrimitive(value)
            is Number -> JsonPrimitive(value)
            is Map<*, *> -> normalizeObject(value)
            is Iterable<*> -> JsonArray(value.map(::normalize))
            is Array<*> -> JsonArray(value.map(::normalize))
            is BooleanArray -> JsonArray(value.map(::JsonPrimitive))
            is ByteArray -> JsonArray(value.map(::JsonPrimitive))
            is ShortArray -> JsonArray(value.map(::JsonPrimitive))
            is IntArray -> JsonArray(value.map(::JsonPrimitive))
            is LongArray -> JsonArray(value.map(::JsonPrimitive))
            is FloatArray -> JsonArray(value.map(::JsonPrimitive))
            is DoubleArray -> JsonArray(value.map(::JsonPrimitive))
            is CharArray -> JsonArray(value.map { JsonPrimitive(it.toString()) })
            else -> normalizePlatformValue(value) ?: JsonPrimitive(value.toString())
        }
}
