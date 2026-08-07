package io.github.easyhooon.ding.core

import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import kotlin.random.Random

internal fun temporaryPersistentStorePath(): String =
    "${NSTemporaryDirectory()}ding-${currentTimeMillis()}-${Random.nextLong()}.db"

internal fun persistentStoreFileExists(storagePath: String): Boolean =
    NSFileManager.defaultManager.fileExistsAtPath(storagePath)
