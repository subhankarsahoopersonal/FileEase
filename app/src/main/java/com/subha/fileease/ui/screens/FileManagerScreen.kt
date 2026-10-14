package com.subha.fileease.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Exact Color Tokens
val Sky500 = Color(0xFF0EA5E9)
val Sky50 = Color(0xFFF0F9FF)
val Indigo500 = Color(0xFF6366F1)
val Indigo50 = Color(0xFFEEF2FF)
val Slate800 = Color(0xFF1E293B)
val Slate700 = Color(0xFF334155)
val Slate600 = Color(0xFF475569)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun FileManagerScreen() {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current

    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var showBottomSheet by remember { mutableStateOf(false) }
    var showRenameSheet by remember { mutableStateOf(false) }
    var showCreateFolderSheet by remember { mutableStateOf(false) }

    var selectedFileForOptions by remember { mutableStateOf<File?>(null) }
    var newFileName by remember { mutableStateOf(TextFieldValue("")) }
    var renameError by remember { mutableStateOf("") }

    val rootPath = remember { Environment.getExternalStorageDirectory() }
    var currentDir by remember { mutableStateOf(rootPath) }
    var filesList by remember { mutableStateOf<List<File>>(emptyList()) }

    var searchQuery by remember { mutableStateOf("") }
    var globalSearchResults by remember { mutableStateOf<List<File>>(emptyList()) }

    var isSearching by remember { mutableStateOf(false) }
    var isTransitioning by remember { mutableStateOf(true) }

    var selectedFiles by remember { mutableStateOf<Set<File>>(emptySet()) }
    val isSelectionMode = selectedFiles.isNotEmpty()

    val listState = rememberLazyListState()
    val scrollPositions = remember { mutableMapOf<String, Pair<Int, Int>>() }

    val purgeGhostFile = { ghostFile: File ->
        filesList = filesList.filter { it.absolutePath != ghostFile.absolutePath }
        globalSearchResults = globalSearchResults.filter { it.absolutePath != ghostFile.absolutePath }
        selectedFiles = selectedFiles - ghostFile

        coroutineScope.launch {
            snackbarHostState.showSnackbar("This file no longer exists")
        }
    }

    val changeDirectory = { newDir: File ->
        focusManager.clearFocus() // Auto-dismiss keyboard on navigation
        if (!newDir.exists()) {
            purgeGhostFile(newDir)
        } else {
            scrollPositions[currentDir.absolutePath] = Pair(
                listState.firstVisibleItemIndex,
                listState.firstVisibleItemScrollOffset
            )
            val isForwardNavigation = newDir.absolutePath.startsWith(currentDir.absolutePath) &&
                    newDir.absolutePath.length > currentDir.absolutePath.length
            if (isForwardNavigation) {
                scrollPositions.remove(newDir.absolutePath)
            }

            selectedFiles = emptySet()
            isTransitioning = true
            currentDir = newDir
            searchQuery = ""
        }
    }

    BackHandler(enabled = currentDir.absolutePath != rootPath.absolutePath || isSelectionMode) {
        if (isSelectionMode) {
            selectedFiles = emptySet()
        } else {
            val parent = currentDir.parentFile ?: rootPath
            changeDirectory(if (parent.exists()) parent else rootPath)
        }
    }

    var hasStoragePermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Environment.isExternalStorageManager()
            } else {
                ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            }
        )
    }

    val settingsLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) {
        hasStoragePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }

    val legacyLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) {
            isGranted -> hasStoragePermission = isGranted
    }

    val requestPermission = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                settingsLauncher.launch(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:${context.packageName}")
                })
            } catch (e: Exception) {
                settingsLauncher.launch(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
            }
        } else {
            legacyLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    LaunchedEffect(currentDir, hasStoragePermission) {
        if (hasStoragePermission) {
            val newFiles = withContext(Dispatchers.IO) {
                try {
                    val allFiles = currentDir.listFiles()?.toList() ?: emptyList()
                    allFiles.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase(Locale.getDefault()) }))
                } catch (e: Exception) {
                    emptyList()
                }
            }

            if (filesList.isEmpty() || newFiles.size != filesList.size) {
                filesList = newFiles
            }

            if (searchQuery.isBlank()) {
                delay(50)
                val savedScroll = scrollPositions[currentDir.absolutePath]
                if (savedScroll != null && savedScroll.first < newFiles.size) {
                    listState.scrollToItem(savedScroll.first, savedScroll.second)
                } else {
                    listState.scrollToItem(0)
                }
            }
            isTransitioning = false
        } else {
            isTransitioning = false
        }
    }

    LaunchedEffect(searchQuery, hasStoragePermission) {
        if (searchQuery.isNotBlank() && hasStoragePermission) {
            isSearching = true
            delay(300)
            val results = withContext(Dispatchers.IO) { searchFilesGlobally(context, searchQuery, currentDir) }
            globalSearchResults = results
            delay(50)
            listState.scrollToItem(0)
            isSearching = false
        } else {
            globalSearchResults = emptyList()
            isSearching = false
            if (searchQuery.isBlank() && !isTransitioning) {
                isTransitioning = true
                delay(50)
                val savedScroll = scrollPositions[currentDir.absolutePath]
                if (savedScroll != null && savedScroll.first < filesList.size) {
                    listState.scrollToItem(savedScroll.first, savedScroll.second)
                } else {
                    listState.scrollToItem(0)
                }
                isTransitioning = false
            }
        }
    }

    val displayedFiles = if (searchQuery.isNotBlank()) globalSearchResults else filesList

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Row(
                    modifier = Modifier
                        .padding(bottom = 24.dp, start = 20.dp, end = 20.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Slate800)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = data.visuals.message,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        topBar = {
            AnimatedContent(targetState = isSelectionMode, label = "TopBarAnimation") { selecting ->
                if (selecting) {
                    SelectionTopBar(
                        selectedCount = selectedFiles.size,
                        onClear = { selectedFiles = emptySet() },
                        onDeleteAll = {
                            val filesToDelete = selectedFiles.toList()

                            filesList = filesList.filter { !filesToDelete.contains(it) }
                            globalSearchResults = globalSearchResults.filter { !filesToDelete.contains(it) }
                            selectedFiles = emptySet()

                            coroutineScope.launch(Dispatchers.IO) {
                                var successCount = 0
                                filesToDelete.forEach { file ->
                                    yield()
                                    if (file.deleteRecursively()) successCount++
                                }
                                withContext(Dispatchers.Main) {
                                    snackbarHostState.showSnackbar("Deleted $successCount files")
                                }
                            }
                        }
                    )
                } else {
                    FileManagerTopBar(
                        showAddFolder = searchQuery.isBlank() && hasStoragePermission,
                        onAddFolderClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                            showCreateFolderSheet = true
                        }
                    )
                }
            }
        },
        containerColor = BgColor
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                // Clear focus & keyboard when user taps empty background space
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {
                        focusManager.clearFocus()
                    })
                }
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            FileSearchBar(
                query = searchQuery,
                isSearching = isSearching,
                onQueryChange = { searchQuery = it }
            )
            Spacer(modifier = Modifier.height(16.dp))
            BreadcrumbNavigation(
                rootPath = rootPath,
                currentDir = currentDir,
                onBreadcrumbClick = { changeDirectory(it) }
            )
            Spacer(modifier = Modifier.height(8.dp))

            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (!hasStoragePermission) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(PrimaryBlue.copy(alpha = 0.1f)).clickable { requestPermission() }.padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Outlined.Folder, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(48.dp))
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text("Storage Permission Required", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                    Text("Tap here to allow access to your files", fontSize = 13.sp, color = TextSecondary)
                                }
                            }
                        }
                    } else if (displayedFiles.isEmpty() && !isTransitioning && !isSearching) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (searchQuery.isNotEmpty()) "No matches found for \"$searchQuery\"" else "This folder is empty",
                                    fontSize = 14.sp,
                                    color = TextTertiary
                                )
                            }
                        }
                    } else {
                        items(
                            items = displayedFiles,
                            key = { file -> file.absolutePath }
                        ) { file ->
                            Box(modifier = Modifier.padding(bottom = 8.dp)) {
                                val (icon, bg, fg) = getFileIconAndColors(file)

                                val subtitle = if (searchQuery.isNotBlank()) {
                                    "In: ${file.parentFile?.name ?: "Storage"} • ${formatDate(file.lastModified())}"
                                } else if (file.isDirectory) {
                                    "Folder • ${formatDate(file.lastModified())}"
                                } else {
                                    "${formatSize(file.length())} • ${formatDate(file.lastModified())}"
                                }

                                val isSelected = selectedFiles.contains(file)

                                FileListItem(
                                    icon = icon, iconBgColor = bg, iconColor = fg,
                                    title = file.name, subtitle = subtitle,
                                    isSelected = isSelected,
                                    isSelectionMode = isSelectionMode,
                                    onOptionsClick = {
                                        focusManager.clearFocus() // Auto-dismiss keyboard on options open
                                        if (!file.exists()) purgeGhostFile(file)
                                        else {
                                            selectedFileForOptions = file
                                            showBottomSheet = true
                                        }
                                    },
                                    onItemClick = {
                                        focusManager.clearFocus() // Auto-dismiss keyboard on item tap
                                        if (!file.exists()) purgeGhostFile(file)
                                        else if (isSelectionMode) {
                                            selectedFiles = if (isSelected) selectedFiles - file else selectedFiles + file
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        } else if (file.isDirectory) {
                                            changeDirectory(file)
                                        } else {
                                            openFileWithIntent(context, file)
                                        }
                                    },
                                    onItemLongClick = {
                                        focusManager.clearFocus()
                                        if (!file.exists()) purgeGhostFile(file)
                                        else {
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                            selectedFiles = selectedFiles + file
                                        }
                                    }
                                )
                            }
                        }
                        item { Spacer(modifier = Modifier.height(100.dp)) }
                    }
                }

                if (isTransitioning || isSearching) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(BgColor),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(top = 60.dp).size(36.dp),
                            color = PrimaryBlue,
                            strokeWidth = 3.dp
                        )
                    }
                }
            }
        }
    }

    if (showBottomSheet && selectedFileForOptions != null) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            containerColor = Color.White,
            dragHandle = { BottomSheetDefaults.DragHandle(color = Slate100) }
        ) {
            FileOptionsDrawerContent(
                file = selectedFileForOptions!!,
                onDismiss = { showBottomSheet = false },
                onRename = {
                    val currentName = selectedFileForOptions?.name ?: ""
                    newFileName = TextFieldValue(
                        text = currentName,
                        selection = TextRange(0, currentName.length)
                    )
                    renameError = ""
                    showBottomSheet = false
                    showRenameSheet = true
                },
                onDelete = {
                    val fileToDelete = selectedFileForOptions!!

                    if (!fileToDelete.exists()) {
                        purgeGhostFile(fileToDelete)
                        showBottomSheet = false
                        return@FileOptionsDrawerContent
                    }

                    filesList = filesList.filter { it.absolutePath != fileToDelete.absolutePath }
                    globalSearchResults = globalSearchResults.filter { it.absolutePath != fileToDelete.absolutePath }
                    showBottomSheet = false

                    coroutineScope.launch(Dispatchers.IO) {
                        val success = fileToDelete.deleteRecursively()
                        withContext(Dispatchers.Main) {
                            if (success) snackbarHostState.showSnackbar("Deleted ${fileToDelete.name}")
                            else snackbarHostState.showSnackbar("Failed to delete ${fileToDelete.name}")
                        }
                    }
                }
            )
        }
    }

    if (showRenameSheet && selectedFileForOptions != null) {
        val renameFocusRequester = remember { FocusRequester() }
        val renameSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        LaunchedEffect(Unit) {
            delay(100)
            renameFocusRequester.requestFocus()
        }

        val attemptRename = {
            val oldFile = selectedFileForOptions!!
            val newNameStr = newFileName.text.trim()

            if (!oldFile.exists()) {
                showRenameSheet = false
                purgeGhostFile(oldFile)
            } else if (newNameStr.isBlank()) {
                renameError = "File name cannot be empty"
            } else if (newNameStr == oldFile.name) {
                renameError = "Please enter a different name"
            } else {
                val newFile = File(oldFile.parentFile, newNameStr)
                if (newFile.exists()) {
                    renameError = "A file with this name already exists"
                } else {

                    val updatedList = filesList.map { if (it.absolutePath == oldFile.absolutePath) newFile else it }
                        .sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase(Locale.getDefault()) }))

                    filesList = updatedList
                    globalSearchResults = globalSearchResults.map { if (it.absolutePath == oldFile.absolutePath) newFile else it }
                        .sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase(Locale.getDefault()) }))

                    coroutineScope.launch {
                        focusManager.clearFocus()
                        renameSheetState.hide()
                        showRenameSheet = false

                        delay(150)

                        val targetIndex = updatedList.indexOfFirst { it.absolutePath == newFile.absolutePath }
                        if (targetIndex != -1) {
                            listState.animateScrollToItem(targetIndex)
                        }
                    }

                    coroutineScope.launch(Dispatchers.IO) {
                        val success = oldFile.renameTo(newFile)
                        withContext(Dispatchers.Main) {
                            if (success) {
                                snackbarHostState.showSnackbar("Renamed successfully")
                            } else {
                                filesList = filesList.map { if (it.absolutePath == newFile.absolutePath) oldFile else it }.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase(Locale.getDefault()) }))
                                globalSearchResults = globalSearchResults.map { if (it.absolutePath == newFile.absolutePath) oldFile else it }.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase(Locale.getDefault()) }))
                                snackbarHostState.showSnackbar("Failed to rename file")
                            }
                        }
                    }
                }
            }
        }

        ModalBottomSheet(
            onDismissRequest = { showRenameSheet = false },
            sheetState = renameSheetState,
            containerColor = Color.White,
            dragHandle = { BottomSheetDefaults.DragHandle(color = Slate100) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .padding(start = 24.dp, end = 24.dp, bottom = 28.dp)
            ) {
                Text("Rename", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(16.dp))

                BasicTextField(
                    value = newFileName,
                    onValueChange = {
                        newFileName = it
                        if (renameError.isNotEmpty()) renameError = ""
                    },
                    modifier = Modifier.focusRequester(renameFocusRequester),
                    textStyle = TextStyle(fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.Medium),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { attemptRename() }),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .border(
                                    width = if (renameError.isNotEmpty()) 1.5.dp else 0.dp,
                                    color = if (renameError.isNotEmpty()) Red500 else Color.Transparent,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clip(RoundedCornerShape(12.dp))
                                .background(Slate100)
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (newFileName.text.isEmpty()) {
                                Text("Enter new name", color = TextTertiary, fontSize = 14.sp)
                            }
                            innerTextField()
                        }
                    }
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .padding(start = 4.dp, top = 4.dp),
                    contentAlignment = Alignment.TopStart
                ) {
                    if (renameError.isNotEmpty()) {
                        Text(
                            text = renameError,
                            color = Red500,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Cancel",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextTertiary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showRenameSheet = false }
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    val isEnabled = renameError.isEmpty() && newFileName.text.isNotBlank()
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isEnabled) PrimaryBlue else PrimaryBlue.copy(alpha = 0.4f))
                            .clickable(enabled = isEnabled) { attemptRename() }
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Rename", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                }
            }
        }
    }

    if (showCreateFolderSheet) {
        val createFolderSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        CreateFolderBottomSheet(
            currentDir = currentDir,
            existingFiles = filesList,
            sheetState = createFolderSheetState,
            onDismiss = { showCreateFolderSheet = false },
            onFolderCreated = { newFolder ->

                val updatedList = (filesList + newFolder).sortedWith(
                    compareBy({ !it.isDirectory }, { it.name.lowercase(Locale.getDefault()) })
                )
                filesList = updatedList

                coroutineScope.launch {
                    focusManager.clearFocus()
                    createFolderSheetState.hide()
                    showCreateFolderSheet = false

                    delay(150)

                    val targetIndex = updatedList.indexOfFirst { it.absolutePath == newFolder.absolutePath }
                    if (targetIndex != -1) {
                        listState.animateScrollToItem(targetIndex)
                    }
                }

                coroutineScope.launch(Dispatchers.IO) {
                    val success = newFolder.mkdirs()
                    withContext(Dispatchers.Main) {
                        if (success) {
                            snackbarHostState.showSnackbar("Folder created")
                        } else {
                            filesList = filesList.filter { it.absolutePath != newFolder.absolutePath }
                            haptics.performHapticFeedback(HapticFeedbackType.Reject)
                            snackbarHostState.showSnackbar("Failed to create folder")
                        }
                    }
                }
            }
        )
    }
}

// -------------------------------------------------------------
// CREATE FOLDER BOTTOM SHEET
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateFolderBottomSheet(
    currentDir: File,
    existingFiles: List<File>,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onFolderCreated: (File) -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val focusRequester = remember { FocusRequester() }

    val initialName = remember {
        var base = "New folder"
        var counter = 1
        while (existingFiles.any { it.name.equals(base, ignoreCase = true) }) {
            base = "New folder ($counter)"
            counter++
        }
        base
    }

    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(initialName, TextRange(0, initialName.length)))
    }
    var validationError by remember { mutableStateOf<String?>(null) }

    fun validate(input: String): Boolean {
        val trimmed = input.trim()
        val illegalRegex = Regex("""[/?%*:|"<>\\]""")
        return when {
            trimmed.isEmpty() -> {
                validationError = "Folder name cannot be empty"
                false
            }
            trimmed.contains(illegalRegex) -> {
                validationError = "Contains invalid characters"
                false
            }
            existingFiles.any { it.name.equals(trimmed, ignoreCase = true) } -> {
                validationError = "A folder with this name already exists"
                false
            }
            else -> {
                validationError = null
                true
            }
        }
    }

    LaunchedEffect(Unit) {
        delay(100)
        focusRequester.requestFocus()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Slate100) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(start = 24.dp, end = 24.dp, bottom = 28.dp)
        ) {
            Text(
                text = "New folder",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            BasicTextField(
                value = textFieldValue,
                onValueChange = {
                    textFieldValue = it
                    validate(it.text)
                },
                modifier = Modifier.focusRequester(focusRequester),
                textStyle = TextStyle(fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.Medium),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (validate(textFieldValue.text)) {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onFolderCreated(File(currentDir, textFieldValue.text.trim()))
                    }
                }),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .border(
                                width = if (validationError != null) 1.5.dp else 0.dp,
                                color = if (validationError != null) Red500 else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clip(RoundedCornerShape(12.dp))
                            .background(Slate100)
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        innerTextField()
                    }
                }
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .padding(start = 4.dp, top = 4.dp),
                contentAlignment = Alignment.TopStart
            ) {
                validationError?.let {
                    Text(text = it, color = Red500, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Cancel",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextTertiary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onDismiss() }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                val isEnabled = validationError == null && textFieldValue.text.isNotBlank()
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isEnabled) PrimaryBlue else PrimaryBlue.copy(alpha = 0.4f))
                        .clickable(enabled = isEnabled) {
                            if (validate(textFieldValue.text)) {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                onFolderCreated(File(currentDir, textFieldValue.text.trim()))
                            }
                        }
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Create", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                }
            }
        }
    }
}

// -------------------------------------------------------------
// NATIVE INTENT ROUTING
// -------------------------------------------------------------
private fun openFileWithIntent(context: Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val extension = file.extension.lowercase()
        val mimeType = when (extension) {
            "pdf" -> "application/pdf"
            "jpg", "jpeg", "png", "gif" -> "image/*"
            "mp4", "mkv" -> "video/*"
            "mp3", "wav" -> "audio/*"
            "txt" -> "text/plain"
            "doc", "docx" -> "application/msword"
            else -> "*/*"
        }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Open file with"))
    } catch (e: Exception) {
        Toast.makeText(context, "No app found to open this file.", Toast.LENGTH_SHORT).show()
    }
}

// -------------------------------------------------------------
// HELPER FUNCTIONS
// -------------------------------------------------------------
private suspend fun searchFilesGlobally(context: Context, query: String, currentDir: File): List<File> {
    val results = mutableSetOf<File>()

    currentDir.listFiles()?.forEach { file ->
        yield()
        if (file.name.contains(query, ignoreCase = true)) {
            results.add(file)
        }
    }

    try {
        val collection = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(MediaStore.Files.FileColumns.DATA)
        val selection = "${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("%$query%")

        context.contentResolver.query(collection, projection, selection, selectionArgs, null)?.use { cursor ->
            val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
            while (cursor.moveToNext()) {
                yield()
                val path = cursor.getString(dataCol)
                if (path != null) {
                    results.add(File(path))
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    return results.toList().sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
}

private fun formatSize(sizeInBytes: Long): String {
    val kb = sizeInBytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1 -> "%.1f GB".format(gb)
        mb >= 1 -> "%.1f MB".format(mb)
        kb >= 1 -> "%.1f KB".format(kb)
        else -> "$sizeInBytes B"
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun getFileIconAndColors(file: File): Triple<ImageVector, Color, Color> {
    if (file.isDirectory) return Triple(Icons.Outlined.Folder, Blue50, PrimaryBlue)

    return when (file.extension.lowercase(Locale.getDefault())) {
        "pdf" -> Triple(Icons.Outlined.PictureAsPdf, Red50, Red500)
        "jpg", "jpeg", "png", "gif" -> Triple(Icons.Outlined.Image, Sky50, Sky500)
        "doc", "docx", "txt" -> Triple(Icons.Outlined.Description, Indigo50, Indigo500)
        "zip", "rar" -> Triple(Icons.Outlined.FolderZip, Slate100, Slate700)
        "mp3", "wav" -> Triple(Icons.Outlined.AudioFile, Sky50, Sky500)
        "mp4", "mkv" -> Triple(Icons.Outlined.VideoFile, Indigo50, Indigo500)
        else -> Triple(Icons.Outlined.InsertDriveFile, Slate100, Slate700)
    }
}

// -------------------------------------------------------------
// UI COMPONENTS
// -------------------------------------------------------------

@Composable
fun FileManagerTopBar(
    showAddFolder: Boolean = true,
    onAddFolderClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceColor.copy(alpha = 0.95f))
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Files", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

        if (showAddFolder) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Slate100)
                    .clickable { onAddFolderClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "Add new folder", tint = Slate700, modifier = Modifier.size(20.dp))
            }
        } else {
            Spacer(modifier = Modifier.size(36.dp))
        }
    }
}

@Composable
fun SelectionTopBar(selectedCount: Int, onClear: () -> Unit, onDeleteAll: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Blue50)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Outlined.Close,
                contentDescription = "Clear",
                tint = TextPrimary,
                modifier = Modifier.size(24.dp).clip(CircleShape).clickable { onClear() }
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text("$selectedCount selected", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        }
        Row {
            Icon(
                Icons.Outlined.DeleteOutline,
                contentDescription = "Delete",
                tint = Red500,
                modifier = Modifier.size(24.dp).clip(CircleShape).clickable { onDeleteAll() }
            )
        }
    }
}

@Composable
fun FileSearchBar(query: String, isSearching: Boolean, onQueryChange: (String) -> Unit) {
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        textStyle = TextStyle(fontSize = 14.sp, color = TextPrimary),
        singleLine = true,
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Slate100.copy(alpha = 0.9f)),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp).fillMaxWidth()
                ) {
                    if (isSearching) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = PrimaryBlue, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Outlined.Search, contentDescription = "Search", tint = TextTertiary, modifier = Modifier.size(20.dp))
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        if (query.isEmpty()) {
                            Text("Search files and folders", color = TextTertiary, fontSize = 14.sp)
                        }
                        innerTextField()
                    }

                    if (query.isNotEmpty()) {
                        Icon(
                            Icons.Outlined.Close,
                            contentDescription = "Clear Search",
                            tint = TextTertiary,
                            modifier = Modifier.size(18.dp).clip(CircleShape).clickable { onQueryChange("") }
                        )
                    }
                }
            }
        }
    )
}

@Composable
fun BreadcrumbNavigation(rootPath: File, currentDir: File, onBreadcrumbClick: (File) -> Unit) {
    val relativePath = currentDir.absolutePath.removePrefix(rootPath.absolutePath).removePrefix("/")
    val segments = if (relativePath.isEmpty()) emptyList() else relativePath.split("/")

    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Internal Storage",
            color = if (segments.isEmpty()) Slate800 else PrimaryBlue,
            fontSize = 13.sp,
            fontWeight = if (segments.isEmpty()) FontWeight.SemiBold else FontWeight.Medium,
            modifier = Modifier.clickable(enabled = segments.isNotEmpty()) { onBreadcrumbClick(rootPath) }
        )

        var pathBuilder = rootPath.absolutePath
        segments.forEachIndexed { index, segment ->
            Text(" › ", color = TextTertiary, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 4.dp))
            pathBuilder += "/$segment"
            val segmentFile = File(pathBuilder)
            val isLast = index == segments.lastIndex

            Text(
                text = segment,
                color = if (isLast) Slate800 else PrimaryBlue,
                fontSize = 13.sp,
                fontWeight = if (isLast) FontWeight.SemiBold else FontWeight.Medium,
                modifier = Modifier.clickable(enabled = !isLast) { onBreadcrumbClick(segmentFile) }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileListItem(
    icon: ImageVector, iconBgColor: Color, iconColor: Color,
    title: String, subtitle: String,
    isSelected: Boolean, isSelectionMode: Boolean,
    onOptionsClick: () -> Unit, onItemClick: () -> Unit, onItemLongClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, if (isSelected) PrimaryBlue.copy(alpha = 0.5f) else Slate100.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) PrimaryBlue.copy(alpha = 0.05f) else SurfaceColor)
            .combinedClickable(
                onClick = { onItemClick() },
                onLongClick = { onItemLongClick() }
            )
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            if (isSelectionMode) {
                Box(modifier = Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                    if (isSelected) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(24.dp))
                    } else {
                        Box(modifier = Modifier.size(20.dp).border(2.dp, Slate600.copy(alpha = 0.3f), CircleShape))
                    }
                }
            } else {
                Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(iconBgColor), contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp))
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(2.dp))
                Text(subtitle, fontSize = 12.sp, color = TextTertiary)
            }
        }
        if (!isSelectionMode) {
            Box(modifier = Modifier.size(32.dp).clip(CircleShape).clickable { onOptionsClick() }, contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.MoreVert, contentDescription = "Options", tint = TextTertiary, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun FileOptionsDrawerContent(
    file: File,
    onDismiss: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    val (icon, bg, fg) = getFileIconAndColors(file)
    val subtitle = if (file.isDirectory) "Folder" else "File • ${formatSize(file.length())}"

    Column(modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 24.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(bg), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(26.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(file.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, fontSize = 12.sp, color = TextTertiary)
            }
        }

        HorizontalDivider(color = Slate100)
        Spacer(modifier = Modifier.height(8.dp))

        DrawerActionItem(icon = Icons.Outlined.Edit, title = "Rename", color = Slate800, iconColor = Slate600, onClick = onRename)
        DrawerActionItem(icon = Icons.Outlined.OpenWith, title = "Move", color = Slate800, iconColor = Slate600, onClick = { onDismiss() })
        DrawerActionItem(icon = Icons.Outlined.DeleteOutline, title = "Delete", color = Red500, iconColor = Red500, onClick = onDelete)
    }
}

@Composable
fun DrawerActionItem(icon: ImageVector, title: String, color: Color, iconColor: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 4.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = title, tint = iconColor, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = color)
    }
}