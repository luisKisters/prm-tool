package com.prmtool.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
    onSettings: () -> Unit
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
            FloatingActionButton(onClick = onAdd) {
                Icon(Icons.Filled.Add, contentDescription = "Add contact")
            }
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
                contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(recent, key = { it.clientId }) { contact ->
                    ContactRow(contact, onRetry = { vm.retry(contact.clientId) })
                }
            }
        }
    }
}

private val dateFormat = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())

@Composable
private fun ContactRow(contact: ContactEntity, onRetry: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.fillMaxWidth(0.82f)) {
                val name = listOf(contact.firstName, contact.lastName)
                    .filter { it.isNotBlank() }.joinToString(" ")
                    .ifBlank { contact.number.ifBlank { "(no name)" } }
                Text(name, fontWeight = FontWeight.SemiBold)
                if (contact.company.isNotBlank()) {
                    Text(contact.company, style = MaterialTheme.typography.bodySmall)
                }
                if (contact.number.isNotBlank()) {
                    Text(contact.number, style = MaterialTheme.typography.bodySmall)
                }
                val meta = buildList {
                    if (contact.events.isNotBlank()) add(contact.events)
                    if (contact.sources.isNotBlank()) add(contact.sources)
                }.joinToString(" · ")
                if (meta.isNotBlank()) {
                    Text(meta, style = MaterialTheme.typography.bodySmall)
                }
                Text(
                    dateFormat.format(Date(contact.createdAt)),
                    style = MaterialTheme.typography.labelSmall
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                StatusLabel(contact.status)
                if (contact.status == SendStatus.FAILED.name) {
                    IconButton(onClick = onRetry) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Retry")
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusLabel(status: String) {
    val (text, color) = when (status) {
        SendStatus.SENT.name -> "Sent" to MaterialTheme.colorScheme.primary
        SendStatus.FAILED.name -> "Failed" to MaterialTheme.colorScheme.error
        else -> "Pending" to MaterialTheme.colorScheme.tertiary
    }
    Text(text, color = color, style = MaterialTheme.typography.labelMedium)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: SettingsViewModel, onBack: () -> Unit) {
    val events by vm.events.collectAsState()
    val sources by vm.sources.collectAsState()
    val webhookUrl by vm.webhookUrl.collectAsState()

    var urlField by androidx.compose.runtime.remember(webhookUrl) {
        androidx.compose.runtime.mutableStateOf(webhookUrl)
    }
    var showEventDialog by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var newSource by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("Webhook", style = MaterialTheme.typography.titleMedium)
                androidx.compose.material3.OutlinedTextField(
                    value = urlField,
                    onValueChange = { urlField = it },
                    label = { Text("n8n webhook URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(6.dp))
                androidx.compose.material3.Button(onClick = { vm.setWebhookUrl(urlField) }) {
                    Text("Save URL")
                }
            }

            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Events", style = MaterialTheme.typography.titleMedium)
                    androidx.compose.material3.TextButton(onClick = { showEventDialog = true }) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Text(" Add")
                    }
                }
            }
            items(events, key = { "e${it.id}" }) { event ->
                Card(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.fillMaxWidth().padding(12.dp),
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.OutlinedTextField(
                        value = newSource,
                        onValueChange = { newSource = it },
                        label = { Text("New source") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(0.7f)
                    )
                    Spacer(Modifier.fillMaxWidth(0.04f))
                    androidx.compose.material3.Button(
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
                        Modifier.fillMaxWidth().padding(12.dp),
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
