/*
package ua.tox1cozz.cutterjar.task.cutterjar

import org.apache.tools.zip.UnixStat
import org.apache.tools.zip.Zip64RequiredException
import org.apache.tools.zip.ZipEntry
import org.apache.tools.zip.ZipOutputStream
import org.gradle.api.GradleException
import org.gradle.api.file.FileCopyDetails
import org.gradle.api.file.FileTreeElement
import org.gradle.api.internal.DocumentationRegistry
import org.gradle.api.internal.file.CopyActionProcessingStreamAction
import org.gradle.api.internal.file.archive.ZipCopyAction
import org.gradle.api.internal.file.copy.CopyAction
import org.gradle.api.internal.file.copy.CopyActionProcessingStream
import org.gradle.api.internal.file.copy.FileCopyDetailsInternal
import org.gradle.api.specs.Spec
import org.gradle.api.tasks.WorkResult
import org.gradle.api.tasks.WorkResults
import org.gradle.api.tasks.bundling.Zip
import java.io.File

internal class CutterCopyAction(
    private val zipFile: File,
    private val packages: List<String>,
    private val compression: CutterTask.Compression,
    private val documentationRegistry: DocumentationRegistry,
    private val encoding: String,
    private val preserveFileTimestamps: Boolean
) : CopyAction {

    override fun execute(stream: CopyActionProcessingStream): WorkResult {
        */
/* Не работает из-за того что compressor.createArchiveOutputStream возвращает релокейтнутый ZipOutputStream
        val outputStream = try {
            compressor.createArchiveOutputStream(zipFile)
        } catch (e: Throwable) {
            throw GradleException("Could not create ZIP '$zipFile'.", e)
        }
        *//*


        val outputStream = ZipOutputStream(zipFile).apply {
            setUseZip64(compression.zip64Mode)
            setMethod(compression.entryCompressionMethod)
        }

        try {
            outputStream.use {
                stream.process(StreamAction(it, encoding))
            }
        } catch (e: Zip64RequiredException) {
            val dsl = documentationRegistry.getDslRefForProperty(Zip::class.java, "zip64")
            throw Zip64RequiredException(
                "${e.message}\n\nTo build this archive, please enable the zip64 extension.\nSee: $dsl"
            )
        }

        */
/*try {
            IoActions.withResource(outputStream) {
                stream.process(StreamAction(it, encoding))
            }
        } catch (e: UncheckedIOException) {
            val cause = e.cause
            if (cause is Zip64RequiredException) {
                val dsl = documentationRegistry.getDslRefForProperty(Zip::class.java, "zip64")
                val message = cause.message
                throw Zip64RequiredException(
                    "$message\n\nTo build this archive, please enable the zip64 extension.\nSee: $dsl"
                )
            }
        }*//*


        return WorkResults.didWork(true)
    }

    private inner class StreamAction(
        val outputStream: ZipOutputStream,
        encoding: String?
    ) : CopyActionProcessingStreamAction {

        init {
            encoding?.let {
                outputStream.encoding = it
            }
        }

        override fun processFile(details: FileCopyDetailsInternal) {
            if (details.isDirectory) {
                visitDir(details)
            } else {
                visitFile(details)
            }
        }

        private fun visitFile(fileDetails: FileCopyDetails) {
            try {
                val entry = ZipEntry(fileDetails.relativePath.pathString).apply {
                    time = getArchiveTimeFor(fileDetails)
                    unixMode = UnixStat.FILE_FLAG or fileDetails.mode
                }
                outputStream.putNextEntry(entry)

                val targetPackage = packages.isEmpty() || packages.any { fileDetails.relativePath.pathString.startsWith(it) }
                if (fileDetails.name.endsWith(".class") && targetPackage) {
//                    println("TARGET CLASS: ${fileDetails.relativePath}")
                }

                fileDetails.copyTo(outputStream)
                outputStream.closeEntry()
            } catch (e: Exception) {
                throw GradleException("Could not add $fileDetails to ZIP '$zipFile'.", e)
            }
        }

        private fun visitDir(dirDetails: FileCopyDetails) {
            try {
                // Trailing slash in name indicates that entry is a directory
                val entry = ZipEntry(dirDetails.relativePath.pathString + '/').apply {
                    time = getArchiveTimeFor(dirDetails)
                    unixMode = UnixStat.DIR_FLAG or dirDetails.mode
                }
                outputStream.putNextEntry(entry)
                outputStream.closeEntry()
            } catch (e: Exception) {
                throw GradleException("Could not add $dirDetails to ZIP '$zipFile'.", e)
            }
        }
    }

    private fun getArchiveTimeFor(details: FileCopyDetails): Long {
        return if (preserveFileTimestamps) details.lastModified else ZipCopyAction.CONSTANT_TIME_FOR_ZIP_ENTRIES
    }
}*/
