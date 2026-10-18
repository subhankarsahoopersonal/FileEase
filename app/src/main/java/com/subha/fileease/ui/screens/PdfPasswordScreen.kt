package com.subha.fileease.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EnhancedEncryption
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.tom_roush.pdfbox.io.MemoryUsageSetting
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.encryption.AccessPermission
import com.tom_roush.pdfbox.pdmodel.encryption.StandardProtectionPolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

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
private val ErrorContainer = Color(0xFFFFDAD6)
private val OnErrorContainer = Color(0xFF93000A)
private val Outline = Color(0xFF727687)
private val OutlineVariant = Color(0xFFC2C6D8)
private val Secondary = Color(0xFF0058BE)

@Composable
fun PdfPasswordScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val vibrator = remember { context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Hardware & File State Management
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var fileName by remember { mutableStateOf("") }
    var fileSizeMb by remember { mutableDoubleStateOf(0.0) }
    var filePages by remember { mutableIntStateOf(0) }
    val hasFile = selectedFileUri != null

    // State Management for inputs and toggles
    var openPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isOpenPasswordVisible by remember { mutableStateOf(false) }

    var isOwnerEnabled by remember { mutableStateOf(true) }
    var ownerPassword by remember { mutableStateOf("") }

    var allowPrinting by remember { mutableStateOf(true) }
    var allowCopying by remember { mutableStateOf(false) }
    var allowAnnotations by remember { mutableStateOf(true) }

    var isProcessing by remember { mutableStateOf(false) }
    var isComplete by remember { mutableStateOf(false) }

    // The Document Picker & Background Metadata Extraction
    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            selectedFileUri = it
            coroutineScope.launch(Dispatchers.IO) {
                var name = "Unknown.pdf"
                var sizeBytes = 0L

                context.contentResolver.query(it, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        name = cursor.getString(nameIndex) ?: "Unknown.pdf"
                        sizeBytes = cursor.getLong(sizeIndex)
                    }
                }

                var pages = 0
                try {
                    context.contentResolver.openFileDescriptor(it, "r")?.use { fd ->
                        val renderer = PdfRenderer(fd)
                        pages = renderer.pageCount
                        renderer.close()
                    }
                } catch (e: Exception) { e.printStackTrace() }

                withContext(Dispatchers.Main) {
                    fileName = name
                    fileSizeMb = Math.round((sizeBytes / (1024.0 * 1024.0)) * 10.0) / 10.0
                    filePages = pages
                }
            }
        }
    }

    Scaffold(
        containerColor = Surface,
        topBar = { PasswordTopBar(onNavigateBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            PasswordBottomBar(isProcessing, isComplete, hasFile) {
                if (hasFile) {
                    if (openPassword.isBlank()) {
                        coroutineScope.launch { snackbarHostState.showSnackbar("Open Password is required") }
                        return@PasswordBottomBar
                    }
                    if (openPassword != confirmPassword) {
                        coroutineScope.launch { snackbarHostState.showSnackbar("Passwords do not match") }
                        return@PasswordBottomBar
                    }

                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    coroutineScope.launch(Dispatchers.IO) {
                        isProcessing = true
                        try {
                            val cleanFileName = fileName.removeSuffix(".pdf")
                            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                            val outputFile = File(downloadsDir, "${cleanFileName}_secured_${System.currentTimeMillis()}.pdf")

                            // PDFBox Cryptographic Engine
                            context.contentResolver.openInputStream(selectedFileUri!!)?.use { inputStream ->
                                val document = PDDocument.load(inputStream, MemoryUsageSetting.setupMixed(50 * 1024 * 1024))

                                val ap = AccessPermission()
                                ap.setCanPrint(allowPrinting)
                                ap.setCanExtractContent(allowCopying)
                                ap.setCanModifyAnnotations(allowAnnotations)

                                // If owner password is empty/disabled, fallback to the open password
                                val actualOwnerPwd = if (isOwnerEnabled && ownerPassword.isNotBlank()) ownerPassword else openPassword

                                val spp = StandardProtectionPolicy(actualOwnerPwd, openPassword, ap)
                                spp.encryptionKeyLength = 256 // Enforce maximum AES-256 security

                                document.protect(spp)
                                document.save(outputFile)
                                document.close()
                            }

                            withContext(Dispatchers.Main) {
                                isProcessing = false
                                isComplete = true

                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    val timings = longArrayOf(0, 80, 100, 120)
                                    val amplitudes = intArrayOf(0, 150, 0, 255)
                                    vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                                } else {
                                    @Suppress("DEPRECATION")
                                    vibrator.vibrate(longArrayOf(0, 80, 100, 120), -1)
                                }

                                val result = snackbarHostState.showSnackbar(
                                    message = "Success! Secured PDF saved to Downloads",
                                    actionLabel = "OPEN",
                                    duration = SnackbarDuration.Long
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", outputFile)
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(uri, "application/pdf")
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Open File"))
                                }
                                delay(1500)
                                isComplete = false
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                isProcessing = false
                                snackbarHostState.showSnackbar("Encryption failed: ${e.localizedMessage}")
                            }
                        }
                    }
                } else {
                    pdfPickerLauncher.launch(arrayOf("application/pdf"))
                }
            }
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
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FileOverviewCard(
                    hasFile = hasFile,
                    fileName = fileName,
                    fileSizeMb = fileSizeMb,
                    filePages = filePages,
                    onChangeClick = { pdfPickerLauncher.launch(arrayOf("application/pdf")) }
                )

                if (hasFile) {
                    SecurityBadgeBanner()

                    PasswordSetupCard(
                        openPassword = openPassword,
                        onOpenPasswordChange = { openPassword = it },
                        confirmPassword = confirmPassword,
                        onConfirmPasswordChange = { confirmPassword = it },
                        isVisible = isOpenPasswordVisible,
                        onVisibilityToggle = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            isOpenPasswordVisible = !isOpenPasswordVisible
                        },
                        isOwnerEnabled = isOwnerEnabled,
                        onOwnerToggle = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            isOwnerEnabled = !isOwnerEnabled
                        },
                        ownerPassword = ownerPassword,
                        onOwnerPasswordChange = { ownerPassword = it }
                    )

                    PermissionsCard(
                        allowPrinting = allowPrinting,
                        onPrintToggle = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            allowPrinting = !allowPrinting
                        },
                        allowCopying = allowCopying,
                        onCopyToggle = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            allowCopying = !allowCopying
                        },
                        allowAnnotations = allowAnnotations,
                        onAnnotToggle = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            allowAnnotations = !allowAnnotations
                        }
                    )
                }

                Spacer(modifier = Modifier.height(112.dp))
            }
        }
    }
}

@Composable
private fun PasswordTopBar(onNavigateBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceContainerLowest.copy(alpha = 0.95f))
            .shadow(elevation = 1.dp, spotColor = Color.Black.copy(alpha = 0.04f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .clickable { onNavigateBack() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.ArrowBack, contentDescription = "Back", tint = OnSurface)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text("Pdf Password", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = OnSurface)
    }
}

@Composable
private fun FileOverviewCard(hasFile: Boolean, fileName: String, fileSizeMb: Double, filePages: Int, onChangeClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp), spotColor = Color.Black.copy(alpha = 0.08f))
            .background(SurfaceContainerLowest, RoundedCornerShape(16.dp))
            .clickable(enabled = !hasFile) { onChangeClick() }
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (hasFile) ErrorContainer else SurfaceContainerLow),
                contentAlignment = Alignment.Center
            ) {
                if (hasFile) {
                    Icon(Icons.Outlined.PictureAsPdf, contentDescription = null, tint = OnErrorContainer, modifier = Modifier.size(26.dp))
                } else {
                    Icon(Icons.Outlined.FolderOpen, contentDescription = null, tint = Primary, modifier = Modifier.size(26.dp))
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                if (hasFile) {
                    Text(fileName, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = OnSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("$filePages pages", fontSize = 12.sp, color = OnSurfaceVariant)
                        Text(" • ", fontSize = 12.sp, color = OnSurfaceVariant)
                        Text("$fileSizeMb MB", fontSize = 12.sp, color = OnSurfaceVariant)
                    }
                } else {
                    Text("No file selected", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = OnSurface)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Tap to browse storage", fontSize = 12.sp, color = OnSurfaceVariant)
                }
            }
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(SurfaceContainerLow)
                .clickable { onChangeClick() }
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Text(if (hasFile) "Change" else "Select", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Primary)
        }
    }
}

@Composable
private fun SecurityBadgeBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceContainer)
            .padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(SurfaceContainerLowest).shadow(2.dp, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.EnhancedEncryption, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text("100% On-Device Offline Encryption", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
            Spacer(modifier = Modifier.height(2.dp))
            Text("Zero cloud transmission. Cryptographic keys never leave this phone's secure hardware module.", fontSize = 12.sp, color = OnSurfaceVariant, lineHeight = 16.sp)
        }
    }
}

@Composable
private fun PasswordSetupCard(
    openPassword: String, onOpenPasswordChange: (String) -> Unit,
    confirmPassword: String, onConfirmPasswordChange: (String) -> Unit,
    isVisible: Boolean, onVisibilityToggle: () -> Unit,
    isOwnerEnabled: Boolean, onOwnerToggle: () -> Unit,
    ownerPassword: String, onOwnerPasswordChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp), spotColor = Color.Black.copy(alpha = 0.08f))
            .background(SurfaceContainerLowest, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Key, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Document Protection", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = OnSurface)
            }
            Box(modifier = Modifier.clip(RoundedCornerShape(50)).background(SurfaceContainer).padding(horizontal = 8.dp, vertical = 2.dp)) {
                Text("Required", fontSize = 10.sp, color = OnSurfaceVariant)
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Open Password", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
            CustomPasswordField(
                value = openPassword, onValueChange = onOpenPasswordChange,
                icon = Icons.Outlined.Password, placeholder = "Enter unlock password",
                isVisible = isVisible, onVisibilityToggle = onVisibilityToggle
            )
            Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Password Strength", fontSize = 10.sp, color = OnSurfaceVariant)
                    val strengthText = if(openPassword.length > 7) "Strong (High Entropy)" else if(openPassword.length > 3) "Medium" else "Weak"
                    Text(strengthText, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Primary)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)).background(SurfaceContainerHigh), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    val strength = openPassword.length
                    Box(modifier = Modifier.weight(1f).fillMaxHeight().background(if(strength > 0) Primary else Color.Transparent))
                    Box(modifier = Modifier.weight(1f).fillMaxHeight().background(if(strength > 3) Primary else Color.Transparent))
                    Box(modifier = Modifier.weight(1f).fillMaxHeight().background(if(strength > 7) Primary else Color.Transparent))
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Confirm Password", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
            BasicTextField(
                value = confirmPassword, onValueChange = onConfirmPasswordChange,
                textStyle = TextStyle(fontSize = 13.sp, color = OnSurface),
                visualTransformation = PasswordVisualTransformation(),
                decorationBox = { innerTextField ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(SurfaceContainerLow).padding(horizontal = 12.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.Lock, contentDescription = null, tint = OnSurfaceVariant, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(modifier = Modifier.weight(1f)) { innerTextField() }
                        if (confirmPassword.isNotEmpty() && confirmPassword == openPassword) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            )
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                val isMatch = confirmPassword.isNotEmpty() && confirmPassword == openPassword
                Icon(if (isMatch) Icons.Outlined.Done else Icons.Outlined.Close, contentDescription = null, tint = if (isMatch) Primary else OnSurfaceVariant, modifier = Modifier.size(13.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (isMatch) "Passwords match" else "Waiting for match", fontSize = 10.sp, color = if (isMatch) Primary else OnSurfaceVariant)
            }
        }

        Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SurfaceContainerLow).clickable { onOwnerToggle() }.padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(SurfaceContainerLowest), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.AdminPanelSettings, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Permissions Password (Owner)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
                        Text("Restricts modification rights", fontSize = 11.sp, color = OnSurfaceVariant)
                    }
                }
                CustomSwitch(isChecked = isOwnerEnabled)
            }

            AnimatedVisibility(visible = isOwnerEnabled) {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    BasicTextField(
                        value = ownerPassword, onValueChange = onOwnerPasswordChange,
                        textStyle = TextStyle(fontSize = 13.sp, color = OnSurface),
                        decorationBox = { innerTextField ->
                            Row(
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(SurfaceContainerLow).padding(horizontal = 12.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Outlined.VpnKey, contentDescription = null, tint = OnSurfaceVariant, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(modifier = Modifier.weight(1f)) { innerTextField() }
                                Icon(Icons.Outlined.ContentCopy, contentDescription = null, tint = OnSurfaceVariant, modifier = Modifier.size(18.dp))
                            }
                        }
                    )
                    Text("Required if recipients need to change permissions later.", fontSize = 11.sp, color = OnSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun CustomPasswordField(value: String, onValueChange: (String) -> Unit, icon: ImageVector, placeholder: String, isVisible: Boolean, onVisibilityToggle: () -> Unit) {
    BasicTextField(
        value = value, onValueChange = onValueChange,
        textStyle = TextStyle(fontSize = 13.sp, color = OnSurface),
        visualTransformation = if (isVisible) VisualTransformation.None else PasswordVisualTransformation(),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(SurfaceContainerLow).padding(horizontal = 12.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, contentDescription = null, tint = OnSurfaceVariant, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (value.isEmpty()) Text(placeholder, fontSize = 13.sp, color = OnSurfaceVariant)
                    innerTextField()
                }
                Box(modifier = Modifier.size(28.dp).clip(CircleShape).clickable { onVisibilityToggle() }, contentAlignment = Alignment.Center) {
                    Icon(if (isVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, contentDescription = null, tint = OnSurfaceVariant, modifier = Modifier.size(20.dp))
                }
            }
        }
    )
}

@Composable
private fun PermissionsCard(
    allowPrinting: Boolean, onPrintToggle: () -> Unit,
    allowCopying: Boolean, onCopyToggle: () -> Unit,
    allowAnnotations: Boolean, onAnnotToggle: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp), spotColor = Color.Black.copy(alpha = 0.08f))
            .background(SurfaceContainerLowest, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.VerifiedUser, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Access Permissions", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = OnSurface)
            }
            Text("Strict PDF/X", fontSize = 10.sp, color = OnSurfaceVariant)
        }

        PermissionRow(Icons.Outlined.Print, "Allow Printing", "High-resolution physical printing", allowPrinting, onPrintToggle)
        PermissionRow(Icons.Outlined.ContentPasteOff, "Allow Content Copying & Extraction", "Block clipboard copying and screen scrapers", allowCopying, onCopyToggle)
        PermissionRow(Icons.Outlined.ModeComment, "Allow Page Annotations & Comments", "Reviewers may highlight and write notes", allowAnnotations, onAnnotToggle)

        Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Verified, contentDescription = null, tint = Primary, modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Compatible with Adobe Acrobat and all readers", fontSize = 10.sp, color = OnSurfaceVariant)
        }
    }
}

@Composable
private fun PermissionRow(icon: ImageVector, title: String, subtitle: String, isChecked: Boolean, onToggle: () -> Unit) {
    val iconBg = if (isChecked) SurfaceContainer else SurfaceContainerLow
    val iconTint = if (isChecked) OnSurface else Outline

    Row(
        modifier = Modifier.fillMaxWidth().clickable { onToggle() }.padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
    ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(iconBg), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
                Spacer(modifier = Modifier.height(2.dp))
                Text(subtitle, fontSize = 12.sp, color = OnSurfaceVariant)
            }
        }
        CustomSwitch(isChecked = isChecked)
    }
}

@Composable
private fun CustomSwitch(isChecked: Boolean) {
    val offset by animateFloatAsState(targetValue = if (isChecked) 20f else 0f, label = "switch")
    val bgColor = if (isChecked) Primary else SurfaceContainerHighest

    Box(modifier = Modifier.width(44.dp).height(24.dp).clip(RoundedCornerShape(50)).background(bgColor).padding(2.dp)) {
        Box(modifier = Modifier.size(20.dp).offset(x = offset.dp).clip(CircleShape).background(SurfaceContainerLowest).shadow(1.dp, CircleShape))
    }
}

@Composable
private fun PasswordBottomBar(isProcessing: Boolean, isComplete: Boolean, hasFile: Boolean, onClick: () -> Unit) {
    val buttonText = when {
        !hasFile -> "Select a PDF to Protect"
        isProcessing -> "Encrypting Document..."
        isComplete -> "PDF Secured!"
        else -> "Encrypt & Protect PDF"
    }

    val buttonIcon = when {
        !hasFile -> Icons.Outlined.Folder
        isComplete -> Icons.Filled.CheckCircle
        else -> Icons.Filled.EnhancedEncryption
    }

    val buttonColor = if (isComplete) Secondary else if (hasFile) Primary else SurfaceContainerHigh
    val contentColor = if (hasFile || isComplete) OnPrimary else OutlineVariant

    val cornerRadius by animateDpAsState(targetValue = if (isProcessing) 50.dp else 16.dp, label = "buttonShape")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceContainerLowest.copy(alpha = 0.98f))
            .shadow(elevation = 24.dp, spotColor = OnSurface.copy(alpha = 0.1f))
            .navigationBarsPadding()
    ) {
        if (hasFile) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 16.dp, bottom = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Shield, contentDescription = null, tint = Primary, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("AES-256 Bit Encryption", fontSize = 12.sp, color = OnSurfaceVariant)
                }
                Text("Offline Key Generation", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Primary)
            }
        } else {
            Spacer(modifier = Modifier.height(16.dp))
        }

        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
                .fillMaxWidth()
                .height(56.dp)
                .shadow(if (!isComplete && hasFile && !isProcessing) 8.dp else 0.dp, RoundedCornerShape(cornerRadius), spotColor = buttonColor.copy(alpha = 0.35f))
                .clip(RoundedCornerShape(cornerRadius))
                .background(buttonColor)
                .clickable(enabled = !isProcessing && !isComplete) { onClick() },
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = Pair(isProcessing, isComplete),
                label = "buttonContent"
            ) { states ->
                val (processing, _) = states
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (processing) {
                        CircularProgressIndicator(color = contentColor, modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(buttonText, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = contentColor)
                    } else {
                        Icon(buttonIcon, contentDescription = null, tint = contentColor, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(buttonText, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = contentColor)
                    }
                }
            }
        }
    }
}