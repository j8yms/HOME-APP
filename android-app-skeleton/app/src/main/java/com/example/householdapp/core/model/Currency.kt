package com.example.householdapp.core.model

import java.text.NumberFormat
import java.util.Locale

data class CurrencyInfo(
    val code: String,
    val label: String,
    val symbol: String
)

object CurrencyCatalog {
    val supported: List<CurrencyInfo> = listOf(
        CurrencyInfo("USD", "US Dollar", "$"),
        CurrencyInfo("EUR", "Euro", "€"),
        CurrencyInfo("GBP", "British Pound", "£"),
        CurrencyInfo("KES", "Kenyan Shilling", "KSh"),
        CurrencyInfo("NGN", "Nigerian Naira", "₦"),
        CurrencyInfo("CAD", "Canadian Dollar", "C$"),
        CurrencyInfo("AUD", "Australian Dollar", "A$"),
        CurrencyInfo("JPY", "Japanese Yen", "¥")
    )

    fun byCode(code: String?): CurrencyInfo =
        supported.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: supported.first()

    fun symbolFor(code: String?): String = byCode(code).symbol

    fun formatMoney(amount: Double, currencyCode: String?): String {
        val symbol = symbolFor(currencyCode)
        val nf = NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
        val digits = nf.format(kotlin.math.abs(amount))
        return if (symbol.length > 2) {
            "$symbol $digits"
        } else {
            "$symbol$digits"
        }
    }
}
