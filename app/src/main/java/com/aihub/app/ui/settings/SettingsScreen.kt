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

private val roadmap = listOf(
    "第一阶段（当前）" to "应用框架、仪表盘、多平台多账号管理、会员信息手动记录、加密存储",
    "第二阶段" to "研究各平台官方授权方式，能自动识别的套餐与额度自动同步，其余继续标注为手动",
    "第三阶段" to "在 App 内整合各平台官方聊天入口，保留原有会员权益",
    "第四阶段" to "额度提醒、使用统计、趋势图表、界面打磨",
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
                title = { Text("设置", fontWeight = FontWeight.Bold) },
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
            }

            SettingsCard {
                Text("开发路线", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }
    }
}

@Composable
private fun Bullet(text: String) {
    Row {
        Text("•  ", style = MaterialTheme.typography.bodySmall)
        Text(text, style = MaterialTheme.typography.bodySmall)
    }
}
