package com.susankhya.kisab.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import android.content.pm.PackageManager
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

private const val UPDATE_MANIFEST_TIMEOUT_MS = 15_000

data class VersionInfo(
    val versionCode: Long,
    val versionName: String
) {
    init {
        require(versionCode > 0L) { "versionCode must be positive" }
        require(versionName.isNotBlank()) { "versionName is required" }
    }
}

data class UpdateInfo(
    val versionCode: Long,
    val versionName: String,
    val apkUrl: String,
    val sha256: String? = null,
    val releaseNotes: String? = null,
    val publishedAt: String? = null
) {
    init {
        require(versionCode > 0L) { "versionCode must be positive" }
        require(versionName.isNotBlank()) { "versionName is required" }
        require(apkUrl.isNotBlank()) { "apkUrl is required" }
        require(isSupportedHttpsUrl(apkUrl)) { "apkUrl must use https" }
        require(isValidSha256(sha256)) { "sha256 must be a 64-character hexadecimal digest" }
    }

    fun isNewerThan(current: VersionInfo): Boolean = versionCode > current.versionCode
}

sealed class UpdateCheckResult {
    data object NoUpdate : UpdateCheckResult()
    data object UnableToCheck : UpdateCheckResult()
    data class UpdateAvailable(val info: UpdateInfo) : UpdateCheckResult()
}

object UpdateDecision {
    fun evaluate(current: VersionInfo, remote: UpdateInfo): UpdateCheckResult {
        if (remote.versionCode <= current.versionCode) return UpdateCheckResult.NoUpdate
        return UpdateCheckResult.UpdateAvailable(remote)
    }
}

data class UpdateManifest(
    val versionCode: Long,
    val versionName: String,
    val apkUrl: String,
    val sha256: String? = null,
    val releaseNotes: String? = null,
    val publishedAt: String? = null
) {
    fun asUpdateInfo(): UpdateInfo = UpdateInfo(
        versionCode = versionCode,
        versionName = versionName,
        apkUrl = apkUrl,
        sha256 = sha256,
        releaseNotes = releaseNotes,
        publishedAt = publishedAt
    )

    companion object {
        fun parse(json: String): UpdateManifest? {
            if (json.isBlank()) return null
            return try {
                val root = JSONObject(json)
                val versionCode = root.optLong("versionCode", -1L)
                val versionName = root.optString("versionName", "").trim()
                val apkUrl = root.optString("apkUrl", "").trim()
                val sha256 = root.optString("sha256", "").trim().ifBlank { null }
                val releaseNotes = root.optString("releaseNotes", "").trim().ifBlank { null }
                val publishedAt = root.optString("publishedAt", "").trim().ifBlank { null }
                if (versionCode <= 0L || versionName.isBlank() || apkUrl.isBlank()) return null
                if (!isSupportedHttpsUrl(apkUrl)) return null
                if (!isValidSha256(sha256)) return null
                UpdateManifest(
                    versionCode = versionCode,
                    versionName = versionName,
                    apkUrl = apkUrl,
                    sha256 = sha256,
                    releaseNotes = releaseNotes,
                    publishedAt = publishedAt
                )
            } catch (_: Throwable) {
                null
            }
        }
    }
}

interface UpdateSource {
    fun checkForUpdate(currentVersion: VersionInfo): UpdateCheckResult
}

class StaticManifestUpdateSource(
    private val manifestUrl: String?,
    private val fetcher: ManifestFetcher = HttpManifestFetcher
) : UpdateSource {
    override fun checkForUpdate(currentVersion: VersionInfo): UpdateCheckResult {
        val url = manifestUrl?.trim().orEmpty()
        if (!isSupportedHttpsUrl(url)) return UpdateCheckResult.UnableToCheck
        val manifest = fetcher.fetch(url) ?: return UpdateCheckResult.UnableToCheck
        return UpdateDecision.evaluate(currentVersion, manifest.asUpdateInfo())
    }
}

fun interface ManifestFetcher {
    fun fetch(url: String): UpdateManifest?
}

object HttpManifestFetcher : ManifestFetcher {
    override fun fetch(url: String): UpdateManifest? {
        val connection = URL(url).openConnection() as? HttpURLConnection ?: return null
        return try {
            connection.connectTimeout = UPDATE_MANIFEST_TIMEOUT_MS
            connection.readTimeout = UPDATE_MANIFEST_TIMEOUT_MS
            connection.instanceFollowRedirects = true
            connection.requestMethod = "GET"
            connection.connect()
            val status = connection.responseCode
            if (status !in 200..299) return null
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            UpdateManifest.parse(body)
        } catch (_: Throwable) {
            null
        } finally {
            connection.disconnect()
        }
    }
}

object UpdateIntegrityVerifier {
    fun sha256Matches(bytes: ByteArray, expectedHex: String?): Boolean {
        val normalizedExpected = expectedHex?.trim() ?: return false
        if (!isValidSha256(normalizedExpected)) return false
        val computed = MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it) }
        return computed.equals(normalizedExpected.lowercase(), ignoreCase = true)
    }

    fun sha256Matches(file: File, expectedHex: String?): Boolean =
        sha256Matches(file.readBytes(), expectedHex)
}

class ApkDownloadResult(
    val file: File?,
    val error: String? = null,
    val checksumVerified: Boolean = false
) {
    val isSuccess: Boolean get() = file != null && error == null
}

class ApkDownloader(
    private val context: Context,
    private val cacheName: String = "kisab-private-update.apk"
) {
    fun download(url: String, expectedSha256: String? = null): ApkDownloadResult {
        if (!isSupportedHttpsUrl(url)) return ApkDownloadResult(null, "unsupported url")
        if (!isValidSha256(expectedSha256)) return ApkDownloadResult(null, "checksum unavailable")
        return try {
            val file = File(context.cacheDir, cacheName)
            file.delete()
            var keepFile = false
            val connection = URL(url).openConnection() as? HttpURLConnection ?: return ApkDownloadResult(null, "download unavailable")
            try {
                connection.connectTimeout = UPDATE_MANIFEST_TIMEOUT_MS
                connection.readTimeout = UPDATE_MANIFEST_TIMEOUT_MS
                connection.instanceFollowRedirects = true
                connection.connect()
                val statusCode = connection.responseCode
                if (statusCode !in 200..299) {
                    return ApkDownloadResult(null, "download failed: $statusCode")
                }
                val bytes = connection.inputStream.use { input ->
                    val output = FileOutputStream(file)
                    output.use { out ->
                        input.copyTo(out)
                    }
                    file.readBytes()
                }
                if (bytes.isEmpty()) return ApkDownloadResult(null, "empty apk")
                val checksumVerified = UpdateIntegrityVerifier.sha256Matches(bytes, expectedSha256)
                if (!checksumVerified) return ApkDownloadResult(null, "checksum mismatch")
                keepFile = true
                ApkDownloadResult(file, checksumVerified = true)
            } finally {
                connection.disconnect()
                if (!keepFile) file.delete()
            }
        } catch (_: Throwable) {
            ApkDownloadResult(null, "download failed")
        }
    }
}

class ApkInstaller(private val context: Context) {
    fun hasInstallPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun openSettingsForInstallPermission() {
        val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
            data = Uri.parse("package:${context.packageName}")
        }
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun launchInstall(file: File): Boolean {
        if (!file.exists() || file.length() <= 0L || !isExpectedApk(file)) return false
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addCategory(Intent.CATEGORY_DEFAULT)
        }
        return try {
            context.startActivity(intent)
            true
        } catch (_: Throwable) {
            false
        }
    }

    private fun isExpectedApk(file: File): Boolean {
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageArchiveInfo(
                file.path,
                PackageManager.PackageInfoFlags.of(0)
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageArchiveInfo(file.path, 0)
        }
        return packageInfo?.packageName == context.packageName
    }
}

private fun isSupportedHttpsUrl(rawUrl: String): Boolean {
    val normalized = rawUrl.trim()
    if (normalized.isBlank()) return false
    return normalized.startsWith("https://") && normalized.length > "https://".length
}

private fun isValidSha256(value: String?): Boolean =
    value?.matches(Regex("[0-9a-fA-F]{64}")) == true
