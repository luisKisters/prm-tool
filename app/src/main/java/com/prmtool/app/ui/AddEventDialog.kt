package com.prmtool.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val pickerFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
private const val DAY_MILLIS = 24L * 60 * 60 * 1000

/**
 * Collects an event name and a start/end date range. The start is normalised to the
 * beginning of the chosen day and the end to the end of its day, so a contact created at
 * any time during the range auto-selects the event.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEventDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, startMillis: Long, endMillis: Long) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var startDay by remember { mutableStateOf<Long?>(null) }
    var endDay by remember { mutableStateOf<Long?>(null) }
    var picking by remember { mutableStateOf<String?>(null) } // "start" | "end" | null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New event") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Event name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { picking = "start" },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(startDay?.let { "Start: ${pickerFormat.format(Date(it))}" } ?: "Pick start date")
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { picking = "end" },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(endDay?.let { "End: ${pickerFormat.format(Date(it))}" } ?: "Pick end date")
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() && startDay != null && endDay != null,
                onClick = {
                    val start = startDay!!
                    // Inclusive end: end of the chosen day.
                    val end = endDay!! + DAY_MILLIS - 1
                    onConfirm(name.trim(), start, end)
                }
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )

    if (picking != null) {
        val state = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { picking = null },
            confirmButton = {
                TextButton(onClick = {
                    val selected = state.selectedDateMillis
                    if (selected != null) {
                        if (picking == "start") startDay = selected else endDay = selected
                    }
                    picking = null
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { picking = null }) { Text("Cancel") } }
        ) {
            DatePicker(state = state)
        }
    }
}
