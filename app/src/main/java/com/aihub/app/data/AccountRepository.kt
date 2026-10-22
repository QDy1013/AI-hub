package com.aihub.app.data

import com.aihub.app.data.db.AccountDao
import com.aihub.app.data.db.AccountEntity
import com.aihub.app.data.security.SecureStore
import kotlinx.coroutines.flow.Flow

/**
 * 账号数据统一入口。普通字段进 Room；私密备注经 Keystore 加密后单独存放，
 * 不进入数据库明文。
 */
class AccountRepository(
    private val dao: AccountDao,
    private val secureStore: SecureStore,
) {
    fun observeAccounts(): Flow<List<AccountEntity>> = dao.observeAll()

    suspend fun getAccount(id: Long): AccountEntity? = dao.getById(id)

    fun getPrivateNote(id: Long): String? = secureStore.get(SecureStore.accountNoteKey(id))

    suspend fun save(account: AccountEntity, privateNote: String?): Long {
        val now = System.currentTimeMillis()
        val id = if (account.id == 0L) {
            dao.insert(account.copy(createdAt = now, updatedAt = now))
        } else {
            dao.update(account.copy(updatedAt = now))
            account.id
        }
        secureStore.put(SecureStore.accountNoteKey(id), privateNote?.trim())
        return id
    }

    suspend fun delete(account: AccountEntity) {
        dao.delete(account)
        secureStore.remove(SecureStore.accountNoteKey(account.id))
    }
}
