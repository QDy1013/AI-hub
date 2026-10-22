package com.aihub.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aihub.app.AiHubApp
import com.aihub.app.BuildConfigInfo
import com.aihub.app.ui.components.AppCard

private val roadmap = listOf(
    "第一阶段" to "应用框架、仪表盘、多平台多账号管理、会员信息手动记录、加密存储",
    "第二阶段" to "DeepSeek API 余额自动同步；从订阅收据识别套餐、费用和续费日；其余平台继续手动记录并标注来源",
)

private val support = listOf(
    "DeepSeek" to "余额：填写自己的 API Key 后自动同步（官方 /user/balance 接口）。充值收据可识别。",
    "ChatGPT" to "套餐、费用、续费日：可从 OpenAI 或应用商店收据识别。消息额度：官方没有公开接口，只能手动记录。",
    "Claude" to "套餐（含 Max 5x / 20x）、费用、续费日：可从 Anthropic 或应用商店收据识别。用量：官方没有公开接口，只能手动记录。",
    "Gemini" to "Google AI Pro / Ultra、费用、续费日：可从 Google Play / Google One 收据识别。额度：没有公开接口，只能手动记录。",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onSecureScreenChanged: (Boolean) -> Unit) {
    val context = LocalContext.current
    val settings = remember { (context.applicationContext as AiHubApp).container.settings }
    var secureScreen by remember { mutableStateOf(settings.secureScreen) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置", style = MaterialTheme.typography.headlineSmall) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SettingsCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("隐私屏幕", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(
                            "禁止截屏，并在最近任务中隐藏 App 内容",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = secureScreen,
                        onCheckedChange = {
                            secureScreen = it
                            settings.secureScreen = it
                            onSecureScreenChanged(it)
                        },
                    )
                }
            }

            SettingsCard {
                Text("数据与安全", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Bullet("所有数据只保存在本机（Room 数据库），不上传任何服务器。")
                Bullet("私密备注使用 Android Keystore 生成的 AES-256 密钥加密。")
                Bullet("已关闭系统云备份，卸载 App 即彻底删除数据。")
                Bullet("App 不索取、不保存任何平台的账号密码；打开官方页面时登录由浏览器完成。")
                Bullet("API Key 与私密备注一样用 Keystore 加密，只发送给对应平台的官方接口，保存后不再显示。")
                Bullet("收据识别在本机完成，收据原文不保存、不上传。")
            }

            SettingsCard {
                Text("自动识别支持情况", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                support.forEach { (platform, desc) ->
                    Column {
                        Text(platform, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            SettingsCard {
                Text("版本内容", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                roadmap.forEach { (stage, desc) ->
                    Column {
                        Text(stage, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Text(
                "AI Hub ${BuildConfigInfo.versionName(context)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { content() }
    }
}

@Composable
private fun Bullet(text: String) {
    Row {
        Text("•  ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
