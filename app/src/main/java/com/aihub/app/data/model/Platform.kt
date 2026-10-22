package com.aihub.app.data.model

/**
 * 支持的平台。套餐价格仅作为录入时的默认值，实际以用户填写为准，
 * 因为各平台会调整价格和地区定价。
 */
enum class Platform(
    val displayName: String,
    val vendor: String,
    val brandColor: Long,
    val chatUrl: String,
    val billingUrl: String,
    val plans: List<PlanPreset>,
    /** 用户创建 API Key 的官方页面，只有支持余额同步的平台才有 */
    val apiKeyUrl: String? = null,
    /** 是否有官方接口可以用用户自己的 API Key 查询余额 */
    val balanceSync: Boolean = false,
) {
    CHATGPT(
        displayName = "ChatGPT",
        vendor = "OpenAI",
        brandColor = 0xFF10A37F,
        chatUrl = "https://chatgpt.com/",
        billingUrl = "https://chatgpt.com/#settings/Subscription",
        plans = listOf(
            PlanPreset("Free", 0.0),
            PlanPreset("Plus", 20.0),
            PlanPreset("Pro", 200.0),
            PlanPreset("Team", 30.0),
        ),
    ),
    CLAUDE(
        displayName = "Claude",
        vendor = "Anthropic",
        brandColor = 0xFFD97757,
        chatUrl = "https://claude.ai/",
        billingUrl = "https://claude.ai/settings/billing",
        plans = listOf(
            PlanPreset("Free", 0.0),
            PlanPreset("Pro", 20.0),
            PlanPreset("Max 5x", 100.0),
            PlanPreset("Max 20x", 200.0),
            PlanPreset("Team", 30.0),
        ),
    ),
    GEMINI(
        displayName = "Gemini",
        vendor = "Google",
        brandColor = 0xFF4285F4,
        chatUrl = "https://gemini.google.com/",
        billingUrl = "https://one.google.com/settings",
        plans = listOf(
            PlanPreset("Free", 0.0),
            PlanPreset("Google AI Pro", 19.99),
            PlanPreset("Google AI Ultra", 249.99),
        ),
    ),
    DEEPSEEK(
        displayName = "DeepSeek",
        vendor = "DeepSeek",
        brandColor = 0xFF4D6BFE,
        chatUrl = "https://chat.deepseek.com/",
        billingUrl = "https://platform.deepseek.com/usage",
        apiKeyUrl = "https://platform.deepseek.com/api_keys",
        balanceSync = true,
        plans = listOf(
            PlanPreset("Free", 0.0),
            PlanPreset("API 按量付费", 0.0),
        ),
    );
}

data class PlanPreset(val name: String, val monthlyPriceUsd: Double)

enum class SubscriptionStatus(val label: String) {
    ACTIVE("生效中"),
    CANCELED("已取消续订"),
    EXPIRED("已过期"),
    FREE("免费版"),
}

enum class BillingCycle(val label: String) {
    MONTHLY("按月"),
    YEARLY("按年"),
}

/** 额度数据的来源。界面必须据此标注，不能把手动值当作实时数据展示。 */
enum class QuotaSource(val label: String) {
    UNKNOWN("未获取"),
    MANUAL("手动记录"),
    AUTO("自动同步"),
}

/** 套餐信息的来源 */
enum class PlanSource(val label: String) {
    MANUAL("手动记录"),
    RECEIPT("收据识别"),
}
