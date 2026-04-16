package org.smarthome.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.*
import org.smarthome.ui.screens.CommandScreen
import org.smarthome.ui.screens.DashboardScreen
import org.smarthome.ui.screens.LogScreen
import org.smarthome.viewmodel.VehicleViewModel


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                VehicleApp()
            }
        }
    }
}

// ─── Nav item model ────────────────────────────────────────────────
data class NavItem(
    val route : String,
    val label : String,
    val icon  : ImageVector
)

// ─── Main App Composable ───────────────────────────────────────────
@Composable
fun VehicleApp() {
    // One ViewModel shared across all screens
    val vm: VehicleViewModel = viewModel()

    val navController = rememberNavController()
    val currentRoute  = navController.currentBackStackEntryAsState().value
                          ?.destination?.route

    val navItems = listOf(
        NavItem("dashboard", "Dashboard", Icons.Default.Dashboard),
        NavItem("commands",  "Commands",  Icons.Default.Send),
        NavItem("log",       "Log",       Icons.Default.List)
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                navItems.forEach { item ->
                    NavigationBarItem(
                        selected = currentRoute == item.route,
                        icon     = { Icon(item.icon, contentDescription = item.label) },
                        label    = { Text(item.label) },
                        onClick  = {
                            navController.navigate(item.route) {
                                // Avoid multiple copies on back stack
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState    = true
                            }
                        }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController    = navController,
            startDestination = "dashboard"
        ) {
            composable("dashboard") { DashboardScreen(vm = vm, innerPadding = padding) }
            composable("commands")  { CommandScreen(vm = vm, innerPadding = padding) }
            composable("log")       { LogScreen(vm = vm, innerPadding = padding) }
        }
    }
}