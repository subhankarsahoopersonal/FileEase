package com.subha.fileease.ui.screens

import android.content.Intent
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.tom_roush.pdfbox.io.MemoryUsageSetting
import com.tom_roush.pdfbox.multipdf.Splitter
import com.tom_roush.pdfbox.pdmodel.PDDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

// Exact color tokens
private val Surface = Color(0xFFFAF8FF)
private val SurfaceContainerLowest = Color(0xFFFFFFFF)
private val SurfaceContainerLow = Color(0xFFF2F3FF)
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

data class PageItem(val number: Int, var isSelected: Boolean)

@Composable
fun PdfSplitScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var fileName by remember { mutableStateOf("") }
    var fileSize by remember { mutableStateOf("") }
    val pages = remember { mutableStateListOf<PageItem>() }

    var selectedMode by remember { mutableStateOf("range") }
    var rangeInput by remember { mutableStateOf("2") }
    var saveAsZip by remember { mutableStateOf(true) }
    var deleteOriginal by remember { mutableStateOf(false) }

    var isProcessing by remember { mutableStateOf(false) }
    var processingComplete by remember { mutableStateOf(false) }
    var splitProgress by remember { mutableFloatStateOf(0f) }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            selectedFileUri = it
            coroutineScope.launch(Dispatchers.IO) {
                var name = "Unknown.pdf"
                var sizeStr = "0 MB"

                context.contentResolver.query(it, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        name = cursor.getString(nameIndex) ?: "Unknown.pdf"
                        val sizeBytes = cursor.getLong(sizeIndex)
                        sizeStr = String.format("%.2f MB", sizeBytes / (1024.0 * 1024.0))
                    }
                }

                var pageCount = 0
                try {
                    context.contentResolver.openFileDescriptor(it, "r")?.use { fd ->
                        val renderer = PdfRenderer(fd)
                        pageCount = renderer.pageCount
                        renderer.close()
                    }
                } catch (e: Exception) { e.printStackTrace() }

                withContext(Dispatchers.Main) {
                    fileName = name
                    fileSize = sizeStr
                    pages.clear()
                    for (i in 1..pageCount) {
                        pages.add(PageItem(number = i, isSelected = true))
                    }
                }
            }
        }
    }

    val selectedCount = pages.count { it.isSelected }
    val totalPages = pages.size
    val hasFile = selectedFileUri != null

    Scaffold(
        containerColor = Surface,
        topBar = { SplitTopBar(onNavigateBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            SplitBottomBar(
                hasFile = hasFile, mode = selectedMode,
                selectedCount = selectedCount, totalPages = totalPages,
                isProcessing = isProcessing, isComplete = processingComplete,
                progress = splitProgress,
                onSplitClick = {
                    if (hasFile) {
                        coroutineScope.launch(Dispatchers.IO) {
                            isProcessing = true
                            splitProgress = 0f
                            try {
                                val cleanFileName = fileName.removeSuffix(".pdf")
                                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                                val outputDir = File(downloadsDir, "FileEase_Split_${System.currentTimeMillis()}")
                                outputDir.mkdirs()

                                val generatedFiles = mutableListOf<File>()

                                context.contentResolver.openInputStream(selectedFileUri!!)?.use { inputStream ->
                                    val document = PDDocument.load(inputStream, MemoryUsageSetting.setupMixed(50 * 1024 * 1024))

                                    when (selectedMode) {
                                        "range" -> {
                                            val newDoc = PDDocument()
                                            val selectedPages = pages.filter { it.isSelected }
                                            selectedPages.forEachIndexed { index, pageItem ->
                                                newDoc.addPage(document.getPage(pageItem.number - 1))
                                                splitProgress = (index + 1f) / selectedPages.size
                                            }
                                            val outFile = File(outputDir, "${cleanFileName}_extracted.pdf")
                                            newDoc.save(outFile)
                                            newDoc.close()
                                            generatedFiles.add(outFile)
                                        }
                                        "all" -> {
                                            val splitter = Splitter()
                                            splitter.setSplitAtPage(1)
                                            val splitDocs = splitter.split(document)
                                            splitDocs.forEachIndexed { index, doc ->
                                                val outFile = File(outputDir, "${cleanFileName}_page_${index + 1}.pdf")
                                                doc.save(outFile)
                                                doc.close()
                                                generatedFiles.add(outFile)
                                                splitProgress = (index + 1f) / splitDocs.size
                                            }
                                        }
                                        "fixed" -> {
                                            val chunkSize = rangeInput.toIntOrNull() ?: 2
                                            val splitter = Splitter()
                                            splitter.setSplitAtPage(chunkSize)
                                            val splitDocs = splitter.split(document)
                                            splitDocs.forEachIndexed { index, doc ->
                                                val outFile = File(outputDir, "${cleanFileName}_part_${index + 1}.pdf")
                                                doc.save(outFile)
                                                doc.close()
                                                generatedFiles.add(outFile)
                                                splitProgress = (index + 1f) / splitDocs.size
                                            }
                                        }
                                    }
                                    document.close()
                                }

                                var finalOutputFile: File = outputDir

                                // Zipping Pipeline
                                if (saveAsZip && generatedFiles.size > 1) {
                                    val zipFile = File(downloadsDir, "${cleanFileName}_split.zip")
                                    ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zos ->
                                        generatedFiles.forEach { file ->
                                            FileInputStream(file).use { fis ->
                                                val entry = ZipEntry(file.name)
                                                zos.putNextEntry(entry)
                                                fis.copyTo(zos)
                                                zos.closeEntry()
                                            }
                                        }
                                    }
                                    generatedFiles.forEach { it.delete() }
                                    outputDir.delete()
                                    finalOutputFile = zipFile
                                } else if (generatedFiles.size == 1) {
                                    val singleFile = generatedFiles.first()
                                    val movedFile = File(downloadsDir, singleFile.name)
                                    singleFile.renameTo(movedFile)
                                    outputDir.delete()
                                    finalOutputFile = movedFile
                                }

                                // DELETION PIPELINE (SAF Secure Delete)
                                var deletedOriginalStatus = ""
                                var wasDeleted = false
                                if (deleteOriginal && selectedFileUri != null) {
                                    try {
                                        wasDeleted = DocumentsContract.deleteDocument(context.contentResolver, selectedFileUri!!)
                                        if (wasDeleted) {
                                            deletedOriginalStatus = " • Original deleted"
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }

                                withContext(Dispatchers.Main) {
                                    // Reset UI immediately if the original file was successfully deleted
                                    if (wasDeleted) {
                                        selectedFileUri = null
                                        fileName = ""
                                        fileSize = ""
                                        pages.clear()
                                        rangeInput = "2"
                                    }

                                    isProcessing = false
                                    processingComplete = true

                                    val result = snackbarHostState.showSnackbar(
                                        message = "Success! Saved to Downloads$deletedOriginalStatus",
                                        actionLabel = if (finalOutputFile.isFile) "OPEN" else null,
                                        duration = SnackbarDuration.Long
                                    )

                                    if (result == SnackbarResult.ActionPerformed && finalOutputFile.isFile) {
                                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", finalOutputFile)
                                        val mime = if (finalOutputFile.name.endsWith(".zip")) "application/zip" else "application/pdf"
                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                            setDataAndType(uri, mime)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(intent, "Open File"))
                                    }

                                    processingComplete = false
                                    splitProgress = 0f
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    isProcessing = false
                                    snackbarHostState.showSnackbar("Split failed: ${e.localizedMessage}")
                                }
                            }
                        }
                    }
                }
            )
        },
        contentWindowInsets = WindowInsets(0.dp)
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
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Outlined.CallSplit, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
                        Text("Split Mode & Page Range", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = OnSurface)
                    }
                    Text("Separate multi-page documents locally without quality loss", fontSize = 13.sp, color = OnSurfaceVariant)
                }

                ActiveFileCard(
                    hasFile = hasFile, fileName = fileName, fileSize = fileSize,
                    totalPages = totalPages, onChangeClick = { pdfPickerLauncher.launch(arrayOf("application/pdf")) }
                )

                if (hasFile) {
                    SplitModeTabs(
                        selectedMode = selectedMode,
                        onModeSelect = { selectedMode = it }
                    )

                    if (selectedMode == "fixed") {
                        RangeInputSection(
                            title = "Pages per Split",
                            subtitle = "Enter the number of pages per document block (e.g., 2)",
                            inputValue = rangeInput,
                            onValueChange = { rangeInput = it },
                            showSelectAll = false, onSelectAll = {}
                        )
                    }

                    if (selectedMode != "all") {
                        DocumentMapGrid(
                            pages = pages,
                            isDisabled = selectedMode == "fixed",
                            onTogglePage = { index ->
                                pages[index] = pages[index].copy(isSelected = !pages[index].isSelected)
                            }
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(SurfaceContainerHigh)
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Extract All Mode: $totalPages individual PDFs will be created.", fontSize = 13.sp, color = Primary, fontWeight = FontWeight.Medium)
                        }
                    }

                    DestinationSettings(
                        saveAsZip = saveAsZip, onZipToggle = { saveAsZip = !saveAsZip },
                        deleteOriginal = deleteOriginal, onDeleteToggle = { deleteOriginal = !deleteOriginal }
                    )

                    ZeroCloudBanner()
                }

                Spacer(modifier = Modifier.height(112.dp))
            }
        }
    }
}

@Composable
private fun SplitTopBar(onNavigateBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface.copy(alpha = 0.95f))
            .shadow(elevation = 1.dp, spotColor = Color.Black.copy(alpha = 0.04f))
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
            // Removed the Column and subtitle. The title now naturally centers vertically.
            Text("Split PDF", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = OnSurface)
        }
        Box(
            modifier = Modifier.size(32.dp).clip(CircleShape).background(Primary),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.Person, contentDescription = "User", tint = OnPrimary, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun ActiveFileCard(hasFile: Boolean, fileName: String, fileSize: String, totalPages: Int, onChangeClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 1.dp, shape = RoundedCornerShape(12.dp), spotColor = Color.Black.copy(alpha = 0.05f))
            .background(SurfaceContainerLowest, RoundedCornerShape(12.dp))
            .clickable(enabled = !hasFile) { onChangeClick() }
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(if (hasFile) ErrorContainer else SurfaceContainerLow),
                contentAlignment = Alignment.Center
            ) {
                if (hasFile) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.Description, contentDescription = null, tint = Error, modifier = Modifier.size(24.dp))
                        Text("PDF", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Error)
                    }
                } else { Icon(Icons.Outlined.FolderOpen, contentDescription = null, tint = Primary, modifier = Modifier.size(24.dp)) }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                if (hasFile) {
                    Text(fileName, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = OnSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("$totalPages pages", fontSize = 12.sp, color = OnSurfaceVariant)
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(OutlineVariant))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(fileSize, fontSize = 12.sp, color = OnSurfaceVariant)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ready", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Primary)
                    }
                } else {
                    Text("No file selected", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
                    Text("Tap to browse storage", fontSize = 12.sp, color = OnSurfaceVariant)
                }
            }
        }
        Box(
            modifier = Modifier.clip(RoundedCornerShape(50)).background(SurfaceContainerLow).clickable { onChangeClick() }.padding(horizontal = 12.dp, vertical = 6.dp)
        ) { Text(if (hasFile) "Change" else "Select", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Primary) }
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
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SurfaceContainerLow).padding(4.dp),
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
    Column(
        modifier = modifier.shadow(if (isSelected) 1.dp else 0.dp, RoundedCornerShape(8.dp)).background(bgColor, RoundedCornerShape(8.dp)).clickable { onClick() }.padding(vertical = 10.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = null, tint = textColor, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = textColor, textAlign = TextAlign.Center, maxLines = 2, lineHeight = 14.sp)
    }
}

@Composable
private fun RangeInputSection(
    title: String, subtitle: String, inputValue: String, onValueChange: (String) -> Unit,
    showSelectAll: Boolean, onSelectAll: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().shadow(1.dp, RoundedCornerShape(12.dp)).background(SurfaceContainerLowest, RoundedCornerShape(12.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
        BasicTextField(
            value = inputValue, onValueChange = onValueChange,
            textStyle = TextStyle(fontSize = 15.sp, color = OnSurface, fontWeight = FontWeight.SemiBold),
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(SurfaceContainerLow).padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.Tag, contentDescription = null, tint = Outline, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.weight(1f)) { innerTextField() }
                    if (showSelectAll) {
                        Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(SurfaceContainerLowest).clickable { onSelectAll() }.padding(horizontal = 10.dp, vertical = 4.dp)) {
                            Text("Select All", fontSize = 10.sp, color = Primary)
                        }
                    }
                }
            }
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Info, contentDescription = null, tint = Tertiary, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(subtitle, fontSize = 12.sp, color = OnSurfaceVariant)
        }
    }
}

@Composable
private fun DocumentMapGrid(pages: List<PageItem>, isDisabled: Boolean = false, onTogglePage: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.alpha(if (isDisabled) 0.5f else 1f)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Document Map", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = OnSurface)
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (isDisabled) "Preview only" else "Tap page to extract", fontSize = 10.sp, color = OnSurfaceVariant)
            }
        }
        val rows = pages.chunked(4)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            rows.forEach { rowItems ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rowItems.forEach { page ->
                        val originalIndex = pages.indexOf(page)
                        PageThumbnail(page = page, modifier = Modifier.weight(1f)) {
                            if (originalIndex != -1 && !isDisabled) onTogglePage(originalIndex)
                        }
                    }
                    repeat(4 - rowItems.size) { Spacer(modifier = Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun PageThumbnail(page: PageItem, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val bgOpacity = if (page.isSelected) 1f else 0.6f
    val textColor = if (page.isSelected) OnSurface else Outline
    val skeletonColor = if (page.isSelected) Primary else Outline

    Column(
        modifier = modifier.shadow(1.dp, RoundedCornerShape(12.dp)).background(SurfaceContainerLowest.copy(alpha = bgOpacity), RoundedCornerShape(12.dp)).clickable { onClick() }.padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(0.75f).clip(RoundedCornerShape(8.dp)).background(SurfaceContainerLow).padding(6.dp)) {
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("P.${page.number}", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = textColor)
                    if (page.isSelected) { Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(Primary), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Check, contentDescription = null, tint = OnPrimary, modifier = Modifier.size(11.dp)) } }
                    else { Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(SurfaceContainerHigh), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Remove, contentDescription = null, tint = Outline, modifier = Modifier.size(10.dp)) } }
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.alpha(if(page.isSelected) 0.7f else 0.4f)) {
                    Box(modifier = Modifier.fillMaxWidth(0.8f).height(4.dp).background(skeletonColor.copy(alpha = 0.4f), RoundedCornerShape(2.dp)))
                    Box(modifier = Modifier.fillMaxWidth().height(4.dp).background(OutlineVariant, RoundedCornerShape(2.dp)))
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text("Page ${page.number}", fontSize = 10.sp, fontWeight = if (page.isSelected) FontWeight.SemiBold else FontWeight.Medium, color = textColor)
    }
}

@Composable
private fun DestinationSettings(saveAsZip: Boolean, onZipToggle: () -> Unit, deleteOriginal: Boolean, onDeleteToggle: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().shadow(1.dp, RoundedCornerShape(12.dp)).background(SurfaceContainerLowest, RoundedCornerShape(12.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Destination Settings", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
        CustomToggleRow(icon = Icons.Outlined.FolderZip, iconBg = SurfaceContainerHigh, iconTint = Primary, title = "Save as single ZIP archive", subtitle = "Keeps files neatly grouped together", isChecked = saveAsZip, onToggle = onZipToggle)
        HorizontalDivider(color = SurfaceContainerLow)
        CustomToggleRow(icon = Icons.Outlined.DeleteSweep, iconBg = SurfaceContainerLow, iconTint = Tertiary, title = "Delete original file after split", subtitle = "Free up internal storage space", isChecked = deleteOriginal, onToggle = onDeleteToggle)
    }
}

@Composable
private fun CustomToggleRow(icon: androidx.compose.ui.graphics.vector.ImageVector, iconBg: Color, iconTint: Color, title: String, subtitle: String, isChecked: Boolean, onToggle: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable { onToggle() }, horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(modifier = Modifier.weight(1f)) {
            Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(iconBg), contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp)) }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
                Text(subtitle, fontSize = 12.sp, color = OnSurfaceVariant)
            }
        }
        val offset by animateFloatAsState(targetValue = if (isChecked) 20f else 0f, label = "")
        Box(modifier = Modifier.width(44.dp).height(24.dp).clip(RoundedCornerShape(50)).background(if (isChecked) Primary else SurfaceContainerHigh).padding(2.dp)) {
            Box(modifier = Modifier.size(20.dp).offset(x = offset.dp).clip(CircleShape).background(SurfaceContainerLowest).shadow(1.dp, CircleShape))
        }
    }
}

@Composable
private fun ZeroCloudBanner() {
    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Primary.copy(alpha = 0.05f)).padding(12.dp), verticalAlignment = Alignment.Top) {
        Icon(Icons.Outlined.VerifiedUser, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text("Zero Cloud Latency", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Primary)
            Text("Splitting runs completely in WebAssembly on this device.", fontSize = 12.sp, color = OnSurfaceVariant, lineHeight = 16.sp)
        }
    }
}

@Composable
private fun SplitBottomBar(hasFile: Boolean, mode: String, selectedCount: Int, totalPages: Int, isProcessing: Boolean, isComplete: Boolean, progress: Float, onSplitClick: () -> Unit) {
    val canSplit = hasFile && (mode == "all" || mode == "fixed" || selectedCount > 0)
    val buttonText = when {
        !hasFile -> "Select a PDF to Split"
        isProcessing -> "Extracting documents..."
        isComplete -> "Splitting Complete!"
        mode == "all" -> "Extract All $totalPages Pages"
        mode == "fixed" -> "Split into Sets"
        else -> "Extract Selected ($selectedCount Pages)"
    }
    val buttonColor = if (isComplete) Secondary else if (canSplit) Primary else SurfaceContainerHigh

    Column(modifier = Modifier.fillMaxWidth().shadow(16.dp, spotColor = Color.Black.copy(alpha = 0.06f)).background(Surface.copy(alpha = 0.95f)).navigationBarsPadding()) {
        if (isProcessing) LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth(), color = Primary, trackColor = SurfaceContainerHigh)
        Box(
            modifier = Modifier.padding(16.dp).fillMaxWidth().height(48.dp).shadow(if (canSplit) 8.dp else 0.dp, RoundedCornerShape(12.dp), spotColor = buttonColor.copy(alpha = 0.4f)).clip(RoundedCornerShape(12.dp)).background(buttonColor).clickable(enabled = canSplit && !isProcessing && !isComplete) { onSplitClick() },
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(if (isComplete) Icons.Outlined.TaskAlt else if (!hasFile) Icons.Outlined.Folder else Icons.Outlined.ContentCut, contentDescription = null, tint = if (canSplit || isComplete) OnPrimary else Outline, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(buttonText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = if (canSplit || isComplete) OnPrimary else Outline)
            }
        }
    }
}