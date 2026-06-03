package com.prmtool.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Send status for a locally-stored contact. */
enum class SendStatus { PENDING, SENT, FAILED }

@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey val clientId: String,
    val firstName: String,
    val lastName: String,
    val company: String,
    val number: String,
    val note: String,
    /** Comma-separated event names selected for this contact. */
    val events: String,
    /** Comma-separated source names selected for this contact. */
    val sources: String,
    /** Absolute path to the recorded voice note, or null. */
    val voicePath: String?,
    val createdAt: Long,
    val status: String
)

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** Inclusive start of the auto-select window (epoch millis). */
    val startMillis: Long,
    /** Inclusive end of the auto-select window (epoch millis). */
    val endMillis: Long
)

@Entity(tableName = "sources")
data class SourceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)
