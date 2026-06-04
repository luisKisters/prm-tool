package com.prmtool.app.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.prmtool.app.PrmApplication
import com.prmtool.app.audio.VoiceRecorder
import com.prmtool.app.data.db.ContactEntity
import com.prmtool.app.data.db.EventEntity
import com.prmtool.app.data.db.SendStatus
import com.prmtool.app.data.db.SourceEntity
import com.prmtool.app.net.ApiClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

private const val EVENT_SOURCE = "Event"

class AddContactViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = (app as PrmApplication).container.repository
    private val recorder = VoiceRecorder(app)
    private val clientId = UUID.randomUUID().toString()

    var firstName by mutableStateOf("")
    var lastName by mutableStateOf("")
    var company by mutableStateOf("")
    var number by mutableStateOf("")
    var note by mutableStateOf("")

    var isRecording by mutableStateOf(false)
        private set
    var hasVoice by mutableStateOf(false)
        private set
    private var voiceFile: File? = null

    val allEvents: StateFlow<List<EventEntity>> =
        repo.events.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allSources: StateFlow<List<SourceEntity>> =
        repo.sources.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedEventIds: SnapshotStateList<Long> = mutableListOf<Long>().toMutableStateList()
    val selectedSources: SnapshotStateList<String> = mutableListOf<String>().toMutableStateList()

    init {
        // Auto-select events whose date range contains "now".
        viewModelScope.launch {
            val active = repo.activeEvents(System.currentTimeMillis())
            if (active.isNotEmpty()) {
                selectedEventIds.addAll(active.map { it.id })
                syncEventSource()
            }
        }
    }

    val canSave: Boolean get() = firstName.isNotBlank() || number.isNotBlank()

    fun toggleEvent(id: Long) {
        if (!selectedEventIds.remove(id)) selectedEventIds.add(id)
        syncEventSource()
    }

    fun toggleSource(name: String) {
        if (!selectedSources.remove(name)) selectedSources.add(name)
    }

    /** Source is "Event" automatically whenever any event is selected. */
    private fun syncEventSource() {
        val hasEvent = selectedEventIds.isNotEmpty()
        if (hasEvent && EVENT_SOURCE !in selectedSources) {
            selectedSources.add(EVENT_SOURCE)
        } else if (!hasEvent) {
            selectedSources.remove(EVENT_SOURCE)
        }
    }

    fun toggleRecording() {
        if (isRecording) {
            voiceFile = recorder.stop()
            hasVoice = voiceFile != null
            isRecording = false
        } else {
            recorder.start(clientId)
            isRecording = true
            hasVoice = false
        }
    }

    fun save(onDone: () -> Unit) {
        val eventNames = allEvents.value.filter { it.id in selectedEventIds }.map { it.name }
        val contact = ContactEntity(
            clientId = clientId,
            firstName = firstName.trim(),
            lastName = lastName.trim(),
            company = company.trim(),
            number = number.trim(),
            note = note.trim(),
            events = eventNames.joinToString(", "),
            sources = selectedSources.joinToString(", "),
            voicePath = voiceFile?.absolutePath,
            createdAt = System.currentTimeMillis(),
            status = SendStatus.DRAFT.name
        )
        viewModelScope.launch {
            repo.saveDraftAndEnrich(contact)
            onDone()
        }
    }

    override fun onCleared() {
        if (isRecording) recorder.cancel()
    }
}

class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as PrmApplication).container.repository

    val recent: StateFlow<List<ContactEntity>> =
        repo.recentContacts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun retryEnrich(clientId: String) = repo.retryEnrich(clientId)
    fun retryCommit(clientId: String) = repo.retryCommit(clientId)
}

/** Backs the review screen for one contact. Editable fields are seeded from the enrichment. */
class ReviewViewModel(app: Application, private val clientId: String) : AndroidViewModel(app) {
    private val repo = (app as PrmApplication).container.repository
    private val settings = (app as PrmApplication).container.settings

    var loaded by mutableStateOf(false)
        private set
    var notFound by mutableStateOf(false)
        private set

    var firstName by mutableStateOf("")
    var lastName by mutableStateOf("")
    var company by mutableStateOf("")
    var companyDomain by mutableStateOf("")
    var number by mutableStateOf("")
    var headline by mutableStateOf("")
    var linkedinUrl by mutableStateOf("")
    var summary by mutableStateOf("")

    var avatarUrl by mutableStateOf("")
        private set
    var enrichedTags by mutableStateOf<List<String>>(emptyList())
        private set

    /** True while we're re-fetching the headline + photo for an edited LinkedIn URL. */
    var linkedinLookupInProgress by mutableStateOf(false)
        private set

    private var lookupJob: Job? = null
    private var lastLookedUpUrl: String = ""

    init {
        viewModelScope.launch {
            val c = repo.getContact(clientId)
            if (c == null) {
                notFound = true
            } else {
                firstName = c.firstName
                lastName = c.lastName
                company = c.company
                companyDomain = c.companyDomain
                number = c.number
                headline = c.headline
                linkedinUrl = c.linkedinUrl
                summary = c.summary
                avatarUrl = c.avatarUrl
                enrichedTags = repo.decodeTags(c.enrichedJson)
                // Seed so editing back to the originally-enriched URL doesn't re-fetch needlessly.
                lastLookedUpUrl = c.linkedinUrl.trim()
            }
            loaded = true
        }
    }

    /**
     * Called as the user edits the LinkedIn URL. When it becomes a valid profile URL we debounce,
     * then re-derive the photo + headline for that profile and apply whatever comes back (blank
     * results clear the photo/headline). A network failure leaves the existing values untouched.
     */
    fun onLinkedinUrlChange(newUrl: String) {
        linkedinUrl = newUrl
        lookupJob?.cancel()

        val trimmed = newUrl.trim()
        if (!isLinkedinProfileUrl(trimmed) || trimmed.equals(lastLookedUpUrl, ignoreCase = true)) {
            linkedinLookupInProgress = false
            return
        }

        lookupJob = viewModelScope.launch {
            delay(700) // let typing settle before hitting the backend
            val baseUrl = settings.apiBaseUrl.first()
            val token = settings.apiToken.first()
            if (baseUrl.isBlank() || token.isBlank()) return@launch

            linkedinLookupInProgress = true
            try {
                val name = listOf(firstName, lastName).filter { it.isNotBlank() }.joinToString(" ")
                val result = withContext(Dispatchers.IO) {
                    ApiClient.lookupLinkedin(baseUrl, token, trimmed, name)
                }
                lastLookedUpUrl = trimmed
                avatarUrl = result.avatarUrl
                headline = result.headline
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Keep the existing photo + headline on lookup failure.
            } finally {
                linkedinLookupInProgress = false
            }
        }
    }

    private fun isLinkedinProfileUrl(url: String): Boolean =
        Regex("""linkedin\.com/in/[^/?\s#]+""", RegexOption.IGNORE_CASE).containsMatchIn(url)

    fun confirm(onDone: () -> Unit) {
        viewModelScope.launch {
            val c = repo.getContact(clientId) ?: return@launch
            repo.saveReviewedAndCommit(
                c.copy(
                    firstName = firstName.trim(),
                    lastName = lastName.trim(),
                    company = company.trim(),
                    companyDomain = companyDomain.trim(),
                    number = number.trim(),
                    headline = headline.trim(),
                    linkedinUrl = linkedinUrl.trim(),
                    avatarUrl = avatarUrl.trim(),
                    summary = summary.trim(),
                )
            )
            onDone()
        }
    }

    fun discard(onDone: () -> Unit) {
        viewModelScope.launch {
            repo.deleteContact(clientId)
            onDone()
        }
    }

    companion object {
        fun factory(clientId: String): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as Application
                ReviewViewModel(app, clientId)
            }
        }
    }
}

class SettingsViewModel(app: Application) : AndroidViewModel(app) {
    private val container = (app as PrmApplication).container
    private val repo = container.repository
    private val settings = container.settings

    val events: StateFlow<List<EventEntity>> =
        repo.events.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val sources: StateFlow<List<SourceEntity>> =
        repo.sources.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val apiBaseUrl: StateFlow<String> =
        settings.apiBaseUrl.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
    val apiToken: StateFlow<String> =
        settings.apiToken.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    fun setApiBaseUrl(url: String) = viewModelScope.launch { settings.setApiBaseUrl(url) }
    fun setApiToken(token: String) = viewModelScope.launch { settings.setApiToken(token) }

    fun addEvent(name: String, startMillis: Long, endMillis: Long) =
        viewModelScope.launch { repo.addEvent(name, startMillis, endMillis) }

    fun deleteEvent(event: EventEntity) = viewModelScope.launch { repo.deleteEvent(event) }

    fun addSource(name: String) = viewModelScope.launch { repo.addSource(name) }

    fun deleteSource(source: SourceEntity) = viewModelScope.launch { repo.deleteSource(source) }
}
