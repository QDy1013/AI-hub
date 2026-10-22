package com.aihub.app.sync

import com.aihub.app.data.sync.DeepSeekBalanceParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeepSeekBalanceParserTest {
    @Test
    fun parsesOfficialResponse() {
        val json = """
            {"is_available":true,"balance_infos":[
              {"currency":"USD","total_balance":"3.50","granted_balance":"0.00","topped_up_balance":"3.50"},
              {"currency":"CNY","total_balance":"110.00","granted_balance":"10.00","topped_up_balance":"100.00"}
            ]}
        """.trimIndent()
        val result = DeepSeekBalanceParser.parse(json)
        assertTrue(result.isAvailable)
        assertEquals(2, result.balances.size)
        val primary = result.primary!!
        assertEquals("CNY", primary.currency)
        assertEquals(110.0, primary.total, 0.0001)
        assertEquals(10.0, primary.granted!!, 0.0001)
    }

    @Test
    fun dropsIncompleteEntriesInsteadOfGuessing() {
        val json = """{"is_available":false,"balance_infos":[{"currency":"CNY","total_balance":""},{"total_balance":"5"}]}"""
        val result = DeepSeekBalanceParser.parse(json)
        assertFalse(result.isAvailable)
        assertTrue(result.balances.isEmpty())
        assertNull(result.primary)
    }
}
