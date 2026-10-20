package com.example.myspacepersonalhub.core.util

import com.example.myspacepersonalhub.core.database.entity.TransactionEntity
import com.example.myspacepersonalhub.core.model.TransactionType
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import kotlin.math.pow

data class CurrencyBalance(
    val currencyCode: String,
    val incomeMinorUnits: Long,
    val expenseMinorUnits: Long,
    val netMinorUnits: Long
)

object CurrencyUtils {
    val SUPPORTED_CURRENCIES = listOf("USD", "EUR", "GBP", "JPY", "CAD", "AUD", "AMD", "BDT", "INR", "CHF", "CNY")

    fun getFractionDigits(currencyCode: String): Int {
        return try {
            val currency = Currency.getInstance(currencyCode.uppercase().trim())
            val digits = currency.defaultFractionDigits
            if (digits >= 0) digits else 2
        } catch (_: Exception) {
            2
        }
    }

    fun getFactor(currencyCode: String): Long {
        val digits = getFractionDigits(currencyCode)
        return (10.0).pow(digits.toDouble()).toLong()
    }

    fun formatAmount(minorUnits: Long, currencyCode: String, locale: Locale = Locale.getDefault()): String {
        val digits = getFractionDigits(currencyCode)
        val factor = getFactor(currencyCode)
        val majorAmount = minorUnits.toDouble() / factor.toDouble()

        return try {
            val format = NumberFormat.getCurrencyInstance(locale)
            format.currency = Currency.getInstance(currencyCode.uppercase().trim())
            format.minimumFractionDigits = digits
            format.maximumFractionDigits = digits
            format.format(majorAmount)
        } catch (_: Exception) {
            "$currencyCode ${String.format(locale, "%.${digits}f", majorAmount)}"
        }
    }

    fun parseToMinorUnits(input: String, currencyCode: String): Long? {
        val clean = input.trim().replace(",", "")
        val doubleVal = clean.toDoubleOrNull() ?: return null
        if (doubleVal < 0) return null
        val factor = getFactor(currencyCode)
        return (doubleVal * factor).toLong()
    }

    fun calculateTotalsByCurrency(transactions: List<TransactionEntity>): Map<String, CurrencyBalance> {
        val grouped = transactions.groupBy { it.currencyCode.uppercase().trim() }
        return grouped.mapValues { (code, txs) ->
            var income = 0L
            var expense = 0L
            for (tx in txs) {
                if (tx.type == TransactionType.INCOME) {
                    income += tx.amountMinorUnits
                } else {
                    expense += tx.amountMinorUnits
                }
            }
            CurrencyBalance(
                currencyCode = code,
                incomeMinorUnits = income,
                expenseMinorUnits = expense,
                netMinorUnits = income - expense
            )
        }
    }
}
