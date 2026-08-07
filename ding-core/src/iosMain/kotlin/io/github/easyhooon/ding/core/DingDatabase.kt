package io.github.easyhooon.ding.core

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor

@Database(
    entities = [NotificationSnapshotEntity::class, RegistrationTokenEntity::class],
    version = 1,
    exportSchema = true,
)
@ConstructedBy(DingDatabaseConstructor::class)
internal abstract class DingDatabase : RoomDatabase() {
    abstract fun dingDao(): DingDao
}

@Suppress("KotlinNoActualForExpect")
internal expect object DingDatabaseConstructor : RoomDatabaseConstructor<DingDatabase> {
    override fun initialize(): DingDatabase
}
