package com.aihub.app.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aihub.app.data.db.AccountEntity
import com.aihub.app.data.model.Platform
import com.aihub.app.ui.AppViewModelProvider
import com.aihub.app.ui.components.AppCard
import com.aihub.app.ui.components.PlanSourcePill
import com.aihub.app.ui.components.PlatformAvatar
import com.aihub.app.ui.components.QuotaBlock
import com.aihub.app.ui.components.SectionLabel
import com.aihub.app.ui.components.StatusPill
import com.aihub.app.ui.theme.Gradients
import com.aihub.app.ui.theme.StatusColors
import com.aihub.app.util.daysUntil
import com.aihub.app.util.formatCountdown
import com.aihub.app.util.formatDate
import com.aihub.app.util.formatDateTime
import com.aihub.app.util.formatMoney

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onAddAccount: (Platform?) -> Unit,
    onOpenAccount: (Long) -> Unit,
    onImportReceipt: () -> Unit,
    viewModel: DashboardViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val syncing by viewModel.syncing.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it, withDismissAction = true)
            viewModel.messageShown()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("AI Hub", style = MaterialTheme.typography.headlineSmall)
                        Text(
                            "我的 AI 会员中心",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    RoundIconButton(Icons.Filled.Email, "导入订阅收据", onClick = onImportReceipt)
                    Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                        if (syncing) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            RoundIconButton(Icons.Filled.Refresh, "同步余额", onClick = viewModel::refresh)
                        }
                    }
                    Spacer(Modifier.width(8.dp))
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
                bottom = padding.calculateBottomPadding() + 32.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { SummaryCard(state) }

            // DeepSeek：唯一能自动同步的数据，单独放一张大卡片
            val deepSeek = state.platforms.firstOrNull { it.platform == Platform.DEEPSEEK }?.accounts.orEmpty()
            val synced = deepSeek.filter { it.apiBalance != null || it.syncError != null }
            if (synced.isNotEmpty()) {
                items(synced, key = { "ds${it.id}" }) { account ->
                    DeepSeekBalanceCard(account, syncing, onRefresh = viewModel::refresh, onOpen = { onOpenAccount(account.id) })
                }
            } else {
                item {
                    DeepSeekConnectCard(
                        hasAccount = deepSeek.isNotEmpty(),
                        onClick = {
                            val first = deepSeek.firstOrNull()
                            if (first == null) onAddAccount(Platform.DEEPSEEK) else onOpenAccount(first.id)
                        },
                    )
                }
            }

            if (state.lowQuota.isNotEmpty() || state.upcomingRenewals.isNotEmpty()) {
                item { SectionLabel("提醒") }
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

            item { SectionLabel("平台概览") }
            items(state.platforms, key = { it.platform.name }) { summary ->
                PlatformCard(
                    summary = summary,
                    onClick = {
                        val top = summary.topAccount
                        if (top == null) onAddAccount(summary.platform) else onOpenAccount(top.id)
                    },
                )
            }

            item { DataNoticeCard(onImportReceipt) }
        }
    }
}

@Composable
private fun RoundIconButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Box(
            Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = description, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurface)
        }
    }
}

/** 渐变卡片背景上叠两团柔光，增加层次感 */
private fun Modifier.glow(primary: Color, secondary: Color): Modifier = drawBehind {
    drawCircle(
        brush = Brush.radialGradient(listOf(primary.copy(alpha = 0.55f), Color.Transparent), center = Offset(size.width * 0.95f, 0f), radius = size.width * 0.6f),
        radius = size.width * 0.6f,
        center = Offset(size.width * 0.95f, 0f),
    )
    drawCircle(
        brush = Brush.radialGradient(listOf(secondary.copy(alpha = 0.35f), Color.Transparent), center = Offset(0f, size.height), radius = size.width * 0.5f),
        radius = size.width * 0.5f,
        center = Offset(0f, size.height),
    )
}

@Composable
private fun SummaryCard(state: DashboardState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Gradients.Hero)
            .glow(Color(0xFF8B5CF6), Color(0xFF22D3EE))
            .padding(horizontal = 22.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("每月订阅支出", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelLarge)
        val costText = if (state.monthlyCost.isEmpty()) {
            formatMoney(0.0, "USD")
        } else {
            state.monthlyCost.entries.joinToString("  +  ") { (cur, amount) -> formatMoney(amount, cur) }
        }
        Text(
            costText,
            color = Color.White,
            style = MaterialTheme.typography.displaySmall.copy(fontFeatureSettings = "tnum"),
        )
        Text(
            if (state.monthlyCost.size > 1) "不同币种分别汇总，不做汇率换算" else "按月折算，年付已平摊到每月",
            color = Color.White.copy(alpha = 0.55f),
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Color.White.copy(alpha = 0.08f))
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SummaryStat("付费会员", state.paidCount.toString(), Modifier.weight(1f))
            StatDivider()
            SummaryStat("账号总数", state.accountCount.toString(), Modifier.weight(1f))
            StatDivider()
            SummaryStat("已用平台", state.platforms.count { it.accounts.isNotEmpty() }.toString(), Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatDivider() {
    Box(Modifier.width(1.dp).height(28.dp).background(Color.White.copy(alpha = 0.15f)))
}

@Composable
private fun SummaryStat(label: String, value: String, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = Color.White, style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum"))
        Text(label, color = Color.White.copy(alpha = 0.65f), style = MaterialTheme.typography.labelMedium)
    }
}

/** DeepSeek API 余额大卡片：金额来自官方接口，标注来源与更新时间 */
@Composable
private fun DeepSeekBalanceCard(account: AccountEntity, syncing: Boolean, onRefresh: () -> Unit, onOpen: () -> Unit) {
    val balance = account.apiBalance
    val currency = account.apiBalanceCurrency ?: "CNY"
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Gradients.DeepSeek)
            .glow(Color(0xFF7DD3FC), Color(0xFF6366F1))
            .clickable(onClick = onOpen)
            .padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PlatformAvatar(Platform.DEEPSEEK, size = 30.dp)
            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                Text("DeepSeek API 余额", color = Color.White, style = MaterialTheme.typography.titleSmall)
                Text(
                    account.label,
                    color = Color.White.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            GlassChip(if (balance != null) "自动同步" else "同步失败", dotColor = if (balance != null) Color(0xFF4ADE80) else Color(0xFFFCA5A5))
        }
        Spacer(Modifier.height(6.dp))
        Text(
            if (balance != null) formatMoney(balance, currency) else "未获取",
            color = Color.White,
            style = MaterialTheme.typography.displaySmall.copy(fontFeatureSettings = "tnum"),
        )
        val toppedUp = account.apiBalanceToppedUp
        val granted = account.apiBalanceGranted
        if (balance != null && (toppedUp != null || granted != null)) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                toppedUp?.let { BalancePart("充值", formatMoney(it, currency)) }
                granted?.let { BalancePart("赠送", formatMoney(it, currency)) }
            }
        }
        if (account.apiBalanceAvailable == false) {
            Text("余额不足，API 暂时无法调用", color = Color(0xFFFDE68A), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
        }
        account.syncError?.let { error ->
            Text(
                if (balance != null) "最近一次同步失败：$error（显示的是上次成功的数据）" else "同步失败：$error",
                color = Color(0xFFFECACA),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Spacer(Modifier.height(6.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.08f))
                .padding(start = 14.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                account.apiBalanceSyncedAt?.let { "更新于 ${formatDateTime(it)}" } ?: "尚未成功同步",
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f),
            )
            Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                if (syncing) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                } else {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Filled.Refresh, contentDescription = "刷新 DeepSeek 余额", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun BalancePart(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.bodySmall)
        Text(value, color = Color.White.copy(alpha = 0.92f), style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"), fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun GlassChip(text: String, dotColor: Color) {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(dotColor))
        Text(text, color = Color.White, style = MaterialTheme.typography.labelSmall)
    }
}

/** 还没有 DeepSeek 余额数据时的引导卡片 */
@Composable
private fun DeepSeekConnectCard(hasAccount: Boolean, onClick: () -> Unit) {
    AppCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PlatformAvatar(Platform.DEEPSEEK, size = 44.dp)
            Column(Modifier.weight(1f).padding(horizontal = 14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    if (hasAccount) "连接 DeepSeek API Key" else "自动同步 DeepSeek 余额",
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    if (hasAccount) "填入你自己的 API Key，余额会通过官方接口自动更新" else "添加 DeepSeek 账号并填入 API Key，余额自动更新",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AlertRow(icon: ImageVector, tint: Color, title: String, subtitle: String, onClick: () -> Unit) {
    AppCard(onClick = onClick, contentPadding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(tint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            }
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
private fun PlatformCard(summary: PlatformSummary, onClick: () -> Unit) {
    val top: AccountEntity? = summary.topAccount
    AppCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PlatformAvatar(summary.platform, size = 44.dp)
            Column(Modifier.weight(1f).padding(start = 14.dp)) {
                Text(summary.platform.displayName, style = MaterialTheme.typography.titleMedium)
                Text(
                    if (top == null) "尚未添加账号" else "${summary.accounts.size} 个账号 · ${top.planName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (top != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PlanSourcePill(top)
                    StatusPill(top.status)
                }
            } else {
                Text(
                    "添加",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                )
            }
        }
        if (top != null) {
            val details = buildList {
                if (top.price > 0) add(formatMoney(top.monthlyCost, top.currency) + " / 月")
                top.renewsAt?.let { add("续费 ${formatDate(it)}（${formatCountdown(it)}）") }
                if (summary.platform == Platform.DEEPSEEK) {
                    top.apiBalance?.let { add("余额 ${formatMoney(it, top.apiBalanceCurrency ?: "CNY")}") }
                }
            }
            if (details.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    details.joinToString("  ·  "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            // DeepSeek 关注的是余额（上方大卡片），额度没记录时不再显示“未获取”
            if (summary.platform != Platform.DEEPSEEK || top.quotaRemaining != null) {
                Spacer(Modifier.height(12.dp))
                QuotaBlock(top)
            }
        }
    }
}

@Composable
private fun DataNoticeCard(onImportReceipt: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("关于数据来源", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
        Text(
            "每项数据都标明了来源：手动记录、收据识别或自动同步。" +
                "ChatGPT、Claude、Gemini 没有公开的个人会员额度接口，额度只能手动记录；" +
                "DeepSeek 填写 API Key 后可自动同步余额。App 不会估算或虚构剩余额度。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
            lineHeight = 18.sp,
        )
        Row(
            Modifier
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.primary)
                .clickable(onClick = onImportReceipt)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(Icons.Filled.Email, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
            Text("从订阅收据识别套餐和续费日", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelLarge)
        }
    }
}
