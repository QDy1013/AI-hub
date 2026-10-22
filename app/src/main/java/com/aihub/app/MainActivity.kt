package com.aihub.app

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.aihub.app.ui.navigation.AiHubNavHost
import com.aihub.app.ui.theme.AIHubTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        applySecureScreen((application as AiHubApp).container.settings.secureScreen)
        setContent {
            AIHubTheme {
                AiHubNavHost(onSecureScreenChanged = ::applySecureScreen)
            }
        }
    }

    private fun applySecureScreen(enabled: Boolean) {
        if (enabled) {
            window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}
