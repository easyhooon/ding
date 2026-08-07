package io.github.easyhooon.ding.core

import kotlinx.serialization.json.JsonElement

internal expect fun normalizePlatformValue(value: Any): JsonElement?
