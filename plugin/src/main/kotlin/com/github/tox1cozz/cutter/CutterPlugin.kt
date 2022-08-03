package com.github.tox1cozz.cutter

import com.github.tox1cozz.cutter.configuration.CutterExtension
import com.github.tox1cozz.cutter.task.cutter.CutterTask
import com.github.tox1cozz.cutter.task.cutterjar.CutterJarTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.configurationcache.extensions.capitalized
import org.gradle.jvm.tasks.Jar
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

        }

        extension.targets.all { target ->
            if (target.cutAlways.get()) {
                return@all
            }

            extension.jars.get().forEach { jarTaskName ->
                val jar = project.tasks.named(jarTaskName, Jar::class.java).get()
                val jarName = if (jar.name == "jar") "" else jar.name.capitalized()
                println("Registered jar: $jarTaskName")

                val cutterName = "cutter${target.name.capitalized()}$jarName"
                val cutterTask = project.tasks.register(cutterName, CutterTask::class.java, target, extension)

                val cutterJarName = "${target.name}Jar${jarName}"
                val cutterJarTask = project.tasks.register(cutterJarName, CutterJarTask::class.java, target)

                val cutterDir = File(project.buildDir, NAME)
                project.afterEvaluate {
                    cutterTask.configure {
                        it.archiveFile.set(jar.archiveFile)
                        it.targetClassesDir.set(File(cutterDir, "transformed/${target.name}"))
                        it.originalFilesDir.set(File(cutterDir, "files/${target.name}"))

                        it.inputs.property("packages", extension.packages.hashCode())

                        it.inputs.property("target", target.name)
                        it.inputs.property("target.cutAlways", target.cutAlways)
                        target.types.forEach { type ->
                            val typeKey = "target.type.${type.name}"
                            it.inputs.property(typeKey, type.name)
                            type.annotations.forEach { annotation ->
                                val annotationKey = "$typeKey.annotation.${annotation.name}"
                                it.inputs.property("$annotationKey.type", annotation.type)
                                it.inputs.property("$annotationKey.parameter", annotation.parameter)
                                it.inputs.property("$annotationKey.value", annotation.value)
                            }
                            type.executors.forEach { executor ->
                                val executorKey = "$typeKey.executor.${executor.name}"
                                it.inputs.property("$executorKey.invoke", executor.invoke)
                                it.inputs.property("$executorKey.value", executor.value)
                            }
                        }
                    }

                    cutterJarTask.configure { task ->
                        task.from(cutterTask.get().targetClassesDir)
                        task.from(cutterTask.get().originalFilesDir)
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
}