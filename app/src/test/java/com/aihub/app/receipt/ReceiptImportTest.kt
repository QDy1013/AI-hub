package com.aihub.app.receipt

import com.aihub.app.data.db.AccountEntity
import com.aihub.app.data.model.BillingCycle
import com.aihub.app.data.model.PlanSource
import com.aihub.app.data.model.Platform
import com.aihub.app.data.model.QuotaSource
import com.aihub.app.data.model.SubscriptionStatus
import com.aihub.app.data.receipt.ParsedReceipt
import com.aihub.app.data.receipt.applyReceipt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class ReceiptImportTest {
    private val now = 1_800_000_000_000L

    private fun receipt(
        platform: Platform? = Platform.CLAUDE,
        plan: String? = "Pro",
        amount: Double? = 20.0,
        currency: String? = "USD",
        cycle: BillingCycle? = BillingCycle.MONTHLY,
        renews: LocalDate? = LocalDate.of(2026, 11, 1),
        canceled: Boolean = false,
        topUp: Boolean = false,
    ) = ParsedReceipt(platform, plan, amount, currency, cycle, LocalDate.of(2026, 10, 1), renews, false, canceled, topUp)

    private val existing = AccountEntity(
        id = 7, platform = Platform.CLAUDE, label = "工作号", email = "a@b.c", planName = "Free",
        status = SubscriptionStatus.FREE, price = 0.0, quotaSource = QuotaSource.MANUAL, quotaLimit = 45, quotaUsed = 3,
    )

    @Test
    fun updatesOnlyPlanFieldsOfExistingAccount() {
        val a = applyReceipt(receipt(), Platform.CLAUDE, existing, now, ZoneOffset.UTC)
        assertEquals(7L, a.id)
        assertEquals("工作号", a.label)
        assertEquals("Pro", a.planName)
        assertEquals(20.0, a.price, 0.0)
        assertEquals(SubscriptionStatus.ACTIVE, a.status)
        assertEquals(LocalDate.of(2026, 11, 1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(), a.renewsAt)
        assertEquals(PlanSource.RECEIPT, a.planSource)
        assertEquals(now, a.planImportedAt)
        // 额度是手动记录的，收据不涉及，保持不变
        assertEquals(QuotaSource.MANUAL, a.quotaSource)
        assertEquals(45, a.quotaLimit)
    }

    @Test
    fun missingFieldsKeepPreviousValues() {
        val base = existing.copy(price = 100.0, planName = "Max 5x", currency = "USD", status = SubscriptionStatus.ACTIVE)
        val a = applyReceipt(receipt(plan = null, amount = null, currency = null, cycle = null, renews = null), Platform.CLAUDE, base, now)
        assertEquals("Max 5x", a.planName)
        assertEquals(100.0, a.price, 0.0)
        assertNull(a.renewsAt)
    }

    @Test
    fun canceledReceiptMarksCanceled() {
        val a = applyReceipt(receipt(canceled = true, amount = null), Platform.CLAUDE, existing, now)
        assertEquals(SubscriptionStatus.CANCELED, a.status)
    }

    @Test
    fun topUpDoesNotBecomeMonthlyCost() {
        val r = receipt(platform = Platform.DEEPSEEK, plan = "API 按量付费", amount = 50.0, currency = "CNY", cycle = null, renews = null, topUp = true)
        val a = applyReceipt(r, Platform.DEEPSEEK, null, now)
        assertEquals(0L, a.id)
        assertEquals("我的 DeepSeek", a.label)
        assertEquals("API 按量付费", a.planName)
        assertEquals(0.0, a.monthlyCost, 0.0)
    }

    @Test
    fun planNameIgnoredWhenUserPicksDifferentPlatform() {
        val a = applyReceipt(receipt(platform = Platform.CHATGPT, plan = "Plus"), Platform.CLAUDE, existing, now)
        assertEquals("Free", a.planName)
    }
}
