package com.github.tox1cozz.cutter

import com.github.tox1cozz.cutter.configuration.CutterExtension
import com.github.tox1cozz.cutter.task.cutter.CutterTask
import com.github.tox1cozz.cutter.task.cutterjar.CutterJarTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.configurationcache.extensions.capitalized
import java.io.File

abstract class CutterPlugin : Plugin<Project> {

    companion object {

        const val NAME = "cutter"
        const val GROUP = "cutter"
        const val EXTENSION = "cutter"
    }

    override fun apply(project: Project) {
        val extension = project.extensions.create(EXTENSION, CutterExtension::class.java, project)
        project.afterEvaluate {
            setup(it, extension)
        }
    }

    private fun setup(project: Project, extension: CutterExtension) {
        val jars = extension.jars.get()
        if (jars.isEmpty()) {
            project.logger.warn("No Jar task found, so Cutter is inactive. Specify jar with processJar method in 'cutter' configuration.")
            return
        }

        val buildTask = project.tasks.getByName("build")

        extension.targets.all { target ->
            if (target.types.isEmpty()) {
                project.logger.warn("Types in target '${target.name}' not specified.")
                return@all
            } else {
                target.types.filter { it.annotations.get().isEmpty() && it.executors.get().isEmpty() }.forEach {
                    project.logger.warn("Type '${it.name}' in target '${target.name}' is empty, specify annotations or/and executors.")
                }
            }

            if (target.cutAlways.get()) {
                return@all
            }

            jars.forEach { jar ->
                val jarTaskName = jar.name
                val taskPostfix = if (jar.name == "jar") "" else jar.name.capitalized()
                val cutterDir = File(project.buildDir, NAME)

                val cutterName = "cutter${target.name.capitalized()}$taskPostfix"
                val cutterTask = project.tasks.create(cutterName, CutterTask::class.java, target, extension)

                val cutterJarName = "${target.name}Jar${taskPostfix}"
                val cutterJarTask = project.tasks.create(cutterJarName, CutterJarTask::class.java, target)

                cutterTask.also {
                    it.archiveFile.set(jar.archiveFile)
                    it.targetClassesDir.set(File(cutterDir, "$jarTaskName/transformed/${target.name}"))
                    it.originalFilesDir.set(File(cutterDir, "$jarTaskName/files/${target.name}"))

                    it.inputs.property("packages", extension.packages.hashCode())

                    it.inputs.property("target", target.name)
                    it.inputs.property("target.cutAlways", target.cutAlways)
                    target.types.forEach { type ->
                        val typeKey = "target.type.${type.name}"
                        it.inputs.property(typeKey, type.name)
                        it.inputs.property("$typeKey.annotations", type.annotations.get().hashCode())
                        it.inputs.property("$typeKey.executors", type.executors.get().hashCode())
                    }
                }

                cutterJarTask.also { task ->
                    task.dependsOn(cutterTask)
                    buildTask.dependsOn(task)

                    task.from(cutterTask.targetClassesDir)
                    task.from(cutterTask.originalFilesDir)
                    task.manifest.from(jar.manifest.effectiveManifest)

                    task.archiveClassifier.set(target.name)
                    jar.archiveFile.get().asFile.also {
                        task.archiveFileName.set("${it.nameWithoutExtension}-${target.name}.${it.extension}")
                    }
                }
            }
        }
    }
}