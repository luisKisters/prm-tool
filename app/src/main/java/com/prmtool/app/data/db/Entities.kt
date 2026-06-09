package com.prmtool.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Lifecycle of a captured contact:
 *   DRAFT        just saved locally, enrichment queued
 *   ENRICHING    enrich request in flight
 *   ENRICHED     enrichment returned, waiting for the user to review/confirm
 *   ENRICH_FAILED enrichment failed after retries (retryable)
 *   COMMITTING   commit request in flight
 *   COMMITTED    written to Twenty + Google Contacts
 *   COMMIT_FAILED commit failed after retries (retryable)
 */
enum class SendStatus {
    DRAFT, ENRICHING, ENRICHED, ENRICH_FAILED, COMMITTING, COMMITTED, COMMIT_FAILED
}

@Entity(tableName = "contacts")
data class ContactEntity(
    /** Stable PRM identifier (also the prmId stored in Twenty + Google). */
    @PrimaryKey val clientId: String,
    val firstName: String,
    val lastName: String,
    val email: String = "",
    val company: String,
    val number: String,
    val note: String,
    /** Comma-separated event names selected for this contact. */
    val events: String,
    /** Comma-separated source names selected for this contact. */
    val sources: String,
    /** Free-text detail about how/where this contact was sourced. */
    val sourceDetails: String = "",
    /** Absolute path to the recorded voice note, or null. */
    val voicePath: String?,
    val createdAt: Long,
    val status: String,

    // --- Enrichment results (filled by EnrichWorker, editable on the review screen) ---
    val transcript: String = "",
    val linkedinUrl: String = "",
    val headline: String = "",
    val avatarUrl: String = "",
    val companyDomain: String = "",
    val companyLinkedinUrl: String = "",
    val companyEmployees: Int? = null,
    val companyAddress: String = "",
    /** JSON array of company enrichment tags, e.g. ["DOMAIN","LINKEDIN"]. */
    val companyEnrichedJson: String = "",
    val summary: String = "",
    /** JSON array of enrichment tags, e.g. ["LINKEDIN","AVATAR"]. */
    val enrichedJson: String = "",

    // --- Commit results (filled by CommitWorker) ---
    val twentyId: String? = null,
    val twentyUrl: String? = null,
    val googleResourceName: String? = null,
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
