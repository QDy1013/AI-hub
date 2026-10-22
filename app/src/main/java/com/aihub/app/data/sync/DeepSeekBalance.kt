package com.aihub.app.data.sync

import org.json.JSONObject

/** DeepSeek 官方余额接口返回的单个币种余额，金额原样来自接口 */
data class ApiBalance(
    val currency: String,
    val total: Double,
    val granted: Double?,
    val toppedUp: Double?,
)

data class DeepSeekBalance(
    /** 接口返回的 is_available：余额是否足够继续调用 API */
    val isAvailable: Boolean,
    val balances: List<ApiBalance>,
) {
    /** 优先展示人民币余额（国内账户默认币种），否则取第一条 */
    val primary: ApiBalance?
        get() = balances.firstOrNull { it.currency == "CNY" } ?: balances.firstOrNull()
}

/**
 * 解析 `GET https://api.deepseek.com/user/balance` 的响应，例如：
 * `{"is_available":true,"balance_infos":[{"currency":"CNY","total_balance":"110.00",
 * "granted_balance":"10.00","topped_up_balance":"100.00"}]}`
 * 金额字段是字符串。缺失或无法解析的条目直接丢弃，不补默认值。
 */
object DeepSeekBalanceParser {
    fun parse(json: String): DeepSeekBalance {
        val root = JSONObject(json)
        val infos = root.optJSONArray("balance_infos")
        val balances = buildList {
            if (infos != null) {
                for (i in 0 until infos.length()) {
                    val item = infos.optJSONObject(i) ?: continue
                    val currency = item.optString("currency").trim().uppercase()
                    val total = item.optString("total_balance").toDoubleOrNull()
                    if (currency.isEmpty() || total == null) continue
                    add(
                        ApiBalance(
                            currency = currency,
                            total = total,
                            granted = item.optString("granted_balance").toDoubleOrNull(),
                            toppedUp = item.optString("topped_up_balance").toDoubleOrNull(),
                        ),
                    )
                }
            }
        }
        return DeepSeekBalance(isAvailable = root.optBoolean("is_available", false), balances = balances)
    }
}
