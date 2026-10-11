package com.example.householdapp.mpesa

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony

// Stage 4: manifest-declared receiver for incoming SMS. It reads ONLY the
// PDUs of the current broadcast, keeps messages whose sender is MPESA, runs
// the on-device parser, and stores parsed fields in the pending inbox.
// Nothing is uploaded; full message bodies are never persisted.
class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        if (!PendingMpesaStore.isCaptureEnabled(context.applicationContext)) return
        val appContext = context.applicationContext
        try {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            // Multi-part messages arrive as several PDUs — handle each part;
            // the parser+dedupe layer keeps exactly one pending item.
            messages.forEach { message ->
                val sender = message.displayOriginatingAddress
                val body = message.displayMessageBody
                val parsed = MpesaParser.parse(sender, body) ?: return@forEach
                PendingMpesaStore.addIfNew(appContext, parsed)
            }
        } catch (e: Exception) {
            // Never crash the broadcast path on a malformed PDU.
        }
    }
}
