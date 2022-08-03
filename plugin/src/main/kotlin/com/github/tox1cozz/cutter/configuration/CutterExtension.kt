package com.github.tox1cozz.cutter.configuration

import com.github.tox1cozz.cutter.CutterTarget
import com.github.tox1cozz.cutter.CutterTargetOnly
import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.util.PatternSet
import org.objectweb.asm.Type
import javax.inject.Inject

abstract class CutterExtension @Inject constructor(private val project: Project) {

    val validation: Property<Boolean> = project.objects.property(Boolean::class.java).convention(true)
    val verbose: Property<Boolean> = project.objects.property(Boolean::class.java).convention(true)
//    val jars: ListProperty<Jar> = project.objects.listProperty(Jar::class.java)
//        .value(project.provider { listOf(project.tasks.named("jar", Jar::class.java).get()) })

    val jars: SetProperty<String> = project.objects.setProperty(String::class.java).empty()

    val packages = PatternSet()

//    fun processJar(jarTask: String) {
//        val task = project.tasks.named(jarTask).get()
//        check(task is Jar) { "Task with name '$jarTask' is not a Jar type" }
//        jars.add(task)
//    }

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
        initDefaultTargets()
    }

    private fun initDefaultTargets() {
        val typeName = "cutter"
        val annotationType = Type.getInternalName(CutterTargetOnly::class.java)
        val targetType = Type.getInternalName(CutterTarget::class.java)
        val invoke = "$targetType:execute(Ljava/lang/Enum;Ljava/lang/Runnable;)V"

        fun TargetTypeConfiguration.registerType(targetName: String) {
            annotations.register(typeName) { annotation ->
                annotation.type.set(annotationType)
                annotation.value.set("$targetType.$targetName")
            }
            executors.register(typeName) { executor ->
                executor.invoke.set(invoke)
                executor.value.set("$targetType:$targetName:L$targetType;")
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

    fun minecraftForgeSideOnlyLegacy() = clientServerTarget(
        name = "minecraftForgeSideOnlyLegacy",
        annotationType = "cpw/mods/fml/relauncher/SideOnly",
        valueType = "cpw/mods/fml/relauncher/Side"
    )

    fun minecraftForgeSideOnly() = clientServerTarget(
        name = "minecraftForgeSideOnly",
        annotationType = "net/minecraftforge/fml/relauncher/SideOnly",
        valueType = "net/minecraftforge/fml/relauncher/Side"
    )

    fun minecraftForgeOnlyIn() = clientServerTarget(
        name = "minecraftForgeOnlyIn",
        annotationType = "net/minecraftforge/api/distmarker/OnlyIn",
        valueType = "net/minecraftforge/api/distmarker/Dist",
        serverValue = "DEDICATED_SERVER"
    )

    fun minecraftFabricEnvironment() = clientServerTarget(
        name = "minecraftFabricEnvironment",
        annotationType = "net/fabricmc/api/Environment",
        valueType = "net/fabricmc/api/EnvType"
    )

    @JvmOverloads
    fun clientServerTarget(
        name: String,
        annotationType: String,
        valueType: String,
        parameterName: String? = null,
        clientValue: String = "CLIENT",
        serverValue: String = "SERVER",
    ) {
        fun TargetTypeConfiguration.registerAnnotation(value: String) {
            annotations.register(name) { annotation ->
                annotation.type.set(annotationType)
                parameterName?.also { param -> annotation.parameter.set(param) }
                annotation.value.set("$valueType.$value")
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
}