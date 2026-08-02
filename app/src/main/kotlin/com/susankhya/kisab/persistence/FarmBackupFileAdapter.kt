package com.susankhya.kisab.persistence

import android.content.Context
import android.net.Uri
import java.nio.charset.StandardCharsets

interface FarmBackupFileAdapter {
    fun readText(uri: String): String
    fun writeText(uri: String, content: String)
}

class AndroidStorageAccessFrameworkBackupFileAdapter(
    private val context: Context
) : FarmBackupFileAdapter {
    override fun readText(uri: String): String {
        val parsedUri = Uri.parse(uri)
        return context.contentResolver.openInputStream(parsedUri)?.bufferedReader(StandardCharsets.UTF_8)?.use { reader ->
            reader.readText()
        } ?: throw IllegalStateException("Unable to read backup file")
    }

    override fun writeText(uri: String, content: String) {
        val parsedUri = Uri.parse(uri)
        context.contentResolver.openOutputStream(parsedUri)?.bufferedWriter(StandardCharsets.UTF_8)?.use { writer ->
            writer.write(content)
        } ?: throw IllegalStateException("Unable to write backup file")
    }
}
