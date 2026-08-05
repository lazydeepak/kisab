package com.susankhya.kisab.persistence

import android.content.Context
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets

interface FarmBackupFileAdapter {
    fun readText(uri: String, maxBytes: Int): String
    fun writeText(uri: String, content: String, maxBytes: Int)
}

class AndroidStorageAccessFrameworkBackupFileAdapter(
    private val context: Context
) : FarmBackupFileAdapter {
    override fun readText(uri: String, maxBytes: Int): String {
        val parsedUri = Uri.parse(uri)
        val inputStream = context.contentResolver.openInputStream(parsedUri)
            ?: throw FarmBackupException(BackupRejectionReason.UNREADABLE, "Unable to read backup file")
        return inputStream.use { stream ->
            readTextWithLimit(stream, maxBytes)
        }
    }

    override fun writeText(uri: String, content: String, maxBytes: Int) {
        if (content.toByteArray(StandardCharsets.UTF_8).size > maxBytes) {
            throw FarmBackupException(BackupRejectionReason.TOO_LARGE, "Backup file is too large")
        }
        val parsedUri = Uri.parse(uri)
        context.contentResolver.openOutputStream(parsedUri)?.bufferedWriter(StandardCharsets.UTF_8)?.use { writer ->
            writer.write(content)
        } ?: throw FarmBackupException(BackupRejectionReason.UNREADABLE, "Unable to write backup file")
    }
}

internal fun readTextWithLimit(inputStream: InputStream, maxBytes: Int): String {
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(8192)
    var totalBytes = 0
    while (true) {
        val readCount = inputStream.read(buffer)
        if (readCount < 0) break
        totalBytes += readCount
        if (totalBytes > maxBytes) {
            throw FarmBackupException(BackupRejectionReason.TOO_LARGE, "Backup file is too large")
        }
        output.write(buffer, 0, readCount)
    }
    return output.toString(StandardCharsets.UTF_8.name())
}
