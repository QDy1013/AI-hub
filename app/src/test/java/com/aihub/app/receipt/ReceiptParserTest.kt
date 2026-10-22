package com.aihub.app.receipt

import com.aihub.app.data.model.BillingCycle
import com.aihub.app.data.model.Platform
import com.aihub.app.data.receipt.ReceiptParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ReceiptParserTest {

    @Test
    fun chatGptPlusReceiptWithBillingPeriod() {
        val text = """
            Your receipt from OpenAI, LLC
            Receipt #2041-9921
            Date paid October 22, 2026
            ChatGPT Plus Subscription
            Oct 22, 2026 – Nov 22, 2026
            Subtotal $20.00
            Tax $0.00
            Total $20.00
            Amount paid $20.00
        """.trimIndent()
        val r = ReceiptParser.parse(text)
        assertEquals(Platform.CHATGPT, r.platform)
        assertEquals("Plus", r.planName)
        assertEquals(20.0, r.amount!!, 0.001)
        assertEquals("USD", r.currency)
        assertEquals(LocalDate.of(2026, 10, 22), r.chargedOn)
        assertEquals(LocalDate.of(2026, 11, 22), r.renewsOn)
        assertFalse(r.renewalInferred)
        assertEquals(BillingCycle.MONTHLY, r.billingCycle)
    }

    @Test
    fun claudeMaxReceiptInfersRenewalAndSaysSo() {
        val text = """
            Anthropic, PBC
            Receipt from Anthropic
            Claude Max plan (5x) – monthly
            Date paid: 2026-10-01
            Amount paid
            US$100.00
        """.trimIndent()
        val r = ReceiptParser.parse(text)
        assertEquals(Platform.CLAUDE, r.platform)
        assertEquals("Max 5x", r.planName)
        assertEquals(100.0, r.amount!!, 0.001)
        assertEquals("USD", r.currency)
        assertEquals(LocalDate.of(2026, 10, 1), r.chargedOn)
        assertEquals(LocalDate.of(2026, 11, 1), r.renewsOn)
        assertTrue(r.renewalInferred)
    }

    @Test
    fun chineseGoogleOneReceipt() {
        val text = """
            Google Play 订单收据
            Google One · Google AI Pro (2 TB) 按月订阅
            订单日期：2026年9月5日
            下次扣款日期：2026年10月5日
            合计：¥139.99/月
        """.trimIndent()
        val r = ReceiptParser.parse(text)
        assertEquals(Platform.GEMINI, r.platform)
        assertEquals("Google AI Pro", r.planName)
        assertEquals(139.99, r.amount!!, 0.001)
        assertEquals("CNY", r.currency)
        assertEquals(LocalDate.of(2026, 10, 5), r.renewsOn)
        assertFalse(r.renewalInferred)
        assertEquals(BillingCycle.MONTHLY, r.billingCycle)
    }

    @Test
    fun canceledSubscriptionKeepsAccessDateAndDoesNotInventRenewal() {
        val text = """
            Your ChatGPT Pro subscription has been canceled.
            You'll keep access until December 3, 2026.
        """.trimIndent()
        val r = ReceiptParser.parse(text)
        assertEquals(Platform.CHATGPT, r.platform)
        assertEquals("Pro", r.planName)
        assertTrue(r.canceled)
        assertEquals(LocalDate.of(2026, 12, 3), r.renewsOn)
        assertFalse(r.renewalInferred)
        assertNull(r.amount)
    }

    @Test
    fun deepSeekTopUpIsNotTreatedAsSubscription() {
        val text = """
            DeepSeek 开放平台 充值成功
            充值金额：50.00 元
            交易时间：2026-10-20 13:22:01
        """.trimIndent()
        val r = ReceiptParser.parse(text)
        assertEquals(Platform.DEEPSEEK, r.platform)
        assertTrue(r.isTopUp)
        assertEquals(50.0, r.amount!!, 0.001)
        assertEquals("CNY", r.currency)
        assertNull(r.billingCycle)
        assertNull(r.renewsOn)
        assertEquals(LocalDate.of(2026, 10, 20), r.chargedOn)
    }

    @Test
    fun ambiguousNumericDateIsIgnored() {
        assertTrue(ReceiptParser.datesIn("Paid 10/11/2026").isEmpty())
        assertEquals(listOf(LocalDate.of(2026, 12, 25)), ReceiptParser.datesIn("Paid 12/25/2026"))
    }

    @Test
    fun europeanAmountFormat() {
        assertEquals(1234.56, ReceiptParser.parseNumber("1.234,56")!!, 0.001)
        assertEquals(20.0, ReceiptParser.parseNumber("20,00")!!, 0.001)
        assertEquals(1234.0, ReceiptParser.parseNumber("1,234")!!, 0.001)
    }

    @Test
    fun unrelatedTextYieldsNothing() {
        assertTrue(ReceiptParser.parse("hello world").isEmpty)
    }
}
