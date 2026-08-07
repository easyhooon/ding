package io.github.easyhooon.ding.core

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert

@Dao
internal interface DingDao {
    @Query("SELECT payloadJson FROM notification_snapshots ORDER BY id ASC")
    suspend fun snapshots(): List<String>

    @Insert
    suspend fun insertSnapshot(snapshot: NotificationSnapshotEntity)

    @Query(
        "DELETE FROM notification_snapshots " +
            "WHERE id NOT IN " +
            "(SELECT id FROM notification_snapshots ORDER BY id DESC LIMIT :maxSnapshots)",
    )
    suspend fun trimSnapshots(maxSnapshots: Int)

    @Transaction
    suspend fun insertAndTrim(
        snapshot: NotificationSnapshotEntity,
        maxSnapshots: Int,
    ) {
        insertSnapshot(snapshot)
        trimSnapshots(maxSnapshots)
    }

    @Query("DELETE FROM notification_snapshots")
    suspend fun clearSnapshots()

    @Query("SELECT value FROM registration_tokens WHERE kind = :kind LIMIT 1")
    suspend fun registrationToken(kind: String): String?

    @Upsert
    suspend fun upsertRegistrationToken(token: RegistrationTokenEntity)
}
