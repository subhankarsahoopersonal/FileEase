package com.subha.fileease.ui.screens

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.Alignment
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
    val isKeyboardVisible = WindowInsets.isImeVisible
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val topLevelRoutes = setOf(
        BottomNavItem.Home.route,
        BottomNavItem.Files.route,
        BottomNavItem.PdfTools.route,
        BottomNavItem.ImageTools.route
    )

    val isBottomBarVisible = !isKeyboardVisible && currentRoute in topLevelRoutes

    Scaffold(
        containerColor = Color(0xFFF8FAFC)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
        ) {
            // The Router
            NavigationGraph(
                navController = navController,
                topLevelRoutes = topLevelRoutes,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF8FAFC))
            )

            // The Overlay Bottom Bar
            if (isBottomBarVisible) {
                Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                    BottomNavigationBar(navController = navController)
                }
            }
        }
    }
}

// 3. The Router (Swaps the screen based on which tab is clicked)
@Composable
fun NavigationGraph(navController: NavHostController, topLevelRoutes: Set<String>, modifier: Modifier) {
    NavHost(
        navController = navController,
        startDestination = BottomNavItem.Home.route,
        modifier = modifier,
        enterTransition = {
            if (initialState.destination.route in topLevelRoutes && targetState.destination.route in topLevelRoutes) {
                EnterTransition.None
            } else {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                )
            }
        },
        exitTransition = {
            if (initialState.destination.route in topLevelRoutes && targetState.destination.route in topLevelRoutes) {
                ExitTransition.None
            } else {
                fadeOut(animationSpec = tween(300, easing = FastOutSlowInEasing))
            }
        },
        popEnterTransition = {
            if (initialState.destination.route in topLevelRoutes && targetState.destination.route in topLevelRoutes) {
                EnterTransition.None
            } else {
                fadeIn(animationSpec = tween(300, easing = FastOutSlowInEasing))
            }
        },
        popExitTransition = {
            if (initialState.destination.route in topLevelRoutes && targetState.destination.route in topLevelRoutes) {
                ExitTransition.None
            } else {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                )
            }
        }
    ) {
        // TOP LEVEL ROUTES
        composable(BottomNavItem.Home.route) {
            Box(modifier = Modifier.fillMaxSize().padding(bottom = 90.dp)) {
                HomeDashboardScreen(
                    onMergeClick = { navController.navigate("pdf_merge") },
                    onSplitClick = { navController.navigate("pdf_split") },
                    onCompressClick = { navController.navigate("pdf_compress") },
                    onImagesToPdfClick = { navController.navigate(BottomNavItem.ImageTools.route) },
                    onViewAllClick = { navController.navigate(BottomNavItem.PdfTools.route) }
                )
            }
        }

        composable(BottomNavItem.Files.route) {
            Box(modifier = Modifier.fillMaxSize().padding(bottom = 90.dp)) {
                FileManagerScreen()
            }
        }

        composable(BottomNavItem.PdfTools.route) {
            Box(modifier = Modifier.fillMaxSize().padding(bottom = 90.dp)) {
                PdfToolsScreen(
                    onMergeClick = { navController.navigate("pdf_merge") },
                    onSplitClick = { navController.navigate("pdf_split") },
                    onCompressClick = { navController.navigate("pdf_compress") },
                    onRotateClick = { navController.navigate("pdf_rotate") },
                    onPasswordClick = { navController.navigate("pdf_password") },
                    onExtractClick = { navController.navigate("pdf_extract") }
                )
            }
        }

        composable(BottomNavItem.ImageTools.route) {
            Box(modifier = Modifier.fillMaxSize().padding(bottom = 90.dp)) {
                StudioScreen()
            }
        }

        // TOOL ROUTES
        composable("pdf_merge") {
            PdfMergeScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable("pdf_split") {
            PdfSplitScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable("pdf_compress") {
            PdfCompressScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable("pdf_rotate") {
            PdfRotateScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable("pdf_password") {
            PdfPasswordScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable("pdf_extract") {
            PdfExtractScreen(onNavigateBack = { navController.popBackStack() })
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