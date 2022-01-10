package ua.tox1cozz.cutter

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.util.GUtil.toCamelCase
import org.gradle.util.GUtil.toLowerCamelCase
import ua.tox1cozz.cutter.Constants.cutterDir
import ua.tox1cozz.cutter.configuration.CutterExtension.Companion.createCutterExtension
import ua.tox1cozz.cutter.configuration.CutterExtension.Companion.cutterExtension
import ua.tox1cozz.cutter.task.cutter.CutterTask
import ua.tox1cozz.cutter.task.cutterjar.CutterJarTask
import java.io.File

abstract class CutterPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.createCutterExtension().targets.all { target ->
            if (target.cutAlways.get()) {
                return@all
            }

            val cutterTask = project.tasks.register(
                "cutter${toCamelCase(target.name)}", // For example: cutterClient, cutterServer
                CutterTask::class.java,
                target
            )

            val cutterJarTask = project.tasks.register(
                "${toLowerCamelCase(target.name)}Jar", // For example: clientJar, serverJar
                CutterJarTask::class.java,
                target
            )

            project.afterEvaluate {
                cutterTask.configure {
                    it.archiveFile.set(it.cutterExtension.parentJarTask.get().archiveFile)
                    it.targetClassesDir.set(File(project.cutterDir, "transformed/${target.name}"))
                    it.originalFilesDir.set(File(project.cutterDir, "files"))

                    it.inputs.property("removeFields", it.cutterExtension.removeFields)
                    it.inputs.property("packages", it.cutterExtension.packages.hashCode())

                    it.inputs.property("target", target.name)
                    it.inputs.property("target.cutAlways", target.cutAlways)
                    target.types.forEach { type ->
                        val typeInput = "target.type.${type.name}"
                        it.inputs.property(typeInput, type.name)
                        it.inputs.property("$typeInput.annotation", type.annotation)
                        it.inputs.property("$typeInput.parameterName", type.parameterName)
                        it.inputs.property("$typeInput.value", type.value)
                    }
                }

                cutterJarTask.configure { task ->
                    val parentJarTask = task.cutterExtension.parentJarTask.get()
                    val archiveFile = parentJarTask.archiveFile

                    task.from(cutterTask.get().targetClassesDir)
                    task.from(cutterTask.get().originalFilesDir)
                    task.manifest.from(parentJarTask.manifest.effectiveManifest)

                    task.archiveClassifier.set(target.name)
                    archiveFile.get().asFile.let {
                        task.archiveFileName.set(
                            "${it.nameWithoutExtension}-${target.name}.${it.extension}"
                        )
                    }
                }
            }
        }
    }
}


/*

cutter {
    validation = true
    package "ua.tox1cozz"

    targets {
        client {
            forge {
                annotation "cpw/mods/fml/relauncher/SideOnly"
                value "cpw/mods/fml/relauncher/Side.CLIENT"
            }
            cutter {
                annotation "ua/tox1cozz/cutter/CutterTargetOnly"
                value "ua/tox1cozz/cutter/CutterTarget.CLIENT"
            }
            cutAlways false
        }
        server {
            forge {
                annotation "cpw/mods/fml/relauncher/SideOnly"
                value "cpw/mods/fml/relauncher/Side.SERVER"
            }
            cutter {
                annotation "ua/tox1cozz/cutter/CutterTargetOnly"
                value "ua/tox1cozz/cutter/CutterTarget.SERVER"
            }
            cutAlways false
        }
        debug {
            cutter {
                annotation "ua/tox1cozz/cutter/CutterTargetOnly"
                value "ua/tox1cozz/cutter/CutterTarget.DEBUG"
            }
            cutAlways true
        }
    }
}



cutter {
    validation = true
    package "ua.tox1cozz"

    targets {
        forge {
            annotation "cpw/mods/fml/relauncher/SideOnly"
            value = "cpw/mods/fml/relauncher/Side"
            types {
                CLIENT {
                    cutAlways false
                }
                SERVER {
                    cutAlways false
                }
            }
        }
        cutter {
            annotation "ua/tox1cozz/cutter/CutterTargetOnly"
            value = "ua/tox1cozz/cutter/CutterTarget"
            types {
                CLIENT {
                    cutAlways false
                }
                SERVER {
                    cutAlways false
                }
                DEBUG {
                    cutAlways true
                }
            }
        }
    }
}

 */