package com.susankhya.kisab.persistence

import com.susankhya.kisab.domain.FarmState
import com.susankhya.kisab.domain.FarmStore

/**
 * Multi-farm [FarmStore] over a synchronous string backend.
 *
 * Layout (store_layout_version = 2):
 * - [KEY_LAYOUT_VERSION]
 * - [KEY_CURRENT_FARM_ID]
 * - [KEY_FARM_IDS] — US-separated farm id index (insertion order)
 * - [KEY_PAYLOAD_PREFIX] + farmId — FarmPersistenceCodec payload per farm
 *
 * Legacy layout (version 0 / absent): [KEY_CURRENT_FARM_ID] + [KEY_LEGACY_FARM_STATE].
 * Migration copies the legacy payload bytes into the per-farm key, preserves farm id
 * and current pointer, then removes the legacy key only after a successful commit.
 */
class MultiFarmStore(private val backend: MultiFarmStoreBackend) : FarmStore {

    override fun loadFarm(farmId: String): FarmState? {
        ensureMigrated()
        val encoded = backend.getString(payloadKey(farmId)) ?: return null
        return FarmPersistenceCodec.decodeOrNull(encoded)
    }

    override fun saveFarm(farm: FarmState) {
        ensureMigrated()
        val encoded = FarmPersistenceCodec.encode(farm)
        val ids = farmIdsMutable()
        if (farm.id !in ids) ids.add(farm.id)
        val puts = mutableMapOf(
            KEY_LAYOUT_VERSION to LAYOUT_VERSION.toString(),
            KEY_FARM_IDS to encodeIds(ids),
            payloadKey(farm.id) to encoded
        )
        check(backend.commit(puts)) { "Failed to persist farm ${farm.id}" }
    }

    override fun setCurrentFarmId(farmId: String) {
        ensureMigrated()
        require(farmId in farmIds()) { "Unknown farm: $farmId" }
        check(backend.commit(mapOf(KEY_CURRENT_FARM_ID to farmId))) {
            "Failed to set current farm"
        }
    }

    override fun currentFarmId(): String? {
        ensureMigrated()
        val id = backend.getString(KEY_CURRENT_FARM_ID)?.takeIf { it.isNotBlank() } ?: return null
        return if (id in farmIds()) id else null
    }

    override fun clear() {
        val removes = backend.allKeys().filter { key ->
            key == KEY_LAYOUT_VERSION ||
                key == KEY_CURRENT_FARM_ID ||
                key == KEY_FARM_IDS ||
                key == KEY_LEGACY_FARM_STATE ||
                key.startsWith(KEY_PAYLOAD_PREFIX)
        }.toSet()
        check(backend.commit(emptyMap(), removes)) { "Failed to clear farm store" }
    }

    override fun deleteFarm(farmId: String) {
        ensureMigrated()
        val ids = farmIdsMutable()
        if (farmId !in ids) return
        ids.remove(farmId)
        val removes = mutableSetOf(payloadKey(farmId))
        val puts = mutableMapOf(
            KEY_LAYOUT_VERSION to LAYOUT_VERSION.toString(),
            KEY_FARM_IDS to encodeIds(ids)
        )
        if (backend.getString(KEY_CURRENT_FARM_ID) == farmId) {
            removes.add(KEY_CURRENT_FARM_ID)
        }
        check(backend.commit(puts, removes)) { "Failed to delete farm $farmId" }
    }

    override fun farmIds(): List<String> {
        ensureMigrated()
        return farmIdsMutable().toList()
    }

    /**
     * Idempotent legacy → multi-farm migration. Failure leaves legacy keys intact.
     */
    fun ensureMigrated() {
        val version = backend.getString(KEY_LAYOUT_VERSION)?.toIntOrNull() ?: 0
        if (version >= LAYOUT_VERSION) return

        val legacyPayload = backend.getString(KEY_LEGACY_FARM_STATE)
        if (legacyPayload == null) {
            // Empty install or already only multi-farm keys without version marker.
            val existingIds = farmIdsMutable()
            val puts = mutableMapOf(KEY_LAYOUT_VERSION to LAYOUT_VERSION.toString())
            if (existingIds.isNotEmpty()) {
                puts[KEY_FARM_IDS] = encodeIds(existingIds)
            }
            check(backend.commit(puts)) { "Failed to mark multi-farm layout version" }
            return
        }

        val farm = FarmPersistenceCodec.decodeOrNull(legacyPayload)
            ?: return // leave legacy intact; do not mark migrated

        val farmId = farm.id
        val current = backend.getString(KEY_CURRENT_FARM_ID)?.takeIf { it.isNotBlank() } ?: farmId
        val puts = mapOf(
            KEY_LAYOUT_VERSION to LAYOUT_VERSION.toString(),
            KEY_CURRENT_FARM_ID to current,
            KEY_FARM_IDS to encodeIds(linkedSetOf(farmId)),
            // Preserve the exact legacy encoded bytes (no re-encode).
            payloadKey(farmId) to legacyPayload
        )
        val ok = backend.commit(puts, setOf(KEY_LEGACY_FARM_STATE))
        check(ok) { "Failed to migrate legacy farm store" }
    }

    private fun farmIdsMutable(): LinkedHashSet<String> {
        val raw = backend.getString(KEY_FARM_IDS)
        if (!raw.isNullOrBlank()) {
            return raw.split(ID_SEPARATOR)
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .toCollection(LinkedHashSet())
        }
        // Recover index from payload keys if farm_ids missing after partial write.
        val recovered = LinkedHashSet<String>()
        for (key in backend.allKeys()) {
            if (key.startsWith(KEY_PAYLOAD_PREFIX)) {
                recovered.add(key.removePrefix(KEY_PAYLOAD_PREFIX))
            }
        }
        return recovered
    }

    companion object {
        const val LAYOUT_VERSION = 2
        const val KEY_LAYOUT_VERSION = "store_layout_version"
        const val KEY_CURRENT_FARM_ID = "current_farm_id"
        const val KEY_FARM_IDS = "farm_ids"
        const val KEY_LEGACY_FARM_STATE = "farm_state"
        const val KEY_PAYLOAD_PREFIX = "farm_payload_"
        private const val ID_SEPARATOR = "\u001F"

        fun payloadKey(farmId: String): String = KEY_PAYLOAD_PREFIX + farmId

        fun encodeIds(ids: Collection<String>): String =
            ids.filter { it.isNotEmpty() }.joinToString(ID_SEPARATOR)
    }
}
