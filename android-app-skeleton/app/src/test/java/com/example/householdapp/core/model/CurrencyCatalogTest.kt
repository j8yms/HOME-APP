package com.example.householdapp.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class CurrencyCatalogTest {
    @Test
    fun symbolsMatchBackendSpec() {
        assertEquals("$", CurrencyCatalog.symbolFor("USD"))
        assertEquals("€", CurrencyCatalog.symbolFor("EUR"))
        assertEquals("£", CurrencyCatalog.symbolFor("GBP"))
        assertEquals("KSh", CurrencyCatalog.symbolFor("KES"))
        assertEquals("₦", CurrencyCatalog.symbolFor("NGN"))
        assertEquals("C$", CurrencyCatalog.symbolFor("CAD"))
        assertEquals("A$", CurrencyCatalog.symbolFor("AUD"))
        assertEquals("¥", CurrencyCatalog.symbolFor("JPY"))
    }

    @Test
    fun formatMoneyUsesSymbolAndTwoDecimals() {
        assertEquals("$1,234.50", CurrencyCatalog.formatMoney(1234.5, "USD"))
        assertEquals("KSh 1,234.50", CurrencyCatalog.formatMoney(1234.5, "KES"))
        assertEquals("$0.00", CurrencyCatalog.formatMoney(0.0, "USD"))
    }

    @Test
    fun formatMoneyUsesAbsoluteValue() {
        assertEquals("$5.00", CurrencyCatalog.formatMoney(-5.0, "USD"))
    }

    @Test
    fun unknownCodeFallsBackToFirstSupported() {
        assertEquals("$", CurrencyCatalog.symbolFor("XYZ"))
        assertEquals("$1.00", CurrencyCatalog.formatMoney(1.0, null))
    }
}
