package ua.tox1cozz.cutter.util

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteExisting
import kotlin.io.path.exists

@OptIn(ExperimentalPathApi::class)
internal object PathExtensions {

    fun Path.deleteDirectoryRecursively() {
        Files.walk(this).sorted(Comparator.reverseOrder()).forEach { it.deleteExisting() }
    }

    fun Path.cleanDirectory() {
        if (exists()) deleteDirectoryRecursively()
        createDirectories()
    }
}