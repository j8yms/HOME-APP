package com.example.householdapp.core.network

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiResponseParsingTest {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val summaryAdapter: JsonAdapter<ApiResponse<WealthSummaryResponse>> = moshi.adapter(
        Types.newParameterizedType(ApiResponse::class.java, WealthSummaryResponse::class.java)
    )

    @Test
    fun wealthSummaryEnvelopeParses() {
        val json = """
            {"success":true,"data":{
              "currency":"KES",
              "totals":{"cash":1000.5,"assets":5000.0,"investments":2500.0,"invested":2000.0,
                "gainLoss":500.0,"liabilities":750.0,"netWorth":7750.5,"passiveMonthly":300.0,
                "targetPassive":1000.0,"passiveProgressPct":30,"targetNetWorth":100000.0,
                "netWorthProgressPct":7,"monthlyChange":250.0,"monthlyChangePct":3.3},
              "counts":{"investments":3,"assets":2,"liabilities":1,"milestones":4,"accounts":1,
                "trades":5,"income":2,"contributions":6,"propAccounts":0,"propPayouts":0},
              "milestones":{"total":4,"achieved":1},
              "freedom":{"monthlyRequirement":8333.33,"annualRequirement":100000.0,
                "targetPassiveIncome":1000.0,"targetNetWorth":100000.0,"targetDate":"2030-12-31",
                "monthlyContribution":500.0,"currentPassiveIncome":300.0,"progressPct":30,
                "updatedAt":"2026-10-01T10:00:00.000Z"}
            }}
        """.trimIndent()

        val response = summaryAdapter.fromJson(json)!!

        assertTrue(response.success)
        val data = requireNotNull(response.data)
        assertEquals("KES", data.currency)
        assertEquals(7750.5, data.totals.netWorth, 0.001)
        assertEquals(30, data.freedom.progressPct)
        assertEquals(4, data.milestones.total)
        assertEquals(1, data.milestones.achieved)
        assertEquals(3, data.counts.investments)
    }

    @Test
    fun wealthItemParsesAllFields() {
        val json = """
            {"id":"inv_1","section":"investments","name":"MMF","type":"money_market",
             "provider":"ABC","value":120000.0,"yieldPct":12.5,"isActive":true,
             "createdAt":"2026-01-01T00:00:00.000Z"}
        """.trimIndent()

        val item = moshi.adapter(WealthItem::class.java).fromJson(json)!!
        assertEquals("inv_1", item.id)
        assertEquals("MMF", item.name)
        assertEquals(120000.0, item.value, 0.001)
        assertEquals(12.5, item.yieldPct, 0.001)
        assertTrue(item.isActive)
        assertEquals("", item.symbol)
    }

    @Test
    fun errorEnvelopeParses() {
        val json = """{"success":false,"error":{"code":"NOT_FOUND","message":"Wealth section not found."}}"""
        val response = summaryAdapter.fromJson(json)!!

        assertEquals(false, response.success)
        assertEquals("NOT_FOUND", response.error?.code)
        assertEquals("Wealth section not found.", response.error?.message)
    }
}
