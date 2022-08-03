package io.github.tox1cozz.cutter.task.cutter

import io.github.tox1cozz.cutter.configuration.CutterExtension
import io.github.tox1cozz.cutter.configuration.ReplaceTokensConfiguration
import io.github.tox1cozz.cutter.configuration.TargetConfiguration
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.LdcInsnNode

internal class ClassTransformer(
    private val target: TargetConfiguration,
    private val extension: CutterExtension,
    private val replaceTokens: ReplaceTokensConfiguration,
    private val inputClasses: Map<String, ClassFile>
) {

    private val annotations: List<AnnotationType> = target.types.flatMap { type ->
        type.annotations.get().map { annotation ->
            val target = annotation.value.get().split('.', limit = 2)
            AnnotationType(
                desc = "L${annotation.type.get()};",
                parameter = annotation.parameter.get(),
                targetType = target[0],
                targetValue = target[1]
            )
        }
    }

    private val executors: List<ExecutorType> = target.types.flatMap { type ->
        type.executors.get().map { executor ->
            val invoke = executor.invoke.get().split(':', limit = 3)
            val target = executor.value.get().split(':', limit = 3)
            ExecutorType(
                invokeOwner = invoke[0],
                invokeName = invoke[1],
                invokeDesc = invoke[2],
                targetOwner = target[0],
                targetName = target[1],
                targetDesc = target[2],
            )
        }
    }

    fun process(): List<ClassFile> {
        val outputClasses = inputClasses.values.toList()
        outputClasses.forEach(::replaceTokens)
        return outputClasses
    }

    fun validate(): List<String> {
        return emptyList()
    }

    private fun replaceTokens(classFile: ClassFile) {
        val tokens = replaceTokens.tokens.get()
        if (tokens.isEmpty()) return

        fun replaceInAnnotations(annotations: MutableList<AnnotationNode>?) {
            if (annotations.isNullOrEmpty()) return
            for (annotation in annotations) {
                if (annotation.values.isNullOrEmpty()) continue
                for (i in 0 until annotation.values.size step 2) {
                    val value = annotation.values[i + 1]
                    if (value is String && value in tokens) {
                        annotation.values[i + 1] = tokens[value]
                        classFile.changed = true
                    } else if (value is List<*> && value.isNotEmpty() && value.first() is String) {
                        annotation.values[i + 1] = value.map {
                            if (it in tokens) {
                                classFile.changed = true
                                return@map tokens[it]
                            }
                            it
                        }
                    }
                }
            }
        }

        val classNode = classFile.node
        replaceInAnnotations(classNode.visibleAnnotations)
        replaceInAnnotations(classNode.invisibleAnnotations)

        for (field in classNode.fields) {
            if (field.value is String && field.value in tokens) {
                field.value = tokens[field.value]
                classFile.changed = true
            }
            replaceInAnnotations(field.visibleAnnotations)
            replaceInAnnotations(field.invisibleAnnotations)
        }

        for (method in classNode.methods) {
            for (insn in method.instructions) {
                if (insn is LdcInsnNode && insn.cst is String) {
                    tokens.forEach { (key, value) ->
                        val string = insn.cst as String
                        if (string.contains(key)) {
                            insn.cst = string.replace(key, value)
                            classFile.changed = true
                        }
                    }
                }
            }

            replaceInAnnotations(method.visibleAnnotations)
            replaceInAnnotations(method.invisibleAnnotations)
            method.visibleParameterAnnotations?.forEach(::replaceInAnnotations)
            method.invisibleParameterAnnotations?.forEach(::replaceInAnnotations)
        }
    }

    private data class RemovedClass(val name: String)
    private data class RemovedField(val owner: String, val name: String, val desc: String)
    private data class RemovedMethod(val owner: String, val name: String, val desc: String)

    private data class AnnotationType(
        val desc: String,
        val parameter: String,
        val targetType: String,
        val targetValue: String
    )

    private data class ExecutorType(
        val invokeOwner: String,
        val invokeName: String,
        val invokeDesc: String,
        val targetOwner: String,
        val targetName: String,
        val targetDesc: String
    )
}