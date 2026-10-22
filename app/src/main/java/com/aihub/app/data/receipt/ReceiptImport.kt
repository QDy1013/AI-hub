package com.aihub.app.data.receipt

import com.aihub.app.data.db.AccountEntity
import com.aihub.app.data.model.PlanSource
import com.aihub.app.data.model.Platform
import com.aihub.app.data.model.SubscriptionStatus
import java.time.ZoneId

/**
 * 把识别结果合并进账号。只覆盖收据里确实识别到的字段，其余保持原值；
 * 套餐来源标为“收据识别”，用户之后手动修改会改回“手动记录”。
 */
fun applyReceipt(
    receipt: ParsedReceipt,
    platform: Platform,
    target: AccountEntity?,
    now: Long = System.currentTimeMillis(),
    zone: ZoneId = ZoneId.systemDefault(),
): AccountEntity {
    val base = target ?: AccountEntity(
        platform = platform,
        label = "我的 ${platform.displayName}",
        email = "",
        planName = "未填写",
        status = SubscriptionStatus.FREE,
    )
    // 平台被用户改过时，收据里的套餐名可能对不上，只在平台一致时采用
    val planName = receipt.planName.takeIf { receipt.platform == platform }

    if (receipt.isTopUp) {
        return base.copy(
            platform = platform,
            planName = planName ?: base.planName,
            status = if (base.status == SubscriptionStatus.FREE) SubscriptionStatus.ACTIVE else base.status,
            planSource = PlanSource.RECEIPT,
            planImportedAt = now,
        )
    }

    val paid = (receipt.amount ?: 0.0) > 0 || (planName != null && planName != "Free")
    return base.copy(
        platform = platform,
        planName = planName ?: base.planName,
        price = receipt.amount ?: base.price,
        currency = receipt.currency ?: base.currency,
        billingCycle = receipt.billingCycle ?: base.billingCycle,
        renewsAt = receipt.renewsOn?.atStartOfDay(zone)?.toInstant()?.toEpochMilli() ?: base.renewsAt,
        status = when {
            receipt.canceled -> SubscriptionStatus.CANCELED
            paid -> SubscriptionStatus.ACTIVE
            else -> base.status
        },
        planSource = PlanSource.RECEIPT,
        planImportedAt = now,
    )
}
