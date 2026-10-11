package com.subha.fileease

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.subha.fileease.ui.screens.MainScreen // Added the correct import for the navigation scaffold
import com.subha.fileease.ui.theme.FileEaseTheme
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize the local PDF engine
        PDFBoxResourceLoader.init(applicationContext)

        setContent {
            FileEaseTheme {
                MainScreen()
            }
        }
    }
}