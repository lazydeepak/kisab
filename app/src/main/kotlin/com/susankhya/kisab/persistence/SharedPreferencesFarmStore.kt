package com.susankhya.kisab.persistence

import android.content.Context
import com.susankhya.kisab.domain.FarmState
import com.susankhya.kisab.domain.FarmStore

class SharedPreferencesFarmStore(context: Context) : FarmStore {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun loadFarm(farmId: String): FarmState? {
        val persistedFarmId = prefs.getString(PREF_CURRENT_FARM_ID, null) ?: return null
        if (persistedFarmId != farmId) return null

        val encoded = prefs.getString(PREF_FARM_STATE, null) ?: return null
        return FarmPersistenceCodec.decodeOrNull(encoded)
    }

    override fun saveFarm(farm: FarmState) {
        prefs.edit()
            .putString(PREF_CURRENT_FARM_ID, farm.id)
            .putString(PREF_FARM_STATE, FarmPersistenceCodec.encode(farm))
            .commit()
    }

    override fun setCurrentFarmId(farmId: String) {
        prefs.edit().putString(PREF_CURRENT_FARM_ID, farmId).commit()
    }

    override fun currentFarmId(): String? = prefs.getString(PREF_CURRENT_FARM_ID, null)

    override fun clear() {
        prefs.edit().remove(PREF_CURRENT_FARM_ID).remove(PREF_FARM_STATE).commit()
    }

    override fun deleteFarm(farmId: String) {
        if (prefs.getString(PREF_CURRENT_FARM_ID, null) == farmId) {
            prefs.edit().remove(PREF_CURRENT_FARM_ID).remove(PREF_FARM_STATE).commit()
        }
    }

    companion object {
        private const val PREFS_NAME = "kisab_farm_store"
        private const val PREF_CURRENT_FARM_ID = "current_farm_id"
        private const val PREF_FARM_STATE = "farm_state"
    }
}
