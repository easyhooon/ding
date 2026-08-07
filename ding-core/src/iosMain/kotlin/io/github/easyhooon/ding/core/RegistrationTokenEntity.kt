package io.github.easyhooon.ding.core

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "registration_tokens")
internal data class RegistrationTokenEntity(
    @PrimaryKey
    val kind: String,
    val value: String,
)
