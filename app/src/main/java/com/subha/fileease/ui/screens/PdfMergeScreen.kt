package com.subha.fileease.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.subha.fileease.utils.PdfProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun PdfMergeScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // State to hold the PDFs the user picks and our status message
    var selectedUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var statusMessage by remember { mutableStateOf("Select PDFs to begin") }
    var isProcessing by remember { mutableStateOf(false) }

    // Launcher to open the Android system file picker (filtered for PDFs)
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            selectedUris = uris
            statusMessage = "${uris.size} files selected"
        }
    }

    // Colors matching the sleek, professional dark-mode vision
    val backgroundColor = Color(0xFF121212)
    val surfaceColor = Color(0xFF1E1E1E)
    val accentColor = Color(0xFF007AFF) // Sharp iOS-style blue for high contrast
    val textColor = Color.White

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Text(
            text = "Merge PDFs",
            color = textColor,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 32.dp, bottom = 24.dp)
        )

        // Select Files Button
        Button(
            onClick = { filePickerLauncher.launch(arrayOf("application/pdf")) },
            colors = ButtonDefaults.buttonColors(containerColor = surfaceColor),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("1. Select PDF Files", color = textColor, fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Display selected files in a scrollable list
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(surfaceColor, RoundedCornerShape(12.dp))
                .padding(8.dp)
        ) {
            if (selectedUris.isEmpty()) {
                Text(
                    text = "No files selected yet.",
                    color = Color.Gray,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn {
                    items(selectedUris) { uri ->
                        Text(
                            text = uri.lastPathSegment ?: "Unknown File",
                            color = Color.LightGray,
                            modifier = Modifier.padding(8.dp),
                            maxLines = 1
                        )
                        HorizontalDivider(color = Color.DarkGray, thickness = 0.5.dp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Status Text
        Text(text = statusMessage, color = Color.LightGray, fontSize = 14.sp)

        Spacer(modifier = Modifier.height(16.dp))

        // Merge Button
        Button(
            onClick = {
                if (selectedUris.size > 1) {
                    isProcessing = true
                    statusMessage = "Merging in progress..."

                    // Run the heavy PDF logic in the background so the UI doesn't freeze
                    coroutineScope.launch(Dispatchers.IO) {
                        val resultFile = PdfProcessor.mergePdfs(context, selectedUris, "FileEase_Merged_${System.currentTimeMillis()}.pdf")

                        withContext(Dispatchers.Main) {
                            isProcessing = false
                            if (resultFile != null) {
                                statusMessage = "Success! Saved in app cache."
                                selectedUris = emptyList() // Clear the list on success
                            } else {
                                statusMessage = "Error merging files."
                            }
                        }
                    }
                } else {
                    statusMessage = "Please select at least 2 PDFs to merge."
                }
            },
            enabled = selectedUris.size > 1 && !isProcessing,
            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            if (isProcessing) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text("2. Merge Files", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}