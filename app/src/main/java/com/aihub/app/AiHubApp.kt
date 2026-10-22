package com.aihub.app

import android.app.Application
import android.content.Context
import com.aihub.app.data.AccountRepository
import com.aihub.app.data.db.AppDatabase
import com.aihub.app.data.security.SecureStore

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
