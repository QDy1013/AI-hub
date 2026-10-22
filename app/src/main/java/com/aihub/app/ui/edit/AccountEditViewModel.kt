package com.aihub.app.ui.edit

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aihub.app.data.AccountRepository
import com.aihub.app.data.db.AccountEntity
import com.aihub.app.data.model.BillingCycle
import com.aihub.app.data.model.Platform
import com.aihub.app.data.model.QuotaSource
import com.aihub.app.data.model.SubscriptionStatus
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

    init {
        if (isNew) {
            savedStateHandle.get<String>(ARG_PLATFORM)
                ?.let { name -> Platform.entries.firstOrNull { it.name == name } }
                ?.let { selectPlatform(it) }
        } else {
            viewModelScope.launch {
                repository.getAccount(accountId)?.let { a ->
                    original = a
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
        val entity = (original ?: AccountEntity(
            platform = f.platform,
            label = "",
            email = "",
            planName = "",
            status = f.status,
        )).copy(
            platform = f.platform,
            label = f.label.trim(),
            email = f.email.trim(),
            planName = f.planName.trim().ifBlank { "未填写" },
            status = f.status,
            billingCycle = f.billingCycle,
            price = f.priceValue ?: 0.0,
            currency = f.currency.trim().uppercase().ifBlank { "USD" },
            renewsAt = f.renewsAt,
            quotaSource = f.quotaSource,
            quotaLimit = if (manual) f.quotaLimitValue else null,
            quotaUsed = if (manual) f.quotaUsedValue else null,
            quotaUnit = f.quotaUnit.trim().ifBlank { "条消息" },
            quotaResetAt = if (manual) f.quotaResetAt else null,
        )
        viewModelScope.launch {
            repository.save(entity, f.privateNote)
            onSaved()
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
