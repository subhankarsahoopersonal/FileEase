package com.subha.fileease.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Defined directly from the design system palette
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0066FF),       // Vibrant cobalt blue for interactive states[cite: 4]
    background = Color(0xFFF4F6F9),    // Cool neutral-grey base app canvas[cite: 4]
    surface = Color(0xFFFFFFFF),       // Crisp elevated container[cite: 4]
    onPrimary = Color(0xFFFFFFFF),
    onBackground = Color(0xFF0F172A),  // Ink Primary (deep obsidian for legibility)[cite: 4]
    onSurface = Color(0xFF0F172A)
)

@Composable
fun FileEaseTheme(
    content: @Composable () -> Unit
) {
    // By passing LightColorScheme directly and removing all `isSystemInDarkTheme()` logic,
    // the app is permanently locked to the light UI regardless of device settings.
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography, // Ensure this points to your Type.kt configuration
        content = content
    )
}