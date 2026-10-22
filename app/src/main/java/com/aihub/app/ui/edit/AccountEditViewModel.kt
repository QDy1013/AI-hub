package com.aihub.app.ui.edit

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aihub.app.data.AccountRepository
import com.aihub.app.data.ApiKeyEdit
import com.aihub.app.data.db.AccountEntity
import com.aihub.app.data.model.BillingCycle
import com.aihub.app.data.model.PlanSource
import com.aihub.app.data.model.Platform
import com.aihub.app.data.model.QuotaSource
import com.aihub.app.data.model.SubscriptionStatus
import com.aihub.app.data.sync.BalanceResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

data class AccountForm(
    val platform: Platform = Platform.CHATGPT,
    val label: String = "",
    val email: String = "",
    val planName: String = "Free",
    val status: SubscriptionStatus = SubscriptionStatus.FREE,
    val billingCycle: BillingCycle = BillingCycle.MONTHLY,
    val price: String = "0",
    val currency: String = "USD",
    val renewsAt: Long? = null,
    val quotaSource: QuotaSource = QuotaSource.UNKNOWN,
    val quotaLimit: String = "",
    val quotaUsed: String = "",
    val quotaUnit: String = "条消息",
    val quotaResetAt: Long? = null,
    val privateNote: String = "",
    /** 新输入的 API Key；已保存的 Key 不会回填到这里 */
    val apiKey: String = "",
    val removeApiKey: Boolean = false,
) {
    val priceValue: Double? get() = price.trim().toDoubleOrNull()
    val quotaLimitValue: Int? get() = quotaLimit.trim().toIntOrNull()
    val quotaUsedValue: Int? get() = quotaUsed.trim().toIntOrNull()

    val labelError: String? get() = if (label.isBlank()) "请填写账号名称" else null
    val priceError: String? get() = priceValue.let { if (it == null || it < 0) "请输入有效金额" else null }
    val quotaError: String?
        get() {
            if (quotaSource != QuotaSource.MANUAL) return null
            val limit = quotaLimitValue ?: return "请填写总额度"
            val used = quotaUsedValue ?: return "请填写已用额度"
            return if (used < 0 || limit <= 0) "额度必须为正数" else null
        }
    val isValid: Boolean get() = labelError == null && priceError == null && quotaError == null
}

class AccountEditViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: AccountRepository,
    private val appScope: CoroutineScope,
) : ViewModel() {

    private val accountId: Long = savedStateHandle.get<Long>(ARG_ID) ?: NEW_ID
    val isNew: Boolean get() = accountId == NEW_ID

    var form by mutableStateOf(AccountForm())
        private set
    var showErrors by mutableStateOf(false)
        private set
    var loaded by mutableStateOf(isNew)
        private set

    private var original: AccountEntity? = null

    /** 数据库中的最新记录，用于显示余额同步结果和套餐来源 */
    var stored by mutableStateOf<AccountEntity?>(null)
        private set
    var hasSavedApiKey by mutableStateOf(false)
        private set
    var syncing by mutableStateOf(false)
        private set

    init {
        if (isNew) {
            savedStateHandle.get<String>(ARG_PLATFORM)
                ?.let { name -> Platform.entries.firstOrNull { it.name == name } }
                ?.let { selectPlatform(it) }
        } else {
            viewModelScope.launch {
                repository.getAccount(accountId)?.let { a ->
                    original = a
                    stored = a
                    hasSavedApiKey = repository.hasApiKey(a.id)
                    form = AccountForm(
                        platform = a.platform,
                        label = a.label,
                        email = a.email,
                        planName = a.planName,
                        status = a.status,
                        billingCycle = a.billingCycle,
                        price = a.price.toPlainString(),
                        currency = a.currency,
                        renewsAt = a.renewsAt,
                        quotaSource = a.quotaSource,
                        quotaLimit = a.quotaLimit?.toString().orEmpty(),
                        quotaUsed = a.quotaUsed?.toString().orEmpty(),
                        quotaUnit = a.quotaUnit,
                        quotaResetAt = a.quotaResetAt,
                        privateNote = repository.getPrivateNote(a.id).orEmpty(),
                    )
                }
                loaded = true
            }
        }
    }

    fun update(transform: (AccountForm) -> AccountForm) {
        form = transform(form)
    }

    fun selectPlatform(platform: Platform) {
        val preset = platform.plans.first()
        form = form.copy(
            platform = platform,
            planName = preset.name,
            price = preset.monthlyPriceUsd.toPlainString(),
            status = if (preset.monthlyPriceUsd > 0) SubscriptionStatus.ACTIVE else SubscriptionStatus.FREE,
            label = if (form.label.isBlank() || form.label == "我的 ${form.platform.displayName}") {
                "我的 ${platform.displayName}"
            } else {
                form.label
            },
        )
    }

    /** 选择预设套餐时同时带出参考价格，用户可再修改 */
    fun selectPlan(name: String, price: Double) {
        form = form.copy(
            planName = name,
            price = price.toPlainString(),
            billingCycle = BillingCycle.MONTHLY,
            status = when {
                price <= 0 -> SubscriptionStatus.FREE
                form.status == SubscriptionStatus.FREE -> SubscriptionStatus.ACTIVE
                else -> form.status
            },
        )
    }

    fun save(onSaved: () -> Unit) {
        val f = form
        if (!f.isValid) {
            showErrors = true
            return
        }
        val manual = f.quotaSource == QuotaSource.MANUAL
        val base = original
        val price = f.priceValue ?: 0.0
        val planName = f.planName.trim().ifBlank { "未填写" }
        val currency = f.currency.trim().uppercase().ifBlank { "USD" }
        // 收据识别的套餐信息被手动改过后，来源改回“手动记录”
        val planUnchanged = base != null && base.planName == planName && base.status == f.status &&
            base.billingCycle == f.billingCycle && base.price == price && base.currency == currency &&
            base.renewsAt == f.renewsAt && base.platform == f.platform
        val planSource = if (base != null && planUnchanged) base.planSource else PlanSource.MANUAL
        val entity = (base ?: AccountEntity(
            platform = f.platform,
            label = "",
            email = "",
            planName = "",
            status = f.status,
        )).copy(
            platform = f.platform,
            label = f.label.trim(),
            email = f.email.trim(),
            planName = planName,
            status = f.status,
            billingCycle = f.billingCycle,
            price = price,
            currency = currency,
            renewsAt = f.renewsAt,
            quotaSource = f.quotaSource,
            quotaLimit = if (manual) f.quotaLimitValue else null,
            quotaUsed = if (manual) f.quotaUsedValue else null,
            quotaUnit = f.quotaUnit.trim().ifBlank { "条消息" },
            quotaResetAt = if (manual) f.quotaResetAt else null,
            planSource = planSource,
            planImportedAt = if (planSource == PlanSource.RECEIPT) base?.planImportedAt else null,
        )
        val keyEdit = when {
            f.apiKey.isNotBlank() -> ApiKeyEdit.Replace(f.apiKey)
            f.removeApiKey -> ApiKeyEdit.Remove
            else -> ApiKeyEdit.Keep
        }
        viewModelScope.launch {
            val id = repository.save(entity, f.privateNote, keyEdit)
            // 换了新 Key 后立即查一次余额；放在应用级作用域里，离开页面也会完成
            if (keyEdit is ApiKeyEdit.Replace && f.platform.balanceSync) {
                appScope.launch { repository.syncBalance(id) }
            }
            onSaved()
        }
    }

    /** 用已保存的 Key 立即查询余额，结果写入数据库并刷新本页显示 */
    fun syncNow(onResult: (String) -> Unit) {
        val id = original?.id ?: return
        if (syncing) return
        viewModelScope.launch {
            syncing = true
            val message = when (val r = repository.syncBalance(id)) {
                is BalanceResult.Success -> "余额已更新"
                is BalanceResult.Failure -> "同步失败：${r.message}"
                null -> "请先填写并保存 API Key"
            }
            stored = repository.getAccount(id)
            syncing = false
            onResult(message)
        }
    }

    fun delete(onDeleted: () -> Unit) {
        val a = original ?: return
        viewModelScope.launch {
            repository.delete(a)
            onDeleted()
        }
    }

    companion object {
        const val ARG_ID = "accountId"
        const val ARG_PLATFORM = "platform"
        const val NEW_ID = -1L
    }
}

private fun Double.toPlainString(): String =
    if (this == Math.floor(this)) toLong().toString() else toString()
