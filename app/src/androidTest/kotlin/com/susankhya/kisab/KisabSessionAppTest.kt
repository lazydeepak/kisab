package com.susankhya.kisab

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.susankhya.foundation.session.StoredSession
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Android instrumented tests for [KisabSessionApp.storage]
 * using real [AndroidKeystoreSessionStorage].
 */
@RunWith(AndroidJUnit4::class)
class KisabSessionAppTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    /** Save and read round-trip works through KisabSessionApp.storage. */
    @Test
    fun savesAndReadsWithFoundationStorage() = runTest {
        val storage = KisabSessionApp().storage(context)
        storage.clear()
        storage.save(StoredSession(id = "kisab-session", payload = "payload"))
        val read = storage.read()
        assertEquals("kisab-session", read?.id)
        assertEquals("payload", read?.payload)
    }

    /** Clear removes the stored session. */
    @Test
    fun clearsStoredSession() = runTest {
        val storage = KisabSessionApp().storage(context)
        storage.clear()
        storage.save(StoredSession(id = "kisab-session", payload = "payload"))
        storage.clear()
        assertNull(storage.read())
    }
}
