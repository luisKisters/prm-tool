package com.prmtool.app.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ContactForm(
    vm: AddContactViewModel,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
    onCancel: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val events by vm.allEvents.collectAsState()
    val sources by vm.allSources.collectAsState()

    val micPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) vm.toggleRecording() }

    fun onMicClick() {
        if (vm.isRecording) {
            vm.toggleRecording()
            return
        }
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) vm.toggleRecording() else micPermission.launch(Manifest.permission.RECORD_AUDIO)
    }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text("New contact", style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = vm.firstName,
            onValueChange = { vm.firstName = it },
            label = { Text("First name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = vm.lastName,
            onValueChange = { vm.lastName = it },
            label = { Text("Last name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = vm.company,
            onValueChange = { vm.company = it },
            label = { Text("Company") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = vm.number,
            onValueChange = { vm.number = it },
            label = { Text("Number") },
            singleLine = true,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = KeyboardType.Phone
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = vm.note,
            onValueChange = { vm.note = it },
            label = { Text("Note") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            FilledTonalButton(onClick = { onMicClick() }) {
                Icon(
                    if (vm.isRecording) Icons.Filled.Stop else Icons.Filled.Mic,
                    contentDescription = null
                )
                Text(
                    if (vm.isRecording) "  Stop" else "  Record",
                )
            }
            Spacer(Modifier.fillMaxWidth(0.05f))
            val status = when {
                vm.isRecording -> "Recording…"
                vm.hasVoice -> "Voice note attached"
                else -> ""
            }
            if (status.isNotEmpty()) Text(status)
        }

        if (events.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text("Events", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                events.forEach { event ->
                    FilterChip(
                        selected = event.id in vm.selectedEventIds,
                        onClick = { vm.toggleEvent(event.id) },
                        label = { Text(event.name) }
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("Source", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
        if (sources.isEmpty()) {
            Text("Add sources in Settings.")
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                sources.forEach { source ->
                    FilterChip(
                        selected = source.name in vm.selectedSources,
                        onClick = { vm.toggleSource(source.name) },
                        label = { Text(source.name) }
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onCancel != null) {
                TextButton(onClick = onCancel) { Text("Cancel") }
                Spacer(Modifier.fillMaxWidth(0.03f))
            }
            Button(
                onClick = { vm.save(onSaved) },
                enabled = vm.canSave
            ) { Text("Save & enrich") }
        }
        Spacer(Modifier.height(8.dp))
    }
}
