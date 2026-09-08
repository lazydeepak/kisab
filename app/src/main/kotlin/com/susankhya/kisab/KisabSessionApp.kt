package com.susankhya.kisab

import android.content.Context
import com.susankhya.foundation.session.AndroidKeystoreSessionStorage
import com.susankhya.foundation.session.SessionStorage

/**
 * Application entry point for the Kisab session infrastructure.
 *
 * Provides the [SessionStorage] backed by the Android Keystore,
 * which secures [KisabSession] credentials at rest. Used by
 * [KisabSessionStorageAdapter] to persist and retrieve session material.
 *
 * The keystore-backed storage ensures tokens are protected by the
 * device's hardware security layer and never appear in plain text
 * in shared preferences or backups.
 */
class KisabSessionApp {
    /**
     * Creates a [SessionStorage] backed by the Android Keystore.
     *
     * @param context application context used to access the keystore
     * @return [AndroidKeystoreSessionStorage] instance for the app
     */
    fun storage(context: Context): SessionStorage = AndroidKeystoreSessionStorage(context)
}
