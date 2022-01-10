package ua.tox1cozz.cutter.configuration

import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

internal typealias TargetTypeConfigurationContainer = NamedDomainObjectContainer<TargetTypeConfiguration>

open class TargetTypeConfiguration @Inject constructor(
    private val name: String,
    objects: ObjectFactory
) : Named {

    override fun getName() = name

    val annotation: Property<String> = objects.property(String::class.java)
    val parameterName: Property<String> = objects.property(String::class.java).convention("value")
    val value: Property<String> = objects.property(String::class.java)
}