package com.aihub.app.ui.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aihub.app.data.db.AccountEntity
import com.aihub.app.data.model.PlanSource
import com.aihub.app.data.model.Platform
import com.aihub.app.data.model.QuotaSource
import com.aihub.app.data.model.SubscriptionStatus
import com.aihub.app.ui.theme.SectionLabelStyle
import com.aihub.app.ui.theme.StatusColors
import com.aihub.app.util.formatCountdown
import com.aihub.app.util.formatDateTime
import com.aihub.app.util.formatMoney

val Platform.color: Color get() = Color(brandColor)

/** 平台图标：圆角方块 + 品牌色渐变 + 首字母，风格接近系统应用图标 */
@Composable
fun PlatformAvatar(platform: Platform, size: Dp = 40.dp) {
    val base = platform.color
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.3f))
            .background(Brush.linearGradient(listOf(lerp(base, Color.White, 0.18f), lerp(base, Color.Black, 0.18f)))),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = platform.displayName.take(1),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.44f).sp,
        )
    }
}

/** 统一的卡片：纯色底 + 极细描边，不用阴影，看起来更干净 */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: Dp = 18.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    val border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    val shape = MaterialTheme.shapes.large
    if (onClick != null) {
        Card(onClick = onClick, modifier = modifier.fillMaxWidth(), shape = shape, colors = colors, border = border) {
            Column(Modifier.padding(contentPadding), content = content)
        }
    } else {
        Card(modifier = modifier.fillMaxWidth(), shape = shape, colors = colors, border = border) {
            Column(Modifier.padding(contentPadding), content = content)
        }
    }
}

/** 分组标题：小号、加字距、弱化颜色 */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = SectionLabelStyle,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(start = 4.dp, top = 12.dp, bottom = 2.dp),
    )
}

@Composable
fun Pill(text: String, color: Color, dot: Boolean = false) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 9.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        if (dot) Box(Modifier.size(6.dp).clip(CircleShape).background(color))
        Text(text = text, color = color, style = MaterialTheme.typography.labelSmall)
    }
}

fun SubscriptionStatus.color(): Color = when (this) {
    SubscriptionStatus.ACTIVE -> StatusColors.Good
    SubscriptionStatus.CANCELED -> StatusColors.Warn
    SubscriptionStatus.EXPIRED -> StatusColors.Bad
    SubscriptionStatus.FREE -> StatusColors.Neutral
}

@Composable
fun StatusPill(status: SubscriptionStatus) = Pill(status.label, status.color(), dot = true)

/** 套餐信息来源标签：只有来自收据识别时才显示，避免手动记录的账号满屏标签 */
@Composable
fun PlanSourcePill(account: AccountEntity) {
    if (account.planSource == PlanSource.RECEIPT) Pill(PlanSource.RECEIPT.label, MaterialTheme.colorScheme.primary)
}

/** 细长圆角进度条 */
@Composable
fun SlimProgress(fraction: Float, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(6.dp)
                .clip(RoundedCornerShape(50))
                .background(color),
        )
    }
}

/**
 * 额度显示：只在有真实来源（手动/自动）的数据时画进度条，并注明来源；
 * 没有数据时明确写“未获取”，不显示任何估算值。
 */
@Composable
fun QuotaBlock(account: AccountEntity, modifier: Modifier = Modifier) {
    val remaining = account.quotaRemaining
    val limit = account.quotaLimit
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (remaining != null && limit != null) {
                    "剩余 $remaining / $limit ${account.quotaUnit}"
                } else {
                    "额度：未获取"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (remaining != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Pill(
                text = account.quotaSource.label,
                color = when (account.quotaSource) {
                    QuotaSource.AUTO -> StatusColors.Good
                    QuotaSource.MANUAL -> StatusColors.Warn
                    QuotaSource.UNKNOWN -> StatusColors.Neutral
                },
            )
        }
        if (remaining != null && limit != null && limit > 0) {
            val fraction = remaining.toFloat() / limit
            SlimProgress(
                fraction = fraction,
                color = when {
                    fraction < 0.15f -> StatusColors.Bad
                    fraction < 0.4f -> StatusColors.Warn
                    else -> StatusColors.Good
                },
            )
        }
        account.quotaResetAt?.let {
            Text(
                text = "额度重置：${formatCountdown(it)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * API 余额：只显示官方接口真实返回过的数值，并注明更新时间。
 * 同步失败时保留上次成功的数据，同时写明失败原因，不会把旧数据当成最新。
 */
@Composable
fun BalanceBlock(account: AccountEntity, modifier: Modifier = Modifier) {
    val balance = account.apiBalance
    val error = account.syncError
    if (balance == null && error == null) return
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (balance != null) {
                    "API 余额 ${formatMoney(balance, account.apiBalanceCurrency ?: "CNY")}"
                } else {
                    "API 余额：未获取"
                },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (balance != null) FontWeight.SemiBold else FontWeight.Normal,
            )
            Pill(
                text = if (balance != null) "自动同步" else "同步失败",
                color = if (balance != null) StatusColors.Good else StatusColors.Bad,
                dot = true,
            )
        }
        account.apiBalanceSyncedAt?.let {
            Text(
                "更新于 ${formatDateTime(it)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (error != null) {
            Text(
                if (balance != null) "最近一次同步失败：$error（上面是上次成功的数据）" else "同步失败：$error",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

/** 用 Custom Tabs 打开官方网页，登录态由浏览器保管，App 接触不到任何密码或 Cookie。 */
fun openOfficialPage(context: Context, url: String) {
    val uri = Uri.parse(url)
    try {
        CustomTabsIntent.Builder().setShowTitle(true).build().launchUrl(context, uri)
    } catch (e: ActivityNotFoundException) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (e2: ActivityNotFoundException) {
            Toast.makeText(context, "没有可用的浏览器", Toast.LENGTH_SHORT).show()
        }
    }
}
