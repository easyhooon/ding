package io.github.easyhooon.ding.core

internal expect fun temporaryPersistentStorePath(): String

internal expect fun persistentStoreFileExists(storagePath: String): Boolean
