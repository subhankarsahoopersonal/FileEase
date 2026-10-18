package com.subha.fileease.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Exact color tokens
private val Surface = Color(0xFFFAF8FF)
private val SurfaceContainerLowest = Color(0xFFFFFFFF)
private val SurfaceContainerLow = Color(0xFFF2F3FF)
private val SurfaceContainer = Color(0xFFEAEDFF)
private val SurfaceContainerHigh = Color(0xFFE2E7FF)
private val OnSurface = Color(0xFF131B2E)
private val OnSurfaceVariant = Color(0xFF424656)
private val Primary = Color(0xFF0050CB)
private val OnPrimary = Color(0xFFFFFFFF)
private val PrimaryFixed = Color(0xFFDAE1FF)
private val OnPrimaryFixed = Color(0xFF001849)
private val Secondary = Color(0xFF0058BE)
private val ErrorContainer = Color(0xFFFFDAD6)
private val OnErrorContainer = Color(0xFF93000A)
private val OutlineVariant = Color(0xFFC2C6D8)
private val SurfaceVariant = Color(0xFFDAE2FD)
private val OnTertiaryFixedVariant = Color(0xFF3B4856)

// Data class for Page State
data class RotatePage(
    val id: Int,
    var isSelected: Boolean,
    var rotation: Int,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val desc1: String,
    val desc2: String
)

@Composable
fun PdfRotateScreen(onNavigateBack: () -> Unit) {
    var activeScope by remember { mutableStateOf("selected") }
    var isProcessing by remember { mutableStateOf(false) }
    var processingComplete by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Mock Data mirroring the design
    val pages = remember {
        mutableStateListOf(
            RotatePage(1, false, 0, "CAD", Icons.Outlined.Architecture, "Rev 3.2", "ISO 9001"),
            RotatePage(2, true, 90, "SCH", Icons.Outlined.AltRoute, "Section B", "Scale 1:50"),
            RotatePage(3, true, 0, "SPEC", Icons.Outlined.Schema, "Specs V2", "Part #99"),
            RotatePage(4, true, 90, "DWG", Icons.Outlined.ViewQuilt, "Foundation", "Grid A-4"),
            RotatePage(5, false, 0, "ELEC", Icons.Outlined.ElectricBolt, "Wiring #1", "240V Line"),
            RotatePage(6, true, 180, "DIAG", Icons.Outlined.PrecisionManufacturing, "Assembly", "Unit 08")
        )
    }

    val modifiedCount = pages.count { it.rotation % 360 != 0 }
    val selectedCount = pages.count { it.isSelected }

    // Helper functions for state modification
    val updateScope = { scope: String ->
        activeScope = scope
        if (scope in listOf("all", "odd", "even")) {
            pages.forEachIndexed { index, page ->
                val num = index + 1
                page.isSelected = when (scope) {
                    "all" -> true
                    "odd" -> num % 2 != 0
                    "even" -> num % 2 == 0
                    else -> page.isSelected
                }
            }
        }
    }

    val rotateActive = { degrees: Int ->
        pages.forEachIndexed { index, page ->
            val num = index + 1
            val shouldRotate = when (activeScope) {
                "all" -> true
                "selected" -> page.isSelected
                "odd" -> num % 2 != 0
                "even" -> num % 2 == 0
                else -> false
            }
            if (shouldRotate) {
                var newRot = (page.rotation + degrees) % 360
                if (newRot < 0) newRot += 360
                page.rotation = newRot
            }
        }
    }

    Scaffold(
        containerColor = Surface,
        topBar = { RotateTopBar(onNavigateBack) },
        bottomBar = {
            RotateBottomBar(isProcessing, processingComplete) {
                coroutineScope.launch {
                    isProcessing = true
                    delay(1200) // Simulated processing time
                    isProcessing = false
                    processingComplete = true
                    delay(2200)
                    processingComplete = false
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Screen Intro
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Rotate PDF", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = OnSurface)
                        Text("Fix orientation and align document blueprints", fontSize = 13.sp, color = OnSurfaceVariant)
                    }
                    Box(
                        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(PrimaryFixed),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.RotateRight, contentDescription = null, tint = Primary)
                    }
                }

                // File Info Card with Active Processing State
                FileInfoCard(isProcessing = isProcessing)

                // Quick Rotation Action Bar
                QuickRotationBar(
                    onRotateCW = { rotateActive(90) },
                    onRotateCCW = { rotateActive(-90) },
                    onRotate180 = { rotateActive(180) }
                )

                // Scope Selector
                ScopeSelectorTabs(
                    activeScope = activeScope,
                    selectedCount = selectedCount,
                    onScopeSelect = updateScope,
                    onResetAll = {
                        pages.forEach { it.rotation = 0 }
                    }
                )

                // Interactive Grid
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    pages.chunked(2).forEach { rowPages ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            rowPages.forEach { page ->
                                PageRotateTile(
                                    page = page,
                                    modifier = Modifier.weight(1f),
                                    onSelectToggle = { page.isSelected = !page.isSelected },
                                    onRotateSingle = {
                                        var newRot = (page.rotation + 90) % 360
                                        if (newRot < 0) newRot += 360
                                        page.rotation = newRot
                                    }
                                )
                            }
                            // Add spacer if the last row only has 1 item
                            if (rowPages.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                // Status Notification Bar with Math Fix
                StatusNotificationBar(modifiedCount = modifiedCount, unchangedCount = 16 - modifiedCount)

                // Decorative Pro Tip
                ProTipCard()

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun RotateTopBar(onNavigateBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceContainerLowest.copy(alpha = 0.9f))
            .shadow(elevation = 1.dp, spotColor = Color.Black.copy(alpha = 0.04f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable { onNavigateBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = "Back", tint = OnSurface)
            }
            Spacer(modifier = Modifier.width(4.dp))
            Column {
                Text("Rotate Pdf", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = OnSurface)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Primary))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Offline Engine Active • 100% On-device", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Primary, letterSpacing = 0.5.sp)
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable { },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Info, contentDescription = "Info", tint = OnSurfaceVariant)
            }
            Spacer(modifier = Modifier.width(4.dp))
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Person, contentDescription = "User", tint = OnPrimary, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun FileInfoCard(isProcessing: Boolean) {
    // Active Processing Pulse Animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alphaPulse by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = if (isProcessing) 0.4f else 1f,
        animationSpec = infiniteRepeatable(tween(800, easing = FastOutLinearInEasing), RepeatMode.Reverse), label = "alphaPulse"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.dp, RoundedCornerShape(12.dp))
            .background(SurfaceContainerLowest, RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(ErrorContainer)
                    .alpha(alphaPulse), // Applied pulse
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.PictureAsPdf, contentDescription = null, tint = OnErrorContainer, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Engineering_Schematics.pdf", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = OnSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("16 pages", fontSize = 11.sp, color = OnSurfaceVariant)
                    Text(" • ", fontSize = 11.sp, color = OnSurfaceVariant)
                    Text("6.4 MB", fontSize = 11.sp, color = OnSurfaceVariant)
                    Spacer(modifier = Modifier.width(8.dp))
                    Row(
                        modifier = Modifier.clip(RoundedCornerShape(50)).background(PrimaryFixed.copy(alpha = 0.6f)).padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.VerifiedUser, contentDescription = null, tint = Primary, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(if (isProcessing) "Saving..." else "Local", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Primary)
                    }
                }
            }
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceContainerLow)
                .clickable(enabled = !isProcessing) { }
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text("Change", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = if (isProcessing) OutlineVariant else Primary)
        }
    }
}

@Composable
private fun QuickRotationBar(onRotateCW: () -> Unit, onRotateCCW: () -> Unit, onRotate180: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("ROTATE DIRECTION", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = OnSurfaceVariant, letterSpacing = 0.5.sp, modifier = Modifier.padding(horizontal = 4.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            RotateActionButton(modifier = Modifier.weight(1f), icon = Icons.Outlined.RotateRight, title = "90° Right", subtitle = "Clockwise", onClick = onRotateCW)
            RotateActionButton(modifier = Modifier.weight(1f), icon = Icons.Outlined.RotateLeft, title = "90° Left", subtitle = "Anti Clockwise", onClick = onRotateCCW)
            RotateActionButton(modifier = Modifier.weight(1f), icon = Icons.Outlined.FlipCameraAndroid, title = "180° Flip", subtitle = "Invert Page", onClick = onRotate180)
        }
    }
}

@Composable
private fun RotateActionButton(modifier: Modifier = Modifier, icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Column(
        modifier = modifier
            .shadow(1.dp, RoundedCornerShape(12.dp))
            .background(SurfaceContainerLowest, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(SurfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Primary, modifier = Modifier.size(22.dp))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = OnSurface, textAlign = TextAlign.Center)
        Text(subtitle, fontSize = 10.sp, color = OnSurfaceVariant, textAlign = TextAlign.Center)
    }
}

@Composable
private fun ScopeSelectorTabs(activeScope: String, selectedCount: Int, onScopeSelect: (String) -> Unit, onResetAll: () -> Unit) {
    Column {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("APPLY SCOPE", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = OnSurfaceVariant, letterSpacing = 0.5.sp)
            Text("Reset All to 0°", fontSize = 10.sp, color = Primary, modifier = Modifier.clickable { onResetAll() })
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ScopePill("Selected ($selectedCount)", isActive = activeScope == "selected") { onScopeSelect("selected") }
            ScopePill("All Pages (16)", isActive = activeScope == "all") { onScopeSelect("all") }
            ScopePill("Odd Pages", isActive = activeScope == "odd") { onScopeSelect("odd") }
            ScopePill("Even Pages", isActive = activeScope == "even") { onScopeSelect("even") }
        }
    }
}

@Composable
private fun ScopePill(text: String, isActive: Boolean, onClick: () -> Unit) {
    val bgColor = if (isActive) Primary else SurfaceContainerLowest
    val textColor = if (isActive) OnPrimary else OnSurfaceVariant
    val elevation = if (isActive) 2.dp else 1.dp

    Box(
        modifier = Modifier
            .shadow(elevation, RoundedCornerShape(50))
            .background(bgColor, RoundedCornerShape(50))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        // Morph the text slightly to give feedback
        AnimatedContent(targetState = text, label = "scopeText") { targetText ->
            Text(targetText, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = textColor)
        }
    }
}

@Composable
private fun PageRotateTile(page: RotatePage, modifier: Modifier = Modifier, onSelectToggle: () -> Unit, onRotateSingle: () -> Unit) {
    val borderColor = if (page.isSelected) Primary else Color.Transparent
    val badgeBg = if (page.rotation == 0) SurfaceContainerHigh else Primary
    val badgeText = if (page.rotation == 0) OnSurfaceVariant else OnPrimary

    // Smoothly animate the rotation of the thumbnail
    val animatedRotation by animateFloatAsState(
        targetValue = page.rotation.toFloat(),
        animationSpec = tween(durationMillis = 300), label = "rotationAnim"
    )

    Column(
        modifier = modifier
            .shadow(1.dp, RoundedCornerShape(12.dp))
            .border(2.dp, borderColor, RoundedCornerShape(12.dp))
            .background(SurfaceContainerLowest, RoundedCornerShape(12.dp))
            .clickable { onSelectToggle() }
            .padding(12.dp)
    ) {
        // Top Label & Rotation Badge
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Page ${page.id}", fontSize = 11.sp, fontWeight = if(page.isSelected) FontWeight.SemiBold else FontWeight.Medium, color = if(page.isSelected) Primary else OnSurfaceVariant)
            Box(modifier = Modifier.clip(RoundedCornerShape(50)).background(badgeBg).padding(horizontal = 8.dp, vertical = 2.dp)) {
                val sign = if (page.rotation > 0 && page.rotation < 180) "+" else ""
                Text("$sign${page.rotation}°", fontSize = 10.sp, fontWeight = if(page.rotation != 0) FontWeight.Bold else FontWeight.Normal, color = badgeText)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Thumbnail Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.75f)
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceContainerLow)
                .padding(10.dp)
        ) {
            // The actual rotating content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .rotate(animatedRotation), // Dynamic Animation
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(20.dp).clip(RoundedCornerShape(4.dp)).background(if(page.isSelected) PrimaryFixed else SurfaceVariant), contentAlignment = Alignment.Center) {
                        Text(page.label, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Primary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.height(4.dp).width(32.dp).clip(RoundedCornerShape(50)).background(OutlineVariant.copy(alpha = 0.6f)))
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(modifier = Modifier.height(4.dp).fillMaxWidth().clip(RoundedCornerShape(50)).background(OutlineVariant.copy(alpha = 0.4f)))
                    Box(modifier = Modifier.height(4.dp).fillMaxWidth(0.8f).clip(RoundedCornerShape(50)).background(OutlineVariant.copy(alpha = 0.4f)))
                    Box(modifier = Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(4.dp)).background(SurfaceContainer), contentAlignment = Alignment.Center) {
                        Icon(page.icon, contentDescription = null, tint = Primary.copy(alpha = 0.8f), modifier = Modifier.size(20.dp))
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(page.desc1, fontSize = 8.sp, color = OnSurfaceVariant)
                    Text(page.desc2, fontSize = 8.sp, color = OnSurfaceVariant)
                }
            }

            // Selection Checkmark Overlay
            if (page.isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 4.dp, y = (-4).dp)
                        .size(24.dp)
                        .shadow(2.dp, CircleShape)
                        .clip(CircleShape)
                        .background(Primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Check, contentDescription = null, tint = OnPrimary, modifier = Modifier.size(16.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Bottom Controls
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            val orientationText = when (page.rotation) {
                90, 270 -> "Landscape"
                180 -> "Inverted"
                else -> "Portrait"
            }
            Text(orientationText, fontSize = 10.sp, fontWeight = if(page.isSelected) FontWeight.Medium else FontWeight.Normal, color = if(page.isSelected) Primary else OnSurfaceVariant)
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if(page.isSelected) PrimaryFixed else SurfaceContainerLow)
                    .clickable { onRotateSingle() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.RotateRight, contentDescription = null, tint = Primary, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun StatusNotificationBar(modifiedCount: Int, unchangedCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceContainerLow)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Sync, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))

            // Number Tweening: Animate the numeric counts when scopes/rotations change
            AnimatedContent(
                targetState = Pair(modifiedCount, unchangedCount),
                transitionSpec = {
                    if (targetState.first > initialState.first) {
                        (slideInVertically { height -> height } + fadeIn()).togetherWith(slideOutVertically { height -> -height } + fadeOut())
                    } else {
                        (slideInVertically { height -> -height } + fadeIn()).togetherWith(slideOutVertically { height -> height } + fadeOut())
                    }.using(SizeTransform(clip = false))
                }, label = "statusCounts"
            ) { counts ->
                Text("${counts.first} pages modified • ${counts.second} pages unchanged", fontSize = 13.sp, color = OnSurface)
            }
        }
        Row(
            modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(SurfaceContainerLowest).padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.Bolt, contentDescription = null, tint = Primary, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Lossless", fontSize = 10.sp, color = Primary)
        }
    }
}

@Composable
private fun ProTipCard() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.horizontalGradient(listOf(PrimaryFixed, SurfaceVariant)))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(36.dp).clip(CircleShape).background(SurfaceContainerLowest).shadow(1.dp, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.Lightbulb, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text("Pro Tip: Multi-Page Select", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = OnPrimaryFixed)
            Text("Tap pages directly or use Odd/Even scope pills to rotate bulk plans instantly.", fontSize = 12.sp, color = OnTertiaryFixedVariant, lineHeight = 16.sp)
        }
    }
}

@Composable
private fun RotateBottomBar(isProcessing: Boolean, isComplete: Boolean, onApplyClick: () -> Unit) {
    val buttonText = when {
        isProcessing -> "Rebuilding Blueprint PDF..."
        isComplete -> "Saved to Documents"
        else -> "Apply & Save Rotated PDF"
    }

    val buttonIcon = when {
        isComplete -> Icons.Outlined.CheckCircle
        else -> Icons.Outlined.SaveAs
    }

    val buttonColor = if (isComplete) Secondary else Primary

    // State Morphing: Animate the corner radius for the transition to a loading pill
    val cornerRadius by animateDpAsState(targetValue = if (isProcessing) 50.dp else 12.dp, label = "buttonShape")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceContainerLowest.copy(alpha = 0.95f))
            .shadow(elevation = 16.dp, spotColor = OnSurface.copy(alpha = 0.06f))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp).padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Primary))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Ready to export 16-page PDF", fontSize = 10.sp, color = OnSurfaceVariant)
            }
            Text("Original Quality Retained", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Primary)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .shadow(if (!isProcessing && !isComplete) 8.dp else 0.dp, RoundedCornerShape(cornerRadius), spotColor = buttonColor.copy(alpha = 0.35f))
                .clip(RoundedCornerShape(cornerRadius))
                .background(buttonColor)
                .clickable(enabled = !isProcessing && !isComplete) { onApplyClick() },
            contentAlignment = Alignment.Center
        ) {
            // State Morphing: Smooth crossfade between idle, processing, and complete states
            AnimatedContent(
                targetState = Pair(isProcessing, isComplete),
                label = "buttonContent"
            ) { states ->
                val (processing, complete) = states
                if (processing) {
                    CircularProgressIndicator(color = OnPrimary, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(buttonIcon, contentDescription = null, tint = OnPrimary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(buttonText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = OnPrimary)
                    }
                }
            }
        }
    }
}