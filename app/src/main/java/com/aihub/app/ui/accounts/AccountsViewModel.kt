package com.aihub.app.ui.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aihub.app.data.AccountRepository
import com.aihub.app.data.db.AccountEntity
import com.aihub.app.data.model.Platform
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AccountsState(
    val filter: Platform? = null,
    val groups: Map<Platform, List<AccountEntity>> = emptyMap(),
    val totalCount: Int = 0,
)

class AccountsViewModel(private val repository: AccountRepository) : ViewModel() {
    private val filter = MutableStateFlow<Platform?>(null)

    val state: StateFlow<AccountsState> = combine(repository.observeAccounts(), filter) { accounts, f ->
        AccountsState(
            filter = f,
            groups = accounts.filter { f == null || it.platform == f }
                .groupBy { it.platform }
                .toSortedMap(compareBy { it.ordinal }),
            totalCount = accounts.size,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AccountsState())

    fun setFilter(platform: Platform?) {
        filter.value = platform
    }

    fun delete(account: AccountEntity) {
        viewModelScope.launch { repository.delete(account) }
    }
}
