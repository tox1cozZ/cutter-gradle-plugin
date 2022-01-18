package ua.tox1cozz.cutter.task.cutter.transform

import kotlinx.metadata.jvm.KotlinClassMetadata
import org.objectweb.asm.Handle
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.*
import ua.tox1cozz.cutter.configuration.CutterExtension
import ua.tox1cozz.cutter.configuration.ReplaceTokensConfiguration
import ua.tox1cozz.cutter.configuration.TargetConfiguration
import ua.tox1cozz.cutter.task.cutter.ClassFile
import ua.tox1cozz.cutter.util.AsmUtils.getKotlinClassHeader

internal class NewClassTransformer(
    target: TargetConfiguration,
    private val config: CutterExtension,
    private val replaceTokens: ReplaceTokensConfiguration,
    private val classes: Map<String, ClassFile>
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

    // TODO: Удалять вырезанные объекты из Metadata аннотации
    private val kotlinClasses = mutableMapOf<String, KotlinClassFile>()

    private val nestedKotlinClasses = mutableMapOf<String, String>()
    private val innerClasses = mutableMapOf<String, String>()

    fun transform(): List<ClassFile> {
        val transformedClasses = mutableMapOf<String, ClassFile>()

        classes.values.forEach { classFile ->
            val classNode = classFile.classNode
            val changeClass: (AnnotationNode) -> Unit = { classFile.changed = true }

            val kotlinAnnotation = classNode.visibleAnnotations?.find { it.desc == Type.getDescriptor(Metadata::class.java) }
            if (kotlinAnnotation != null) {
                val header = kotlinAnnotation.getKotlinClassHeader()
                val metadata = KotlinClassMetadata.read(header)!!
                kotlinClasses[classNode.name] = KotlinClassFile(classFile, kotlinAnnotation, metadata)

                if (metadata is KotlinClassMetadata.Class) {
                    val klass = metadata.toKmClass()
                    klass.companionObject?.let {
                        val child = classNode.name + "$" + it
                        nestedKotlinClasses[child] = classNode.name
                    }
                    for (nestedClass in klass.nestedClasses) {
                        val child = classNode.name + "$" + nestedClass
                        nestedKotlinClasses[child] = classNode.name
                    }
                }
            }

            if (removeTargetAnnotations(classNode.visibleAnnotations, changeClass) ||
                removeTargetAnnotations(classNode.invisibleAnnotations, changeClass)
            ) {
                println("Cut class: ${classNode.name}")
                classNode.innerClasses.forEach { innerClass ->
                    if (innerClass.outerName == classNode.name) {
                        innerClasses[innerClass.name] = classNode.name
                    }
                }
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

        removeNestedClasses(transformedClasses)
        removeInnerClasses(transformedClasses)
        removeNestedKotlinClasses(transformedClasses)
        replaceTokens(transformedClasses)

        return transformedClasses.values.toList()
    }

    fun validation() {

    }

    private fun removeInnerClasses(classes: MutableMap<String, ClassFile>) {
        classes.values.removeIf { classFile ->
            val classNode = classFile.classNode
            val parentClass = innerClasses[classNode.name] ?: return@removeIf false
            if (classes[parentClass] == null) {
                println("Cut inner class in parent $parentClass: ${classNode.name}")
                return@removeIf true
            }
            false
        }
    }

    private fun removeNestedKotlinClasses(classes: MutableMap<String, ClassFile>) {
        classes.values.removeIf { classFile ->
            val classNode = classFile.classNode
            val parentClass = nestedKotlinClasses[classNode.name] ?: return@removeIf false
            if (classes[parentClass] == null) {
                println("Cut nested kotlin class in parent $parentClass: ${classNode.name}")
                return@removeIf true
            }
            false
        }
    }

    private fun removeNestedClasses(classes: MutableMap<String, ClassFile>) {
        classes.values.filter { classFile ->
            val classNode = classFile.classNode
            if (classNode.outerClass != null) {
                val parentClass = classes[classNode.outerClass]?.classNode ?: return@filter true
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
            classes.remove(classNode.name)
        }

        // Kotlin nested classes
        classes.values.removeIf { classFile ->
            val classNode = classFile.classNode
            val parentClass = classes[classNode.outerClass]?.classNode
            false
        }
    }

    private fun replaceTokens(classes: MutableMap<String, ClassFile>) {
        val tokens = replaceTokens.tokens.get()
        if (tokens.isEmpty()) return

        fun replaceInAnnotations(
            classFile: ClassFile,
            annotations: MutableList<AnnotationNode>?
        ) {
            if (annotations.isNullOrEmpty()) return
            for (annotation in annotations) {
                if (annotation.values.isNullOrEmpty()) continue
                for (i in 0 until annotation.values.size step 2) {
                    val value = annotation.values[i + 1]
                    if (value is String && value in tokens) {
                        annotation.values[i + 1] = tokens[value]
                        classFile.changed = true
                    } else if (value is MutableList<*> && value.isNotEmpty() && value.first() is String) {
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

        for (classFile in classes.values) {
            val classNode = classFile.classNode
            replaceInAnnotations(classFile, classNode.visibleAnnotations)
            replaceInAnnotations(classFile, classNode.invisibleAnnotations)

            for (field in classNode.fields) {
                if (field.value is String && field.value in tokens) {
                    field.value = tokens[field.value]
                    classFile.changed = true
                }
                replaceInAnnotations(classFile, field.visibleAnnotations)
                replaceInAnnotations(classFile, field.invisibleAnnotations)
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

                replaceInAnnotations(classFile, method.visibleAnnotations)
                replaceInAnnotations(classFile, method.invisibleAnnotations)
                method.visibleParameterAnnotations?.forEach { replaceInAnnotations(classFile, it) }
                method.invisibleParameterAnnotations?.forEach { replaceInAnnotations(classFile, it) }
            }
        }
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

private data class KotlinClassFile(
    val classFile: ClassFile,
    val annotation: AnnotationNode,
    val metadata: KotlinClassMetadata
)