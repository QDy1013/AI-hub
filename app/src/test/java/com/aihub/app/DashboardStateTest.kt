package com.aihub.app

import com.aihub.app.data.db.AccountEntity
import com.aihub.app.data.model.BillingCycle
import com.aihub.app.data.model.Platform
import com.aihub.app.data.model.QuotaSource
import com.aihub.app.data.model.SubscriptionStatus
import com.aihub.app.ui.dashboard.DashboardViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DashboardStateTest {
    private val now = 1_800_000_000_000L
    private val day = 86_400_000L

    private fun account(
        id: Long,
        platform: Platform = Platform.CLAUDE,
        price: Double = 20.0,
        status: SubscriptionStatus = SubscriptionStatus.ACTIVE,
        cycle: BillingCycle = BillingCycle.MONTHLY,
        currency: String = "USD",
        renewsAt: Long? = null,
        source: QuotaSource = QuotaSource.UNKNOWN,
        limit: Int? = null,
        used: Int? = null,
    ) = AccountEntity(
        id = id, platform = platform, label = "a$id", email = "", planName = "Pro",
        status = status, billingCycle = cycle, price = price, currency = currency, renewsAt = renewsAt,
        quotaSource = source, quotaLimit = limit, quotaUsed = used,
    )

    @Test
    fun monthlyCostIsGroupedByCurrencyAndYearlyIsDividedBy12() {
        val state = DashboardViewModel.buildState(
            listOf(
                account(1, price = 20.0),
                account(2, price = 240.0, cycle = BillingCycle.YEARLY),
                account(3, price = 140.0, currency = "cny"),
                account(4, price = 20.0, status = SubscriptionStatus.EXPIRED),
            ),
            now,
        )
        assertEquals(40.0, state.monthlyCost["USD"]!!, 0.001)
        assertEquals(140.0, state.monthlyCost["CNY"]!!, 0.001)
        assertEquals(3, state.paidCount)
        assertEquals(4, state.accountCount)
    }

    @Test
    fun unknownQuotaIsNeverReportedAsRemaining() {
        val a = account(1, source = QuotaSource.UNKNOWN, limit = 100, used = 10)
        assertNull(a.quotaRemaining)
        assertTrue(DashboardViewModel.buildState(listOf(a), now).lowQuota.isEmpty())
    }

    @Test
    fun lowManualQuotaAndNearRenewalsAreFlagged() {
        val low = account(1, source = QuotaSource.MANUAL, limit = 100, used = 90)
        val soon = account(2, renewsAt = now + 3 * day)
        val far = account(3, renewsAt = now + 90 * day)
        val state = DashboardViewModel.buildState(listOf(low, soon, far), now)
        assertEquals(listOf(1L), state.lowQuota.map { it.id })
        assertEquals(listOf(2L), state.upcomingRenewals.map { it.id })
        assertEquals(Platform.entries.size, state.platforms.size)
    }
}
