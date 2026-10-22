package com.aihub.app.data

import com.aihub.app.data.db.AccountDao
import com.aihub.app.data.db.AccountEntity
import com.aihub.app.data.security.SecureStore
import com.aihub.app.data.sync.BalanceResult
import com.aihub.app.data.sync.DeepSeekClient
import kotlinx.coroutines.flow.Flow

/** 编辑账号时对 API Key 的操作。Key 只在用户输入时写入，界面上从不回显。 */
sealed interface ApiKeyEdit {
    data object Keep : ApiKeyEdit
    data object Remove : ApiKeyEdit
    data class Replace(val key: String) : ApiKeyEdit
}

/** 一次批量同步的结果 */
data class SyncSummary(val succeeded: Int, val failures: List<Pair<AccountEntity, String>>) {
    val attempted: Int get() = succeeded + failures.size
}

/**
 * 账号数据统一入口。普通字段进 Room；私密备注和 API Key 经 Keystore 加密后单独存放，
 * 不进入数据库明文。
 */
class AccountRepository(
    private val dao: AccountDao,
    private val secureStore: SecureStore,
    private val deepSeek: DeepSeekClient = DeepSeekClient(),
) {
    fun observeAccounts(): Flow<List<AccountEntity>> = dao.observeAll()

    suspend fun getAccount(id: Long): AccountEntity? = dao.getById(id)

    fun getPrivateNote(id: Long): String? = secureStore.get(SecureStore.accountNoteKey(id))

    fun hasApiKey(id: Long): Boolean = secureStore.get(SecureStore.apiKeyKey(id)) != null

    suspend fun save(account: AccountEntity, privateNote: String?, apiKey: ApiKeyEdit = ApiKeyEdit.Keep): Long {
        val now = System.currentTimeMillis()
        val id = if (account.id == 0L) {
            dao.insert(account.copy(createdAt = now, updatedAt = now))
        } else {
            // 同步结果只由同步流程写入；编辑页拿到的可能是旧值，这里以数据库里的为准
            val current = dao.getById(account.id)
            dao.update(
                account.copy(
                    updatedAt = now,
                    apiBalance = current?.apiBalance,
                    apiBalanceCurrency = current?.apiBalanceCurrency,
                    apiBalanceSyncedAt = current?.apiBalanceSyncedAt,
                    apiBalanceToppedUp = current?.apiBalanceToppedUp,
                    apiBalanceGranted = current?.apiBalanceGranted,
                    apiBalanceAvailable = current?.apiBalanceAvailable,
                    syncError = current?.syncError,
                ),
            )
            account.id
        }
        secureStore.put(SecureStore.accountNoteKey(id), privateNote?.trim())

        val effectiveKeyEdit = if (account.platform.balanceSync) apiKey else ApiKeyEdit.Remove
        when (effectiveKeyEdit) {
            ApiKeyEdit.Keep -> Unit
            ApiKeyEdit.Remove -> if (hasApiKey(id) || account.apiBalance != null) {
                secureStore.remove(SecureStore.apiKeyKey(id))
                clearBalance(id)
            }
            is ApiKeyEdit.Replace -> {
                secureStore.put(SecureStore.apiKeyKey(id), effectiveKeyEdit.key.trim())
                clearBalance(id)
            }
        }
        return id
    }

    /** 收据导入：只改套餐相关字段，不动备注和 API Key */
    suspend fun saveImported(account: AccountEntity): Long {
        val now = System.currentTimeMillis()
        return if (account.id == 0L) {
            dao.insert(account.copy(createdAt = now, updatedAt = now))
        } else {
            dao.update(account.copy(updatedAt = now))
            account.id
        }
    }

    suspend fun delete(account: AccountEntity) {
        dao.delete(account)
        secureStore.remove(SecureStore.accountNoteKey(account.id))
        secureStore.remove(SecureStore.apiKeyKey(account.id))
    }

    /**
     * 用保存的 API Key 调官方接口刷新余额。没有 Key 或平台不支持时返回 null。
     * 失败时保留上一次成功的数据，同时记录失败原因，界面会同时显示两者。
     */
    suspend fun syncBalance(id: Long): BalanceResult? {
        val account = dao.getById(id) ?: return null
        if (!account.platform.balanceSync) return null
        val key = secureStore.get(SecureStore.apiKeyKey(id)) ?: return null
        val result = deepSeek.fetchBalance(key)
        // 请求期间账号可能被编辑过，重新读取后只更新同步相关字段
        val latest = dao.getById(id) ?: return result
        val updated = when (result) {
            is BalanceResult.Success -> {
                val primary = result.balance.primary ?: return result
                latest.copy(
                    apiBalance = primary.total,
                    apiBalanceCurrency = primary.currency,
                    apiBalanceSyncedAt = System.currentTimeMillis(),
                    apiBalanceToppedUp = primary.toppedUp,
                    apiBalanceGranted = primary.granted,
                    apiBalanceAvailable = result.balance.isAvailable,
                    syncError = null,
                )
            }
            is BalanceResult.Failure -> latest.copy(syncError = result.message)
        }
        dao.update(updated)
        return result
    }

    /** 同步所有已填写 API Key 的账号；staleAfterMs 不为空时只同步超过该时长未更新的账号 */
    suspend fun syncAllBalances(accounts: List<AccountEntity>, staleAfterMs: Long? = null): SyncSummary {
        val now = System.currentTimeMillis()
        var ok = 0
        val failures = mutableListOf<Pair<AccountEntity, String>>()
        accounts
            .filter { it.platform.balanceSync && hasApiKey(it.id) }
            .filter { a -> staleAfterMs == null || a.apiBalanceSyncedAt.let { it == null || now - it > staleAfterMs } }
            .forEach { a ->
                when (val r = syncBalance(a.id)) {
                    is BalanceResult.Success -> ok++
                    is BalanceResult.Failure -> failures += a to r.message
                    null -> Unit
                }
            }
        return SyncSummary(ok, failures)
    }

    private suspend fun clearBalance(id: Long) {
        val current = dao.getById(id) ?: return
        dao.update(
            current.copy(
                apiBalance = null,
                apiBalanceCurrency = null,
                apiBalanceSyncedAt = null,
                apiBalanceToppedUp = null,
                apiBalanceGranted = null,
                apiBalanceAvailable = null,
                syncError = null,
            ),
        )
    }
}
