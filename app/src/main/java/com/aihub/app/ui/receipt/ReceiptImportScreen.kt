package com.aihub.app.ui.receipt

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aihub.app.data.model.BillingCycle
import com.aihub.app.data.model.Platform
import com.aihub.app.data.receipt.ParsedReceipt
import com.aihub.app.ui.AppViewModelProvider
import com.aihub.app.ui.components.PlatformAvatar
import com.aihub.app.util.formatMoney
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ReceiptImportScreen(
    onBack: () -> Unit,
    onImported: (Long) -> Unit,
    viewModel: ReceiptImportViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val context = LocalContext.current
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val result = viewModel.result
    val platform = viewModel.platform

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("导入订阅收据") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "在邮箱里打开 OpenAI、Anthropic、Google、App Store / Google Play 发来的订阅收据或扣款邮件，" +
                    "用“分享”发送到 AI Hub，或者复制全文粘贴到下面。识别完全在本机进行，原文不保存、不上传。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = viewModel.text,
                onValueChange = viewModel::updateText,
                label = { Text("收据内容") },
                minLines = 6,
                maxLines = 12,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    readClipboard(context)?.let {
                        viewModel.updateText(it)
                        viewModel.parse()
                    }
                }) { Text("粘贴") }
                Button(onClick = viewModel::parse, enabled = viewModel.text.isNotBlank()) { Text("识别") }
                if (viewModel.text.isNotEmpty()) {
                    TextButton(onClick = viewModel::clear) { Text("清空") }
                }
            }

            if (result != null) {
                if (result.isEmpty) {
                    Text(
                        "没有识别到订阅信息。请确认粘贴的是完整的收据或扣款邮件，也可以直接在账号里手动填写。",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    ResultCard(result)
                }

                Text("平台", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Platform.entries.forEach { p ->
                        FilterChip(
                            selected = platform == p,
                            onClick = { viewModel.selectPlatform(p) },
                            label = { Text(p.displayName) },
                            leadingIcon = { PlatformAvatar(p, size = 18.dp) },
                        )
                    }
                }

                if (platform != null) {
                    Text("写入哪个账号", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        accounts.filter { it.platform == platform }.forEach { a ->
                            FilterChip(
                                selected = viewModel.targetId == a.id,
                                onClick = { viewModel.selectTarget(a.id) },
                                label = { Text(a.label) },
                            )
                        }
                        FilterChip(
                            selected = viewModel.targetId == null,
                            onClick = { viewModel.selectTarget(null) },
                            label = { Text("新建账号") },
                        )
                    }
                    Text(
                        "只更新识别到的字段（套餐、费用、周期、续费日、状态），其余信息、备注和 API Key 保持不变。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(
                        onClick = { viewModel.apply(onImported) },
                        enabled = !result.isEmpty,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                    ) {
                        Text("确认写入")
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ResultCard(r: ParsedReceipt) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("识别结果", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Field("平台", r.platform?.displayName)
            Field("套餐", r.planName)
            Field(
                if (r.isTopUp) "充值金额" else "扣款金额",
                if (r.amount != null && r.currency != null) formatMoney(r.amount, r.currency) else null,
            )
            if (!r.isTopUp) {
                Field(
                    "计费周期",
                    when (r.billingCycle) {
                        BillingCycle.MONTHLY -> "按月"
                        BillingCycle.YEARLY -> "按年"
                        null -> null
                    },
                )
            }
            Field("扣款日期", r.chargedOn?.let(::dateText))
            if (!r.isTopUp) {
                Field(
                    if (r.canceled) "到期日" else "下次续费",
                    r.renewsOn?.let { dateText(it) + if (r.renewalInferred) "（按扣款日推算，请核对）" else "" },
                )
            }
            if (r.canceled) {
                Text(
                    "收据显示订阅已取消，状态将记为“已取消续订”。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            if (r.isTopUp) {
                Text(
                    "这是 API 充值记录，不计入每月订阅支出。余额请在账号里填写 API Key 自动同步。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun Field(label: String, value: String?) {
    Row(verticalAlignment = Alignment.Top) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.35f),
        )
        Text(
            value ?: "未识别（保持原值）",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (value != null) FontWeight.Medium else FontWeight.Normal,
            color = if (value != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
            modifier = Modifier.weight(0.65f),
        )
    }
}

private fun dateText(date: LocalDate): String = date.toString()

private fun readClipboard(context: Context): String? {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = clipboard.primaryClip ?: return null
    if (clip.itemCount == 0) return null
    return clip.getItemAt(0).coerceToText(context)?.toString()?.takeIf { it.isNotBlank() }
}
