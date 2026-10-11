package com.subha.fileease.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

// 1. Define the tabs for the Bottom Navigation Bar
sealed class BottomNavItem(val route: String, val title: String, val icon: ImageVector) {
    object Home : BottomNavItem("home", "Home", Icons.Default.Home)
    object Files : BottomNavItem("files", "Files", Icons.Default.Folder)
    object PdfTools : BottomNavItem("pdf_tools", "PDFs", Icons.Default.Build)
    object ImageTools : BottomNavItem("image_tools", "Studio", Icons.Default.Image)
}

// 2. The Master Scaffold that holds the Top Content and the Bottom Bar
@Composable
fun MainScreen() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = { BottomNavigationBar(navController = navController) },
        containerColor = Color(0xFFF8FAFC) // Light theme background
    ) { innerPadding ->
        NavigationGraph(navController = navController, modifier = Modifier.padding(innerPadding))
    }
}

// 3. The Router (Swaps the screen based on which tab is clicked)
@Composable
fun NavigationGraph(navController: NavHostController, modifier: Modifier) {
    NavHost(navController = navController, startDestination = BottomNavItem.Home.route, modifier = modifier) {

        composable(BottomNavItem.Home.route) {
            HomeDashboardScreen()
        }

        composable(BottomNavItem.Files.route) {
            FileManagerScreen()
        }

        composable(BottomNavItem.PdfTools.route) {
            PdfToolsScreen()
        }

        composable(BottomNavItem.ImageTools.route) {
            // Placeholder until we build the Image Studio
            Text("Image Studio", modifier = Modifier.padding(16.dp))
        }
    }
}

// 4. The UI for the Bottom Navigation Bar
@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Files,
        BottomNavItem.PdfTools,
        BottomNavItem.ImageTools
    )

    NavigationBar(
        containerColor = Color.White,
        contentColor = Color(0xFF0F172A)
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(imageVector = item.icon, contentDescription = item.title) },
                label = { Text(text = item.title) },
                selected = currentRoute == item.route,
                onClick = {
                    navController.navigate(item.route) {
                        // Pop up to the start destination of the graph to avoid building up a large stack of destinations
                        navController.graph.startDestinationRoute?.let { route ->
                            popUpTo(route) { saveState = true }
                        }
                        // Avoid multiple copies of the same destination
                        launchSingleTop = true
                        // Restore state when reselecting a previously selected item
                        restoreState = true
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF0066FF), // Primary Blue
                    selectedTextColor = Color(0xFF0066FF),
                    indicatorColor = Color(0xFFEFF6FF), // Blue 50
                    unselectedIconColor = Color(0xFF94A3B8), // Muted Gray
                    unselectedTextColor = Color(0xFF94A3B8)
                )
            )
        }
    }
}