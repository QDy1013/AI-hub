package com.aihub.app

import android.graphics.Bitmap
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.aihub.app.data.db.AccountEntity
import com.aihub.app.data.model.Platform
import com.aihub.app.data.model.SubscriptionStatus
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

/**
 * DeepSeek 余额卡片与收据导入页的截图。余额直接写入数据库（模拟一次成功同步），
 * 不需要真实的 API Key；测试结束后删除该账号。
 */
@RunWith(AndroidJUnit4::class)
class DeepSeekScreensTest {

    @get:Rule
    val compose = createEmptyComposeRule()

    private lateinit var scenario: ActivityScenario<MainActivity>
    private val container get() = ApplicationProvider.getApplicationContext<AiHubApp>().container
    private var accountId = 0L

    @Before
    fun setUp() {
        container.settings.secureScreen = false
        accountId = runBlocking {
            container.accountRepository.saveImported(
                AccountEntity(
                    platform = Platform.DEEPSEEK,
                    label = "DeepSeek 开发",
                    email = "",
                    planName = "API 按量付费",
                    status = SubscriptionStatus.ACTIVE,
                    apiBalance = 110.0,
                    apiBalanceCurrency = "CNY",
                    apiBalanceSyncedAt = System.currentTimeMillis(),
                    apiBalanceToppedUp = 100.0,
                    apiBalanceGranted = 10.0,
                    apiBalanceAvailable = true,
                ),
            )
        }
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    @After
    fun tearDown() {
        scenario.close()
        runBlocking {
            container.accountRepository.getAccount(accountId)?.let { container.accountRepository.delete(it) }
        }
    }

    @Test
    fun deepSeekBalanceAndReceiptImport() {
        // 仪表盘：DeepSeek 余额大卡片，金额、构成与来源
        waitForText("DeepSeek API 余额")
        compose.onNodeWithText("¥110.00").assertExists()
        compose.onNodeWithText("¥100.00").assertExists()
        shot("10_dashboard_deepseek")

        // 点卡片进入账号编辑页，API Key 区块排在前面
        compose.onNodeWithText("DeepSeek API 余额").performClick()
        waitForText("编辑账号")
        shot("11_edit_deepseek")
        compose.onNodeWithContentDescription("返回").performClick()

        // 收据导入页：粘贴一段 Claude 收据并识别
        waitForText("DeepSeek API 余额")
        compose.onNodeWithContentDescription("导入订阅收据").performClick()
        compose.onNodeWithTag("field_receipt").performTextInput(
            "Receipt from Anthropic\nClaude Max plan (5x) – monthly\nDate paid: 2026-10-01\nAmount paid\nUS$100.00",
        )
        compose.onNodeWithText("识别").performClick()
        waitForText("Max 5x")
        shot("12_receipt_import")
    }

    private fun waitForText(text: String) {
        compose.waitUntil(5_000) { compose.onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun shot(name: String) {
        compose.waitForIdle()
        Thread.sleep(400)
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val bitmap = instrumentation.uiAutomation.takeScreenshot() ?: return
        val dir = instrumentation.targetContext.getExternalFilesDir("screenshots") ?: return
        FileOutputStream(File(dir, "$name.png")).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
