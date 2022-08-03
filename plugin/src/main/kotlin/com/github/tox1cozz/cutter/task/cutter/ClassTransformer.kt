package com.github.tox1cozz.cutter.task.cutter

import com.github.tox1cozz.cutter.configuration.CutterExtension
import com.github.tox1cozz.cutter.configuration.ReplaceTokensConfiguration
import com.github.tox1cozz.cutter.configuration.TargetConfiguration

internal class ClassTransformer(
    target: TargetConfiguration,
    private val extension: CutterExtension,
    private val replaceTokens: ReplaceTokensConfiguration,
    private val inputClasses: Map<String, ClassFile>
) {

    fun process(): List<ClassFile> {
        return inputClasses.values.toList()
    }

    fun validate(): List<String> {
        return emptyList()
    }
}