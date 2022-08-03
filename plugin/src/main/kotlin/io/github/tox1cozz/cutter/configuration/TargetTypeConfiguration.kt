package io.github.tox1cozz.cutter.configuration

import org.gradle.api.Action
import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import java.util.*
import javax.inject.Inject

internal typealias TargetTypeConfigurationContainer = NamedDomainObjectContainer<TargetTypeConfiguration>

abstract class TargetTypeConfiguration @Inject constructor(
    private val name: String,
    private val objects: ObjectFactory
) : Named {

    override fun getName() = name

    val annotations: ListProperty<TargetTypeAnnotationConfiguration> =
        objects.listProperty(TargetTypeAnnotationConfiguration::class.java).empty()

    val executors: ListProperty<TargetTypeExecutorConfiguration> =
        objects.listProperty(TargetTypeExecutorConfiguration::class.java).empty()

    fun annotation(builder: TargetTypeAnnotationConfiguration.() -> Unit) =
        annotations.add(TargetTypeAnnotationConfiguration(objects).apply(builder))

    fun annotation(action: Action<TargetTypeAnnotationConfiguration>) =
        annotations.add(TargetTypeAnnotationConfiguration(objects).apply { action.execute(this) })

    fun executor(builder: TargetTypeExecutorConfiguration.() -> Unit) =
        executors.add(TargetTypeExecutorConfiguration(objects).apply(builder))

    fun executor(action: Action<TargetTypeExecutorConfiguration>) =
        executors.add(TargetTypeExecutorConfiguration(objects).apply { action.execute(this) })
}

class TargetTypeAnnotationConfiguration(objects: ObjectFactory) {

    val type: Property<String> = objects.property(String::class.java)
    val parameter: Property<String> = objects.property(String::class.java).convention("value")
    val value: Property<String> = objects.property(String::class.java)

    override fun equals(other: Any?): Boolean {
        if (other !is TargetTypeAnnotationConfiguration) return false
        if (type.get() != other.type.get()) return false
        if (parameter.get() != other.parameter.get()) return false
        if (value.get() != other.value.get()) return false
        return true
    }

    override fun hashCode() = Objects.hash(type.get(), parameter.get(), value.get())

    override fun toString() = "TargetTypeAnnotationConfiguration(type=${type.get()}, parameter=${parameter.get()}, value=${value.get()})"
}

class TargetTypeExecutorConfiguration(objects: ObjectFactory) {

    val invoke: Property<String> = objects.property(String::class.java)
    val value: Property<String> = objects.property(String::class.java)

    override fun equals(other: Any?): Boolean {
        if (other !is TargetTypeExecutorConfiguration) return false
        if (invoke.get() != other.invoke.get()) return false
        if (value.get() != other.value.get()) return false
        return true
    }

    override fun hashCode() = Objects.hash(invoke.get(), value.get())

    override fun toString() = "TargetTypeExecutorConfiguration(invoke=${invoke.get()}, value=${value.get()})"
}