package com.prmtool.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prmtool.app.ui.AddContactViewModel
import com.prmtool.app.ui.ContactForm
import com.prmtool.app.ui.PrmTheme

/**
 * Launched by the Quick Settings tile. Uses a translucent theme so the contact form
 * floats as a modal card over whatever app is currently on screen.
 */
class TileActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PrmTheme {
                // Tapping the dimmed scrim outside the card dismisses.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { finish() },
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .heightIn(max = 640.dp)
                            // Consume clicks so taps inside the card don't dismiss.
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {},
                        shape = androidx.compose.material3.MaterialTheme.shapes.large,
                        tonalElevation = 6.dp,
                        shadowElevation = 8.dp
                    ) {
                        val vm: AddContactViewModel = viewModel()
                        ContactForm(
                            vm = vm,
                            onSaved = { finish() },
                            onCancel = { finish() }
                        )
                    }
                }
            }
        }
    }
}
