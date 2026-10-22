package com.aihub.app

import android.graphics.Bitmap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

/**
 * 端到端走一遍主流程，并在每一步截图（CI 会把截图拉回来人工检查）。
 * 假设是全新安装：CI 每次都在新模拟器上运行。
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class MainFlowTest {

    @get:Rule
    val compose = createEmptyComposeRule()

    private lateinit var scenario: ActivityScenario<MainActivity>

    @Before
    fun launch() {
        // 隐私屏幕会让截图变黑，测试时关闭
        ApplicationProvider.getApplicationContext<AiHubApp>().container.settings.secureScreen = false
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    @After
    fun close() {
        scenario.close()
    }

    @Test
    fun addEditAndDeleteAccounts() {
        // 1. 空仪表盘
        compose.onNodeWithText("平台概览").assertExists()
        shot("01_dashboard_empty")

        // 2. 从仪表盘的 Claude 卡片添加账号，记录一个偏低的手动额度
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Claude"))
        compose.onNodeWithText("Claude").performClick()
        compose.onNodeWithText("添加账号").assertExists()
        compose.onNodeWithText("Pro").performClick()
        compose.onNodeWithTag("field_email").performTextInput("me@example.com")
        compose.onNodeWithText("手动记录").performScrollTo().performClick()
        compose.onNodeWithTag("field_quota_used").performScrollTo().performTextInput("40")
        compose.onNodeWithTag("field_quota_limit").performTextInput("45")
        shot("02_edit_claude")
        compose.onNodeWithTag("btn_save").performScrollTo().performClick()

        // 3. 仪表盘出现额度偏低提醒
        waitForText("Claude · 我的 Claude 额度偏低")
        shot("03_dashboard_with_claude")

        // 4. 账号页添加第二个平台的账号
        compose.onNodeWithText("账号").performClick()
        compose.onNodeWithText("添加账号").performClick()
        compose.onNodeWithText("ChatGPT").performClick()
        compose.onNodeWithText("Plus").performClick()
        compose.onNodeWithTag("btn_save").performScrollTo().performClick()
        waitForText("我的 ChatGPT")
        shot("04_accounts_list")

        // 5. 仪表盘汇总费用：Claude Pro $20 + ChatGPT Plus $20
        compose.onNodeWithText("仪表盘").performClick()
        waitForText("$40.00")
        shot("05_dashboard_two_accounts")

        // 6. 设置页
        compose.onNodeWithText("设置").performClick()
        compose.onNodeWithText("隐私屏幕").assertExists()
        shot("06_settings")

        // 7. 打开 Claude 账号并删除
        compose.onNodeWithText("账号").performClick()
        compose.onNodeWithText("我的 Claude").performClick()
        compose.onNodeWithText("编辑账号").assertExists()
        shot("07_edit_existing")
        compose.onNodeWithContentDescription("删除账号").performClick()
        shot("08_delete_dialog")
        compose.onNodeWithText("删除").performClick()
        compose.waitUntil(5_000) { compose.onAllNodes(hasText("我的 Claude")).fetchSemanticsNodes().isEmpty() }
        compose.onNodeWithText("我的 ChatGPT").assertExists()
        shot("09_after_delete")
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
