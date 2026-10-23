package com.subha.fileease.ui.screens

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

// Premium Dual-State Icon Setup with Rounded aesthetics
sealed class BottomNavItem(val route: String, val title: String, val selectedIcon: ImageVector, val unselectedIcon: ImageVector) {
    object Home : BottomNavItem("home", "Home", Icons.Rounded.Home, Icons.Outlined.Home)
    object Files : BottomNavItem("files", "Files", Icons.Rounded.Folder, Icons.Outlined.Folder)
    object PdfTools : BottomNavItem("pdf_tools", "PDFs", Icons.Rounded.PictureAsPdf, Icons.Outlined.PictureAsPdf)
    object ImageTools : BottomNavItem("image_tools", "Studio", Icons.Rounded.GridView, Icons.Outlined.GridView)
}

fun Modifier.navBounceClick(onClick: () -> Unit): Modifier = composed {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        animationSpec = spring(stiffness = 400f, dampingRatio = 0.6f),
        label = "nav_bounce"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .pointerInput(Unit) {
            while (true) {
                awaitPointerEventScope {
                    awaitFirstDown(requireUnconsumed = false)
                    isPressed = true
                    waitForUpOrCancellation()
                    isPressed = false
                }
            }
        }
        .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        )
}

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
        containerColor = Color(0xFFFAF8FF),
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
        ) {
            NavigationGraph(
                navController = navController,
                topLevelRoutes = topLevelRoutes,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFFAF8FF))
            )

            if (isBottomBarVisible) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 24.dp)
                ) {
                    FloatingBottomNavigationBar(navController = navController)
                }
            }
        }
    }
}

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
        val screenPadding = Modifier.fillMaxSize()

        composable(BottomNavItem.Home.route) {
            Box(modifier = screenPadding) {
                HomeDashboardScreen(
                    onMergeClick = { navController.navigate("pdf_merge") },
                    onSplitClick = { navController.navigate("pdf_split") },
                    onCompressClick = { navController.navigate("pdf_compress") },
                    onImagesToPdfClick = { navController.navigate("images_to_pdf") },
                    onViewAllClick = { navController.navigate(BottomNavItem.PdfTools.route) },
                    onRecentViewAllClick = { navController.navigate(BottomNavItem.Files.route) }
                )
            }
        }

        composable(BottomNavItem.Files.route) {
            Box(modifier = screenPadding) {
                FileManagerScreen()
            }
        }

        composable(BottomNavItem.PdfTools.route) {
            Box(modifier = screenPadding) {
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
            Box(modifier = screenPadding) {
                StudioScreen(
                    onImagesToPdfClick = { navController.navigate("images_to_pdf") },
                    onPdfToImagesClick = { navController.navigate("pdf_to_images") },
                    onJpgToPngClick = { navController.navigate("jpg_to_png") },
                    onTextToPdfClick = { navController.navigate("text_to_pdf") },
                    onCropImageClick = { navController.navigate("crop_image") },
                    onResizeImageClick = { navController.navigate("resize_image") },
                    onCompressImageClick = { navController.navigate("image_compress") },
                    onRotateImageClick = { navController.navigate("rotate_image") }
                )
            }
        }

        composable("pdf_merge") { PdfMergeScreen(onNavigateBack = { navController.popBackStack() }) }
        composable("pdf_split") { PdfSplitScreen(onNavigateBack = { navController.popBackStack() }) }
        composable("pdf_compress") { PdfCompressScreen(onNavigateBack = { navController.popBackStack() }) }
        composable("pdf_rotate") { PdfRotateScreen(onNavigateBack = { navController.popBackStack() }) }
        composable("pdf_password") { PdfPasswordScreen(onNavigateBack = { navController.popBackStack() }) }
        composable("pdf_extract") { PdfExtractScreen(onNavigateBack = { navController.popBackStack() }) }
        composable("images_to_pdf") { ImagesToPdfScreen(onNavigateBack = { navController.popBackStack() }) }
        composable("pdf_to_images") { PdfToImagesScreen(onNavigateBack = { navController.popBackStack() }) }
        composable("jpg_to_png") { JpgToPngScreen(onNavigateBack = { navController.popBackStack() }) }
        composable("text_to_pdf") { TextToPdfScreen(onNavigateBack = { navController.popBackStack() }) }
        composable("crop_image") { CropImageScreen(onNavigateBack = { navController.popBackStack() }) }
        composable("image_compress") { CompressImageScreen(onNavigateBack = { navController.popBackStack() }) }
        composable("resize_image") { ResizeImageScreen(onNavigateBack = { navController.popBackStack() }) }
        composable("rotate_image") { RotateImageScreen(onNavigateBack = { navController.popBackStack() }) }
    }
}

@Composable
fun FloatingBottomNavigationBar(navController: NavHostController) {
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Files,
        BottomNavItem.PdfTools,
        BottomNavItem.ImageTools
    )
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val selectedIndex = items.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 20.dp,
                shape = RoundedCornerShape(50),
                spotColor = Color(0xFF0050CB).copy(alpha = 0.12f),
                ambientColor = Color(0xFF0050CB).copy(alpha = 0.04f)
            )
            .background(Color.White, RoundedCornerShape(50))
            .padding(8.dp)
            .height(68.dp)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val tabWidth = maxWidth / items.size

            val indicatorOffset by animateDpAsState(
                targetValue = tabWidth * selectedIndex,
                animationSpec = spring(
                    stiffness = 250f,
                    dampingRatio = 0.65f
                ),
                label = "indicatorOffset"
            )

            Box(
                modifier = Modifier
                    .offset(x = indicatorOffset)
                    .width(tabWidth)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFFF2F6FF))
            )

            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { item ->
                    val isSelected = currentRoute == item.route

                    val currentIcon = if (isSelected) item.selectedIcon else item.unselectedIcon

                    val contentColor by animateColorAsState(
                        targetValue = if (isSelected) Color(0xFF2A55F5) else Color(0xFF424656),
                        animationSpec = tween(300),
                        label = "NavColorAnim"
                    )

                    val iconScale by animateFloatAsState(
                        targetValue = if (isSelected) 1.1f else 1.0f,
                        animationSpec = spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium),
                        label = "IconScaleAnim"
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(50))
                            .navBounceClick {
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
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = currentIcon,
                            contentDescription = item.title,
                            tint = contentColor,
                            modifier = Modifier
                                .graphicsLayer {
                                    scaleX = iconScale
                                    scaleY = iconScale
                                }
                                .size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = item.title,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = contentColor
                        )
                    }
                }
            }
        }
    }
}