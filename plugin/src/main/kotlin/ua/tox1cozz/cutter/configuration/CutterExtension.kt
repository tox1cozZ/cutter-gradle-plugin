package ua.tox1cozz.cutter.configuration

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.provider.Property
import org.gradle.api.tasks.util.PatternSet
import org.gradle.jvm.tasks.Jar
import org.objectweb.asm.Type
import ua.tox1cozz.cutter.Constants
import ua.tox1cozz.cutter.CutterTarget
import ua.tox1cozz.cutter.CutterTargetOnly
import javax.inject.Inject

// ГОТОВО: Сделать инпуты для параметров экстеншена в тасках
// НЕ БУДУ: Сделать пропс языка. Не делать некоторые фичи (например, в Java нет локальный функций как в Kotlin, зачем их искать?), чтобы ускорить вырезалку
open class CutterExtension @Inject constructor(
    private val project: Project
) {

    internal companion object {

        fun Project.createCutterExtension(): CutterExtension {
            return extensions.create(Constants.EXTENSION, CutterExtension::class.java, this)
        }

        val Project.cutterExtension: CutterExtension get() = project.extensions.getByType(CutterExtension::class.java)
        val Task.cutterExtension get() = project.cutterExtension
    }

    val validation: Property<Boolean> = project.objects.property(Boolean::class.java)
        .convention(true)

    val removeFields: Property<Boolean> = project.objects.property(Boolean::class.java)
        .convention(false)

    val verbose: Property<Boolean> = project.objects.property(Boolean::class.java)
        .convention(false)

    val parentJarTask: Property<Jar> = project.objects.property(Jar::class.java)
        .convention(project.tasks.named("jar", Jar::class.java))

    fun parentJarTask(jarTask: String) {
        val task = project.tasks.named(jarTask).get()
        check(task is Jar) { "Task with name '$jarTask' is not a Jar type" }
        parentJarTask.set(task)
    }

    val packages = PatternSet()

    val targets: TargetConfigurationContainer = project.objects.domainObjectContainer(TargetConfiguration::class.java)
    fun targets(config: Action<TargetConfigurationContainer>) = config.execute(targets)

    init {
        val typeName = "cutter"
        val annotationType = Type.getInternalName(CutterTargetOnly::class.java)
        val valueType = Type.getInternalName(CutterTarget::class.java)
        targets.register(TargetConfiguration.CLIENT_NAME) { client ->
            client.cutAlways.set(false)
            client.types.register(typeName) { cutter ->
                cutter.annotation.set(annotationType)
                cutter.value.set("$valueType.${CutterTarget.CLIENT.name}")
            }
        }
        targets.register(TargetConfiguration.SERVER_NAME) { server ->
            server.cutAlways.set(false)
            server.types.register(typeName) { cutter ->
                cutter.annotation.set(annotationType)
                cutter.value.set("$valueType.${CutterTarget.SERVER.name}")
            }
        }
        targets.register(TargetConfiguration.DEBUG_NAME) { debug ->
            debug.cutAlways.set(true)
            debug.types.register(typeName) { cutter ->
                cutter.annotation.set(annotationType)
                cutter.value.set("$valueType.${CutterTarget.DEBUG.name}")
            }
        }
    }

    fun minecraftForgeSideOnlyLegacy() {
        clientServerTarget(
            "minecraftForgeSideOnlyLegacy",
            "cpw/mods/fml/relauncher/SideOnly",
            "cpw/mods/fml/relauncher/Side"
        )
    }

    // TODO: Заполнить 1.12 аннотации
    fun minecraftForgeSideOnly() {
        clientServerTarget(
            "minecraftForgeSideOnly",
            "cpw/mods/fml/relauncher/SideOnly",
            "cpw/mods/fml/relauncher/Side"
        )
    }

    fun minecraftForgeOnlyIn() {
        clientServerTarget(
            "minecraftForgeOnlyIn",
            "net/minecraftforge/api/distmarker/OnlyIn",
            "net/minecraftforge/api/distmarker/Dist",
            serverValue = "DEDICATED_SERVER"
        )
    }

    fun minecraftFabricEnvironment() {
        clientServerTarget(
            "minecraftFabricEnvironment",
            "net/fabricmc/api/Environment",
            "net/fabricmc/api/EnvType"
        )
    }

    @JvmOverloads
    fun clientServerTarget(
        name: String,
        annotationType: String,
        valueType: String,
        parameterName: String? = null,
        clientValue: String = "CLIENT",
        serverValue: String = "SERVER",
    ) {
        targets.named(TargetConfiguration.CLIENT_NAME) { client ->
            client.types.register(name) { cutter ->
                cutter.annotation.set(annotationType)
                parameterName?.let { cutter.parameterName.set(it) }
                cutter.value.set("$valueType.$clientValue")
            }
        }
        targets.named(TargetConfiguration.SERVER_NAME) { server ->
            server.types.register(name) { cutter ->
                cutter.annotation.set(annotationType)
                parameterName?.let { cutter.parameterName.set(it) }
                cutter.value.set("$valueType.$serverValue")
            }
        }
    }
}