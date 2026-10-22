package com.aihub.app.ui.receipt

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aihub.app.SharedTextInbox
import com.aihub.app.data.AccountRepository
import com.aihub.app.data.db.AccountEntity
import com.aihub.app.data.model.Platform
import com.aihub.app.data.receipt.ParsedReceipt
import com.aihub.app.data.receipt.ReceiptParser
import com.aihub.app.data.receipt.applyReceipt
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 收据导入：文字只在本机内存中识别，不保存原文、不上传。
 * 识别结果必须由用户确认目标账号后才写入。
 */
class ReceiptImportViewModel(
    private val repository: AccountRepository,
    private val inbox: SharedTextInbox,
) : ViewModel() {

    var text by mutableStateOf("")
        private set
    var result by mutableStateOf<ParsedReceipt?>(null)
        private set
    /** 用户选择的平台；默认取识别结果 */
    var platform by mutableStateOf<Platform?>(null)
        private set
    /** 写入的目标账号，null 表示新建 */
    var targetId by mutableStateOf<Long?>(null)
        private set

    val accounts: StateFlow<List<AccountEntity>> = repository.observeAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        // 分享进来的文字（包括页面已打开时再次分享）自动填入并识别
        viewModelScope.launch {
            inbox.pending.filterNotNull().collect {
                inbox.consume()?.let { shared ->
                    text = shared
                    parse()
                }
            }
        }
    }

    fun updateText(value: String) {
        text = value
    }

    fun parse() {
        val parsed = ReceiptParser.parse(text)
        result = parsed
        selectPlatform(parsed.platform)
    }

    fun selectPlatform(value: Platform?) {
        platform = value
        targetId = null
        if (value == null) return
        // 同平台只有一个账号时默认写入它，否则让用户自己选
        viewModelScope.launch {
            val single = repository.observeAccounts().first().singleOrNull { it.platform == value }
            if (platform == value && targetId == null) targetId = single?.id
        }
    }

    fun selectTarget(id: Long?) {
        targetId = id
    }

    fun apply(onDone: (Long) -> Unit) {
        val parsed = result ?: return
        val p = platform ?: return
        viewModelScope.launch {
            val target = targetId?.let { repository.getAccount(it) }?.takeIf { it.platform == p }
            val id = repository.saveImported(applyReceipt(parsed, p, target))
            onDone(id)
        }
    }

    fun clear() {
        text = ""
        result = null
        platform = null
        targetId = null
    }
}
