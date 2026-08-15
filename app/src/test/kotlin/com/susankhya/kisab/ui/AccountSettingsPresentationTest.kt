package com.susankhya.kisab.ui

import com.susankhya.kisab.domain.AccountLink
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountSettingsPresentationTest {

    @Test
    fun unlinkedIsLocalOnly() {
        val state = AccountSettingsPresentation.uiState(
            AccountLink.Unlinked("user-abc"),
            hasActiveSession = null
        )
        assertEquals(AccountConnectionStatus.LOCAL_ONLY, state.status)
        assertFalse(state.isConnected)
        assertFalse(state.showSignInRequired)
    }

    @Test
    fun linkedIsConnectedEvenWithoutSessionKnowledge() {
        val state = AccountSettingsPresentation.uiState(
            AccountLink.Linked("user-abc", "account-server-xyz", 1L),
            hasActiveSession = null
        )
        assertEquals(AccountConnectionStatus.CONNECTED, state.status)
        assertTrue(state.isConnected)
        assertFalse(state.showSignInRequired)
    }

    @Test
    fun linkedWithoutSessionShowsSignInRequiredWhenKnown() {
        val state = AccountSettingsPresentation.uiState(
            AccountLink.Linked("user-abc", "account-1", 1L),
            hasActiveSession = false
        )
        assertEquals(AccountConnectionStatus.CONNECTED, state.status)
        assertTrue(state.showSignInRequired)
    }

    @Test
    fun linkedWithSessionDoesNotShowSignInRequired() {
        val state = AccountSettingsPresentation.uiState(
            AccountLink.Linked("user-abc", "account-1", 1L),
            hasActiveSession = true
        )
        assertEquals(AccountConnectionStatus.CONNECTED, state.status)
        assertFalse(state.showSignInRequired)
    }

    @Test
    fun unlinkedNeverShowsSignInRequiredEvenIfSessionFalse() {
        val state = AccountSettingsPresentation.uiState(
            AccountLink.Unlinked("user-abc"),
            hasActiveSession = false
        )
        assertFalse(state.showSignInRequired)
    }

    @Test
    fun shortenedAccountIdIsSupportFriendlyNotFullOpaqueBlob() {
        val full = "account-0123456789abcdef-long"
        val short = AccountSettingsPresentation.shortenedAccountId(full)
        assertTrue(short.length < full.length)
        assertFalse(short.contains("token"))
        assertTrue(short.startsWith("account-"))
    }
}
