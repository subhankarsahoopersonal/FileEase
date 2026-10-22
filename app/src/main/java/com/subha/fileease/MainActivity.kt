package com.subha.fileease

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import com.subha.fileease.ui.screens.MainScreen
import com.subha.fileease.ui.theme.FileEaseTheme
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // MUST BE CALLED FIRST: Hooks the splash screen to the Android lifecycle
        installSplashScreen()

        super.onCreate(savedInstanceState)

        // Initialize the local PDF engine
        PDFBoxResourceLoader.init(applicationContext)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            FileEaseTheme {
                MainScreen()
            }
        }
    }
}