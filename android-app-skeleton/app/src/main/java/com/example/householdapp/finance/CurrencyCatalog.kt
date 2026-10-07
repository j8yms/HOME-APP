package com.example.householdapp.finance

object CurrencyCatalog {
    fun formatMoney(amount: Double, currency: String): String {
        val formatted = com.example.householdapp.core.model.CurrencyCatalog.formatMoney(amount, currency)
        return if (amount < 0) "-$formatted" else formatted
    }
}
