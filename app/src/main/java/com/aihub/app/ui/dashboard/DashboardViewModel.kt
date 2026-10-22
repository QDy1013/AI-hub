package com.aihub.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aihub.app.data.AccountRepository
import com.aihub.app.data.db.AccountEntity
import com.aihub.app.data.model.Platform
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class PlatformSummary(
    val platform: Platform,
    val accounts: List<AccountEntity>,
) {
    /** 该平台下最高档的付费账号，用于卡片上显示“当前会员等级” */
    val topAccount: AccountEntity?
        get() = accounts.filter { it.isPaid }.maxByOrNull { it.monthlyCost } ?: accounts.firstOrNull()
}

data class DashboardState(
    val loading: Boolean = true,
    val accountCount: Int = 0,
    val paidCount: Int = 0,
    /** 按币种汇总的月均费用，不同币种不做汇率换算以免误导 */
    val monthlyCost: Map<String, Double> = emptyMap(),
    val platforms: List<PlatformSummary> = emptyList(),
    val upcomingRenewals: List<AccountEntity> = emptyList(),
    val lowQuota: List<AccountEntity> = emptyList(),
)

class DashboardViewModel(repository: AccountRepository) : ViewModel() {

    val state: StateFlow<DashboardState> = repository.observeAccounts()
        .map { accounts -> buildState(accounts, System.currentTimeMillis()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardState())

    companion object {
        private const val RENEWAL_WINDOW_MS = 30L * 24 * 60 * 60 * 1000
        private const val LOW_QUOTA_RATIO = 0.2

        fun buildState(accounts: List<AccountEntity>, now: Long): DashboardState {
            val paid = accounts.filter { it.isPaid }
            return DashboardState(
                loading = false,
                accountCount = accounts.size,
                paidCount = paid.size,
                monthlyCost = paid.groupBy { it.currency.uppercase() }
                    .mapValues { (_, list) -> list.sumOf { it.monthlyCost } },
                platforms = Platform.entries.map { p ->
                    PlatformSummary(p, accounts.filter { it.platform == p })
                },
                upcomingRenewals = accounts
                    .filter { it.renewsAt != null && it.renewsAt - now in -RENEWAL_WINDOW_MS..RENEWAL_WINDOW_MS }
                    .sortedBy { it.renewsAt },
                lowQuota = accounts.filter { a ->
                    val remaining = a.quotaRemaining
                    val limit = a.quotaLimit
                    remaining != null && limit != null && limit > 0 && remaining.toDouble() / limit < LOW_QUOTA_RATIO
                },
            )
        }
    }
}
