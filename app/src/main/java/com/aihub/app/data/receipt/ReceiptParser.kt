package com.aihub.app.data.receipt

import com.aihub.app.data.model.BillingCycle
import com.aihub.app.data.model.Platform
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * 从订阅收据 / 扣款邮件的文字中识别出的信息。
 * 每个字段都可能为 null：识别不到就留空，交给用户确认或手动补充，不做猜测。
 */
data class ParsedReceipt(
    val platform: Platform?,
    val planName: String?,
    val amount: Double?,
    val currency: String?,
    val billingCycle: BillingCycle?,
    val chargedOn: LocalDate?,
    val renewsOn: LocalDate?,
    /** true 表示收据里没有写明续费日，renewsOn 是按“扣款日 + 一个计费周期”推算的 */
    val renewalInferred: Boolean,
    /** 收据表明订阅已取消（到期后不再续费） */
    val canceled: Boolean,
    /** DeepSeek 等按量付费平台的充值记录：金额是充值额，不是月费 */
    val isTopUp: Boolean,
) {
    val isEmpty: Boolean
        get() = platform == null && planName == null && amount == null && chargedOn == null && renewsOn == null
}

/**
 * 纯文本规则识别，完全在本机运行，不联网。
 * 支持中英文常见写法：OpenAI / Anthropic / Google One / DeepSeek 的收据、App Store 和 Google Play 的订阅邮件。
 */
object ReceiptParser {

    fun parse(text: String): ParsedReceipt {
        val lower = text.lowercase(Locale.ROOT)
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }

        val platform = detectPlatform(lower)
        val dates = findDates(lines)
        val renewal = dates.firstOrNull { it.kind == DateKind.RENEWAL }?.date
        val period = findPeriod(lines)
        val charged = dates.firstOrNull { it.kind == DateKind.CHARGE }?.date
            ?: period?.first
            ?: dates.firstOrNull { it.kind == DateKind.OTHER }?.date

        val isTopUp = platform == Platform.DEEPSEEK
        val cycle = if (isTopUp) null else detectCycle(lower) ?: period?.let(::cycleFromPeriod)
        val canceled = CANCELED.any { it.containsMatchIn(lower) }

        var renewsOn = renewal ?: period?.second
        var inferred = false
        if (renewsOn == null && charged != null && cycle != null && !canceled && !isTopUp) {
            renewsOn = when (cycle) {
                BillingCycle.MONTHLY -> charged.plusMonths(1)
                BillingCycle.YEARLY -> charged.plusYears(1)
            }
            inferred = true
        }

        val money = findAmount(lines)
        return ParsedReceipt(
            platform = platform,
            planName = platform?.let { detectPlan(it, lower) },
            amount = money?.first,
            currency = money?.second,
            billingCycle = cycle,
            chargedOn = charged,
            renewsOn = renewsOn,
            renewalInferred = inferred,
            canceled = canceled,
            isTopUp = isTopUp,
        )
    }

    // ---------- 平台与套餐 ----------

    private val PLATFORM_KEYWORDS = mapOf(
        Platform.CHATGPT to listOf("chatgpt", "openai"),
        Platform.CLAUDE to listOf("claude", "anthropic"),
        Platform.GEMINI to listOf("google one", "google ai pro", "google ai ultra", "gemini", "ai premium"),
        Platform.DEEPSEEK to listOf("deepseek", "深度求索"),
    )

    internal fun detectPlatform(lower: String): Platform? =
        PLATFORM_KEYWORDS
            .mapValues { (_, words) -> words.sumOf { countOccurrences(lower, it) } }
            .filterValues { it > 0 }
            .maxByOrNull { it.value }
            ?.key

    private fun countOccurrences(text: String, word: String): Int {
        var count = 0
        var index = text.indexOf(word)
        while (index >= 0) {
            count++
            index = text.indexOf(word, index + word.length)
        }
        return count
    }

    internal fun detectPlan(platform: Platform, lower: String): String? = when (platform) {
        Platform.CHATGPT -> when {
            Regex("""chatgpt\s+pro\b""").containsMatchIn(lower) -> "Pro"
            Regex("""chatgpt\s+plus\b""").containsMatchIn(lower) -> "Plus"
            Regex("""chatgpt\s+(team|business)\b""").containsMatchIn(lower) -> "Team"
            Regex("""\bplus\b""").containsMatchIn(lower) -> "Plus"
            Regex("""\b(team|business)\b""").containsMatchIn(lower) -> "Team"
            Regex("""\bpro\b""").containsMatchIn(lower) -> "Pro"
            else -> null
        }
        Platform.CLAUDE -> {
            val max = Regex("""\bmax\b[^\n]{0,20}?\b(5|20)\s*x""").find(lower)
            when {
                max != null -> "Max ${max.groupValues[1]}x"
                Regex("""\bmax\s+plan\b|claude\s+max\b""").containsMatchIn(lower) -> "Max"
                Regex("""\bteam\b""").containsMatchIn(lower) -> "Team"
                Regex("""\bpro\b""").containsMatchIn(lower) -> "Pro"
                else -> null
            }
        }
        Platform.GEMINI -> when {
            "ai ultra" in lower -> "Google AI Ultra"
            "ai pro" in lower || "ai premium" in lower || "gemini advanced" in lower -> "Google AI Pro"
            else -> null
        }
        Platform.DEEPSEEK -> "API 按量付费"
    }

    // ---------- 计费周期与取消 ----------

    private val YEARLY = listOf(
        Regex("""\b(annual|annually|yearly)\b"""), Regex("""per\s+year|/\s*(year|yr)\b|a\s+year\b|12\s+months"""),
        Regex("每年|年付|包年|年度|按年"),
    )
    private val MONTHLY = listOf(
        Regex("""\bmonthly\b"""), Regex("""per\s+month|/\s*(month|mo)\b|a\s+month\b|1\s+month\b"""),
        Regex("每月|月付|包月|按月|连续包月"),
    )

    internal fun detectCycle(lower: String): BillingCycle? {
        val yearly = YEARLY.any { it.containsMatchIn(lower) }
        val monthly = MONTHLY.any { it.containsMatchIn(lower) }
        return when {
            yearly && !monthly -> BillingCycle.YEARLY
            monthly && !yearly -> BillingCycle.MONTHLY
            else -> null
        }
    }

    private fun cycleFromPeriod(period: Pair<LocalDate, LocalDate>): BillingCycle? {
        val days = ChronoUnit.DAYS.between(period.first, period.second)
        return when (days) {
            in 27L..32L -> BillingCycle.MONTHLY
            in 360L..367L -> BillingCycle.YEARLY
            else -> null
        }
    }

    private val CANCELED = listOf(
        Regex("""(has been|was|been)\s+cancel+ed"""),
        Regex("""subscription\s+(is\s+)?cancel+ed"""),
        Regex("""you('ve| have)\s+cancel+ed"""),
        Regex("""will\s+not\s+renew|won't\s+renew|won’t\s+renew"""),
        Regex("已取消|已退订|不会自动续|不再续订|不再续费"),
    )

    // ---------- 金额 ----------

    private val CURRENCY_SYMBOLS = linkedMapOf(
        "us$" to "USD", "hk$" to "HKD", "ca$" to "CAD", "a$" to "AUD", "s$" to "SGD", "nt$" to "TWD",
        "$" to "USD", "€" to "EUR", "£" to "GBP", "￥" to "CNY", "¥" to "CNY",
        "rmb" to "CNY", "cny" to "CNY", "usd" to "USD", "eur" to "EUR", "gbp" to "GBP", "hkd" to "HKD",
        "jpy" to "JPY", "元" to "CNY",
    )
    private val SYMBOL_PATTERN = CURRENCY_SYMBOLS.keys.joinToString("|") { Regex.escape(it) }
    private const val NUMBER = """\d{1,3}(?:[,.]\d{3})+(?:[.,]\d{1,2})?|\d+(?:[.,]\d{1,2})?"""
    private val PREFIX_AMOUNT = Regex("""($SYMBOL_PATTERN)\s?($NUMBER)""", RegexOption.IGNORE_CASE)
    private val SUFFIX_AMOUNT = Regex("""($NUMBER)\s?($SYMBOL_PATTERN)""", RegexOption.IGNORE_CASE)

    private val TOTAL_KEYWORDS = Regex(
        """amount\s+paid|amount\s+charged|\btotal\b|charged|you\s+paid|实付|实际支付|支付金额|付款金额|合计|总计|总额|金额""",
    )
    private val SUBTOTAL = Regex("""subtotal|小计|\btax\b|税""")

    /** 优先取“实付 / Total”所在行的金额；没有这样的行就取全文最大的金额（通常是总价） */
    internal fun findAmount(lines: List<String>): Pair<Double, String>? {
        val all = mutableListOf<Pair<Double, String>>()
        lines.forEachIndexed { i, line ->
            val found = amountsIn(line)
            all += found
            val lower = line.lowercase(Locale.ROOT)
            if (found.isNotEmpty() && TOTAL_KEYWORDS.containsMatchIn(lower) && !SUBTOTAL.containsMatchIn(lower)) {
                return found.first()
            }
            // 标签和金额分两行的写法：上一行是“Total”，这一行只有金额
            if (found.isNotEmpty() && i > 0) {
                val prev = lines[i - 1].lowercase(Locale.ROOT)
                if (amountsIn(lines[i - 1]).isEmpty() && TOTAL_KEYWORDS.containsMatchIn(prev) && !SUBTOTAL.containsMatchIn(prev)) {
                    return found.first()
                }
            }
        }
        return all.maxByOrNull { it.first }
    }

    private fun amountsIn(line: String): List<Pair<Double, String>> {
        val result = mutableListOf<Pair<Double, String>>()
        val taken = mutableListOf<IntRange>()
        PREFIX_AMOUNT.findAll(line).forEach { m ->
            val value = parseNumber(m.groupValues[2]) ?: return@forEach
            result += value to CURRENCY_SYMBOLS.getValue(m.groupValues[1].lowercase(Locale.ROOT))
            taken += m.range
        }
        SUFFIX_AMOUNT.findAll(line).forEach { m ->
            if (taken.any { it.first <= m.range.last && m.range.first <= it.last }) return@forEach
            val value = parseNumber(m.groupValues[1]) ?: return@forEach
            result += value to CURRENCY_SYMBOLS.getValue(m.groupValues[2].lowercase(Locale.ROOT))
        }
        return result.filter { it.first > 0 }
    }

    /** 兼容 1,234.56 / 1.234,56 / 20,00 这几种写法 */
    internal fun parseNumber(raw: String): Double? {
        val s = raw.replace(" ", "")
        val lastSep = maxOf(s.lastIndexOf('.'), s.lastIndexOf(','))
        val normalized = if (lastSep >= 0 && s.length - lastSep - 1 in 1..2) {
            s.substring(0, lastSep).replace(",", "").replace(".", "") + "." + s.substring(lastSep + 1)
        } else {
            s.replace(",", "").replace(".", "")
        }
        return normalized.toDoubleOrNull()
    }

    // ---------- 日期 ----------

    private enum class DateKind { RENEWAL, CHARGE, OTHER }
    private data class FoundDate(val date: LocalDate, val kind: DateKind)

    private val RENEWAL_KEYWORDS = Regex(
        """renew|next\s+(billing|payment|charge|invoice)|will\s+be\s+(charged|billed)|billed\s+again|""" +
            """expire|access\s+until|active\s+until|valid\s+until|ends\s+on|until|""" +
            """续费|续订|下次扣|下次付款|下次扣费|下个扣费|到期|有效期至|截止""",
    )
    private val CHARGE_KEYWORDS = Regex(
        """paid|payment\s+date|invoice\s+date|date\s+of\s+issue|issued|order\s+date|purchase|receipt\s+date|""" +
            """支付时间|付款时间|付款日期|交易时间|订单时间|扣款|购买""",
    )

    private val MONTHS = mapOf(
        "jan" to 1, "feb" to 2, "mar" to 3, "apr" to 4, "may" to 5, "jun" to 6,
        "jul" to 7, "aug" to 8, "sep" to 9, "oct" to 10, "nov" to 11, "dec" to 12,
    )
    private const val MONTH_NAME = """(jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)[a-z]*\.?"""
    private val DATE_PATTERNS: List<Pair<Regex, (MatchResult) -> LocalDate?>> = listOf(
        Regex("""(\d{4})\s*年\s*(\d{1,2})\s*月\s*(\d{1,2})\s*日?""") to { m ->
            date(m.groupValues[1], m.groupValues[2], m.groupValues[3])
        },
        Regex("""(?<!\d)(\d{4})[-/.](\d{1,2})[-/.](\d{1,2})(?!\d)""") to { m ->
            date(m.groupValues[1], m.groupValues[2], m.groupValues[3])
        },
        Regex("""\b$MONTH_NAME\s+(\d{1,2})(?:st|nd|rd|th)?,?\s+(\d{4})(?!\d)""") to { m ->
            date(m.groupValues[3], MONTHS[m.groupValues[1]].toString(), m.groupValues[2])
        },
        Regex("""(?<!\d)(\d{1,2})(?:st|nd|rd|th)?\s+$MONTH_NAME,?\s+(\d{4})(?!\d)""") to { m ->
            date(m.groupValues[3], MONTHS[m.groupValues[2]].toString(), m.groupValues[1])
        },
        // 12/25/2026 或 25/12/2026：只有能明确分辨日和月时才采用，10/11/2026 这种有歧义的直接忽略
        Regex("""(?<![\d/])(\d{1,2})/(\d{1,2})/(\d{4})(?!\d)""") to { m ->
            val a = m.groupValues[1].toInt()
            val b = m.groupValues[2].toInt()
            when {
                a > 12 && b <= 12 -> date(m.groupValues[3], b.toString(), a.toString())
                b > 12 && a <= 12 -> date(m.groupValues[3], a.toString(), b.toString())
                else -> null
            }
        },
    )

    private fun date(year: String, month: String, day: String): LocalDate? =
        runCatching { LocalDate.of(year.toInt(), month.toInt(), day.toInt()) }.getOrNull()
            ?.takeIf { it.year in 2015..2100 }

    /** 一行里按出现顺序找出所有日期 */
    internal fun datesIn(line: String): List<LocalDate> {
        val lower = line.lowercase(Locale.ROOT)
        val hits = mutableListOf<Pair<IntRange, LocalDate>>()
        DATE_PATTERNS.forEach { (regex, build) ->
            regex.findAll(lower).forEach { m ->
                if (hits.none { it.first.first <= m.range.last && m.range.first <= it.first.last }) {
                    build(m)?.let { hits += m.range to it }
                }
            }
        }
        return hits.sortedBy { it.first.first }.map { it.second }
    }

    private fun findDates(lines: List<String>): List<FoundDate> {
        val result = mutableListOf<FoundDate>()
        lines.forEachIndexed { i, line ->
            val dates = datesIn(line)
            if (dates.isEmpty()) return@forEachIndexed
            val own = classify(line.lowercase(Locale.ROOT))
            // 标签在上一行、日期在下一行的写法
            val kind = if (own == DateKind.OTHER && i > 0 && datesIn(lines[i - 1]).isEmpty()) {
                classify(lines[i - 1].lowercase(Locale.ROOT))
            } else {
                own
            }
            dates.forEach { result += FoundDate(it, kind) }
        }
        return result
    }

    private fun classify(lower: String): DateKind = when {
        RENEWAL_KEYWORDS.containsMatchIn(lower) -> DateKind.RENEWAL
        CHARGE_KEYWORDS.containsMatchIn(lower) -> DateKind.CHARGE
        else -> DateKind.OTHER
    }

    /** “Oct 22, 2026 – Nov 22, 2026” 这样的计费区间：返回 (开始, 结束) */
    private fun findPeriod(lines: List<String>): Pair<LocalDate, LocalDate>? {
        for (line in lines) {
            val dates = datesIn(line)
            if (dates.size >= 2 && Regex("""[-–—~至]|\bto\b|\bthrough\b""").containsMatchIn(line.lowercase(Locale.ROOT))) {
                val start = dates.first()
                val end = dates.last()
                if (end.isAfter(start)) return start to end
            }
        }
        return null
    }
}
