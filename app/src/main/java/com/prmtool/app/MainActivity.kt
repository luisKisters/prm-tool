package com.prmtool.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.prmtool.app.ui.AddContactViewModel
import com.prmtool.app.ui.ContactForm
import com.prmtool.app.ui.HomeScreen
import com.prmtool.app.ui.HomeViewModel
import com.prmtool.app.ui.PrmTheme
import com.prmtool.app.ui.SettingsScreen
import com.prmtool.app.ui.SettingsViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PrmTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val nav = rememberNavController()
                    NavHost(navController = nav, startDestination = "home") {
                        composable("home") {
                            val vm: HomeViewModel = viewModel()
                            HomeScreen(
                                vm = vm,
                                onAdd = { nav.navigate("add") },
                                onSettings = { nav.navigate("settings") }
                            )
                        }
                        composable("add") {
                            val vm: AddContactViewModel = viewModel()
                            ContactForm(
                                vm = vm,
                                onSaved = { nav.popBackStack() },
                                onCancel = { nav.popBackStack() }
                            )
                        }
                        composable("settings") {
                            val vm: SettingsViewModel = viewModel()
                            SettingsScreen(vm = vm, onBack = { nav.popBackStack() })
                        }
                    }
                }
            }
        }
    }
}
