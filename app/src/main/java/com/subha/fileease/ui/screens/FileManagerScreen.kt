package com.subha.fileease.ui.screens

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// New Color Tokens from the HTML design
val Sky500 = Color(0xFF0EA5E9)
val Sky50 = Color(0xFFF0F9FF)
val Indigo500 = Color(0xFF6366F1)
val Indigo50 = Color(0xFFEEF2FF)
val Slate800 = Color(0xFF1E293B)
val Slate700 = Color(0xFF334155)
val Slate600 = Color(0xFF475569)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileManagerScreen() {
    var showBottomSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { FileManagerTopBar() },
        containerColor = BgColor // From our existing palette (F8FAFC)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            FileSearchBar()

            Spacer(modifier = Modifier.height(16.dp))
            BreadcrumbNavigation()

            Spacer(modifier = Modifier.height(8.dp))

            // File List
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FileListItem(
                    icon = Icons.Outlined.Folder,
                    iconBgColor = Blue50,
                    iconColor = PrimaryBlue,
                    title = "Project Assets",
                    subtitle = "12 items • Oct 24, 2023",
                    onOptionsClick = { showBottomSheet = true }
                )
                FileListItem(
                    icon = Icons.Outlined.PictureAsPdf,
                    iconBgColor = Red50,
                    iconColor = Red500,
                    title = "Annual_Report.pdf",
                    subtitle = "4.2 MB • 2 hours ago",
                    onOptionsClick = { showBottomSheet = true }
                )
                FileListItem(
                    icon = Icons.Outlined.Image,
                    iconBgColor = Sky50,
                    iconColor = Sky500,
                    title = "branding_v2.png",
                    subtitle = "1.8 MB • Yesterday",
                    onOptionsClick = { showBottomSheet = true }
                )
                FileListItem(
                    icon = Icons.Outlined.Description,
                    iconBgColor = Indigo50,
                    iconColor = Indigo500,
                    title = "meeting_notes.docx",
                    subtitle = "45 KB • 3 days ago",
                    onOptionsClick = { showBottomSheet = true }
                )
            }
            Spacer(modifier = Modifier.height(100.dp)) // Scroll clearance
        }
    }

    // Contextual Bottom Drawer
    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            containerColor = Color.White,
            dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0xFFCBD5E1)) }
        ) {
            FileOptionsDrawerContent()
        }
    }
}

@Composable
fun FileManagerTopBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceColor.copy(alpha = 0.95f))
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Files",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Slate100)
                .clickable { },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = "Add new file or folder",
                tint = Slate700,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun FileSearchBar() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(50))
            .background(Slate100.copy(alpha = 0.9f))
            .clickable { },
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = "Search",
                tint = TextTertiary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Search files and folders",
                color = TextTertiary,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun BreadcrumbNavigation() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Internal Storage", color = PrimaryBlue, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Text(" › ", color = TextTertiary, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 4.dp))
        Text("Documents", color = PrimaryBlue, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Text(" › ", color = TextTertiary, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 4.dp))
        Text("Work", color = Slate800, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun FileListItem(
    icon: ImageVector,
    iconBgColor: Color,
    iconColor: Color,
    title: String,
    subtitle: String,
    onOptionsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Slate100.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceColor)
            .clickable { }
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = TextTertiary
                )
            }
        }

        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .clickable { onOptionsClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.MoreVert,
                contentDescription = "Options",
                tint = TextTertiary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun FileOptionsDrawerContent() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, bottom = 24.dp)
    ) {
        // Selected File Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Blue50),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Folder, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(26.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(text = "Project Assets", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(text = "Folder • 12 items", fontSize = 12.sp, color = TextTertiary)
            }
        }

        HorizontalDivider(color = Slate100)
        Spacer(modifier = Modifier.height(8.dp))

        // Actions
        DrawerActionItem(icon = Icons.Outlined.Edit, title = "Rename", color = Slate800, iconColor = Slate600)
        DrawerActionItem(icon = Icons.Outlined.OpenWith, title = "Move", color = Slate800, iconColor = Slate600)
        DrawerActionItem(icon = Icons.Outlined.DeleteOutline, title = "Delete", color = Red500, iconColor = Red500)
    }
}

@Composable
fun DrawerActionItem(icon: ImageVector, title: String, color: Color, iconColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { }
            .padding(horizontal = 4.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = title, tint = iconColor, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = color)
    }
}