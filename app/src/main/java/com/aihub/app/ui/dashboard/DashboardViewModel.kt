package com.aihub.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aihub.app.data.AccountRepository
import com.aihub.app.data.SyncSummary
import com.aihub.app.data.db.AccountEntity
import com.aihub.app.data.model.Platform
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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

class DashboardViewModel(private val repository: AccountRepository) : ViewModel() {

    val state: StateFlow<DashboardState> = repository.observeAccounts()
        .map { accounts -> buildState(accounts, System.currentTimeMillis()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardState())

    private val _syncing = MutableStateFlow(false)
    val syncing: StateFlow<Boolean> = _syncing.asStateFlow()

    /** 同步结果提示，显示后由界面调用 [messageShown] 清除 */
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        // 打开仪表盘时，静默刷新超过 1 小时没更新的余额
        sync(staleAfterMs = AUTO_SYNC_STALE_MS, quiet = true)
    }

    fun refresh() = sync(staleAfterMs = null, quiet = false)

    fun messageShown() {
        _message.value = null
    }

    private fun sync(staleAfterMs: Long?, quiet: Boolean) {
        if (_syncing.value) return
        viewModelScope.launch {
            _syncing.value = true
            try {
                val accounts = repository.observeAccounts().first()
                val summary = repository.syncAllBalances(accounts, staleAfterMs)
                val text = describe(summary)
                if (!quiet || summary.failures.isNotEmpty()) _message.value = text
            } finally {
                _syncing.value = false
            }
        }
    }

    companion object {
        private const val AUTO_SYNC_STALE_MS = 60L * 60 * 1000

        fun describe(summary: SyncSummary): String = when {
            summary.attempted == 0 ->
                "没有可自动同步的账号。目前只有 DeepSeek 提供官方余额接口，在账号里填写 API Key 后即可同步。"
            summary.failures.isEmpty() -> "已同步 ${summary.succeeded} 个账号的余额"
            else -> {
                val (account, reason) = summary.failures.first()
                val head = "${account.platform.displayName} · ${account.label}：$reason"
                if (summary.failures.size == 1) "同步失败：$head" else "${summary.failures.size} 个账号同步失败，例如 $head"
            }
        }

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
