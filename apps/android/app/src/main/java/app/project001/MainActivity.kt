package app.project001

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import app.project001.data.ThemeChoice
import app.project001.ui.App
import app.project001.ui.theme.AppTheme
import app.project001.ui.theme.Project001Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT))
        super.onCreate(savedInstanceState)
        setContent {
            val choice by graph.settings.theme.collectAsState()
            val dark = when (choice) {
                ThemeChoice.Light -> false
                ThemeChoice.Dark -> true
                ThemeChoice.System -> isSystemInDarkTheme()
            }
            // Status and navigation bar icons follow the app's theme, not the phone's.
            LaunchedEffect(dark) {
                val bars = if (dark) SystemBarStyle.dark(Color.TRANSPARENT) else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
            }
            Project001Theme(dark = dark) {
                Box(Modifier.fillMaxSize().background(AppTheme.colors.paper)) {
                    App()
                }
            }
        }
    }
}
