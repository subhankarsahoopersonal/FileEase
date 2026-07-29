package com.subha.fileease

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import com.subha.fileease.ui.theme.FileEaseTheme
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize the local PDF engine
        PDFBoxResourceLoader.init(applicationContext)

        setContent {
            FileEaseTheme {
                // We will build the Dashboard UI here next
                Surface(modifier = Modifier.fillMaxSize()) {
                    Text(text = "FileEase Ready")
                }
            }
        }
    }
}
