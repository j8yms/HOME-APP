package com.example.householdapp.mpesa

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MpesaParserTest {

    @Test
    fun ignoresNonMpesaSender() {
        assertNull(MpesaParser.parse("JOHN", "QK12ABCDEF Confirmed. Ksh100.00 sent to X on 11/10/26 at 8:00 PM."))
    }

    @Test
    fun parsesSentMessage() {
        val parsed = MpesaParser.parse(
            "MPESA",
            "QK12ABCDEF Confirmed. Ksh2,500.00 sent to Jane Doe 0712345678 on 11/10/26 at 8:45 PM. " +
                "New M-PESA balance is Ksh10,000.00. Transaction cost, Ksh29.00."
        )
        assertNotNull(parsed)
        assertEquals("QK12ABCDEF", parsed!!.code)
        assertEquals(2500.0, parsed.amount, 0.001)
        assertEquals(MpesaKind.SENT, parsed.kind)
        assertTrue(parsed.counterparty.contains("Jane Doe"))
        assertEquals(10000.0, parsed.balance ?: 0.0, 0.001)
        assertEquals("code:QK12ABCDEF", parsed.dedupeKey)
    }

    @Test
    fun parsesReceivedMessage() {
        val parsed = MpesaParser.parse(
            "MPESA",
            "QK99ZZ1122 Confirmed.You have received Ksh1,500.00 from JOHN DOE 0722123456 on 11/10/26 at 9:00 AM " +
                "New M-PESA balance is Ksh12,000.00."
        )
        assertNotNull(parsed)
        assertEquals("QK99ZZ1122", parsed!!.code)
        assertEquals(1500.0, parsed.amount, 0.001)
        assertEquals(MpesaKind.RECEIVED, parsed.kind)
        assertEquals("income", MpesaParser.suggestedType(parsed.kind))
    }

    @Test
    fun parsesPaybillMessage() {
        val parsed = MpesaParser.parse(
            "MPESA",
            "QK77PAYBILL Confirmed. Ksh500.00 paid to KPLC PREPAID on 11/10/26 at 10:00 AM. " +
                "Account Number 123456. New M-PESA balance is Ksh9,500.00."
        )
        assertNotNull(parsed)
        assertEquals(MpesaKind.PAYBILL, parsed!!.kind)
        assertTrue(parsed.counterparty.contains("KPLC"))
        assertEquals("expense", MpesaParser.suggestedType(parsed.kind))
    }

    @Test
    fun parsesReversalMessage() {
        val parsed = MpesaParser.parse(
            "MPESA",
            "QK55REVERS Confirmed. Reversal of transaction QK12ABCDEF of Ksh2,500.00 has been processed on 11/10/26 at 11:00 AM."
        )
        assertNotNull(parsed)
        assertEquals(MpesaKind.REVERSAL, parsed!!.kind)
        assertEquals("uncertain", MpesaParser.suggestedType(parsed.kind))
    }

    @Test
    fun rejectsUnsupportedMessage() {
        assertNull(MpesaParser.parse("MPESA", "Your Fuliza limit is Ksh0.00. Thank you."))
    }

    @Test
    fun dedupeFallsBackToHashWithoutCode() {
        val parsed = MpesaParser.parse(
            "MPESA",
            "Confirmed. Ksh300.00 airtime purchased on 11/10/26 at 7:00 AM. New M-PESA balance is Ksh100.00."
        )
        assertNotNull(parsed)
        assertEquals(MpesaKind.AIRTIME, parsed!!.kind)
        assertTrue(parsed.dedupeKey.startsWith("hash:"))
    }
}
