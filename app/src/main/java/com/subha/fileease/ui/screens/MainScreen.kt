package com.subha.fileease.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
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
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MainScreen() {
    val navController = rememberNavController()

    // Detect if the software keyboard is actively open
    val isKeyboardVisible = WindowInsets.isImeVisible

    // Track current route to hide bottom bar on sub-screens
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Define which screens are allowed to show the bottom navigation bar
    val topLevelRoutes = listOf(
        BottomNavItem.Home.route,
        BottomNavItem.Files.route,
        BottomNavItem.PdfTools.route,
        BottomNavItem.ImageTools.route
    )

    // Bar is visible only if keyboard is hidden AND we are on a top-level tab
    val isBottomBarVisible = !isKeyboardVisible && currentRoute in topLevelRoutes

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = isBottomBarVisible,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                BottomNavigationBar(navController = navController)
            }
        },
        containerColor = Color(0xFFF8FAFC) // Light theme background
    ) { innerPadding ->
        NavigationGraph(
            navController = navController,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

// 3. The Router (Swaps the screen based on which tab is clicked)
@Composable
fun NavigationGraph(navController: NavHostController, modifier: Modifier) {
    NavHost(navController = navController, startDestination = BottomNavItem.Home.route, modifier = modifier) {

        composable(BottomNavItem.Home.route) {
            HomeDashboardScreen(
                onMergeClick = { navController.navigate("pdf_merge") },
                onSplitClick = { navController.navigate("pdf_split") },
                onCompressClick = { navController.navigate("pdf_compress") },
                onImagesToPdfClick = { navController.navigate(BottomNavItem.ImageTools.route) },
                onViewAllClick = { navController.navigate(BottomNavItem.PdfTools.route) }
            )
        }

        composable(BottomNavItem.Files.route) {
            FileManagerScreen()
        }

        composable(BottomNavItem.PdfTools.route) {
            PdfToolsScreen(
                onMergeClick = { navController.navigate("pdf_merge") },
                onSplitClick = { navController.navigate("pdf_split") },
                onCompressClick = { navController.navigate("pdf_compress") },
                onRotateClick = { navController.navigate("pdf_rotate") },
                onPasswordClick = { navController.navigate("pdf_password") },
                onExtractClick = { navController.navigate("pdf_extract") }
            )
        }

        composable("pdf_merge") {
            PdfMergeScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("pdf_split") {
            PdfSplitScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("pdf_compress") {
            PdfCompressScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("pdf_rotate") {
            PdfRotateScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("pdf_password") {
            PdfPasswordScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("pdf_extract") {
            PdfExtractScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(BottomNavItem.ImageTools.route) {
            StudioScreen()
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
        contentColor = Color(0xFF0F172A),
        tonalElevation = 0.dp
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(imageVector = item.icon, contentDescription = item.title) },
                label = { Text(text = item.title) },
                selected = currentRoute == item.route,
                onClick = {
                    if (currentRoute != item.route) {
                        navController.navigate(item.route) {
                            navController.graph.startDestinationRoute?.let { route ->
                                popUpTo(route) { saveState = true }
                            }
                            launchSingleTop = true
                            restoreState = item.route != BottomNavItem.Home.route
                        }
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF0066FF),
                    selectedTextColor = Color(0xFF0066FF),
                    indicatorColor = Color(0xFFEFF6FF),
                    unselectedIconColor = Color(0xFF94A3B8),
                    unselectedTextColor = Color(0xFF94A3B8)
                )
            )
        }
    }
}