package com.prmtool.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.prmtool.app.data.db.ContactEntity
import com.prmtool.app.data.db.SendStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    vm: HomeViewModel,
    onAdd: () -> Unit,
    onSettings: () -> Unit,
    onReview: (String) -> Unit,
) {
    val recent by vm.recent.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PRM Tool") },
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAdd,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("New contact") },
            )
        }
    ) { padding ->
        if (recent.isEmpty()) {
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { Text("No contacts yet. Tap + to add one.") }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(recent, key = { it.clientId }) { contact ->
                    ContactRow(
                        contact = contact,
                        onReview = { onReview(contact.clientId) },
                        onRetryEnrich = { vm.retryEnrich(contact.clientId) },
                        onRetryCommit = { vm.retryCommit(contact.clientId) },
                    )
                }
            }
        }
    }
}

private val dateFormat = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())

@Composable
private fun ContactRow(
    contact: ContactEntity,
    onReview: () -> Unit,
    onRetryEnrich: () -> Unit,
    onRetryCommit: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RowAvatar(contact.avatarUrl)
                Spacer(Modifier.size(14.dp))
                Column(Modifier.fillMaxWidth(0.78f)) {
                    val name = listOf(contact.firstName, contact.lastName)
                        .filter { it.isNotBlank() }.joinToString(" ")
                        .ifBlank { contact.number.ifBlank { "(no name)" } }
                    Text(name, fontWeight = FontWeight.SemiBold)
                    val subtitle = contact.headline.ifBlank { contact.company }
                    if (subtitle.isNotBlank()) {
                        Text(subtitle, style = MaterialTheme.typography.bodySmall)
                    }
                    Text(
                        dateFormat.format(Date(contact.createdAt)),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                    StatusLabel(contact.status)
                }
            }

            // Per-status action row.
            when (contact.status) {
                SendStatus.ENRICHED.name -> {
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = onReview, modifier = Modifier.fillMaxWidth()) {
                        Text("Review & confirm")
                    }
                }
                SendStatus.ENRICH_FAILED.name -> {
                    Spacer(Modifier.height(12.dp))
                    FilledTonalButton(onClick = onRetryEnrich, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.Refresh, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text("Retry enrichment")
                    }
                }
                SendStatus.COMMIT_FAILED.name -> {
                    Spacer(Modifier.height(12.dp))
                    FilledTonalButton(onClick = onRetryCommit, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.Refresh, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text("Retry save")
                    }
                }
                SendStatus.COMMITTED.name -> {
                    if (!contact.twentyUrl.isNullOrBlank()) {
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = { runCatching { uriHandler.openUri(contact.twentyUrl) } }) {
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null)
                            Spacer(Modifier.size(8.dp))
                            Text("Open in Twenty")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RowAvatar(url: String) {
    val shape = CircleShape
    if (url.isBlank()) {
        Box(
            Modifier.size(48.dp).clip(shape).background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    } else {
        AsyncImage(
            model = url,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(48.dp).clip(shape),
        )
    }
}

@Composable
private fun StatusLabel(status: String) {
    val (text, color) = when (status) {
        SendStatus.COMMITTED.name -> "Saved" to MaterialTheme.colorScheme.primary
        SendStatus.ENRICHED.name -> "Needs review" to MaterialTheme.colorScheme.tertiary
        SendStatus.ENRICHING.name -> "Enriching…" to MaterialTheme.colorScheme.secondary
        SendStatus.COMMITTING.name -> "Saving…" to MaterialTheme.colorScheme.secondary
        SendStatus.DRAFT.name -> "Queued" to MaterialTheme.colorScheme.secondary
        SendStatus.ENRICH_FAILED.name -> "Enrich failed" to MaterialTheme.colorScheme.error
        SendStatus.COMMIT_FAILED.name -> "Save failed" to MaterialTheme.colorScheme.error
        else -> status to MaterialTheme.colorScheme.secondary
    }
    Text(text, color = color, style = MaterialTheme.typography.labelMedium)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: SettingsViewModel, onBack: () -> Unit) {
    val events by vm.events.collectAsState()
    val sources by vm.sources.collectAsState()
    val apiBaseUrl by vm.apiBaseUrl.collectAsState()
    val apiToken by vm.apiToken.collectAsState()

    var urlField by remember(apiBaseUrl) { mutableStateOf(apiBaseUrl) }
    var tokenField by remember(apiToken) { mutableStateOf(apiToken) }
    var showEventDialog by remember { mutableStateOf(false) }
    var newSource by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("Backend", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = urlField,
                    onValueChange = { urlField = it },
                    label = { Text("API base URL") },
                    placeholder = { Text("https://your-app.vercel.app") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = tokenField,
                    onValueChange = { tokenField = it },
                    label = { Text("API secret token") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = {
                        vm.setApiBaseUrl(urlField)
                        vm.setApiToken(tokenField)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Save backend settings") }
            }

            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Events", style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = { showEventDialog = true }) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Text(" Add")
                    }
                }
            }
            items(events, key = { "e${it.id}" }) { event ->
                Card(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.fillMaxWidth(0.85f)) {
                            Text(event.name, fontWeight = FontWeight.SemiBold)
                            Text(
                                "${dayFormat.format(Date(event.startMillis))} – " +
                                    dayFormat.format(Date(event.endMillis)),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        IconButton(onClick = { vm.deleteEvent(event) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete")
                        }
                    }
                }
            }

            item {
                Text("Sources", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newSource,
                        onValueChange = { newSource = it },
                        label = { Text("New source") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(0.7f)
                    )
                    Spacer(Modifier.fillMaxWidth(0.04f))
                    Button(
                        onClick = {
                            if (newSource.isNotBlank()) {
                                vm.addSource(newSource.trim())
                                newSource = ""
                            }
                        }
                    ) { Text("Add") }
                }
            }
            items(sources, key = { "s${it.id}" }) { source ->
                Card(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(source.name)
                        IconButton(onClick = { vm.deleteSource(source) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete")
                        }
                    }
                }
            }
        }
    }

    if (showEventDialog) {
        AddEventDialog(
            onDismiss = { showEventDialog = false },
            onConfirm = { name, start, end ->
                vm.addEvent(name, start, end)
                showEventDialog = false
            }
        )
    }
}

private val dayFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
