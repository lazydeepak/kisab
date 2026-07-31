package com.susankhya.kisab.session

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.susankhya.foundation.session.AndroidKeystoreSessionStorage
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class KisabSessionStorageAdapterTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun savesAndReadsSessionsUsingFoundationStorage() = runTest {
        val adapter = KisabSessionStorageAdapter(
            AndroidKeystoreSessionStorage(context, prefsName = "kisab_test_store", keyAlias = "kisab_test_key")
        )

        adapter.save(KisabSession(sessionId = "session-1", accessToken = "token-1", refreshToken = "refresh-1"))

        val read = adapter.read()

        assertEquals("session-1", read?.sessionId)
        assertEquals("token-1", read?.accessToken)
        assertEquals("refresh-1", read?.refreshToken)
    }

    @Test
    fun clearsStoredSession() = runTest {
        val adapter = KisabSessionStorageAdapter(
            AndroidKeystoreSessionStorage(context, prefsName = "kisab_test_store_clear", keyAlias = "kisab_test_key_clear")
        )

        adapter.save(KisabSession(sessionId = "session-2", accessToken = "token-2"))
        adapter.clear()

        assertNull(adapter.read())
    }

    @Test
    fun preservesSessionAcrossRecreatedStorageInstance() = runTest {
        val firstAdapter = KisabSessionStorageAdapter(
            AndroidKeystoreSessionStorage(context, prefsName = "kisab_test_store_recreate", keyAlias = "kisab_test_key_recreate")
        )

        firstAdapter.save(KisabSession(sessionId = "session-3", accessToken = "token-3", refreshToken = "refresh-3"))

        val secondAdapter = KisabSessionStorageAdapter(
            AndroidKeystoreSessionStorage(context, prefsName = "kisab_test_store_recreate", keyAlias = "kisab_test_key_recreate")
        )
        val read = secondAdapter.read()

        assertEquals("session-3", read?.sessionId)
        assertEquals("token-3", read?.accessToken)
        assertEquals("refresh-3", read?.refreshToken)
    }
}
