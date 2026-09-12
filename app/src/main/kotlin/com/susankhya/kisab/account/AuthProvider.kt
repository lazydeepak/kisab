package com.susankhya.kisab.account

/**
 * Future authentication providers that prove a person to the Kisab backend.
 * Provider subject IDs are never Kisab account IDs.
 */
enum class AuthProvider {
    GOOGLE,
    APPLE,
    /** Email OTP (ADR-0004). The code is a provider credential, never an account id. */
    EMAIL
}
