package ua.tox1cozz.cutter.task.cutterjar

import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.TaskAction
import org.gradle.jvm.tasks.Jar
import ua.tox1cozz.cutter.Constants
import ua.tox1cozz.cutter.configuration.TargetConfiguration
import javax.inject.Inject

@CacheableTask
abstract class CutterJarTask @Inject constructor(
    private val target: TargetConfiguration
) : Jar() {

    init {
        description = "Assembles a jar archive with ${target.name} target build"
        group = Constants.GROUP
    }

    @TaskAction
    fun execute() {

    }
}