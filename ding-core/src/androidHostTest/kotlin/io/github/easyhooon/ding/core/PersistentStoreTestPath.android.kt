package io.github.easyhooon.ding.core

import java.nio.file.Files
import java.nio.file.Path

internal actual fun temporaryPersistentStorePath(): String =
    Files.createTempDirectory("ding-store")
        .resolve("ding.preferences_pb")
        .toString()

internal actual fun persistentStoreFileExists(storagePath: String): Boolean =
    Files.exists(Path.of(storagePath))
