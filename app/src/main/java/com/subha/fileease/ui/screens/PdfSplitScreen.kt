package com.subha.fileease.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.draw.alpha

// Exact color tokens from the Stitch HTML Tailwind config
private val Surface = Color(0xFFFAF8FF)
private val SurfaceContainerLowest = Color(0xFFFFFFFF)
private val SurfaceContainerLow = Color(0xFFF2F3FF)
private val SurfaceContainer = Color(0xFFEAEDFF)
private val SurfaceContainerHigh = Color(0xFFE2E7FF)
private val OnSurface = Color(0xFF131B2E)
private val OnSurfaceVariant = Color(0xFF424656)
private val Primary = Color(0xFF0050CB)
private val OnPrimary = Color(0xFFFFFFFF)
private val Secondary = Color(0xFF0058BE)
private val ErrorContainer = Color(0xFFFFDAD6)
private val Error = Color(0xFFBA1A1A)
private val Outline = Color(0xFF727687)
private val OutlineVariant = Color(0xFFC2C6D8)
private val Tertiary = Color(0xFF4E5A69)

data class PageMock(val number: Int, var isSelected: Boolean)

@Composable
fun PdfSplitScreen(onNavigateBack: () -> Unit) {
    // State Management mapping the HTML JS behavior
    var selectedMode by remember { mutableStateOf("range") } // range, all, fixed
    var rangeInput by remember { mutableStateOf("1-8, 12, 15-20") }

    var saveAsZip by remember { mutableStateOf(true) }
    var deleteOriginal by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    var processingComplete by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Mocking the grid from the HTML
    val pages = remember {
        mutableStateListOf(
            PageMock(1, true), PageMock(2, true), PageMock(3, true), PageMock(4, true),
            PageMock(9, false), PageMock(10, false), PageMock(11, false), PageMock(12, true)
        )
    }

    Scaffold(
        containerColor = Surface,
        topBar = { SplitTopBar(onNavigateBack) },
        bottomBar = {
            SplitBottomBar(
                mode = selectedMode,
                isProcessing = isProcessing,
                isComplete = processingComplete,
                onSplitClick = {
                    coroutineScope.launch {
                        isProcessing = true
                        delay(1200) // Simulate processing time from HTML
                        isProcessing = false
                        processingComplete = true
                        delay(2500) // Show success state
                        processingComplete = false
                    }
                }
            )
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
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Screen Sub-Header
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Outlined.CallSplit, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
                        Text("Split Mode & Page Range", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = OnSurface)
                    }
                    Text("Separate multi-page documents locally without quality loss", fontSize = 13.sp, color = OnSurfaceVariant)
                }

                ActiveFileCard()

                SplitModeTabs(
                    selectedMode = selectedMode,
                    onModeSelect = { selectedMode = it }
                )

                RangeInputSection(
                    inputValue = rangeInput,
                    onValueChange = { rangeInput = it }
                )

                DocumentMapGrid(pages = pages)

                DestinationSettings(
                    saveAsZip = saveAsZip, onZipToggle = { saveAsZip = !saveAsZip },
                    deleteOriginal = deleteOriginal, onDeleteToggle = { deleteOriginal = !deleteOriginal }
                )

                ZeroCloudBanner()

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SplitTopBar(onNavigateBack: () -> Unit) {
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
                Text("Pdf Compressor", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = OnSurface) // Kept text exactly as HTML
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
private fun ActiveFileCard() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 1.dp, shape = RoundedCornerShape(12.dp), spotColor = Color.Black.copy(alpha = 0.05f))
            .background(SurfaceContainerLowest, RoundedCornerShape(12.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(ErrorContainer),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.Description, contentDescription = null, tint = Error, modifier = Modifier.size(24.dp))
                    Text("PDF", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Error)
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Annual_Strategy_Deck.pdf", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = OnSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("32 pages", fontSize = 12.sp, color = OnSurfaceVariant)
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(OutlineVariant))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("18.4 MB", fontSize = 12.sp, color = OnSurfaceVariant)
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(OutlineVariant))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Ready", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Primary)
                }
            }
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(SurfaceContainerLow)
                .clickable { }
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text("Change", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Primary)
        }
    }
}

@Composable
private fun SplitModeTabs(selectedMode: String, onModeSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Split Method", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
            Text("3 modes available", fontSize = 11.sp, color = Tertiary)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceContainerLow)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SplitModeButton(modifier = Modifier.weight(1f), title = "Page Range", icon = Icons.Outlined.DataArray, isSelected = selectedMode == "range") { onModeSelect("range") }
            SplitModeButton(modifier = Modifier.weight(1f), title = "Extract All", icon = Icons.Outlined.BurstMode, isSelected = selectedMode == "all") { onModeSelect("all") }
            SplitModeButton(modifier = Modifier.weight(1f), title = "Fixed Sets", icon = Icons.Outlined.AutoStories, isSelected = selectedMode == "fixed") { onModeSelect("fixed") }
        }
    }
}

@Composable
private fun SplitModeButton(modifier: Modifier = Modifier, title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    val bgColor = if (isSelected) SurfaceContainerLowest else Color.Transparent
    val textColor = if (isSelected) Primary else OnSurfaceVariant
    val elevation = if (isSelected) 1.dp else 0.dp

    Column(
        modifier = modifier
            .shadow(elevation, RoundedCornerShape(8.dp))
            .background(bgColor, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = null, tint = textColor, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = textColor, maxLines = 1)
    }
}

@Composable
private fun RangeInputSection(inputValue: String, onValueChange: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.dp, RoundedCornerShape(12.dp))
            .background(SurfaceContainerLowest, RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Custom Pages Specification", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(SurfaceContainerHigh)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = Primary, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("14 pages selected", fontSize = 10.sp, color = Primary)
            }
        }

        BasicTextField(
            value = inputValue,
            onValueChange = onValueChange,
            textStyle = TextStyle(fontSize = 15.sp, color = OnSurface, fontWeight = FontWeight.SemiBold),
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceContainerLow)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.Tag, contentDescription = null, tint = Outline, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.weight(1f)) { innerTextField() }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(SurfaceContainerLowest)
                            .clickable { onValueChange("1-32") }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("Select All", fontSize = 10.sp, color = Primary)
                    }
                }
            }
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Info, contentDescription = null, tint = Tertiary, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Use commas to separate single pages, hyphens for continuous page runs.", fontSize = 12.sp, color = OnSurfaceVariant)
        }
    }
}

@Composable
private fun DocumentMapGrid(pages: MutableList<PageMock>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Document Map", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = OnSurface)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Tap page to toggle", fontSize = 10.sp, color = OnSurfaceVariant)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Showing 1-12 of 32", fontSize = 12.sp, color = Tertiary)
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier.size(28.dp).clip(CircleShape).background(SurfaceContainerLow).clickable { }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.GridView, contentDescription = null, tint = OnSurfaceVariant, modifier = Modifier.size(16.dp))
                }
            }
        }

        // Using a non-scrollable grid to act just like the visual mock
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pages.take(4).forEach { page ->
                    PageThumbnail(page = page, modifier = Modifier.weight(1f)) {
                        val index = pages.indexOf(page)
                        pages[index] = page.copy(isSelected = !page.isSelected)
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pages.drop(4).take(4).forEach { page ->
                    PageThumbnail(page = page, modifier = Modifier.weight(1f)) {
                        val index = pages.indexOf(page)
                        pages[index] = page.copy(isSelected = !page.isSelected)
                    }
                }
            }
        }
    }
}

@Composable
private fun PageThumbnail(page: PageMock, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val bgOpacity = if (page.isSelected) 1f else 0.6f
    val textColor = if (page.isSelected) OnSurface else Outline
    val textWeight = if (page.isSelected) FontWeight.SemiBold else FontWeight.Medium
    val skeletonColor = if (page.isSelected) Primary else Outline

    Column(
        modifier = modifier
            .shadow(1.dp, RoundedCornerShape(12.dp))
            .background(SurfaceContainerLowest.copy(alpha = bgOpacity), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.75f)
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceContainerLow)
                .padding(6.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    val formattedNum = page.number.toString().padStart(2, '0')
                    Text("P.$formattedNum", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = textColor)

                    if (page.isSelected) {
                        Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(Primary), contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.Check, contentDescription = null, tint = OnPrimary, modifier = Modifier.size(11.dp))
                        }
                    } else {
                        Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(SurfaceContainerHigh), contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.Remove, contentDescription = null, tint = Outline, modifier = Modifier.size(10.dp))
                        }
                    }
                }

                // Skeleton mock content
                Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.alpha(if(page.isSelected) 0.7f else 0.4f)) {
                    Box(modifier = Modifier.fillMaxWidth(0.8f).height(4.dp).background(skeletonColor.copy(alpha = 0.4f), RoundedCornerShape(2.dp)))
                    Box(modifier = Modifier.fillMaxWidth().height(4.dp).background(OutlineVariant, RoundedCornerShape(2.dp)))
                    Box(modifier = Modifier.fillMaxWidth(0.6f).height(4.dp).background(OutlineVariant, RoundedCornerShape(2.dp)))
                }

                Box(modifier = Modifier.fillMaxWidth().height(4.dp).background(skeletonColor.copy(alpha = 0.2f), RoundedCornerShape(2.dp)))
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text("Page ${page.number}", fontSize = 10.sp, fontWeight = textWeight, color = textColor)
    }
}

@Composable
private fun DestinationSettings(
    saveAsZip: Boolean, onZipToggle: () -> Unit,
    deleteOriginal: Boolean, onDeleteToggle: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.dp, RoundedCornerShape(12.dp))
            .background(SurfaceContainerLowest, RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Destination Settings", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)

        CustomToggleRow(
            icon = Icons.Outlined.FolderZip,
            iconBg = SurfaceContainerHigh,
            iconTint = Primary,
            title = "Save as single ZIP archive",
            subtitle = "Keeps files neatly grouped together",
            isChecked = saveAsZip,
            onToggle = onZipToggle
        )

        Divider(color = SurfaceContainerLow)

        CustomToggleRow(
            icon = Icons.Outlined.DeleteSweep,
            iconBg = SurfaceContainerLow,
            iconTint = Tertiary,
            title = "Delete original file after split",
            subtitle = "Free up 18.4 MB of internal storage",
            isChecked = deleteOriginal,
            onToggle = onDeleteToggle
        )
    }
}

@Composable
private fun CustomToggleRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconBg: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    isChecked: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onToggle() },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(modifier = Modifier.weight(1f)) {
            Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(iconBg), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
                Text(subtitle, fontSize = 12.sp, color = OnSurfaceVariant)
            }
        }

        // Custom Pill Switch
        val offset by animateFloatAsState(targetValue = if (isChecked) 20f else 0f)
        val bgColor = if (isChecked) Primary else SurfaceContainerHigh

        Box(
            modifier = Modifier
                .width(44.dp)
                .height(24.dp)
                .clip(RoundedCornerShape(50))
                .background(bgColor)
                .padding(2.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .offset(x = offset.dp)
                    .clip(CircleShape)
                    .background(SurfaceContainerLowest)
                    .shadow(1.dp, CircleShape)
            )
        }
    }
}

@Composable
private fun ZeroCloudBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Primary.copy(alpha = 0.05f))
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(Icons.Outlined.VerifiedUser, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text("Zero Cloud Latency", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Primary)
            Text("Splitting runs completely in WebAssembly on this device. Your data never leaves your hand.", fontSize = 12.sp, color = OnSurfaceVariant, lineHeight = 16.sp)
        }
    }
}

@Composable
private fun SplitBottomBar(
    mode: String,
    isProcessing: Boolean,
    isComplete: Boolean,
    onSplitClick: () -> Unit
) {
    val buttonText = when {
        isProcessing -> "Processing on-device..."
        isComplete -> "Splitting Complete!"
        mode == "all" -> "Extract All 32 Pages"
        mode == "fixed" -> "Split into Sets of 2"
        else -> "Split Document (14 Pages)"
    }

    val buttonIcon = when {
        isComplete -> Icons.Outlined.TaskAlt
        else -> Icons.Outlined.ContentCut
    }

    val buttonColor = if (isComplete) Secondary else Primary

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceContainerLowest.copy(alpha = 0.9f))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .shadow(8.dp, RoundedCornerShape(12.dp), spotColor = buttonColor.copy(alpha = 0.4f))
                .clip(RoundedCornerShape(12.dp))
                .background(buttonColor)
                .clickable(enabled = !isProcessing && !isComplete) { onSplitClick() },
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isProcessing) {
                    CircularProgressIndicator(color = OnPrimary, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(buttonIcon, contentDescription = null, tint = OnPrimary, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(buttonText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = OnPrimary)
            }
        }
    }
}