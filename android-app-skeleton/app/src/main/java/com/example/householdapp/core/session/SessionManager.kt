package com.example.householdapp.core.session

import android.content.Context
import android.content.SharedPreferences
import com.example.householdapp.core.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object SessionManager {
    private const val PREFS = "session_prefs"
    private const val KEY_IS_AUTHENTICATED = "is_authenticated"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_EMAIL = "email"
    private const val KEY_DISPLAY_NAME = "display_name"
    private const val KEY_ROLE = "role"
    private const val KEY_XP_TOTAL = "xp_total"
    private const val KEY_LEVEL = "level"
    private const val KEY_COINS_TOTAL = "coins_total"
    private const val KEY_CURRENT_STREAK = "current_streak"
    private const val KEY_LONGEST_STREAK = "longest_streak"
    private const val KEY_PHOTO_URL = "photo_url"
    private const val KEY_HOUSEHOLD_SIDE = "household_side"
    private const val KEY_PARTNER_ID = "partner_id"
    private const val KEY_PARTNER_DISPLAY_NAME = "partner_display_name"
    private const val KEY_PARTNER_EMAIL = "partner_email"
    private const val KEY_PARTNER_XP = "partner_xp"
    private const val KEY_PARTNER_LEVEL = "partner_level"
    private const val KEY_PARTNER_COINS = "partner_coins"
    private const val KEY_PARTNER_STREAK = "partner_streak"
    private const val KEY_PARTNER_LONGEST_STREAK = "partner_longest_streak"
    private const val KEY_CURRENCY = "currency"
    private const val KEY_HOUSEHOLD_ID = "household_id"
    private const val KEY_PERSONAL_IDENTITY = "personal_identity"

    private val _sessionState = MutableStateFlow(SessionState())
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        restoreSession()
    }

    fun setSession(sessionState: SessionState) {
        _sessionState.value = sessionState
        saveSession(sessionState)
    }

    fun updateUser(user: UserProfile) {
        _sessionState.update { current ->
            current.copy(
                isAuthenticated = true,
                user = user,
                email = user.email
            )
        }
        saveSession(_sessionState.value)
    }

    fun updatePartner(partner: UserProfile?) {
        _sessionState.update { it.copy(partner = partner) }
        saveSession(_sessionState.value)
    }

    fun setCurrency(currency: String) {
        _sessionState.update { it.copy(currency = currency) }
        saveSession(_sessionState.value)
    }

    fun clearSession() {
        _sessionState.value = SessionState()
        prefs?.edit()?.clear()?.apply()
    }

    private fun saveSession(state: SessionState) {
        val editor = prefs?.edit() ?: return
        editor.putBoolean(KEY_IS_AUTHENTICATED, state.isAuthenticated)
        state.user?.let { user ->
            editor.putString(KEY_USER_ID, user.userId)
            editor.putString(KEY_EMAIL, user.email)
            editor.putString(KEY_DISPLAY_NAME, user.displayName)
            editor.putString(KEY_ROLE, user.role)
            editor.putInt(KEY_XP_TOTAL, user.xpTotal)
            editor.putInt(KEY_LEVEL, user.level)
            editor.putInt(KEY_COINS_TOTAL, user.coinsTotal)
            editor.putInt(KEY_CURRENT_STREAK, user.currentStreak)
            editor.putInt(KEY_LONGEST_STREAK, user.longestStreak)
            editor.putString(KEY_PHOTO_URL, user.photoUrl)
            editor.putString(KEY_HOUSEHOLD_SIDE, user.householdSide)
            editor.putString(KEY_HOUSEHOLD_ID, user.householdId)
            editor.putString(KEY_PERSONAL_IDENTITY, user.personalIdentity)
        }
        state.partner?.let { partner ->
            editor.putString(KEY_PARTNER_ID, partner.userId)
            editor.putString(KEY_PARTNER_DISPLAY_NAME, partner.displayName)
            editor.putString(KEY_PARTNER_EMAIL, partner.email)
            editor.putInt(KEY_PARTNER_XP, partner.xpTotal)
            editor.putInt(KEY_PARTNER_LEVEL, partner.level)
            editor.putInt(KEY_PARTNER_COINS, partner.coinsTotal)
            editor.putInt(KEY_PARTNER_STREAK, partner.currentStreak)
            editor.putInt(KEY_PARTNER_LONGEST_STREAK, partner.longestStreak)
        }
        editor.putString(KEY_EMAIL, state.email)
        editor.putString(KEY_CURRENCY, state.currency)
        editor.apply()
    }

    private fun safeBoolean(p: SharedPreferences, key: String, default: Boolean): Boolean {
        val value = p.all[key] ?: return default
        return if (value is Boolean) value else default
    }

    private fun safeInt(p: SharedPreferences, key: String, default: Int): Int {
        val value = p.all[key] ?: return default
        return if (value is Int) value else default
    }

    private fun safeString(p: SharedPreferences, key: String, default: String): String {
        val value = p.all[key] ?: return default
        return if (value is String) value else default
    }

    private fun safeNullableString(p: SharedPreferences, key: String): String? {
        val value = p.all[key] ?: return null
        return if (value is String) value else null
    }

    private fun restoreSession() {
        val p = prefs ?: return
        val isAuthenticated = safeBoolean(p, KEY_IS_AUTHENTICATED, false)
        val userId = safeNullableString(p, KEY_USER_ID)

        if (!isAuthenticated || userId.isNullOrBlank()) return

        val user = UserProfile(
            userId = userId,
            email = safeString(p, KEY_EMAIL, ""),
            displayName = safeString(p, KEY_DISPLAY_NAME, ""),
            role = safeString(p, KEY_ROLE, ""),
            xpTotal = safeInt(p, KEY_XP_TOTAL, 0),
            level = safeInt(p, KEY_LEVEL, 1),
            coinsTotal = safeInt(p, KEY_COINS_TOTAL, 0),
            currentStreak = safeInt(p, KEY_CURRENT_STREAK, 0),
            longestStreak = safeInt(p, KEY_LONGEST_STREAK, 0),
            photoUrl = safeString(p, KEY_PHOTO_URL, ""),
            householdSide = safeString(p, KEY_HOUSEHOLD_SIDE, ""),
            householdId = safeString(p, KEY_HOUSEHOLD_ID, ""),
            personalIdentity = safeString(p, KEY_PERSONAL_IDENTITY, "")
        )

        val partnerId = safeNullableString(p, KEY_PARTNER_ID)
        val partner = if (!partnerId.isNullOrBlank()) {
            UserProfile(
                userId = partnerId,
                email = safeString(p, KEY_PARTNER_EMAIL, ""),
                displayName = safeString(p, KEY_PARTNER_DISPLAY_NAME, ""),
                role = "member",
                xpTotal = safeInt(p, KEY_PARTNER_XP, 0),
                level = safeInt(p, KEY_PARTNER_LEVEL, 1),
                coinsTotal = safeInt(p, KEY_PARTNER_COINS, 0),
                currentStreak = safeInt(p, KEY_PARTNER_STREAK, 0),
                longestStreak = safeInt(p, KEY_PARTNER_LONGEST_STREAK, 0),
                householdId = "",
                personalIdentity = ""
            )
        } else null

        _sessionState.value = SessionState(
            isAuthenticated = true,
            user = user,
            partner = partner,
            email = safeString(p, KEY_EMAIL, ""),
            currency = safeString(p, KEY_CURRENCY, "USD"),
            householdId = user.householdId,
            personalIdentity = user.personalIdentity
        )
    }
}