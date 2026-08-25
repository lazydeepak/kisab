package com.susankhya.kisab.persistence

import android.content.Context
import com.susankhya.kisab.domain.FarmState
import com.susankhya.kisab.domain.FarmStore

/**
 * Production [FarmStore]: multi-farm SharedPreferences layout with legacy
 * single-farm migration. All critical writes use synchronous [commit].
 */
class SharedPreferencesFarmStore(context: Context) : FarmStore {
    private val delegate = MultiFarmStore(SharedPreferencesMultiFarmBackend(context))

    override fun loadFarm(farmId: String): FarmState? = delegate.loadFarm(farmId)

    override fun saveFarm(farm: FarmState) = delegate.saveFarm(farm)

    override fun setCurrentFarmId(farmId: String) = delegate.setCurrentFarmId(farmId)

    override fun currentFarmId(): String? = delegate.currentFarmId()

    override fun clear() = delegate.clear()

    override fun deleteFarm(farmId: String) = delegate.deleteFarm(farmId)

    override fun farmIds(): List<String> = delegate.farmIds()

    private class SharedPreferencesMultiFarmBackend(
        context: Context
    ) : MultiFarmStoreBackend {
        private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        override fun getString(key: String): String? = prefs.getString(key, null)

        override fun commit(puts: Map<String, String>, removes: Set<String>): Boolean {
            val editor = prefs.edit()
            for ((key, value) in puts) {
                editor.putString(key, value)
            }
            for (key in removes) {
                editor.remove(key)
            }
            return editor.commit()
        }

        override fun allKeys(): Set<String> = prefs.all.keys.filterNotNull().toSet()

        companion object {
            private const val PREFS_NAME = "kisab_farm_store"
        }
    }
}
