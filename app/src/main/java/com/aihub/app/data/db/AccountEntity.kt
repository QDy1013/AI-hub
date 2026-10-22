package com.aihub.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.aihub.app.data.model.BillingCycle
import com.aihub.app.data.model.Platform
import com.aihub.app.data.model.QuotaSource
import com.aihub.app.data.model.SubscriptionStatus

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val platform: Platform,
    val label: String,
    val email: String,
    val planName: String,
    val status: SubscriptionStatus,
    val billingCycle: BillingCycle = BillingCycle.MONTHLY,
    /** 每个计费周期的费用 */
    val price: Double = 0.0,
    val currency: String = "USD",
    /** 下次续费 / 到期时间，epoch 毫秒 */
    val renewsAt: Long? = null,
    val quotaSource: QuotaSource = QuotaSource.UNKNOWN,
    val quotaLimit: Int? = null,
    val quotaUsed: Int? = null,
    val quotaUnit: String = "条消息",
    val quotaResetAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
) {
    val monthlyCost: Double
        get() = when (billingCycle) {
            BillingCycle.MONTHLY -> price
            BillingCycle.YEARLY -> price / 12.0
        }

    val isPaid: Boolean
        get() = price > 0 && status != SubscriptionStatus.EXPIRED && status != SubscriptionStatus.FREE

    /** 只有手动或自动来源且两个值都有时才返回剩余额度，否则返回 null（不猜测）。 */
    val quotaRemaining: Int?
        get() = if (quotaSource != QuotaSource.UNKNOWN && quotaLimit != null && quotaUsed != null) {
            (quotaLimit - quotaUsed).coerceAtLeast(0)
        } else {
            null
        }
}
