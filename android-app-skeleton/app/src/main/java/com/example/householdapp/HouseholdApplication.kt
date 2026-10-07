package com.example.householdapp

import android.app.Application
import android.util.Log

/**
 * Installs a process-wide crash diagnostic for debug/field diagnosis.
 *
 * It only ADDS context (thread + exception + full stack) to Logcat and then
 * delegates to Android's default handler, so system crash reporting behaves
 * exactly as before. No financial data, tokens, or user content is logged.
 */
class HouseholdApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e(
                LOG_TAG,
                "Uncaught fatal error on thread '${thread.name}': " +
                    "${throwable.javaClass.name}: ${throwable.message}",
                throwable
            )
            previous?.uncaughtException(thread, throwable)
        }
    }

    companion object {
        private const val LOG_TAG = "HomeManagerCrash"
    }
}
