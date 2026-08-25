package com.susankhya.kisab.account

/**
 * Opaque credential obtained from a provider SDK (ID token / authorization code).
 * Must not be logged or stored in ordinary prefs. Handed only to [AccountApi].
 */
data class ProviderCredential(
    val provider: AuthProvider,
    /** Opaque assertion from the provider; never treated as a Kisab account id. */
    val assertion: String
) {
    init {
        require(assertion.isNotBlank()) { "provider assertion is required" }
    }
}
