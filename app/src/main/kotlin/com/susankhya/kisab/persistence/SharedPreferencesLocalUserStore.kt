package com.susankhya.kisab.persistence

import android.content.Context
import com.susankhya.kisab.domain.LocalUser
import com.susankhya.kisab.domain.LocalUserStore

/**
 * SharedPreferences-backed [LocalUserStore]. Identity and ownership live in a
 * dedicated prefs file, separate from [SharedPreferencesFarmStore] and from
 * farm backup envelopes.
 */
class SharedPreferencesLocalUserStore(context: Context) : LocalUserStore {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun loadUser(): LocalUser? {
        val userId = prefs.getString(KEY_USER_ID, null)?.takeIf { it.isNotBlank() } ?: return null
        val createdAt = prefs.getLong(KEY_CREATED_AT, MISSING)
        if (createdAt == MISSING) return null
        return LocalUser(userId = userId, createdAtMillis = createdAt)
    }

    override fun saveUser(user: LocalUser) {
        prefs.edit()
            .putString(KEY_USER_ID, user.userId)
            .putLong(KEY_CREATED_AT, user.createdAtMillis)
            .commit()
    }

    override fun ownedFarmIds(userId: String): Set<String> {
        val current = loadUser() ?: return emptySet()
        if (current.userId != userId) return emptySet()
        val raw = prefs.getString(KEY_OWNED_FARM_IDS, null) ?: return emptySet()
        if (raw.isBlank()) return emptySet()
        return raw.split(SEPARATOR).map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    }

    override fun addOwnedFarm(userId: String, farmId: String) {
        val current = loadUser() ?: return
        if (current.userId != userId) return
        val next = ownedFarmIds(userId) + farmId
        prefs.edit().putString(KEY_OWNED_FARM_IDS, next.sorted().joinToString(SEPARATOR)).commit()
    }

    override fun removeOwnedFarm(userId: String, farmId: String) {
        val current = loadUser() ?: return
        if (current.userId != userId) return
        val next = ownedFarmIds(userId) - farmId
        prefs.edit().putString(KEY_OWNED_FARM_IDS, next.sorted().joinToString(SEPARATOR)).commit()
    }

    companion object {
        private const val PREFS_NAME = "kisab_local_user"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_CREATED_AT = "created_at_millis"
        private const val KEY_OWNED_FARM_IDS = "owned_farm_ids"
        private const val SEPARATOR = "\u001F"
        private const val MISSING = Long.MIN_VALUE
    }
}
