package com.aihub.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.aihub.app.data.model.BillingCycle
import com.aihub.app.data.model.PlanSource
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
    /** 套餐信息的来源：手动填写，或从用户导入的订阅收据中识别 */
    val planSource: PlanSource = PlanSource.MANUAL,
    /** 套餐信息最近一次从收据识别的时间 */
    val planImportedAt: Long? = null,
    /** API 账户余额，只在官方接口成功返回后写入（例如 DeepSeek /user/balance） */
    val apiBalance: Double? = null,
    val apiBalanceCurrency: String? = null,
    val apiBalanceSyncedAt: Long? = null,
    /** 余额构成：充值部分与赠送部分，接口没返回时为 null */
    val apiBalanceToppedUp: Double? = null,
    val apiBalanceGranted: Double? = null,
    /** 接口返回的 is_available：余额是否足够继续调用 */
    val apiBalanceAvailable: Boolean? = null,
    /** 最近一次自动同步失败的原因；成功后清空 */
    val syncError: String? = null,
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
