package com.example.householdapp.mpesa

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

// Stage 4: on-device pending M-Pesa inbox.
//
// Stored per pending item (NO full SMS body, NO unrelated messages):
// parsed code/amount/kind/counterparty/dateTime, dedupe key, status.
// A separate confirmed-key set prevents re-processing the same message.
// Capture can be disabled; disabling stops new detections (existing pending
// items stay until the owner dismisses/confirms them).

data class PendingMpesaItem(
    val id: String,
    val code: String,
    val amount: Double,
    val kind: String,
    val counterparty: String,
    val dateTime: String,
    val dedupeKey: String,
    val suggestedType: String,
    val detectedAt: Long = System.currentTimeMillis()
)

object PendingMpesaStore {
    private const val PREFS = "mpesa_inbox_prefs"
    private const val KEY_ITEMS = "pending_items_v1"
    private const val KEY_CONFIRMED = "confirmed_keys_v1"
    private const val KEY_CAPTURE_ENABLED = "capture_enabled"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isCaptureEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_CAPTURE_ENABLED, true)

    fun setCaptureEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_CAPTURE_ENABLED, enabled).apply()
    }

    private fun readItems(p: SharedPreferences): MutableList<PendingMpesaItem> {
        val out = mutableListOf<PendingMpesaItem>()
        val raw = p.getString(KEY_ITEMS, "[]").orEmpty()
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                out.add(
                    PendingMpesaItem(
                        id = o.optString("id"),
                        code = o.optString("code"),
                        amount = o.optDouble("amount"),
                        kind = o.optString("kind"),
                        counterparty = o.optString("counterparty"),
                        dateTime = o.optString("dateTime"),
                        dedupeKey = o.optString("dedupeKey"),
                        suggestedType = o.optString("suggestedType"),
                        detectedAt = o.optLong("detectedAt")
                    )
                )
            }
        } catch (e: Exception) {
            // Corrupt inbox: start empty rather than crash; confirmed keys
            // are stored separately so dedupe history survives.
        }
        return out
    }

    private fun writeItems(p: SharedPreferences, items: List<PendingMpesaItem>) {
        val arr = JSONArray()
        items.forEach {
            arr.put(
                JSONObject()
                    .put("id", it.id)
                    .put("code", it.code)
                    .put("amount", it.amount)
                    .put("kind", it.kind)
                    .put("counterparty", it.counterparty)
                    .put("dateTime", it.dateTime)
                    .put("dedupeKey", it.dedupeKey)
                    .put("suggestedType", it.suggestedType)
                    .put("detectedAt", it.detectedAt)
            )
        }
        p.edit().putString(KEY_ITEMS, arr.toString()).apply()
    }

    fun confirmedKeys(context: Context): Set<String> =
        prefs(context).getStringSet(KEY_CONFIRMED, emptySet()).orEmpty().toSet()

    private fun addConfirmedKey(p: SharedPreferences, key: String) {
        val updated = confirmedKeys(p).toMutableSet()
        updated.add(key)
        p.edit().putStringSet(KEY_CONFIRMED, updated).apply()
    }

    private fun confirmedKeys(p: SharedPreferences): Set<String> =
        p.getStringSet(KEY_CONFIRMED, emptySet()).orEmpty().toSet()

    fun listPending(context: Context): List<PendingMpesaItem> =
        readItems(prefs(context)).sortedByDescending { it.detectedAt }

    // Returns the pending item, or null when this message was already seen
    // (pending or confirmed) — the acceptance-test dedupe guarantee.
    fun addIfNew(context: Context, parsed: ParsedMpesa): PendingMpesaItem? {
        if (!isCaptureEnabled(context)) return null
        val p = prefs(context)
        val items = readItems(p)
        if (items.any { it.dedupeKey == parsed.dedupeKey }) return null
        if (confirmedKeys(p).contains(parsed.dedupeKey)) return null
        val item = PendingMpesaItem(
            id = "mpesa_${System.currentTimeMillis()}_${(0..9999).random()}",
            code = parsed.code,
            amount = parsed.amount,
            kind = parsed.kind.name,
            counterparty = parsed.counterparty,
            dateTime = parsed.dateTime,
            dedupeKey = parsed.dedupeKey,
            suggestedType = MpesaParser.suggestedType(parsed.kind)
        )
        items.add(item)
        writeItems(p, items)
        return item
    }

    fun markConfirmed(context: Context, itemId: String) {
        val p = prefs(context)
        val items = readItems(p)
        val target = items.firstOrNull { it.id == itemId } ?: return
        addConfirmedKey(p, target.dedupeKey)
        writeItems(p, items.filter { it.id != itemId })
    }

    fun dismiss(context: Context, itemId: String) {
        val p = prefs(context)
        writeItems(p, readItems(p).filter { it.id != itemId })
    }
}
