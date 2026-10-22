package com.aihub.app

import android.content.Intent
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
        if (savedInstanceState == null) handleShare(intent)
        setContent {
            AIHubTheme {
                AiHubNavHost(onSecureScreenChanged = ::applySecureScreen)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleShare(intent)
    }

    /** 从邮件等 App 分享过来的收据文字，交给收据导入页在本机识别 */
    private fun handleShare(intent: Intent?) {
        if (intent?.action != Intent.ACTION_SEND || intent.type?.startsWith("text/") != true) return
        val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT).orEmpty()
        val body = intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString().orEmpty()
        val text = listOf(subject, body).filter { it.isNotBlank() }.joinToString("\n")
        if (text.isNotBlank()) {
            (application as AiHubApp).container.sharedText.offer(text)
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
