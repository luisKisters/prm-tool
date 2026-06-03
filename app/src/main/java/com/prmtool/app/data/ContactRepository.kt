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
import com.prmtool.app.net.SendWorker
import kotlinx.coroutines.flow.Flow
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

    suspend fun getContact(clientId: String): ContactEntity? = contactDao.getByClientId(clientId)

    suspend fun updateStatus(clientId: String, status: SendStatus) =
        contactDao.updateStatus(clientId, status.name)

    /** Events whose [start, end] window contains [nowMillis] — used to pre-select on the form. */
    suspend fun activeEvents(nowMillis: Long): List<EventEntity> = eventDao.activeAt(nowMillis)

    suspend fun addEvent(name: String, startMillis: Long, endMillis: Long) =
        eventDao.insert(EventEntity(name = name, startMillis = startMillis, endMillis = endMillis))

    suspend fun deleteEvent(event: EventEntity) = eventDao.delete(event)

    suspend fun addSource(name: String) = sourceDao.insert(SourceEntity(name = name))

    suspend fun deleteSource(source: SourceEntity) = sourceDao.delete(source)

    /** Persist the contact locally and enqueue a send (tries now, retries with backoff). */
    suspend fun saveAndSend(contact: ContactEntity) {
        contactDao.insert(contact)
        enqueueSend(contact.clientId)
    }

    fun retry(clientId: String) = enqueueSend(clientId)

    private fun enqueueSend(clientId: String) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = OneTimeWorkRequestBuilder<SendWorker>()
            .setInputData(workDataOf(SendWorker.KEY_CLIENT_ID to clientId))
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(appContext)
            .enqueueUniqueWork("send_$clientId", ExistingWorkPolicy.REPLACE, request)
    }
}
