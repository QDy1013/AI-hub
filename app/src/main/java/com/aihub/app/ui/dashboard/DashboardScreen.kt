package com.aihub.app.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aihub.app.data.db.AccountEntity
import com.aihub.app.data.model.Platform
import com.aihub.app.ui.AppViewModelProvider
import com.aihub.app.ui.components.PlatformAvatar
import com.aihub.app.ui.components.QuotaBlock
import com.aihub.app.ui.components.StatusPill
import com.aihub.app.ui.theme.StatusColors
import com.aihub.app.util.daysUntil
import com.aihub.app.util.formatCountdown
import com.aihub.app.util.formatDate
import com.aihub.app.util.formatMoney

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onAddAccount: (Platform?) -> Unit,
    onOpenAccount: (Long) -> Unit,
    viewModel: DashboardViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("AI Hub", fontWeight = FontWeight.Bold)
                        Text(
                            "我的 AI 会员中心",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 4.dp,
                bottom = padding.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { SummaryCard(state) }

            if (state.lowQuota.isNotEmpty() || state.upcomingRenewals.isNotEmpty()) {
                item { SectionTitle("提醒") }
                items(state.lowQuota, key = { "q${it.id}" }) { account ->
                    AlertRow(
                        icon = Icons.Filled.Warning,
                        tint = StatusColors.Bad,
                        title = "${account.platform.displayName} · ${account.label} 额度偏低",
                        subtitle = "剩余 ${account.quotaRemaining} / ${account.quotaLimit} ${account.quotaUnit}（${account.quotaSource.label}）",
                        onClick = { onOpenAccount(account.id) },
                    )
                }
                items(state.upcomingRenewals, key = { "r${it.id}" }) { account ->
                    val renews = account.renewsAt ?: return@items
                    val overdue = daysUntil(renews) < 0
                    AlertRow(
                        icon = Icons.Filled.DateRange,
                        tint = if (overdue) StatusColors.Bad else StatusColors.Warn,
                        title = "${account.platform.displayName} · ${account.label} ${account.planName}",
                        subtitle = "${if (overdue) "已到期" else "续费/到期"}：${formatDate(renews)}（${formatCountdown(renews)}）",
                        onClick = { onOpenAccount(account.id) },
                    )
                }
            }

            item { SectionTitle("平台概览") }
            items(state.platforms, key = { it.platform.name }) { summary ->
                PlatformCard(
                    summary = summary,
                    onClick = {
                        val top = summary.topAccount
                        if (top == null) onAddAccount(summary.platform) else onOpenAccount(top.id)
                    },
                )
            }

            item { DataNoticeCard() }
        }
    }
}

@Composable
private fun SummaryCard(state: DashboardState) {
    val gradient = Brush.linearGradient(listOf(Color(0xFF4F46E5), Color(0xFF7C3AED), Color(0xFF0EA5E9)))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(gradient)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("每月订阅支出", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelLarge)
        val costText = if (state.monthlyCost.isEmpty()) {
            formatMoney(0.0, "USD")
        } else {
            state.monthlyCost.entries.joinToString("  +  ") { (cur, amount) -> formatMoney(amount, cur) }
        }
        Text(costText, color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            SummaryStat("付费会员", state.paidCount.toString())
            SummaryStat("账号总数", state.accountCount.toString())
            SummaryStat("已用平台", state.platforms.count { it.accounts.isNotEmpty() }.toString())
        }
    }
}

@Composable
private fun SummaryStat(label: String, value: String) {
    Column {
        Text(value, color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Text(label, color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 8.dp, start = 4.dp),
    )
}

@Composable
private fun AlertRow(icon: ImageVector, tint: Color, title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = tint.copy(alpha = 0.08f)),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
            Column(Modifier.padding(start = 12.dp)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun PlatformCard(summary: PlatformSummary, onClick: () -> Unit) {
    val top: AccountEntity? = summary.topAccount
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PlatformAvatar(summary.platform)
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(summary.platform.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        if (top == null) "尚未添加账号" else "${summary.accounts.size} 个账号 · ${top.planName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (top != null) StatusPill(top.status)
            }
            if (top == null) {
                Text(
                    "点击添加 ${summary.platform.vendor} 账号",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            } else {
                top.renewsAt?.let {
                    Text(
                        "下次续费/到期：${formatDate(it)}（${formatCountdown(it)}）",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                QuotaBlock(top)
            }
        }
    }
}

@Composable
private fun DataNoticeCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
    ) {
        Row(Modifier.padding(14.dp)) {
            Icon(Icons.Filled.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(
                "当前版本的会员与额度信息均为手动记录，并已在每处标明来源。" +
                    "无法自动获取的数据显示为“未获取”，App 不会估算或虚构剩余额度。",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 10.dp),
            )
        }
    }
}
