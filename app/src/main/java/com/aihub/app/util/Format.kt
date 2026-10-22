package com.aihub.app.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
private val dateTimeFormatter = DateTimeFormatter.ofPattern("MM-dd HH:mm")

fun formatDate(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).format(dateFormatter)

fun formatDateTime(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).format(dateTimeFormatter)

fun formatMoney(amount: Double, currency: String): String {
    val symbol = when (currency.uppercase(Locale.ROOT)) {
        "USD" -> "$"
        "CNY", "RMB" -> "¥"
        "EUR" -> "€"
        "HKD" -> "HK$"
        "JPY" -> "¥"
        else -> "$currency "
    }
    return symbol + String.format(Locale.ROOT, "%.2f", amount)
}

/** 把时间差转换成“3 天后 / 5 小时后 / 已过期 2 天”这样的描述。 */
fun formatCountdown(targetMillis: Long, now: Long = System.currentTimeMillis()): String {
    val diff = targetMillis - now
    val minutes = abs(diff) / 60_000
    val hours = minutes / 60
    val days = hours / 24
    val span = when {
        days >= 1 -> "$days 天"
        hours >= 1 -> "$hours 小时"
        else -> "${minutes.coerceAtLeast(1)} 分钟"
    }
    return if (diff >= 0) "$span 后" else "已过 $span"
}

fun daysUntil(targetMillis: Long, now: Long = System.currentTimeMillis()): Long =
    Math.floorDiv(targetMillis - now, 86_400_000L)
