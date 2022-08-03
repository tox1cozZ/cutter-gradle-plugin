package io.github.tox1cozz.cutter.task.cutterjar

import io.github.tox1cozz.cutter.CutterPlugin
import io.github.tox1cozz.cutter.configuration.TargetConfiguration
import io.github.tox1cozz.cutter.task.TargetTask
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.util.PatternSet
import org.gradle.jvm.tasks.Jar
import javax.inject.Inject

@CacheableTask
abstract class CutterJarTask @Inject constructor(
    @get:Internal final override val target: TargetConfiguration
) : Jar(), TargetTask {

    init {
        description = "Assembles a jar archive with ${target.name} target build"
        group = CutterPlugin.GROUP
    }

    @TaskAction
    fun execute() {
    }

    @JvmOverloads
    fun excludeMinecraftAssets(
        vararg keep: String = arrayOf(
            "**/lang/*.lang",
            "**/lang/*.json"
        )
    ) = excludeResources("assets", *keep)

    fun excludeResources(resourcesDir: String, vararg keep: String) {
        val keepPattern = PatternSet().apply {
            include(*keep)
            isCaseSensitive = false
        }.asSpec
        filesMatching("$resourcesDir/**") {
            if (!keepPattern.isSatisfiedBy(it)) {
                it.exclude()
            }
        }
    }
}