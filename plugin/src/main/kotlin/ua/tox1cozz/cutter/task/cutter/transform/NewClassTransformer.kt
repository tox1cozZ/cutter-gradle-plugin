package ua.tox1cozz.cutter.task.cutter.transform

import org.objectweb.asm.Handle
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.InvokeDynamicInsnNode
import org.objectweb.asm.tree.MethodNode
import ua.tox1cozz.cutter.configuration.CutterExtension
import ua.tox1cozz.cutter.configuration.TargetConfiguration
import ua.tox1cozz.cutter.task.cutter.ClassFile

internal class NewClassTransformer(
    target: TargetConfiguration,
    private val classes: Map<String, ClassFile>,
    private val config: CutterExtension
) {

    /*companion object {

        val CUTTER_CLASS = Type.getInternalName(Cutter::class.java)
        const val CUTTER_FIELD_VALUE_NAME = "fieldValue"
        const val CUTTER_FIELD_VALUE_DESC = "(Ljava/lang/Enum;Ljava/util/function/Supplier;)Ljava/lang/Object;"
    }*/

    private val types = target.types.associate {
        val value = it.value.get().split('.', limit = 2)
        val annotation = "L${it.annotation.get()};"
        annotation to TargetType(annotation, it.parameterName.get(), value[0], value[1])
    }

    fun transform(): List<ClassFile> {
        val transformedClasses = mutableMapOf<String, ClassFile>()

        classes.values.forEach { classFile ->
            val classNode = classFile.classNode
            val changeClass: (AnnotationNode) -> Unit = { classFile.changed = true }

            if (removeTargetAnnotations(classNode.visibleAnnotations, changeClass) ||
                removeTargetAnnotations(classNode.invisibleAnnotations, changeClass)
            ) {
                println("Cut class: ${classNode.name}")
                return@forEach
            }

            transformedClasses[classNode.name] = classFile

            val deleteMethods = mutableListOf<MethodNode>()
            for (method in classNode.methods) {
                if (removeTargetAnnotations(method.visibleAnnotations, changeClass) ||
                    removeTargetAnnotations(method.invisibleAnnotations, changeClass)
                ) {
                    deleteMethods += method
                    println("Cut method in class ${classNode.name}: ${method.name}${method.desc}")
                    removeLambdas(classNode, method, deleteMethods)
                }
            }
            deleteMethods.forEach { deleted ->
                classNode.methods.removeIf { it.name == deleted.name && it.desc == deleted.desc }
            }

            val fields = classNode.fields.iterator()
            while (fields.hasNext()) {
                val field = fields.next()
                if (removeTargetAnnotations(field.visibleAnnotations, changeClass) ||
                    removeTargetAnnotations(field.invisibleAnnotations, changeClass)
                ) {
                    println("Cut field in class ${classNode.name}: ${field.name} # ${field.desc}")
                    fields.remove()
                }
            }
        }

        // Cut nested classes
        transformedClasses.values.filter { classFile ->
            val classNode = classFile.classNode
            if (classNode.outerClass != null) {
                val parentClass = transformedClasses[classNode.outerClass]?.classNode ?: return@filter true
                if (classNode.outerMethod != null && classNode.outerMethodDesc != null) {
                    return@filter parentClass.methods.none { it.name == classNode.outerMethod && it.desc == classNode.outerMethodDesc }
                }
            }
            false
        }.forEach { classFile ->
            val classNode = classFile.classNode
            if (classNode.outerMethod != null && classNode.outerMethodDesc != null) {
                println("Cut nested class in parent ${classNode.outerClass} # (${classNode.outerMethod}${classNode.outerMethodDesc}): ${classNode.name}")
            } else {
                println("Cut nested class in parent ${classNode.outerClass}: ${classNode.name}")
            }
            transformedClasses.remove(classNode.name)
        }

        return transformedClasses.values.toList()
    }

    private fun removeLambdas(classNode: ClassNode, method: MethodNode, deleteMethods: MutableList<MethodNode>) {
        for (insn in method.instructions) {
            if (insn !is InvokeDynamicInsnNode) continue
            if (insn.bsm.owner != "java/lang/invoke/LambdaMetafactory") continue
            if (insn.bsm.name != "metafactory" && insn.bsm.name != "altMetafactory") continue

            val handle = insn.bsmArgs[1] as Handle
            if (handle.owner != classNode.name) continue
            if (handle.tag != Opcodes.H_INVOKESTATIC) continue

            val lambdaMethod = classNode.methods.first { it.name == handle.name && it.desc == handle.desc }
            deleteMethods += lambdaMethod
            println("Cut lambda in class ${classNode.name} for method ${method.name}${method.desc}: ${lambdaMethod.name}${lambdaMethod.desc}")
            removeLambdas(classNode, lambdaMethod, deleteMethods)
        }
    }

    private fun removeTargetAnnotations(
        annotations: MutableList<AnnotationNode>?,
        onRemove: ((AnnotationNode) -> Unit)? = null
    ): Boolean {
        if (annotations.isNullOrEmpty()) return false

        var cutElement = false
        annotations.removeIf {
            if (it.values.isNullOrEmpty()) return@removeIf false
            val type = types[it.desc] ?: return@removeIf false

            for (i in 0 until it.values.size step 2) {
                val name = it.values[i] as String
                val value = it.values[i + 1]

                // A two elements String array (for enumeration values)
                if (value !is Array<*> || !value.isArrayOf<String>() || value.size != 2) continue

                if (name == type.parameterName && value[0] == "L${type.targetType};" && value[1] != type.targetValue) {
                    cutElement = true
                    break
                }
            }

            onRemove?.invoke(it)
            true
        }
        return cutElement
    }
}

private data class TargetType(
    val annotationDesc: String,
    val parameterName: String,
    val targetType: String,
    val targetValue: String
)