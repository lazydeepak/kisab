package com.susankhya.kisab.persistence

/**
 * Synchronous string key/value backend for multi-farm farm storage.
 * Production uses SharedPreferences with [commit]; tests use an in-memory map.
 */
interface MultiFarmStoreBackend {
    fun getString(key: String): String?

    /** Atomically apply puts and removes; returns false if the write failed. */
    fun commit(puts: Map<String, String>, removes: Set<String> = emptySet()): Boolean

    fun allKeys(): Set<String>
}
