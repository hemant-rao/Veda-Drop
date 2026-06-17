package com.example.data.remote

import android.content.Context

/**
 * Persists GlamGo session tokens. GlamGo has TWO independent server identities
 * (customer + partner) that the app can switch between, so we keep a separate
 * access/refresh pair per role plus the currently-active role. This mirrors the
 * backend's identity separation (a customer token cannot call partner routes).
 */
class TokenStore(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("glamgo_session", Context.MODE_PRIVATE)

    var activeRole: String?
        get() = prefs.getString(KEY_ACTIVE_ROLE, null)
        set(value) { prefs.edit().putString(KEY_ACTIVE_ROLE, value).apply() }

    fun accessToken(role: String? = activeRole): String? =
        role?.let { prefs.getString("access_$it", null) }

    fun refreshToken(role: String? = activeRole): String? =
        role?.let { prefs.getString("refresh_$it", null) }

    fun hasSession(role: String): Boolean = prefs.getString("access_$role", null) != null

    fun save(role: String, access: String, refresh: String, makeActive: Boolean = true) {
        prefs.edit()
            .putString("access_$role", access)
            .putString("refresh_$role", refresh)
            .apply()
        if (makeActive) activeRole = role
    }

    fun updateAccess(role: String, access: String, refresh: String?) {
        val e = prefs.edit().putString("access_$role", access)
        if (refresh != null) e.putString("refresh_$role", refresh)
        e.apply()
    }

    fun clearRole(role: String) {
        prefs.edit().remove("access_$role").remove("refresh_$role").apply()
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_ACTIVE_ROLE = "active_role"
    }
}
