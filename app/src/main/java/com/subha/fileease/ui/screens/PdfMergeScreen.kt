package com.subha.fileease.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Exact color tokens from the provided Stitch HTML Tailwind config
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
private val PrimaryFixed = Color(0xFFDAE1FF)
private val Secondary = Color(0xFF0058BE)
private val SecondaryContainer = Color(0xFF2170E4)
private val Error = Color(0xFFBA1A1A)
private val ErrorContainer = Color(0xFFFFDAD6)
private val OutlineVariant = Color(0xFFC2C6D8)
private val Outline = Color(0xFF727687)

data class PdfFileItem(
    val id: Int,
    val name: String,
    val pages: Int,
    val sizeMb: Double
)

@Composable
fun PdfMergeScreen(onNavigateBack: () -> Unit) {
    // State Management for the UI
    var fileList by remember {
        mutableStateOf(
            listOf(
                PdfFileItem(1, "Quarterly_Report_2024.pdf", 14, 3.2),
                PdfFileItem(2, "Financial_Summary.pdf", 6, 1.1),
                PdfFileItem(3, "Appendix_A.pdf", 8, 0.84)
            )
        )
    }
    var outputName by remember { mutableStateOf("Merged_Document_2024.pdf") }
    var addToc by remember { mutableStateOf(true) }
    var normalizeSize by remember { mutableStateOf(false) }
    var isMerging by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        containerColor = Surface,
        topBar = { MergeTopBar(onNavigateBack) },
        bottomBar = {
            MergeBottomBar(isMerging) {
                // Simulate merge action UI state
                coroutineScope.launch {
                    isMerging = true
                    delay(2000)
                    isMerging = false
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
            Spacer(modifier = Modifier.height(8.dp))
            StatusBanner()

            Spacer(modifier = Modifier.height(16.dp))
            FileQueueSection(
                files = fileList,
                onClearAll = { fileList = emptyList() },
                onAddMore = {
                    val newFile = PdfFileItem(System.currentTimeMillis().toInt(), "Supplementary_Notes.pdf", 4, 0.6)
                    fileList = fileList + newFile
                },
                onRemove = { fileToRemove ->
                    fileList = fileList.filter { it.id != fileToRemove.id }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
            SummaryStrip(files = fileList)

            Spacer(modifier = Modifier.height(16.dp))
            OutputConfiguration(
                outputName = outputName,
                onNameChange = { outputName = it },
                addToc = addToc,
                onTocToggle = { addToc = !addToc },
                normalizeSize = normalizeSize,
                onNormalizeToggle = { normalizeSize = !normalizeSize }
            )

            Spacer(modifier = Modifier.height(16.dp))
            SecurityBadge()

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun MergeTopBar(onNavigateBack: () -> Unit) {
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
                Text("Merge Pdf", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = OnSurface)
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
private fun StatusBanner() {
    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(PrimaryFixed.copy(alpha = 0.6f))
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceContainerLowest),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.CallMerge, contentDescription = null, tint = Primary, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Merge Sequence Engine", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Primary)
                    Text("Arrange cards to set final page order", fontSize = 12.sp, color = OnSurfaceVariant)
                }
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(SurfaceContainerLowest)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text("v3.4 Local", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Primary)
            }
        }
    }
}

@Composable
private fun FileQueueSection(
    files: List<PdfFileItem>,
    onClearAll: () -> Unit,
    onAddMore: () -> Unit,
    onRemove: (PdfFileItem) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Source Files", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(SurfaceContainerHigh)
                        .padding(horizontal = 6.dp, vertical = 1.dp)
                ) {
                    Text("${files.size}", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Primary)
                }
            }
            Text(
                text = "Clear All",
                fontSize = 11.sp,
                color = Primary,
                modifier = Modifier.clickable { onClearAll() }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (files.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceContainerLowest)
                    .padding(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier.size(48.dp).clip(CircleShape).background(SurfaceContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.FolderOpen, contentDescription = null, tint = Primary)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Queue is empty", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = OnSurface)
                    Text("Tap below to pick PDF documents from your storage", fontSize = 12.sp, color = OnSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                files.forEachIndexed { index, file ->
                    PdfFileCard(file, index + 1) { onRemove(file) }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceContainerLowest)
                .clickable { onAddMore() }
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(Primary.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Add, contentDescription = null, tint = Primary, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add More PDFs", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Primary)
            }
        }
    }
}

@Composable
private fun PdfFileCard(file: PdfFileItem, order: Int, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 1.dp, shape = RoundedCornerShape(16.dp), spotColor = Color.Black.copy(alpha = 0.05f))
            .background(SurfaceContainerLowest, RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Outlined.DragIndicator, contentDescription = "Drag", tint = OutlineVariant, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))

        Box(
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(ErrorContainer.copy(alpha = 0.6f)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Outlined.Description, contentDescription = null, tint = Error, modifier = Modifier.size(20.dp))
                Text("PDF", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Error)
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(file.name, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = OnSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${file.pages} pages", fontSize = 12.sp, color = OnSurfaceVariant)
                Spacer(modifier = Modifier.width(6.dp))
                Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(OutlineVariant))
                Spacer(modifier = Modifier.width(6.dp))
                Text("${file.sizeMb} MB", fontSize = 12.sp, color = OnSurfaceVariant)
                Spacer(modifier = Modifier.width(6.dp))
                Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(SurfaceContainer).padding(horizontal = 6.dp, vertical = 2.dp)) {
                    Text("Order #$order", fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = Primary)
                }
            }
        }

        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable { onRemove() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.Delete, contentDescription = "Remove", tint = Outline, modifier = Modifier.size(19.dp))
        }
    }
}

@Composable
private fun SummaryStrip(files: List<PdfFileItem>) {
    val totalPages = files.sumOf { it.pages }
    val totalSize = files.sumOf { it.sizeMb }

    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.horizontalGradient(listOf(PrimaryContainer, SecondaryContainer)))
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(SurfaceContainerLowest.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.AllInbox, contentDescription = null, tint = OnPrimaryContainer)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("QUEUE ESTIMATION", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = OnPrimaryContainer.copy(alpha = 0.9f))
                    Text("${files.size} files · $totalPages pages · ~${"%.1f".format(totalSize)} MB total", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = OnPrimaryContainer)
                }
            }
            Box(
                modifier = Modifier.size(32.dp).clip(CircleShape).background(SurfaceContainerLowest.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Speed, contentDescription = null, tint = OnPrimaryContainer, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun OutputConfiguration(
    outputName: String, onNameChange: (String) -> Unit,
    addToc: Boolean, onTocToggle: () -> Unit,
    normalizeSize: Boolean, onNormalizeToggle: () -> Unit
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .shadow(1.dp, RoundedCornerShape(16.dp))
            .background(SurfaceContainerLowest, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Output Configuration", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = OnSurface)
            Box(modifier = Modifier.clip(RoundedCornerShape(50)).background(SurfaceContainer).padding(horizontal = 8.dp, vertical = 2.dp)) {
                Text("Standard PDF 1.7", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Primary)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Output File Name", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = OnSurfaceVariant)
        Spacer(modifier = Modifier.height(6.dp))

        BasicTextField(
            value = outputName,
            onValueChange = onNameChange,
            textStyle = TextStyle(fontSize = 13.sp, color = OnSurface),
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceContainerLow)
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.EditNote, contentDescription = null, tint = Primary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.weight(1f)) { innerTextField() }
                    Icon(Icons.Outlined.ModeEdit, contentDescription = null, tint = Outline, modifier = Modifier.size(18.dp))
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))
        Divider(color = SurfaceContainerHigh.copy(alpha = 0.6f))
        Spacer(modifier = Modifier.height(16.dp))

        CustomToggleRow(
            icon = Icons.Outlined.ListAlt,
            title = "Add Table of Contents",
            subtitle = "Generate bookmarks from source file names automatically",
            isChecked = addToc,
            onToggle = onTocToggle
        )

        Spacer(modifier = Modifier.height(16.dp))

        CustomToggleRow(
            icon = Icons.Outlined.AspectRatio,
            title = "Normalize Page Size",
            subtitle = "Auto-scale all varied sheets into unified A4 dimensions",
            isChecked = normalizeSize,
            onToggle = onNormalizeToggle
        )

        Spacer(modifier = Modifier.height(16.dp))

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
                Icon(Icons.Outlined.Tune, contentDescription = null, tint = Secondary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("Compression Level", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
                    Text("Smart Balanced (Recommended)", fontSize = 12.sp, color = OnSurfaceVariant)
                }
            }
            Text("Standard", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Primary)
        }
    }
}

@Composable
private fun CustomToggleRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, isChecked: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onToggle() },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Row(modifier = Modifier.weight(1f)) {
            Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(SurfaceContainer), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = Primary, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
                Text(subtitle, fontSize = 12.sp, color = OnSurfaceVariant, lineHeight = 16.sp)
            }
        }

        // Custom Pill Switch
        val offset by animateFloatAsState(targetValue = if (isChecked) 20f else 0f)
        val bgColor = if (isChecked) Primary else SurfaceContainerHighest

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
private fun SecurityBadge() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Outlined.Security, contentDescription = null, tint = Primary, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Zero Server Uploads • Encrypted sandboxed memory", fontSize = 10.sp, color = OnSurfaceVariant.copy(alpha = 0.7f))
    }
}

@Composable
private fun MergeBottomBar(isMerging: Boolean, onMergeClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceContainerLowest.copy(alpha = 0.9f))
            .shadow(elevation = 16.dp, spotColor = Color.Black.copy(alpha = 0.06f))
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (isMerging) Primary else PrimaryContainer)
                .clickable(enabled = !isMerging) { onMergeClick() },
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isMerging) {
                    CircularProgressIndicator(color = OnPrimary, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Merging documents...", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = OnPrimary)
                } else {
                    Icon(Icons.Outlined.MergeType, contentDescription = null, tint = OnPrimary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Merge & Save PDF", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = OnPrimary)
                }
            }
        }
    }
}