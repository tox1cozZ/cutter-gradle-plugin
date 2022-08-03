package io.github.tox1cozz.cutter.configuration

import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

internal typealias TargetConfigurationContainer = NamedDomainObjectContainer<TargetConfiguration>

abstract class TargetConfiguration @Inject constructor(
    private val name: String,
    objects: ObjectFactory
) : Named {

    companion object {

        const val CLIENT_NAME = "client"
        const val SERVER_NAME = "server"
        const val DEBUG_NAME = "debug"
    }

    override fun getName() = name

    val cutAlways: Property<Boolean> = objects.property(Boolean::class.java).convention(false)

    val types: TargetTypeConfigurationContainer = objects.domainObjectContainer(TargetTypeConfiguration::class.java)

    fun types(config: Action<TargetTypeConfigurationContainer>) = config.execute(types)
    fun types(config: Closure<Unit>): TargetTypeConfigurationContainer = types.configure(config)
    fun types(config: TargetTypeConfigurationContainer.() -> Unit) {
        types.configure(object : Closure<Unit>(this, this) {
            fun doCall() {
                @Suppress("UNCHECKED_CAST")
                config(delegate as TargetTypeConfigurationContainer)
            }
        })
    }
}