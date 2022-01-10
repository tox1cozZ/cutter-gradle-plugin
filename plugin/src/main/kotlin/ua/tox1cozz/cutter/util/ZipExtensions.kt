package ua.tox1cozz.cutter.util

import java.io.File
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

internal object ZipExtensions {

    fun File.eachZip(block: (ZipInputStream, ZipEntry) -> Unit) {
        ZipInputStream(inputStream()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                block(zip, entry)
            }
            zip.closeEntry()
        }
    }

    // Создаёт пустые папки, пофиксить бы
    fun ZipEntry.unzip(zip: ZipInputStream, destinationDir: File, bufferSize: Int = DEFAULT_BUFFER_SIZE) {
        val file = newFile(destinationDir, this)
        if (isDirectory) {
            if (!file.isDirectory && !file.mkdirs()) {
                throw IOException("Failed to create directory $file")
            }
        } else {
            // fix for Windows-created archives
            val parent = file.parentFile
            if (!parent.isDirectory && !parent.mkdirs()) {
                throw IOException("Failed to create directory $parent")
            }

            file.outputStream().use {
                zip.copyTo(it, bufferSize)
            }
        }
    }

    private fun newFile(destinationDir: File, zipEntry: ZipEntry): File {
        val destFile = File(destinationDir, zipEntry.name)
        val destDirPath = destinationDir.canonicalPath
        val destFilePath = destFile.canonicalPath
        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw IOException("Entry is outside of the target dir: ${zipEntry.name}")
        }
        return destFile
    }
}