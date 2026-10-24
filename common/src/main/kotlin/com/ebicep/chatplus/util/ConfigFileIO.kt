package com.ebicep.chatplus.util

import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardCopyOption

object ConfigFileIO {

    fun backupFile(target: File): File = File(target.parentFile, "${target.name}.bak")

    fun tempFile(target: File): File = File(target.parentFile, "${target.name}.tmp")

    fun readTextWithBackup(target: File): String? {
        readTextIfPresent(target)?.let { return it }
        return readTextIfPresent(backupFile(target))
    }

    fun writeTextAtomically(target: File, text: String) {
        val parent = target.parentFile ?: throw IllegalStateException("Config file has no parent directory: ${target.absolutePath}")
        if (!parent.exists()) {
            parent.mkdirs()
        }
        val temp = tempFile(target)
        try {
            FileOutputStream(temp).use { output ->
                val bytes = text.toByteArray(StandardCharsets.UTF_8)
                output.write(bytes)
                output.fd.sync()
            }
            if (target.exists() && target.length() > 0) {
                target.copyTo(backupFile(target), overwrite = true)
            }
            Files.move(temp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
        } catch (e: Exception) {
            if (temp.exists()) {
                temp.delete()
            }
            throw e
        }
    }

    private fun readTextIfPresent(file: File): String? {
        if (!file.exists() || file.length() == 0L) {
            return null
        }
        return runCatching { file.readText() }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
    }
}
