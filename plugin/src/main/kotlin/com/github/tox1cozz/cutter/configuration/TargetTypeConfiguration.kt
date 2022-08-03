package com.github.tox1cozz.cutter.configuration

import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

internal typealias TargetTypeConfigurationContainer = NamedDomainObjectContainer<TargetTypeConfiguration>

abstract class TargetTypeConfiguration @Inject constructor(
    private val name: String,
    objects: ObjectFactory
) : Named {

    override fun getName() = name

    val annotations: TargetTypeAnnotationConfigurationContainer =
        objects.domainObjectContainer(TargetTypeAnnotationConfiguration::class.java)

    val executors: TargetTypeExecutorConfigurationContainer =
        objects.domainObjectContainer(TargetTypeExecutorConfiguration::class.java)

    fun annotations(config: Action<TargetTypeAnnotationConfigurationContainer>) = config.execute(annotations)
    fun annotations(config: Closure<Unit>): TargetTypeAnnotationConfigurationContainer = annotations.configure(config)
    fun annotations(config: TargetTypeAnnotationConfigurationContainer.() -> Unit) {
        annotations.configure(object : Closure<Unit>(this, this) {
            fun doCall() {
                @Suppress("UNCHECKED_CAST")
                config(delegate as TargetTypeAnnotationConfigurationContainer)
            }
        })
    }

    fun executors(config: Action<TargetTypeExecutorConfigurationContainer>) = config.execute(executors)
    fun executors(config: Closure<Unit>): TargetTypeExecutorConfigurationContainer = executors.configure(config)
    fun executors(config: TargetTypeExecutorConfigurationContainer.() -> Unit) {
        executors.configure(object : Closure<Unit>(this, this) {
            fun doCall() {
                @Suppress("UNCHECKED_CAST")
                config(delegate as TargetTypeExecutorConfigurationContainer)
            }
        })
    }
}

internal typealias TargetTypeAnnotationConfigurationContainer = NamedDomainObjectContainer<TargetTypeAnnotationConfiguration>

abstract class TargetTypeAnnotationConfiguration @Inject constructor(
    private val name: String,
    objects: ObjectFactory
) : Named {

    override fun getName() = name

    val type: Property<String> = objects.property(String::class.java)
    val parameter: Property<String> = objects.property(String::class.java).convention("value")
    val value: Property<String> = objects.property(String::class.java)
}

internal typealias TargetTypeExecutorConfigurationContainer = NamedDomainObjectContainer<TargetTypeExecutorConfiguration>

abstract class TargetTypeExecutorConfiguration @Inject constructor(
    private val name: String,
    objects: ObjectFactory
) : Named {

    override fun getName() = name

    // com/github/tox1cozz/cutter/Cutter:execute(Ljava/lang/Enum;Ljava/lang/Runnable;)V
    val invoke: Property<String> = objects.property(String::class.java)

    // com/github/tox1cozz/cutter/CutterTarget:DEBUG:Lcom/github/tox1cozz/cutter/CutterTarget;
    val value: Property<String> = objects.property(String::class.java)
}