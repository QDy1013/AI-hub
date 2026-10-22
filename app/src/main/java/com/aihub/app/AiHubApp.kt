package com.aihub.app

import android.app.Application
import android.content.Context
import com.aihub.app.data.AccountRepository
import com.aihub.app.data.db.AppDatabase
import com.aihub.app.data.security.SecureStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AiHubApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

/** 简单的手动依赖注入，项目规模变大后可换成 Hilt。 */
class AppContainer(context: Context) {
    private val database = AppDatabase.build(context)
    val secureStore = SecureStore(context)
    val accountRepository = AccountRepository(database.accountDao(), secureStore)
    val settings = AppSettings(context)
    val sharedText = SharedTextInbox()
    /** 生命周期跟随进程的协程作用域，用于离开页面后仍需完成的同步 */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
}

/**
 * 其他 App（如 Gmail、邮件）分享到 AI Hub 的文字，暂存在内存里等收据导入页取走。
 * 不落盘：收据原文识别完即丢弃。
 */
class SharedTextInbox {
    private val _pending = MutableStateFlow<String?>(null)
    val pending: StateFlow<String?> = _pending.asStateFlow()

    fun offer(text: String) {
        _pending.value = text.take(MAX_LENGTH)
    }

    fun consume(): String? = _pending.value.also { _pending.value = null }

    private companion object {
        const val MAX_LENGTH = 20_000
    }
}

class AppSettings(context: Context) {
    private val prefs = context.getSharedPreferences("aihub_settings", Context.MODE_PRIVATE)

    /** 开启后在最近任务中隐藏界面、禁止截屏 */
    var secureScreen: Boolean
        get() = prefs.getBoolean(KEY_SECURE_SCREEN, true)
        set(value) = prefs.edit().putBoolean(KEY_SECURE_SCREEN, value).apply()

    private companion object {
        const val KEY_SECURE_SCREEN = "secure_screen"
    }
}
