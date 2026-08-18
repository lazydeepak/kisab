package com.susankhya.kisab.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationFoundationTest {

    @Test
    fun permissionPresentationApiBelow33AlwaysOn() {
        assertEquals(
            NotificationPermissionUiState.ON,
            NotificationPermissionPresentation.uiState(granted = false, requiresRuntime = false)
        )
    }

    @Test
    fun permissionPresentationApi33GrantedOn() {
        assertEquals(
            NotificationPermissionUiState.ON,
            NotificationPermissionPresentation.uiState(granted = true, requiresRuntime = true)
        )
    }

    @Test
    fun permissionPresentationApi33DeniedOff() {
        assertEquals(
            NotificationPermissionUiState.OFF,
            NotificationPermissionPresentation.uiState(granted = false, requiresRuntime = true)
        )
    }

    @Test
    fun channelRouting() {
        assertEquals(NotificationChannels.UPDATES_ID, NotificationChannels.channelIdFor(NotificationType.APP_UPDATE))
        assertEquals(NotificationChannels.REMINDERS_ID, NotificationChannels.channelIdFor(NotificationType.BACKUP_REMINDER))
        assertEquals(NotificationChannels.UPDATES_ID, NotificationChannels.channelIdFor(NotificationType.GENERAL_OPERATIONAL_NOTICE))
    }

    @Test
    fun deepLinksAreConservative() {
        assertEquals(NotificationDeepLink.UPDATE_INFO, NotificationType.APP_UPDATE.deepLink())
        assertEquals(NotificationDeepLink.BACKUP_DATA, NotificationType.BACKUP_REMINDER.deepLink())
        assertEquals(NotificationDeepLink.NOTIFICATION_SETTINGS, NotificationType.GENERAL_OPERATIONAL_NOTICE.deepLink())
    }

    @Test
    fun parseValidAppUpdate() {
        val msg = IncomingPushMessage.parse(
            mapOf(
                "type" to "APP_UPDATE",
                "title" to "Update available",
                "body" to "A newer Kisab is ready."
            )
        )
        assertEquals(NotificationType.APP_UPDATE, msg!!.type)
        assertEquals("Update available", msg.title)
    }

    @Test
    fun parseUnknownTypeIgnored() {
        assertNull(
            IncomingPushMessage.parse(
                mapOf("type" to "MARKETING_BLAST", "title" to "Buy", "body" to "Stuff")
            )
        )
    }

    @Test
    fun parseIgnoresActionLikeFieldsAndStillWorks() {
        val msg = IncomingPushMessage.parse(
            mapOf(
                "type" to "BACKUP_REMINDER",
                "title" to "Backup",
                "body" to "Export a copy",
                "action" to "DELETE_ALL_FARMS",
                "url" to "https://evil.example/install.apk",
                "command" to "reset"
            )
        )
        assertEquals(NotificationType.BACKUP_REMINDER, msg!!.type)
        assertEquals("Backup", msg.title)
    }

    @Test
    fun parseRejectsBlankTitleOrBody() {
        assertNull(IncomingPushMessage.parse(mapOf("type" to "APP_UPDATE", "title" to " ", "body" to "x")))
        assertNull(IncomingPushMessage.parse(mapOf("type" to "APP_UPDATE", "title" to "x", "body" to "")))
    }

    @Test
    fun parseTruncatesOverlongText() {
        val long = "a".repeat(500)
        val msg = IncomingPushMessage.parse(
            mapOf("type" to "GENERAL_OPERATIONAL_NOTICE", "title" to long, "body" to long)
        )!!
        assertEquals(200, msg.title.length)
        assertEquals(200, msg.body.length)
    }

    @Test
    fun categoryMapping() {
        assertEquals(NotificationCategory.APP_UPDATES, NotificationType.APP_UPDATE.toCategory())
        assertEquals(NotificationCategory.BACKUP_REMINDERS, NotificationType.BACKUP_REMINDER.toCategory())
    }

    @Test
    fun inMemoryPreferencesDefaultOnAndCanDisable() {
        val prefs = InMemoryNotificationPreferences()
        assertTrue(prefs.isCategoryEnabled(NotificationCategory.APP_UPDATES))
        prefs.setCategoryEnabled(NotificationCategory.APP_UPDATES, false)
        assertFalse(prefs.isCategoryEnabled(NotificationCategory.APP_UPDATES))
    }

    @Test
    fun tokenStoreReplace() {
        val store = InMemoryPushTokenStore()
        assertNull(store.latestToken())
        store.saveToken("token-a")
        assertEquals("token-a", store.latestToken())
        store.saveToken("token-b")
        assertEquals("token-b", store.latestToken())
        store.clear()
        assertNull(store.latestToken())
    }

    @Test
    fun disabledCategorySuppressesRenderingGate() {
        val prefs = InMemoryNotificationPreferences()
        prefs.setCategoryEnabled(NotificationCategory.APP_UPDATES, false)
        val message = IncomingPushMessage(
            type = NotificationType.APP_UPDATE,
            title = "t",
            body = "b"
        )
        val category = message.type.toCategory()!!
        assertFalse(prefs.isCategoryEnabled(category))
    }

    @Test
    fun routerDropsUnknownPayloadAtParseGate() {
        assertNull(IncomingPushMessage.parse(mapOf("foo" to "bar")))
        assertNull(IncomingPushMessage.parse(emptyMap()))
    }

    private class InMemoryNotificationPreferences : NotificationPreferences {
        private val map = mutableMapOf(
            NotificationCategory.APP_UPDATES to true,
            NotificationCategory.BACKUP_REMINDERS to true
        )

        override fun isCategoryEnabled(category: NotificationCategory): Boolean = map[category] != false

        override fun setCategoryEnabled(category: NotificationCategory, enabled: Boolean) {
            map[category] = enabled
        }
    }

    private class InMemoryPushTokenStore : PushTokenStore {
        private var token: String? = null
        override fun latestToken(): String? = token
        override fun saveToken(token: String) {
            this.token = token.trim()
        }
        override fun clear() {
            token = null
        }
    }
}
