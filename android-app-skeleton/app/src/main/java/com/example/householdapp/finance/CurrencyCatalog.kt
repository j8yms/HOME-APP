package com.example.householdapp.finance

object CurrencyCatalog {
    fun formatMoney(amount: Double, currency: String): String {
        return when (currency) {
            "USD" -> "%.2f".format(amount)
            "EUR" -> "%.2f".format(amount)
            "GBP" -> "%.2f".format(amount)
            "JPY" -> "%.2f".format(amount)
            else -> "%.2f".format(amount)
        }
    }
}