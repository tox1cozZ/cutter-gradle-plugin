package ua.tox1cozz.cutter

import org.gradle.api.Project
import org.gradle.api.Task
import org.objectweb.asm.Type
import ua.tox1cozz.cutter.configuration.CutterExtension
import java.io.File

internal object Constants {

    const val NAME = "cutter"
    const val GROUP = NAME
    const val EXTENSION = NAME

    val Project.cutterDir get() = File(buildDir, NAME)
}