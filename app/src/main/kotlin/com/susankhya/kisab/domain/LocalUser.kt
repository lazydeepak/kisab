package com.susankhya.kisab.domain

/**
 * Stable offline Kisab identity. Generated once on the device and never
 * derived from device IDs, accounts, or personal data. Later an online Kisab
 * account may *link* to this identity without replacing it or farm IDs.
 */
data class LocalUser(
    val userId: String,
    val createdAtMillis: Long
)
