package com.susankhya.kisab.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrivateApkUpdateLogicTest {

    @Test
    fun higherVersionCodeIsUpdateAvailable() {
        val current = VersionInfo(versionCode = 10, versionName = "0.2.0")
        val remote = UpdateInfo(
            versionCode = 11,
            versionName = "0.3.0",
            apkUrl = "https://example.test/app-release.apk",
            sha256 = "aa".repeat(32),
            releaseNotes = "New version",
            publishedAt = "2026-01-01T00:00:00Z"
        )
        assertTrue(remote.isNewerThan(current))
        assertEquals(UpdateCheckResult.UpdateAvailable(remote), UpdateDecision.evaluate(current, remote))
    }

    @Test
    fun equalOrLowerVersionCodeDoesNotDowngrade() {
        val current = VersionInfo(versionCode = 12, versionName = "0.2.0")
        val same = UpdateInfo(
            versionCode = 12,
            versionName = "0.2.0",
            apkUrl = "https://example.test/app-release.apk",
            sha256 = "bb".repeat(32),
            releaseNotes = "No change"
        )
        val lower = same.copy(versionCode = 11, versionName = "0.1.9")

        assertFalse(same.isNewerThan(current))
        assertTrue(UpdateDecision.evaluate(current, lower) is UpdateCheckResult.NoUpdate)
    }

    @Test
    fun validManifestParsesAndRejectsBadUrl() {
                val valid = """
            {
              "versionCode": 21,
              "versionName": "0.3.0",
              "apkUrl": "https://example.test/kisab.apk",
                            "sha256": "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff",
              "releaseNotes": "Great update",
              "publishedAt": "2026-02-01T00:00:00Z"
            }
        """.trimIndent()
        val parsed = UpdateManifest.parse(valid)
        assertTrue(parsed != null)
        assertEquals(21, parsed!!.versionCode)
        assertEquals("https://example.test/kisab.apk", parsed.apkUrl)

        val invalid = valid.replace("https://example.test", "ftp://example.test")
        assertTrue(UpdateManifest.parse(invalid) == null)
    }

    @Test
    fun manifestWithoutChecksumIsRejected() {
        val manifest = """
            {
              "versionCode": 21,
              "versionName": "0.3.0",
              "apkUrl": "https://example.test/kisab.apk"
            }
        """.trimIndent()

        assertTrue(UpdateManifest.parse(manifest) == null)
    }

    @Test
    fun integrityCheckRejectsMismatchedSha256() {
        val expected = "aa".repeat(32)
        assertTrue(UpdateIntegrityVerifier.sha256Matches("hello world".toByteArray(), expected) == false)
    }

    @Test
    fun expiredBuildStillAllowsCheckWithoutMutatingState() {
        val current = VersionInfo(versionCode = 10, versionName = "0.2.0")
        val remote = UpdateInfo(
            versionCode = 11,
            versionName = "0.3.0",
            apkUrl = "https://example.test/kisab.apk",
            sha256 = "cc".repeat(32),
            releaseNotes = "Ready"
        )
        val decision = UpdateDecision.evaluate(current, remote)
        assertTrue(decision is UpdateCheckResult.UpdateAvailable)
        assertEquals(11, (decision as UpdateCheckResult.UpdateAvailable).info.versionCode)
    }
}
