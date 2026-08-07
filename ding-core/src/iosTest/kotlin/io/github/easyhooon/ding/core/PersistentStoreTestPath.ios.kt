package io.github.easyhooon.ding.core

import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import kotlin.random.Random

internal actual fun temporaryPersistentStorePath(): String =
    "${NSTemporaryDirectory()}ding-${currentTimeMillis()}-${Random.nextLong()}.preferences_pb"

internal actual fun persistentStoreFileExists(storagePath: String): Boolean =
    NSFileManager.defaultManager.fileExistsAtPath(storagePath)
