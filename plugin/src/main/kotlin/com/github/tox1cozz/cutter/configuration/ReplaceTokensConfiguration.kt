package com.github.tox1cozz.cutter.configuration

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.Input
import javax.inject.Inject

abstract class ReplaceTokensConfiguration @Inject constructor(objects: ObjectFactory) {

    @get:Input
    val tokens: MapProperty<String, String> = objects.mapProperty(String::class.java, String::class.java).empty()

    fun token(key: String, value: Any) = tokens.put(key, value.toString())
    fun tokens(map: MutableMap<String, Any>) = map.forEach { (key, value) -> token(key, value) }
}