package com.subha.fileease.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
private val ErrorContainer = Color(0xFFFFDAD6)
private val OnErrorContainer = Color(0xFF93000A)
private val Outline = Color(0xFF727687)

@Composable
fun PdfPasswordScreen(onNavigateBack: () -> Unit) {
    // State Management for inputs and toggles
    var openPassword by remember { mutableStateOf("Vault#Secure99") }
    var confirmPassword by remember { mutableStateOf("Vault#Secure99") }
    var isOpenPasswordVisible by remember { mutableStateOf(false) }

    var isOwnerEnabled by remember { mutableStateOf(true) }
    var ownerPassword by remember { mutableStateOf("AdminMaster#2024") }

    var allowPrinting by remember { mutableStateOf(true) }
    var allowCopying by remember { mutableStateOf(false) }
    var allowAnnotations by remember { mutableStateOf(true) }

    var isProcessing by remember { mutableStateOf(false) }
    var isComplete by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        containerColor = Surface,
        topBar = { PasswordTopBar(onNavigateBack) },
        bottomBar = {
            // Blueprint: The Command Anchor outside the scrollable area
            PasswordBottomBar(isProcessing, isComplete) {
                coroutineScope.launch {
                    isProcessing = true
                    delay(700)
                    isProcessing = false
                    isComplete = true
                    delay(2200)
                    isComplete = false
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
            // Subtitle Banner
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp, bottom = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.LockReset, contentDescription = null, tint = Primary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("SECURITY MODULE", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Primary, letterSpacing = 0.5.sp)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("Encrypt and secure your documents with offline cryptographic keys.", fontSize = 13.sp, color = OnSurfaceVariant)
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp) // Enhanced spacing for depth
            ) {
                // 1. File Overview Card
                FileOverviewCard()

                // 2. Security Badge Banner
                SecurityBadgeBanner()

                // 3. Password Setup Form Card
                PasswordSetupCard(
                    openPassword = openPassword,
                    onOpenPasswordChange = { openPassword = it },
                    confirmPassword = confirmPassword,
                    onConfirmPasswordChange = { confirmPassword = it },
                    isVisible = isOpenPasswordVisible,
                    onVisibilityToggle = { isOpenPasswordVisible = !isOpenPasswordVisible },
                    isOwnerEnabled = isOwnerEnabled,
                    onOwnerToggle = { isOwnerEnabled = !isOwnerEnabled },
                    ownerPassword = ownerPassword,
                    onOwnerPasswordChange = { ownerPassword = it }
                )

                // 4. Granular Permissions Card
                PermissionsCard(
                    allowPrinting = allowPrinting, onPrintToggle = { allowPrinting = !allowPrinting },
                    allowCopying = allowCopying, onCopyToggle = { allowCopying = !allowCopying },
                    allowAnnotations = allowAnnotations, onAnnotToggle = { allowAnnotations = !allowAnnotations }
                )

                // Extra padding to ensure scrolling clears the floating bottom anchor
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
            .statusBarsPadding() // Blueprint: Immersive Bleed
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
                Text("Pdf Password", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = OnSurface)
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
private fun FileOverviewCard() {
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
            Box(
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(ErrorContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.PictureAsPdf, contentDescription = null, tint = OnErrorContainer, modifier = Modifier.size(26.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("Confidential_Executive_Brief.pdf", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = OnSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(2.dp))
                Text("12 pages • 2.4 MB", fontSize = 12.sp, color = OnSurfaceVariant)
            }
        }
        Row(
            modifier = Modifier.clip(RoundedCornerShape(50)).background(SurfaceContainer).padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.Shield, contentDescription = null, tint = Primary, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("AES-256", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Primary)
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
            .shadow(4.dp, RoundedCornerShape(16.dp), spotColor = Color.Black.copy(alpha = 0.08f)) // Blueprint: Z-Axis Depth
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

        // Open Password
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Open Password", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
            CustomPasswordField(
                value = openPassword, onValueChange = onOpenPasswordChange,
                icon = Icons.Outlined.Password, placeholder = "Enter unlock password",
                isVisible = isVisible, onVisibilityToggle = onVisibilityToggle
            )
            // Strength Meter
            Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Password Strength", fontSize = 10.sp, color = OnSurfaceVariant)
                    Text("Strong (High Entropy)", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Primary)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)).background(SurfaceContainerHigh), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Primary))
                    Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Primary))
                    Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Primary))
                }
            }
        }

        // Confirm Password
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
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
                    }
                }
            )
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                Icon(Icons.Outlined.Done, contentDescription = null, tint = Primary, modifier = Modifier.size(13.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Passwords match", fontSize = 10.sp, color = Primary)
            }
        }

        // Owner Password Section
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
                    Text("Required if recipients need to change these document permissions later.", fontSize = 11.sp, color = OnSurfaceVariant)
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
            .shadow(4.dp, RoundedCornerShape(16.dp), spotColor = Color.Black.copy(alpha = 0.08f)) // Blueprint: Z-Axis Depth
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
            Text("Compatible with Adobe Acrobat, Apple Preview, and all standard readers", fontSize = 10.sp, color = OnSurfaceVariant)
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
private fun PasswordBottomBar(isProcessing: Boolean, isComplete: Boolean, onClick: () -> Unit) {
    val buttonColor = if (isProcessing) PrimaryContainer else if (isComplete) SurfaceContainerLowest else PrimaryContainer

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceContainerLowest.copy(alpha = 0.98f))
            .shadow(elevation = 24.dp, spotColor = OnSurface.copy(alpha = 0.1f))
            .navigationBarsPadding() // Blueprint: Anchor bottom spacing for gestures
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp, bottom = 12.dp)
                .fillMaxWidth()
                .height(56.dp) // Blueprint: 56dp robust execution height
                .shadow(if (!isComplete) 8.dp else 0.dp, RoundedCornerShape(16.dp), spotColor = buttonColor.copy(alpha = 0.35f))
                .clip(RoundedCornerShape(16.dp))
                .background(buttonColor)
                .clickable(enabled = !isProcessing && !isComplete) { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isProcessing) {
                    CircularProgressIndicator(color = OnPrimaryContainer, modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Encrypting Document...", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = OnPrimaryContainer)
                } else if (isComplete) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Primary, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("PDF Secured!", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Primary)
                } else {
                    Icon(Icons.Filled.EnhancedEncryption, contentDescription = null, tint = OnPrimaryContainer, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Encrypt & Protect PDF", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = OnPrimaryContainer)
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Speed, contentDescription = null, tint = OnSurfaceVariant, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Estimated completion: ~0.4s on device", fontSize = 11.sp, color = OnSurfaceVariant)
        }
    }
}