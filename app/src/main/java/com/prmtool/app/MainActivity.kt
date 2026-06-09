package com.prmtool.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.prmtool.app.ui.AddContactSheet
import com.prmtool.app.ui.HomeScreen
import com.prmtool.app.ui.HomeViewModel
import com.prmtool.app.ui.PrmTheme
import com.prmtool.app.ui.ReviewScreen
import com.prmtool.app.ui.ReviewViewModel
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
                            var showAdd by remember { mutableStateOf(false) }
                            var addEpoch by remember { mutableIntStateOf(0) }
                            HomeScreen(
                                vm = vm,
                                onAdd = { addEpoch++; showAdd = true },
                                onSettings = { nav.navigate("settings") },
                                onReview = { clientId -> nav.navigate("review/$clientId") },
                            )
                            if (showAdd) {
                                AddContactSheet(epoch = addEpoch, onDismiss = { showAdd = false })
                            }
                        }
                        composable(
                            route = "review/{clientId}",
                            arguments = listOf(navArgument("clientId") { type = NavType.StringType }),
                        ) { entry ->
                            val clientId = entry.arguments?.getString("clientId").orEmpty()
                            val vm: ReviewViewModel = viewModel(factory = ReviewViewModel.factory(clientId))
                            ReviewScreen(
                                vm = vm,
                                onBack = { nav.popBackStack() },
                                onCommitted = { nav.popBackStack() },
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
