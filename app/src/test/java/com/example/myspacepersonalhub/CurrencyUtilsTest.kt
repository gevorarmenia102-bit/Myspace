package com.example.myspacepersonalhub

import com.example.myspacepersonalhub.core.database.entity.TransactionEntity
import com.example.myspacepersonalhub.core.model.TransactionType
import com.example.myspacepersonalhub.core.util.CurrencyUtils
import org.junit.Assert.*
import org.junit.Test
import java.util.Locale

class CurrencyUtilsTest {

    @Test
    fun testParseToMinorUnits() {
        val usdMinor = CurrencyUtils.parseToMinorUnits("12.50", "USD")
        assertEquals(1250L, usdMinor)

        val jpyMinor = CurrencyUtils.parseToMinorUnits("1000", "JPY")
        assertEquals(1000L, jpyMinor)

        val invalid = CurrencyUtils.parseToMinorUnits("abc", "USD")
        assertNull(invalid)

        val negative = CurrencyUtils.parseToMinorUnits("-10.00", "USD")
        assertNull(negative)
    }

    @Test
    fun testCalculateTotalsByCurrency_IndependentTotals() {
        val transactions = listOf(
            TransactionEntity(type = TransactionType.INCOME, amountMinorUnits = 5000L, currencyCode = "USD", title = "Salary"),
            TransactionEntity(type = TransactionType.EXPENSE, amountMinorUnits = 1200L, currencyCode = "USD", title = "Groceries"),
            TransactionEntity(type = TransactionType.INCOME, amountMinorUnits = 100000L, currencyCode = "AMD", title = "Consulting"),
            TransactionEntity(type = TransactionType.EXPENSE, amountMinorUnits = 25000L, currencyCode = "AMD", title = "Rent")
        )

        val balances = CurrencyUtils.calculateTotalsByCurrency(transactions)

        assertTrue(balances.containsKey("USD"))
        assertTrue(balances.containsKey("AMD"))

        val usd = balances["USD"]!!
        assertEquals(5000L, usd.incomeMinorUnits)
        assertEquals(1200L, usd.expenseMinorUnits)
        assertEquals(3800L, usd.netMinorUnits)

        val amd = balances["AMD"]!!
        assertEquals(100000L, amd.incomeMinorUnits)
        assertEquals(25000L, amd.expenseMinorUnits)
        assertEquals(75000L, amd.netMinorUnits)
    }

    @Test
    fun testFormatAmount() {
        val formattedUsd = CurrencyUtils.formatAmount(1250L, "USD", Locale.US)
        assertTrue(formattedUsd.contains("12.50"))
    }
}
