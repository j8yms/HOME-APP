package com.example.householdapp.core.session

import android.content.Context
import java.util.UUID

object DeviceIdentity {
    private const val PREFS = "device_prefs"
    private const val KEY_DEVICE_ID = "device_id"

    fun getOrCreate(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        var id = prefs.getString(KEY_DEVICE_ID, null)
        if (id == null) {
            id = "d_" + UUID.randomUUID().toString().replace("-", "").take(12)
            prefs.edit().putString(KEY_DEVICE_ID, id).apply()
        }
        return id
    }
}
