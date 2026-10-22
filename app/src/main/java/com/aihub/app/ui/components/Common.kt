package com.aihub.app.ui.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aihub.app.data.db.AccountEntity
import com.aihub.app.data.model.Platform
import com.aihub.app.data.model.QuotaSource
import com.aihub.app.data.model.SubscriptionStatus
import com.aihub.app.ui.theme.StatusColors
import com.aihub.app.util.formatCountdown

val Platform.color: Color get() = Color(brandColor)

@Composable
fun PlatformAvatar(platform: Platform, size: Dp = 40.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(platform.color),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = platform.displayName.take(1),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.42f).sp,
        )
    }
}

@Composable
fun Pill(text: String, color: Color) {
    Text(
        text = text,
        color = color,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

fun SubscriptionStatus.color(): Color = when (this) {
    SubscriptionStatus.ACTIVE -> StatusColors.Good
    SubscriptionStatus.CANCELED -> StatusColors.Warn
    SubscriptionStatus.EXPIRED -> StatusColors.Bad
    SubscriptionStatus.FREE -> StatusColors.Neutral
}

@Composable
fun StatusPill(status: SubscriptionStatus) = Pill(status.label, status.color())

/**
 * 额度显示：只在有真实来源（手动/自动）的数据时画进度条，并注明来源；
 * 没有数据时明确写“未获取”，不显示任何估算值。
 */
@Composable
fun QuotaBlock(account: AccountEntity, modifier: Modifier = Modifier) {
    val remaining = account.quotaRemaining
    val limit = account.quotaLimit
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier.fillMaxWidth(),
                color = when {
                    fraction < 0.15f -> StatusColors.Bad
                    fraction < 0.4f -> StatusColors.Warn
                    else -> StatusColors.Good
                },
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
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
