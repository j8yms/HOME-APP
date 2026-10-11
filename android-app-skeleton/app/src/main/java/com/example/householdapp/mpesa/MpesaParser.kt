package com.example.householdapp.mpesa

import java.security.MessageDigest

// Stage 4: on-device M-Pesa SMS parsing. Only Safaricom M-Pesa sender
// messages ("MPESA") are considered; everything else is ignored so unrelated
// personal SMS are never collected.
//
// Privacy: parse() returns structured fields only. Callers must persist the
// parsed fields (plus a dedupe hash), never the full message body.

enum class MpesaKind {
    RECEIVED,
    SENT,
    PAYBILL,
    TILL,
    WITHDRAW,
    DEPOSIT,
    AIRTIME,
    REVERSAL,
    UNSUPPORTED
}

data class ParsedMpesa(
    val code: String,
    val amount: Double,
    val currency: String = "KES",
    val dateTime: String = "",
    val kind: MpesaKind,
    val counterparty: String = "",
    val balance: Double? = null,
    // Stable dedupe key: transaction code when present, else a hash of the
    // normalized body (fallback matching for code-less messages).
    val dedupeKey: String
)

private val CODE_REGEX = Regex("^\\s*([A-Z0-9]{10})\\s+Confirmed\\b", RegexOption.IGNORE_CASE)
private val AMOUNT_REGEX = Regex("Ksh\\s*([\\d,]+\\.\\d{2})", RegexOption.IGNORE_CASE)
private val DATETIME_REGEX = Regex("on\\s+(\\d{1,2}/\\d{1,2}/\\d{2,4})\\s+at\\s+([\\d:]+\\s*[AP]M)", RegexOption.IGNORE_CASE)
private val BALANCE_REGEX = Regex("New M-PESA balance is Ksh\\s*([\\d,]+\\.\\d{2})", RegexOption.IGNORE_CASE)
private val SENT_TO_REGEX = Regex("sent to\\s+(.+?)\\s+(\\d{7,})\\s+on\\b", RegexOption.IGNORE_CASE)
private val RECEIVED_FROM_REGEX = Regex("received Ksh[\\d,.]+\\s+from\\s+(.+?)\\s+(\\d{7,})\\s+on\\b", RegexOption.IGNORE_CASE)
private val PAID_TO_REGEX = Regex("paid to\\s+(.+?)\\s+on\\b", RegexOption.IGNORE_CASE)
private val ACCOUNT_REGEX = Regex("Account Number\\s+(\\S+)", RegexOption.IGNORE_CASE)
private val WITHDRAW_REGEX = Regex("Withdraw Ksh[\\d,.]+\\s+from\\s+(.+?)\\s+on\\b", RegexOption.IGNORE_CASE)
private val GIVE_CASH_REGEX = Regex("Give Ksh[\\d,.]+\\s+cash to\\s+(.+?)\\s+on\\b", RegexOption.IGNORE_CASE)

fun normalizeAmount(raw: String): Double =
    raw.replace(",", "").toDoubleOrNull() ?: 0.0

fun sha256Hex(input: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val bytes = digest.digest(input.toByteArray(Charsets.UTF_8))
    return bytes.joinToString("") { "%02x".format(it) }
}

object MpesaParser {
    fun isSupportedSender(sender: String?): Boolean =
        sender != null && sender.trim().equals("MPESA", ignoreCase = true)

    fun parse(sender: String?, body: String?): ParsedMpesa? {
        if (!isSupportedSender(sender)) return null
        val text = body?.trim().orEmpty()
        if (text.isEmpty()) return null

        val code = CODE_REGEX.find(text)?.groupValues?.getOrNull(1)?.uppercase().orEmpty()
        val amount = AMOUNT_REGEX.find(text)?.groupValues?.getOrNull(1)?.let(::normalizeAmount) ?: 0.0
        if (amount <= 0) return null

        val dateMatch = DATETIME_REGEX.find(text)
        val dateTime = if (dateMatch != null) {
            "${dateMatch.groupValues[1]} ${dateMatch.groupValues[2]}"
        } else ""
        val balance = BALANCE_REGEX.find(text)?.groupValues?.getOrNull(1)?.let(::normalizeAmount)

        val lowered = text.lowercase()
        val kind = when {
            "reversal" in lowered || "has been reversed" in lowered -> MpesaKind.REVERSAL
            "you have received" in lowered -> MpesaKind.RECEIVED
            "paid to" in lowered && ("paybill" in lowered || "account number" in lowered || "business number" in lowered) -> MpesaKind.PAYBILL
            "paid to" in lowered -> MpesaKind.TILL
            "sent to" in lowered -> MpesaKind.SENT
            "withdraw" in lowered -> MpesaKind.WITHDRAW
            "give ksh" in lowered && "cash to" in lowered -> MpesaKind.DEPOSIT
            "airtime" in lowered -> MpesaKind.AIRTIME
            else -> MpesaKind.UNSUPPORTED
        }
        if (kind == MpesaKind.UNSUPPORTED) return null

        val counterparty = when (kind) {
            MpesaKind.RECEIVED -> RECEIVED_FROM_REGEX.find(text)?.let {
                "${it.groupValues[1].trim()} ${it.groupValues[2].trim()}".trim()
            }.orEmpty()
            MpesaKind.SENT -> SENT_TO_REGEX.find(text)?.let {
                "${it.groupValues[1].trim()} ${it.groupValues[2].trim()}".trim()
            }.orEmpty()
            MpesaKind.PAYBILL, MpesaKind.TILL -> {
                val paid = PAID_TO_REGEX.find(text)?.groupValues?.getOrNull(1)?.trim().orEmpty()
                val account = ACCOUNT_REGEX.find(text)?.groupValues?.getOrNull(1)?.trim().orEmpty()
                listOf(paid, account).filter { it.isNotBlank() }.joinToString(" • ")
            }
            MpesaKind.WITHDRAW -> WITHDRAW_REGEX.find(text)?.groupValues?.getOrNull(1)?.trim().orEmpty()
            MpesaKind.DEPOSIT -> GIVE_CASH_REGEX.find(text)?.groupValues?.getOrNull(1)?.trim().orEmpty()
            else -> ""
        }

        val dedupeKey = if (code.isNotBlank()) {
            "code:$code"
        } else {
            "hash:" + sha256Hex(text.lowercase().replace(Regex("\\s+"), " ").take(500))
        }

        return ParsedMpesa(
            code = code,
            amount = amount,
            currency = "KES",
            dateTime = dateTime,
            kind = kind,
            counterparty = counterparty,
            balance = balance,
            dedupeKey = dedupeKey
        )
    }

    // Owner-facing suggestion only — confirmation always decides the final
    // classification. Transfers and uncertain events are never auto-posted.
    fun suggestedType(kind: MpesaKind): String = when (kind) {
        MpesaKind.RECEIVED -> "income"
        MpesaKind.PAYBILL, MpesaKind.TILL, MpesaKind.AIRTIME -> "expense"
        MpesaKind.SENT, MpesaKind.WITHDRAW, MpesaKind.DEPOSIT, MpesaKind.REVERSAL -> "uncertain"
        MpesaKind.UNSUPPORTED -> "uncertain"
    }
}
