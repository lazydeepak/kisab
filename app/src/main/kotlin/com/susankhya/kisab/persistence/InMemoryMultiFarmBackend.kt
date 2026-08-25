package com.susankhya.kisab.persistence

/** In-memory [MultiFarmStoreBackend] for unit tests. */
class InMemoryMultiFarmBackend(
    initial: Map<String, String> = emptyMap()
) : MultiFarmStoreBackend {
    private val data = linkedMapOf<String, String>().apply { putAll(initial) }

    override fun getString(key: String): String? = data[key]

    override fun commit(puts: Map<String, String>, removes: Set<String>): Boolean {
        for (key in removes) data.remove(key)
        data.putAll(puts)
        return true
    }

    override fun allKeys(): Set<String> = data.keys.toSet()

    fun snapshot(): Map<String, String> = data.toMap()
}
