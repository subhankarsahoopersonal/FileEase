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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.alpha
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Exact color tokens from the Stitch HTML Tailwind config
private val Surface = Color(0xFFFAF8FF)
private val SurfaceContainerLowest = Color(0xFFFFFFFF)
private val SurfaceContainerLow = Color(0xFFF2F3FF)
private val SurfaceContainer = Color(0xFFEAEDFF)
private val SurfaceContainerHigh = Color(0xFFE2E7FF)
private val SurfaceContainerHighest = Color(0xFFDAE2FD)
private val OnSurface = Color(0xFF131B2E)
private val OnSurfaceVariant = Color(0xFF424656)
private val Primary = Color(0xFF0050CB)
private val OnPrimary = Color(0xFFFFFFFF)
private val PrimaryContainer = Color(0xFF0066FF)
private val OnPrimaryContainer = Color(0xFFF8F7FF)
private val Secondary = Color(0xFF0058BE)
private val ErrorContainer = Color(0xFFFFDAD6)
private val OnErrorContainer = Color(0xFF93000A)
private val Outline = Color(0xFF727687)
private val OutlineVariant = Color(0xFFC2C6D8)

data class ExtractPage(val id: Int, var isSelected: Boolean)

@Composable
fun PdfExtractScreen(onNavigateBack: () -> Unit) {
    // State Management for inputs and toggles
    val coroutineScope = rememberCoroutineScope()
    var isProcessing by remember { mutableStateOf(false) }
    var processingComplete by remember { mutableStateOf(false) }
    var extractionMode by remember { mutableStateOf("single") } // single, separate
    var deleteOriginal by remember { mutableStateOf(false) }

    // Page Grid State
    val pages = remember {
        mutableStateListOf(
            ExtractPage(1, false), ExtractPage(2, true), ExtractPage(3, true),
            ExtractPage(4, false), ExtractPage(5, true), ExtractPage(6, false)
        )
    }

    // Dynamic Derived States
    val selectedCount = pages.count { it.isSelected }
    var rangeInput by remember { mutableStateOf(generateRangeString(pages)) }

    // Update input box whenever selection changes
    LaunchedEffect(selectedCount) {
        rangeInput = generateRangeString(pages)
    }

    val handleApplyRange = {
        val selectedIds = parseRangeString(rangeInput)
        pages.forEachIndexed { index, page ->
            pages[index] = page.copy(isSelected = selectedIds.contains(page.id))
        }
    }

    Scaffold(
        containerColor = Surface,
        topBar = { ExtractTopBar(onNavigateBack) },
        bottomBar = {
            // Blueprint: The Command Anchor outside the scrollable area
            ExtractBottomBar(selectedCount, isProcessing, processingComplete) {
                coroutineScope.launch {
                    isProcessing = true
                    delay(1200)
                    isProcessing = false
                    processingComplete = true
                    delay(2000)
                    processingComplete = false
                }
            }
        },
        contentWindowInsets = WindowInsets(0.dp) // Blueprint: Immersive Bleed foundation
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
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp) // Enhanced spacing for depth
            ) {

                // File Overview Card
                SourceDocumentStatusCard()

                // Selection Controls & Syntax Card
                SelectionControlsCard(
                    rangeInput = rangeInput,
                    onRangeChange = { rangeInput = it },
                    selectedCount = selectedCount,
                    onApplyText = handleApplyRange,
                    onSelectAll = { pages.forEachIndexed { i, p -> pages[i] = p.copy(isSelected = true) } },
                    onSelectNone = { pages.forEachIndexed { i, p -> pages[i] = p.copy(isSelected = false) } },
                    onSelectInvert = { pages.forEachIndexed { i, p -> pages[i] = p.copy(isSelected = !p.isSelected) } }
                )

                // Visual Page Grid
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("DOCUMENT PAGES (SAMPLE 1-6)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = OnSurfaceVariant, letterSpacing = 0.5.sp)
                        Text("Tap card to toggle", fontSize = 10.sp, color = Outline)
                    }

                    // Chunked grid to prevent nested scroll crashes
                    pages.chunked(3).forEach { rowPages ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            rowPages.forEach { page ->
                                ExtractPageTile(
                                    page = page,
                                    modifier = Modifier.weight(1f),
                                    onSelectToggle = {
                                        val idx = pages.indexOfFirst { it.id == page.id }
                                        if (idx != -1) pages[idx] = page.copy(isSelected = !page.isSelected)
                                    }
                                )
                            }
                            // Fixed: Safely fill remainder of the row to avoid layout stretching
                            if (rowPages.size < 3) {
                                repeat(3 - rowPages.size) { Spacer(modifier = Modifier.weight(1f)) }
                            }
                        }
                    }

                    Box(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), contentAlignment = Alignment.Center) {
                        Box(modifier = Modifier.clip(RoundedCornerShape(50)).background(SurfaceContainer).padding(horizontal = 12.dp, vertical = 6.dp)) {
                            Text("+ 42 additional pages hidden for fast rendering", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = OnSurfaceVariant)
                        }
                    }
                }

                // Extraction Mode Radios
                ExtractionModeCard(extractionMode, onModeChange = { extractionMode = it })

                // Advanced Settings (Delete Source)
                AdvancedSettingsCard(deleteOriginal, onToggle = { deleteOriginal = !deleteOriginal })

                // Lightning Speed Banner
                SpeedBenefitCard()

                // Extra padding to ensure scrolling clears the floating bottom anchor
                Spacer(modifier = Modifier.height(112.dp))
            }
        }
    }
}

// ---- HELPER LOGIC ----
private fun generateRangeString(pages: List<ExtractPage>): String {
    val selected = pages.filter { it.isSelected }.map { it.id }.sorted()
    if (selected.isEmpty()) return ""
    return selected.joinToString(", ")
}

private fun parseRangeString(input: String): Set<Int> {
    val parts = input.split(",").map { it.trim() }
    val wanted = mutableSetOf<Int>()
    parts.forEach { part ->
        if (part.contains("-")) {
            val rangeParts = part.split("-").mapNotNull { it.toIntOrNull() }
            if (rangeParts.size == 2) {
                for (i in rangeParts[0]..rangeParts[1]) wanted.add(i)
            }
        } else {
            part.toIntOrNull()?.let { wanted.add(it) }
        }
    }
    return wanted
}
// ---- END LOGIC ----

@Composable
private fun ExtractTopBar(onNavigateBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface.copy(alpha = 0.95f))
            .shadow(elevation = 1.dp, spotColor = Color.Black.copy(alpha = 0.04f))
            .statusBarsPadding() // Blueprint: Immersive Bleed
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape).clickable { onNavigateBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = "Back", tint = OnSurface)
            }
            Spacer(modifier = Modifier.width(4.dp))
            Column {
                Text("Extract Pages", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = OnSurface)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Primary))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Offline Engine Active • 100% On-device", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Primary, letterSpacing = 0.5.sp)
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(44.dp).clip(CircleShape).clickable { }, contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Info, contentDescription = "Info", tint = OnSurfaceVariant)
            }
            Spacer(modifier = Modifier.width(4.dp))
            Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(Primary), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Person, contentDescription = "User", tint = OnPrimary, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun SourceDocumentStatusCard() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp), spotColor = Color.Black.copy(alpha = 0.08f)) // Blueprint: Z-Axis Depth
            .background(SurfaceContainerLowest, RoundedCornerShape(16.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(ErrorContainer.copy(alpha = 0.6f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.PictureAsPdf, contentDescription = null, tint = OnErrorContainer, modifier = Modifier.size(26.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Legal_Discovery_Bundle.pdf", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = OnSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(modifier = Modifier.clip(RoundedCornerShape(50)).background(SurfaceContainerHigh).padding(horizontal = 6.dp, vertical = 2.dp)) {
                        Text("v2.1", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Primary)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    Text("48 pages", fontSize = 12.sp, color = OnSurfaceVariant)
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(OutlineVariant))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("24.5 MB", fontSize = 12.sp, color = OnSurfaceVariant)
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(OutlineVariant))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Indexed", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Secondary)
                }
            }
        }
        Row(
            modifier = Modifier.clip(RoundedCornerShape(50)).background(SurfaceContainerLow).clickable { }.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.SwapHoriz, contentDescription = null, tint = Primary, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Change", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Primary)
        }
    }
}

@Composable
private fun SelectionControlsCard(
    rangeInput: String, onRangeChange: (String) -> Unit, selectedCount: Int,
    onApplyText: () -> Unit, onSelectAll: () -> Unit, onSelectNone: () -> Unit, onSelectInvert: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp), spotColor = Color.Black.copy(alpha = 0.08f)) // Blueprint: Z-Axis Depth
            .background(SurfaceContainerLowest, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Page Selection", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = OnSurface)
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier.clip(RoundedCornerShape(50)).background(PrimaryContainer).padding(horizontal = 10.dp, vertical = 4.dp)) {
                    Text("$selectedCount active", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = OnPrimaryContainer)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                QuickActionBtn("All", onSelectAll)
                QuickActionBtn("None", onSelectNone)
                QuickActionBtn("Invert", onSelectInvert)
            }
        }

        // Syntax input bar
        BasicTextField(
            value = rangeInput, onValueChange = onRangeChange,
            textStyle = TextStyle(fontSize = 13.sp, color = OnSurface),
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SurfaceContainerLow).padding(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Outlined.Keyboard, contentDescription = null, tint = OnSurfaceVariant, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (rangeInput.isEmpty()) Text("e.g. 2, 3, 5, 12-16", fontSize = 13.sp, color = Outline)
                        innerTextField()
                    }
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(PrimaryContainer).clickable { onApplyText() }.padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text("Apply", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = OnPrimary)
                    }
                }
            }
        )
    }
}

@Composable
private fun QuickActionBtn(title: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier.clip(RoundedCornerShape(50)).background(SurfaceContainerLow).clickable { onClick() }.padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = OnSurfaceVariant)
    }
}

@Composable
private fun ExtractPageTile(page: ExtractPage, modifier: Modifier = Modifier, onSelectToggle: () -> Unit) {
    val borderColor = if (page.isSelected) PrimaryContainer.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.04f)
    val elevation = if (page.isSelected) 4.dp else 1.dp
    val thumbBg = if (page.isSelected) SurfaceContainerHigh.copy(alpha = 0.6f) else SurfaceContainerLow

    val pColor = if (page.isSelected) Primary else Outline
    val checkBg = if (page.isSelected) PrimaryContainer else SurfaceContainerLowest
    val checkTint = if (page.isSelected) OnPrimary else Color.Transparent

    val labelColor = if (page.isSelected) Primary else OnSurface
    val labelWeight = if (page.isSelected) FontWeight.Bold else FontWeight.Medium

    Column(
        modifier = modifier
            .shadow(elevation, RoundedCornerShape(16.dp), spotColor = borderColor) // Blueprint: Z-Axis Depth
            .background(SurfaceContainerLowest, RoundedCornerShape(16.dp))
            .clickable { onSelectToggle() }
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().aspectRatio(0.75f).clip(RoundedCornerShape(10.dp)).background(thumbBg).padding(10.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("P.${page.id}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = pColor)
                    Box(modifier = Modifier.size(22.dp).clip(CircleShape).background(checkBg).shadow(if(page.isSelected) 2.dp else 0.dp), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Check, contentDescription = null, tint = checkTint, modifier = Modifier.size(14.dp))
                    }
                }

                // Mock Lines
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(modifier = Modifier.height(6.dp).fillMaxWidth(0.8f).clip(RoundedCornerShape(50)).background(pColor.copy(alpha = 0.4f)))
                    Box(modifier = Modifier.height(4.dp).fillMaxWidth().clip(RoundedCornerShape(50)).background(if(page.isSelected) Primary.copy(alpha = 0.3f) else OutlineVariant.copy(alpha = 0.25f)))
                    Box(modifier = Modifier.height(4.dp).fillMaxWidth(0.8f).clip(RoundedCornerShape(50)).background(if(page.isSelected) Primary.copy(alpha = 0.25f) else OutlineVariant.copy(alpha = 0.25f)))
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.height(4.dp).width(24.dp).clip(RoundedCornerShape(50)).background(pColor.copy(alpha = 0.4f)))
                    Text("Cover", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = pColor)
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text("Page ${page.id}", fontSize = 12.sp, fontWeight = labelWeight, color = labelColor)
    }
}

@Composable
private fun ExtractionModeCard(extractionMode: String, onModeChange: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp), spotColor = Color.Black.copy(alpha = 0.08f)) // Blueprint: Z-Axis Depth
            .background(SurfaceContainerLowest, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Extraction Mode", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = OnSurface)
            Icon(Icons.Outlined.Tune, contentDescription = null, tint = Outline, modifier = Modifier.size(20.dp))
        }

        // Radio 1: Combined
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if(extractionMode == "single") SurfaceContainerLow else Color.Transparent).clickable { onModeChange("single") }.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            RadioButton(selected = extractionMode == "single", onClick = { onModeChange("single") }, modifier = Modifier.size(20.dp).padding(top = 2.dp), colors = RadioButtonDefaults.colors(selectedColor = Primary, unselectedColor = Outline))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Single Combined PDF", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.clip(RoundedCornerShape(50)).background(PrimaryContainer).padding(horizontal = 8.dp, vertical = 2.dp)) {
                        Text("Recommended", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = OnPrimaryContainer)
                    }
                }
                Text("Merges the extracted sequence into one unified, ready-to-share document.", fontSize = 12.sp, color = OnSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
            }
        }

        // Radio 2: Separate
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if(extractionMode == "separate") SurfaceContainerLow else Color.Transparent).clickable { onModeChange("separate") }.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            RadioButton(selected = extractionMode == "separate", onClick = { onModeChange("separate") }, modifier = Modifier.size(20.dp).padding(top = 2.dp), colors = RadioButtonDefaults.colors(selectedColor = Primary, unselectedColor = Outline))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("Separate Pages (ZIP Archive)", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
                Text("Produces individual PDF files per page packaged into a lightweight ZIP.", fontSize = 12.sp, color = OnSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

@Composable
private fun AdvancedSettingsCard(deleteOriginal: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp), spotColor = Color.Black.copy(alpha = 0.08f)) // Blueprint: Z-Axis Depth
            .background(SurfaceContainerLowest, RoundedCornerShape(16.dp))
            .clickable { onToggle() }
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(SurfaceContainerHigh), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.AutoDelete, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text("Delete pages from source", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
                Spacer(modifier = Modifier.height(2.dp))
                Text("Non-destructive duplicate created in Trash", fontSize = 12.sp, color = OnSurfaceVariant)
            }
        }
        val offset by animateFloatAsState(targetValue = if (deleteOriginal) 20f else 0f, label = "switch")
        val bgColor = if (deleteOriginal) PrimaryContainer else SurfaceContainerHigh
        Box(modifier = Modifier.width(44.dp).height(24.dp).clip(RoundedCornerShape(50)).background(bgColor).padding(2.dp)) {
            Box(modifier = Modifier.size(20.dp).offset(x = offset.dp).clip(CircleShape).background(SurfaceContainerLowest).shadow(1.dp, CircleShape))
        }
    }
}

@Composable
private fun SpeedBenefitCard() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(16.dp), spotColor = PrimaryContainer.copy(alpha = 0.25f))
            .background(Brush.linearGradient(listOf(PrimaryContainer, Secondary)), RoundedCornerShape(16.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Bolt, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text("Zero Cloud Upload", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(2.dp))
                Text("Processed instantly in device WebAssembly sandbox.", fontSize = 13.sp, color = Color.White.copy(alpha = 0.8f))
            }
        }
        Text("0.3s", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

@Composable
private fun ExtractBottomBar(selectedCount: Int, isProcessing: Boolean, isComplete: Boolean, onClick: () -> Unit) {
    val estSize = "%.1f".format(selectedCount * 0.47)
    val buttonText = when {
        isProcessing -> "Generating PDF..."
        isComplete -> "Saved to Downloads"
        selectedCount == 0 -> "Select Pages to Extract"
        else -> "Extract $selectedCount ${if(selectedCount==1)"Page" else "Pages"}"
    }
    val buttonIcon = when {
        isProcessing -> Icons.Outlined.Refresh
        isComplete -> Icons.Outlined.CheckCircle
        else -> Icons.Outlined.Download
    }
    val buttonColor = if (isComplete) Secondary else PrimaryContainer

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceContainerLowest.copy(alpha = 0.98f))
            .shadow(elevation = 24.dp, spotColor = OnSurface.copy(alpha = 0.1f))
            .navigationBarsPadding() // Blueprint: Anchor bottom spacing for gestures
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 16.dp, bottom = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(PrimaryContainer))
                Spacer(modifier = Modifier.width(6.dp))
                Text("$selectedCount pages selected", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
            }
            Text(if(selectedCount>0) "~$estSize MB estimated" else "0 MB", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = OnSurfaceVariant)
        }

        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
                .fillMaxWidth()
                .height(56.dp) // Blueprint: 56dp robust execution height
                .shadow(if (!isComplete && selectedCount > 0) 8.dp else 0.dp, RoundedCornerShape(16.dp), spotColor = PrimaryContainer.copy(alpha = 0.35f))
                .clip(RoundedCornerShape(16.dp))
                .background(if(selectedCount==0) buttonColor.copy(alpha = 0.5f) else buttonColor)
                .clickable(enabled = selectedCount > 0 && !isProcessing && !isComplete) { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isProcessing) {
                    CircularProgressIndicator(color = OnPrimaryContainer, modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
                } else {
                    Icon(buttonIcon, contentDescription = null, tint = OnPrimaryContainer, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(buttonText, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = OnPrimaryContainer)
            }
        }
    }
}