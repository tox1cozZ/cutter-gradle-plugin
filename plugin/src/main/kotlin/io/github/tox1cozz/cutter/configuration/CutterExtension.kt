package io.github.tox1cozz.cutter.configuration

import groovy.lang.Closure
import io.github.tox1cozz.cutter.Cutter
import io.github.tox1cozz.cutter.CutterTarget
import io.github.tox1cozz.cutter.CutterTargetOnly
import io.github.tox1cozz.cutter.task.TargetTask
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.util.PatternSet
import org.gradle.jvm.tasks.Jar
import org.objectweb.asm.Type
import javax.inject.Inject

abstract class CutterExtension @Inject constructor(private val project: Project) {

    val validation: Property<Boolean> = project.objects.property(Boolean::class.java).convention(true)
    val verbose: Property<Boolean> = project.objects.property(Boolean::class.java).convention(true)
    val jars: ListProperty<Jar> = project.objects.listProperty(Jar::class.java).empty()

    val packages = PatternSet()

    val targets: TargetConfigurationContainer = project.objects.domainObjectContainer(TargetConfiguration::class.java)
    fun targets(config: Action<TargetConfigurationContainer>) = config.execute(targets)
    fun targets(config: Closure<Unit>): TargetConfigurationContainer = targets.configure(config)
    fun targets(config: TargetConfigurationContainer.() -> Unit) {
        targets.configure(object : Closure<Unit>(this, this) {
            fun doCall() {
                @Suppress("UNCHECKED_CAST")
                config(delegate as TargetConfigurationContainer)
            }
        })
    }

    init {
        project.tasks.findByName("jar")?.also {
            if (it is Jar) {
                jars.add(it)
            }
        }
        initDefaultTargets()
    }

    fun processJar(jarTask: Jar) = processJar(jarTask.name)

    fun processJar(jarTaskName: String) {
        jars.add(
            project.provider {
                project.tasks.getByName(jarTaskName).let {
                    check(it is Jar) { "Task with name '$jarTaskName' is not a Jar type" }
                    it
                }
            }
        )
    }

    private fun initDefaultTargets() {
        val typeName = "cutter"

        fun TargetTypeConfiguration.registerType(targetName: String) {
            val annotationType = Type.getInternalName(CutterTargetOnly::class.java)
            val targetType = Type.getInternalName(CutterTarget::class.java)
            val cutterType = Type.getInternalName(Cutter::class.java)

            annotation {
                type.set(annotationType)
                value.set("$targetType.$targetName")
            }
            executor {
                invoke.set("$cutterType:execute:(Ljava/lang/Enum;Ljava/lang/Runnable;)V")
                value.set("$targetType:$targetName:L$targetType;")
            }
        }

        targets.register(TargetConfiguration.CLIENT_NAME) { client ->
            client.cutAlways.set(false)
            client.types.register(typeName) {
                it.registerType(CutterTarget.CLIENT.name)
            }
        }
        targets.register(TargetConfiguration.SERVER_NAME) { server ->
            server.cutAlways.set(false)
            server.types.register(typeName) {
                it.registerType(CutterTarget.SERVER.name)
            }
        }
        targets.register(TargetConfiguration.DEBUG_NAME) { debug ->
            debug.cutAlways.set(true)
            debug.types.register(typeName) {
                it.registerType(CutterTarget.DEBUG.name)
            }
        }
    }

    fun minecraftForgeSideOnlyLegacy() = clientServerTargetAnnotation(
        name = "minecraftForgeSideOnlyLegacy",
        annotationType = "cpw/mods/fml/relauncher/SideOnly",
        valueType = "cpw/mods/fml/relauncher/Side"
    )

    fun minecraftForgeSideOnly() = clientServerTargetAnnotation(
        name = "minecraftForgeSideOnly",
        annotationType = "net/minecraftforge/fml/relauncher/SideOnly",
        valueType = "net/minecraftforge/fml/relauncher/Side"
    )

    fun minecraftForgeOnlyIn() {
        val typeName = "minecraftForgeOnlyIn"
        val targetType = "net/minecraftforge/api/distmarker/Dist"

        clientServerTargetAnnotation(
            name = typeName,
            annotationType = "net/minecraftforge/api/distmarker/OnlyIn",
            valueType = targetType,
            serverValue = "DEDICATED_SERVER"
        )

        fun TargetTypeConfiguration.registerKotlinForForgeExecutor(targetName: String) {
            executor {
                invoke.set("thedarkcolour/kotlinforforge/forge/ForgeKt:runWhenOn:(Lnet/minecraftforge/api/distmarker/Dist;Lkotlin/jvm/functions/Function0;)V")
                value.set("$targetType:$targetName:L$targetType;")
            }
        }

        fun TargetTypeConfiguration.registerForgeDistExecutor(executorMethod: String, targetName: String) {
            executor {
                invoke.set("net/minecraftforge/fml/DistExecutor:$executorMethod:(Lnet/minecraftforge/api/distmarker/Dist;Ljava/util/function/Supplier;)V")
                value.set("$targetType:$targetName:L$targetType;")
            }
        }

        targets.named(TargetConfiguration.CLIENT_NAME) { client ->
            client.types.named(typeName) {
                it.registerKotlinForForgeExecutor("CLIENT")
                it.registerForgeDistExecutor("runWhenOn", "CLIENT")
                it.registerForgeDistExecutor("unsafeRunWhenOn", "CLIENT")
                it.registerForgeDistExecutor("safeRunWhenOn", "CLIENT")
            }
        }
        targets.named(TargetConfiguration.SERVER_NAME) { server ->
            server.types.named(typeName) {
                it.registerKotlinForForgeExecutor("DEDICATED_SERVER")
                it.registerForgeDistExecutor("runWhenOn", "DEDICATED_SERVER")
                it.registerForgeDistExecutor("unsafeRunWhenOn", "DEDICATED_SERVER")
                it.registerForgeDistExecutor("safeRunWhenOn", "DEDICATED_SERVER")
            }
        }
    }

    fun minecraftFabricEnvironment() = clientServerTargetAnnotation(
        name = "minecraftFabricEnvironment",
        annotationType = "net/fabricmc/api/Environment",
        valueType = "net/fabricmc/api/EnvType"
    )

    @JvmOverloads
    fun clientServerTargetAnnotation(
        name: String,
        annotationType: String,
        valueType: String,
        parameterName: String? = null,
        clientValue: String = "CLIENT",
        serverValue: String = "SERVER",
    ) {
        fun TargetTypeConfiguration.registerAnnotation(annotationValue: String) {
            annotation {
                type.set(annotationType)
                parameterName?.also { parameter.set(it) }
                value.set("$valueType.$annotationValue")
            }
        }

        targets.named(TargetConfiguration.CLIENT_NAME) { client ->
            client.types.register(name) {
                it.registerAnnotation(clientValue)
            }
        }
        targets.named(TargetConfiguration.SERVER_NAME) { server ->
            server.types.register(name) {
                it.registerAnnotation(serverValue)
            }
        }
    }

    fun <T> configureClientTasks(
        taskType: Class<T>,
        action: Action<T>
    ) where T : Task, T : TargetTask = configureTasks(TargetConfiguration.CLIENT_NAME, taskType, action)

    fun <T> configureClientTasks(
        taskType: Class<T>,
        block: T.() -> Unit
    ) where T : Task, T : TargetTask = configureTasks(TargetConfiguration.CLIENT_NAME, taskType, block)

    fun <T> configureServerTasks(
        taskType: Class<T>,
        action: Action<T>
    ) where T : Task, T : TargetTask = configureTasks(TargetConfiguration.SERVER_NAME, taskType, action)

    fun <T> configureServerTasks(
        taskType: Class<T>,
        block: T.() -> Unit
    ) where T : Task, T : TargetTask = configureTasks(TargetConfiguration.SERVER_NAME, taskType, block)

    @JvmOverloads
    fun <T> configureTasks(
        targetName: String? = null,
        taskType: Class<T>,
        block: T.() -> Unit
    ) where T : Task, T : TargetTask = configureTasks(targetName, taskType) { action -> block(action) }

    @JvmOverloads
    fun <T> configureTasks(
        targetName: String? = null,
        taskType: Class<T>,
        action: Action<T>
    ) where T : Task, T : TargetTask {
        if (targetName != null) {
            val target = targets.getByName(targetName)
            project.tasks.withType(taskType) {
                if (it.target == target) {
                    action.execute(it)
                }
            }
        } else {
            project.tasks.withType(taskType, action)
        }
    }
}