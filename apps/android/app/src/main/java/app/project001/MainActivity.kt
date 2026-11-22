package app.project001

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import app.project001.ui.App
import app.project001.ui.theme.AppTheme
import app.project001.ui.theme.Project001Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            Project001Theme {
                Box(Modifier.fillMaxSize().background(AppTheme.colors.paper)) {
                    App()
                }
            }
        }
    }
}
