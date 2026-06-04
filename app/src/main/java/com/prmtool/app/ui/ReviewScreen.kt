package com.prmtool.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ReviewScreen(
    vm: ReviewViewModel,
    onBack: () -> Unit,
    onCommitted: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Review & confirm") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (!vm.loaded) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        if (vm.notFound) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("This contact no longer exists.")
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            // Avatar + headline header.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(vm.avatarUrl)
                Spacer(Modifier.size(16.dp))
                Column {
                    Text(
                        listOf(vm.firstName, vm.lastName).filter { it.isNotBlank() }
                            .joinToString(" ").ifBlank { "(no name)" },
                        style = MaterialTheme.typography.titleLarge,
                    )
                    if (vm.headline.isNotBlank()) {
                        Text(vm.headline, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            if (vm.enrichedTags.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    vm.enrichedTags.forEach { tag ->
                        AssistChip(onClick = {}, label = { Text(prettyTag(tag)) })
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Text("Details", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))

            Field("First name", vm.firstName) { vm.firstName = it }
            Field("Last name", vm.lastName) { vm.lastName = it }
            Field("Company", vm.company) { vm.company = it }
            Field("Company domain", vm.companyDomain) { vm.companyDomain = it }
            Field("Phone", vm.number, keyboard = KeyboardType.Phone) { vm.number = it }
            Field("Job title / headline", vm.headline) { vm.headline = it }
            Field("LinkedIn URL", vm.linkedinUrl) { vm.linkedinUrl = it }
            Field("Summary", vm.summary, minLines = 3) { vm.summary = it }

            if (vm.linkedinUrl.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { runCatching { uriHandler.openUri(vm.linkedinUrl) } },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Open LinkedIn profile") }
            }

            Spacer(Modifier.height(28.dp))
            Button(
                onClick = { vm.confirm(onCommitted) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Icon(Icons.Filled.Check, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Confirm & save to contacts")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { vm.discard(onBack) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Delete, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Discard")
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun Avatar(url: String) {
    val shape = CircleShape
    if (url.isBlank()) {
        Box(
            Modifier
                .size(72.dp)
                .clip(shape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
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
            contentDescription = "Profile picture",
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(72.dp).clip(shape),
        )
    }
}

@Composable
private fun Field(
    label: String,
    value: String,
    minLines: Int = 1,
    keyboard: KeyboardType = KeyboardType.Text,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = minLines == 1,
        minLines = minLines,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = keyboard),
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(10.dp))
}

private fun prettyTag(tag: String): String = when (tag) {
    "LINKEDIN" -> "LinkedIn"
    "JOB_TITLE" -> "Job title"
    "AVATAR" -> "Photo"
    "COMPANY" -> "Company domain"
    "EMAIL" -> "Email"
    "PHONE" -> "Phone"
    "CITY" -> "City"
    else -> tag.lowercase().replaceFirstChar { it.uppercase() }
}
