package com.prmtool.app.data

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.prmtool.app.data.db.ContactDao
import com.prmtool.app.data.db.ContactEntity
import com.prmtool.app.data.db.EventDao
import com.prmtool.app.data.db.EventEntity
import com.prmtool.app.data.db.SendStatus
import com.prmtool.app.data.db.SourceDao
import com.prmtool.app.data.db.SourceEntity
import com.prmtool.app.net.CommitRequest
import com.prmtool.app.net.CommitResponse
import com.prmtool.app.net.CommitWorker
import com.prmtool.app.net.EnrichResponse
import com.prmtool.app.net.EnrichWorker
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.util.concurrent.TimeUnit

class ContactRepository(
    private val contactDao: ContactDao,
    private val eventDao: EventDao,
    private val sourceDao: SourceDao,
    private val appContext: Context
) {
    val recentContacts: Flow<List<ContactEntity>> = contactDao.recent()
    val events: Flow<List<EventEntity>> = eventDao.all()
    val sources: Flow<List<SourceEntity>> = sourceDao.all()

    private val tagsSerializer = ListSerializer(String.serializer())

    suspend fun getContact(clientId: String): ContactEntity? = contactDao.getByClientId(clientId)

    fun observeContact(clientId: String): Flow<ContactEntity?> = contactDao.observe(clientId)

    suspend fun updateStatus(clientId: String, status: SendStatus) =
        contactDao.updateStatus(clientId, status.name)

    suspend fun deleteContact(clientId: String) = contactDao.delete(clientId)

    /** Events whose [start, end] window contains [nowMillis] — used to pre-select on the form. */
    suspend fun activeEvents(nowMillis: Long): List<EventEntity> = eventDao.activeAt(nowMillis)

    suspend fun addEvent(name: String, startMillis: Long, endMillis: Long) =
        eventDao.insert(EventEntity(name = name, startMillis = startMillis, endMillis = endMillis))

    suspend fun deleteEvent(event: EventEntity) = eventDao.delete(event)

    suspend fun addSource(name: String) = sourceDao.insert(SourceEntity(name = name))

    suspend fun deleteSource(source: SourceEntity) = sourceDao.delete(source)

    // --- Capture → enrich ---------------------------------------------------

    /** Persist a freshly captured contact as DRAFT and queue enrichment (runs when online). */
    suspend fun saveDraftAndEnrich(contact: ContactEntity) {
        contactDao.insert(contact.copy(status = SendStatus.DRAFT.name))
        enqueueEnrich(contact.clientId)
    }

    fun retryEnrich(clientId: String) = enqueueEnrich(clientId)

    /** Store the enrichment result and move the contact to ENRICHED (awaiting review). */
    suspend fun applyEnrichment(clientId: String, result: EnrichResponse) {
        val contact = contactDao.getByClientId(clientId) ?: return
        contactDao.update(
            contact.copy(
                transcript = result.transcript,
                email = result.email.ifBlank { contact.email },
                linkedinUrl = result.linkedinUrl,
                headline = result.headline,
                avatarUrl = result.avatarUrl,
                companyDomain = result.companyDomain,
                companyLinkedinUrl = result.companyLinkedinUrl,
                companyEmployees = result.companyEmployees,
                companyAddress = result.companyAddress,
                companyEnrichedJson = Json.encodeToString(tagsSerializer, result.companyEnriched),
                summary = result.summary,
                enrichedJson = Json.encodeToString(tagsSerializer, result.enriched),
                status = SendStatus.ENRICHED.name,
            )
        )
    }

    // --- Review → commit ----------------------------------------------------

    /** Save the user's edits from the review screen and queue the commit. */
    suspend fun saveReviewedAndCommit(contact: ContactEntity) {
        contactDao.update(contact.copy(status = SendStatus.COMMITTING.name))
        enqueueCommit(contact.clientId)
    }

    fun retryCommit(clientId: String) = enqueueCommit(clientId)

    fun buildCommitRequest(contact: ContactEntity): CommitRequest = CommitRequest(
        prmId = contact.clientId,
        firstName = contact.firstName,
        lastName = contact.lastName,
        email = contact.email,
        company = contact.company,
        companyDomain = contact.companyDomain,
        companyLinkedinUrl = contact.companyLinkedinUrl,
        companyEmployees = contact.companyEmployees,
        companyAddress = contact.companyAddress,
        companyEnriched = decodeTags(contact.companyEnrichedJson),
        number = contact.number,
        headline = contact.headline,
        linkedinUrl = contact.linkedinUrl,
        avatarUrl = contact.avatarUrl,
        summary = contact.summary,
        note = contact.note,
        events = contact.events,
        sources = contact.sources,
        sourceDetails = contact.sourceDetails,
        enriched = decodeTags(contact.enrichedJson),
    )

    suspend fun applyCommitResult(clientId: String, result: CommitResponse) {
        val contact = contactDao.getByClientId(clientId) ?: return
        contactDao.update(
            contact.copy(
                twentyId = result.twentyId,
                twentyUrl = result.twentyUrl,
                googleResourceName = result.googleResourceName,
                status = SendStatus.COMMITTED.name,
            )
        )
    }

    fun decodeTags(json: String): List<String> =
        if (json.isBlank()) emptyList() else runCatching {
            Json.decodeFromString(tagsSerializer, json)
        }.getOrDefault(emptyList())

    // --- WorkManager plumbing ----------------------------------------------

    private fun enqueueEnrich(clientId: String) {
        val request = OneTimeWorkRequestBuilder<EnrichWorker>()
            .setInputData(workDataOf(EnrichWorker.KEY_CLIENT_ID to clientId))
            .setConstraints(onlineConstraints())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(appContext)
            .enqueueUniqueWork("enrich_$clientId", ExistingWorkPolicy.REPLACE, request)
    }

    private fun enqueueCommit(clientId: String) {
        val request = OneTimeWorkRequestBuilder<CommitWorker>()
            .setInputData(workDataOf(CommitWorker.KEY_CLIENT_ID to clientId))
            .setConstraints(onlineConstraints())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(appContext)
            .enqueueUniqueWork("commit_$clientId", ExistingWorkPolicy.REPLACE, request)
    }

    private fun onlineConstraints() = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()
}
