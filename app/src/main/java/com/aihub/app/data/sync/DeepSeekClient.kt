package com.aihub.app.data.sync

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

sealed interface BalanceResult {
    data class Success(val balance: DeepSeekBalance) : BalanceResult
    data class Failure(val message: String) : BalanceResult
}

/**
 * 调用 DeepSeek 官方余额接口。只使用用户自己创建的 API Key，
 * 只发往 api.deepseek.com（HTTPS），Key 不写日志、不发往其他地方。
 */
class DeepSeekClient(private val endpoint: String = ENDPOINT) {

    suspend fun fetchBalance(apiKey: String): BalanceResult = withContext(Dispatchers.IO) {
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 15_000
            instanceFollowRedirects = false
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Authorization", "Bearer ${apiKey.trim()}")
        }
        try {
            when (val code = connection.responseCode) {
                HttpURLConnection.HTTP_OK -> {
                    val body = connection.inputStream.bufferedReader().use { it.readText() }
                    val balance = DeepSeekBalanceParser.parse(body)
                    if (balance.balances.isEmpty()) {
                        BalanceResult.Failure("接口没有返回余额信息")
                    } else {
                        BalanceResult.Success(balance)
                    }
                }
                HttpURLConnection.HTTP_UNAUTHORIZED -> BalanceResult.Failure("API Key 无效或已被删除")
                HttpURLConnection.HTTP_FORBIDDEN -> BalanceResult.Failure("API Key 没有查询余额的权限")
                429 -> BalanceResult.Failure("请求过于频繁，请稍后再试")
                in 500..599 -> BalanceResult.Failure("DeepSeek 服务暂时不可用（$code）")
                else -> BalanceResult.Failure("查询失败（HTTP $code）")
            }
        } catch (e: IOException) {
            BalanceResult.Failure("网络连接失败，请检查网络")
        } catch (e: JSONException) {
            BalanceResult.Failure("接口返回格式无法识别")
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        const val ENDPOINT = "https://api.deepseek.com/user/balance"
    }
}
