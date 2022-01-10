package ua.tox1cozz.cutter.task.cutter

import org.objectweb.asm.Handle
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type
import org.objectweb.asm.tree.*
import ua.tox1cozz.cutter.Cutter
import ua.tox1cozz.cutter.configuration.CutterExtension
import ua.tox1cozz.cutter.configuration.TargetConfiguration
import ua.tox1cozz.cutter.util.AsmUtils.createZero
import ua.tox1cozz.cutter.util.AsmUtils.subList
import java.lang.reflect.Modifier.*

internal class ClassTransformer(
    private val target: TargetConfiguration,
    private val classes: MutableMap<String, ClassFile>,
    private val cutter: CutterExtension
) {

    // TODO: Записывать лог в файл
    val log = mutableListOf<String>()

    private companion object {

        val CUTTER_CLASS: String = Type.getInternalName(Cutter::class.java)
        const val CUTTER_FIELD_VALUE_NAME = "fieldValue"
        const val CUTTER_FIELD_VALUE_DESC = "(Ljava/lang/Enum;Ljava/util/function/Supplier;)Ljava/lang/Object;"

        val LAMBDA_META_FACTORY = Handle(
            H_INVOKESTATIC,
            "java/lang/invoke/LambdaMetafactory",
            "metafactory",
            "(Ljava/lang/invoke/MethodHandles\$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;",
            false
        )

        val LAMBDA_ALT_META_FACTORY = Handle(
            H_INVOKESTATIC,
            "java/lang/invoke/LambdaMetafactory",
            "altMetafactory",
            "(Ljava/lang/invoke/MethodHandles\$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite;",
            false
        )
    }

    private data class TargetType(
        val annotationDesc: String,
        val parameterName: String,
        val targetType: String,
        val targetValue: String
    )

    private val types = target.types.map {
        val value = it.value.get().split('.', limit = 2)
        TargetType(
            "L${it.annotation.get()};",
            it.parameterName.get(),
            value[0],
            value[1]
        )
    }

    private val localClassCache = mutableMapOf<Triple<String, String, String>, ClassNode>()

//    private val localClassCache = classes.values.filter {
//        it.classNode.outerClass != null && it.classNode.outerMethod != null && it.classNode.outerMethodDesc != null
//    }.mapTo(HashSet()) {
//        Triple(it.classNode.outerClass, it.classNode.outerMethod, it.classNode.outerMethodDesc).also { my ->
//            println(my)
//        }
//    }

    private data class CutField(val owner: String, val name: String, val desc: String)
    private data class CutMethod(val owner: String, val name: String, val desc: String, val type: String = "method")

    private val cutClasses = mutableSetOf<String>()
    private val cutFields = mutableSetOf<CutField>()
    private val cutMethods = mutableSetOf<CutMethod>()

    private fun findClassByName(name: String?): ClassFile? {
        if (name == null) return null
        if (cutClasses.contains(name)) return null

        return classes[name]
    }

    // TODO: Исправлять @kotlin.Metadata аннотацию после удаления полей/методов
    // ГОТОВО: В старых версиях kotlin (до 1.5) лямбда компилируется в класс, а не метод (invokedynamic)
    // TODO: Если класс не изменялся - просто копировать его на выход
    // TODO: Вместе с полями вырезать их геттеры/сеттеры
    // TODO: Вырезать Cutter.execute
    // ГОТОВО (Проверить на Java): Вырезать лямбды из методов, которые компилируются под неподходящий билд
    // ГОТОВО (Проверить на Java): Вырезать внутренние классы и методы (их имена начинается на className$methodName$), которые компилируются под неподходящий билд

    // TODO: При валидации искать Cutter.fieldValue во всех методах. Если они есть где-то кроме <init> или <clinit> - ругаться
    fun transform() {
        classes.values.forEach {
            if (removeObjectByAnnotation(it.classNode.visibleAnnotations, it.classNode.invisibleAnnotations)) {
                cutClass(it.classNode, true)
                return@forEach
            }

            // Fill local class cache
            if (it.classNode.outerClass != null && it.classNode.outerMethod != null && it.classNode.outerMethodDesc != null) {
                val key = Triple(it.classNode.outerClass, it.classNode.outerMethod, it.classNode.outerMethodDesc)
                localClassCache[key] = it.classNode
            }
        }

        classes.values.forEach { classFile ->
            if (cutClasses.contains(classFile.classNode.name)) return@forEach

            val classNode = classFile.classNode
            classNode.fields.forEach { field ->
                processField(classNode, field)
            }
            classNode.methods.forEach { method ->
                processMethod(classNode, method)
            }
        }

        println("Methods cut:")
        cutMethods.forEach { cutMethod ->
            println("Cut ${cutMethod.type} in class ${cutMethod.owner}: ${cutMethod.name}${cutMethod.desc}")
            findClassByName(cutMethod.owner)?.let { classFile ->
                classFile.classNode.methods.removeIf { it.name == cutMethod.name && it.desc == cutMethod.desc }
            }
        }

        println("Fields cut:")
        cutFields.forEach { cutField ->
            println("Cut field in class ${cutField.owner}: ${cutField.name}${cutField.desc}")
            if (cutter.removeFields.get()) { // Completely delete a field only if the option is enabled
                findClassByName(cutField.owner)?.let { classFile ->
                    classFile.classNode.fields.removeIf { it.name == cutField.name && it.desc == cutField.desc }
                }
            }
        }

        println("Classes cut:")
        cutClasses.forEach { className ->
            println("Cut class: $className")
            classes.remove(className)
        }
    }

    fun validate() {

    }

    private fun processField(classNode: ClassNode, field: FieldNode) {
        if (removeObjectByAnnotation(field.visibleAnnotations, field.invisibleAnnotations)) {
            cutFields.add(CutField(classNode.name, field.name, field.desc))
        }
    }

    private fun processMethod(classNode: ClassNode, method: MethodNode) {
        val removed = removeObjectByAnnotation(method.visibleAnnotations, method.invisibleAnnotations)
        if (removed) {
            cutMethod(classNode, method, "method")
            return
        }

        // Constructors (fields initialization)
        if (method.name == "<init>" || method.name == "<clinit>") {
            processFieldsInitializations(classNode, method)
        }
    }

    private fun cutClass(classNode: ClassNode, removeInners: Boolean) {
        cutClasses.add(classNode.name)
        if (removeInners) {
            removeInnerClasses(classNode)
        }
    }

    private fun cutMethod(classNode: ClassNode, method: MethodNode, type: String) {
        cutMethods.add(CutMethod(classNode.name, method.name, method.desc, type))
        removeLambdas(classNode, method)
        removeLocalFunctions(classNode, method)
        removeLocalClasses(classNode, method)
    }

    // Kotlin local functions. Cuts only if the function is called. Unused functions are ignored
    private fun removeLocalFunctions(classNode: ClassNode, method: MethodNode) {
        method.instructions.forEach { insn ->
            if (insn is MethodInsnNode && insn.opcode == INVOKESTATIC &&
                insn.owner == classNode.name && insn.name.startsWith("${method.name}$")
            ) {
                val localFunc = classNode.methods.first { it.name == insn.name && it.desc == insn.desc }
                if (isPrivate(localFunc.access) && isStatic(localFunc.access) && isFinal(localFunc.access)) {
                    cutMethod(classNode, localFunc, "local function")
                }
            }
        }
    }

    private fun removeLocalClasses(targetClass: ClassNode, method: MethodNode) {
        val localClass = localClassCache[Triple(targetClass.name, method.name, method.desc)] ?: return
        cutClass(localClass, true)
        targetClass.innerClasses.removeIf { it.name == localClass.name }

//        classes.values.forEach { classFile ->
//            if (classFile.classNode.outerClass == targetClass.name && classFile.classNode.outerMethod == method.name
//                && classFile.classNode.outerMethodDesc == method.desc
//            ) {
//                cutClasses.add(classFile.classNode.name)
//                removeInnerClasses(classFile.classNode)
//                targetClass.innerClasses.removeIf { it.name == classFile.classNode.name }
//            }
//        }
    }

    private fun removeLambdas(classNode: ClassNode, method: MethodNode) {
        method.instructions.forEach { insn ->
            if (insn is InvokeDynamicInsnNode && (insn.bsm == LAMBDA_META_FACTORY || insn.bsm == LAMBDA_ALT_META_FACTORY)) {
                val lambda = insn.bsmArgs[1] as Handle
                if (lambda.owner != classNode.name) return@forEach
                val lambdaMethod = classNode.methods.first { it.name == lambda.name && it.desc == lambda.desc }
                cutMethod(classNode, lambdaMethod, "lambda")
            }
        }
    }

    private fun processFieldsInitializations(classNode: ClassNode, constructor: MethodNode) {
        val targetFields = mutableListOf<FieldInsnNode>()
        val insnsToRemove = mutableListOf<AbstractInsnNode>()

        // PUTFIELD
        var currentField: FieldInsnNode? = null

        // INVOKESTATIC "ua/tox1cozz/cutter/Cutter", "fieldValue", "(Ljava/lang/Enum;Ljava/util/function/Supplier;)Ljava/lang/Object;"
        var invokeCutter: MethodInsnNode? = null

        // GETSTATIC "ua/tox1cozz/cutter/CutterTarget" "@TARGET@", "Lua/tox1cozz/cutter/CutterTarget;"
        var cutterTarget: FieldInsnNode? = null

        val insnList = constructor.instructions
        var currentInsn = insnList.last
        while (currentInsn != null) {
            if (currentInsn.opcode == PUTFIELD) {
                currentField = currentInsn as FieldInsnNode
            } else if (currentField != null) {
                if (currentInsn.opcode == INVOKESTATIC && currentInsn is MethodInsnNode) {
                    if (currentInsn.owner == CUTTER_CLASS && currentInsn.name == CUTTER_FIELD_VALUE_NAME
                        && currentInsn.desc == CUTTER_FIELD_VALUE_DESC
                    ) {
                        invokeCutter = currentInsn
                    }
                } else if (currentInsn.opcode == GETSTATIC && currentInsn is FieldInsnNode && invokeCutter != null) {
                    val notValid = isNotValidTargetInvoke(currentInsn)
                    if (notValid != null) {
                        cutterTarget = currentInsn

                        val invokeDynamic = invokeCutter.previous
                        if (invokeDynamic is InvokeDynamicInsnNode &&
                            (invokeDynamic.bsm == LAMBDA_META_FACTORY || invokeDynamic.bsm == LAMBDA_ALT_META_FACTORY)
                        ) { // Default lambda compilation
                            val lambda = invokeDynamic.bsmArgs[1] as Handle
                            val lambdaMethod = classNode.methods.find { it.name == lambda.name && it.desc == lambda.desc }

                            // If method in current class
                            if (lambdaMethod != null) {
                                if (notValid) {
                                    cutMethod(classNode, lambdaMethod, "lambda")
                                } else {
                                    insnsToRemove.add(invokeDynamic)

                                    lambdaMethod.name += "\$initializer"

                                    // Redirect to direct invoke lambda method
                                    invokeCutter.opcode = INVOKESTATIC
                                    invokeCutter.owner = classNode.name
                                    invokeCutter.name = lambdaMethod.name
                                    invokeCutter.desc = lambdaMethod.desc
                                    invokeCutter.itf = false
                                }
                            } else if (!notValid) {
                                // Redirect to direct invoke reference method
                                invokeCutter.opcode = INVOKESTATIC
                                invokeCutter.owner = lambda.owner
                                invokeCutter.name = lambda.name
                                invokeCutter.desc = lambda.desc
                                invokeCutter.itf = false
                            }
                        } else {
                            val invokeClass = cutterTarget.next.next
                            if (invokeClass.opcode == NEW && invokeClass is TypeInsnNode
                                && classNode.innerClasses.any { it.name == invokeClass.desc } // Kotlin lambda compilation to inner class with value capture
                            ) {
                                if (notValid) {
                                    classNode.innerClasses.removeIf { it.name == invokeClass.desc }
                                    findClassByName(invokeClass.desc)?.let {
                                        cutClass(it.classNode, false)
                                    }
                                } else {
                                    insnsToRemove.add(invokeCutter)
                                }
                            } else if (invokeClass.opcode == GETSTATIC && invokeClass is FieldInsnNode
                                && invokeClass.name == "INSTANCE" && classNode.innerClasses.any { it.name == invokeClass.owner } // Kotlin lambda compilation to inner class
                            ) {
                                if (notValid) {
                                    classNode.innerClasses.removeIf { it.name == invokeClass.owner }
                                    findClassByName(invokeClass.owner)?.let {
                                        cutClass(it.classNode, false)
                                    }
                                } else {
                                    insnsToRemove.add(invokeCutter)
                                }
                            }
                        }

                        if (notValid) {
                            targetFields.add(currentField)
                            insnsToRemove.addAll(insnList.subList(cutterTarget, currentField))
                        } else {
                            insnsToRemove.add(cutterTarget)
                            insnsToRemove.add(cutterTarget.next) // CHECKCAST "java/lang/Enum"
                        }

                        // Reset values
                        currentField = null
                        invokeCutter = null
                        cutterTarget = null
                    }
                }
            }

            currentInsn = currentInsn.previous
        }

        // Remove dead code
        insnsToRemove.forEach { insnList.remove(it) }

        // Set default value to field
        targetFields.forEach { field ->
            cutFields.add(CutField(classNode.name, field.name, field.desc))
            if (cutter.removeFields.get()) {
                insnList.remove(field.previous) // ALOAD 0 (this)
                insnList.remove(field) // PUTFIELD
            } else {
                insnList.insertBefore(field, createZero(field.desc))
            }
        }
    }

    // TODO: Переделать. Походу, visitInnerClass регает все вложенные классы которые юзает, а не только создает
    private fun removeInnerClasses(classNode: ClassNode) {
        classNode.innerClasses.toTypedArray().forEach { innerClass ->
            val outerName = innerClass.outerName ?: findClassByName(innerClass.name)?.classNode?.outerClass
            if (outerName == classNode.name) {
                findClassByName(innerClass.name)?.let {
//                    println("Cut inner class ${innerClass.name} of parent ${classNode.name}")

                    cutClass(it.classNode, true)
                }

//                classes.remove(innerClass.name)?.let {
//                    println("Cut inner class: ${innerClass.name}")
//                    removeInnerClasses(it.classNode)
//                }
            } else if (innerClass.name == classNode.name) {
                findClassByName(innerClass.outerName)?.let { outer ->
//                    println("Remove inner by second for ${innerClass.outerName}")
                    outer.classNode.innerClasses.removeIf { it.name == innerClass.name }
                }
            }

//            println("Inner for ${classNode.name}: name = ${innerClass.name}, outerName = ${innerClass.outerName}")
        }
    }

    // Removed all target annotations
    private fun removeObjectByAnnotation(
        visibleAnnotations: MutableList<AnnotationNode>?,
        invisibleAnnotations: MutableList<AnnotationNode>?
    ): Boolean {
        fun MutableList<AnnotationNode>?.checkTarget(): Boolean {
            var result = false
            this?.removeIf { annotation ->
                isNotValidTargetAnnotation(annotation).let {
                    if (it == true) result = true
                    it != null
                }
            }
            return result
        }

        return visibleAnnotations.checkTarget() || invisibleAnnotations.checkTarget()
    }

    private fun isNotValidTargetInvoke(insn: FieldInsnNode): Boolean? {
        types.forEach { type ->
            if (insn.owner == type.targetType && insn.desc == "L${type.targetType};") {
                return insn.name != type.targetValue
            }
        }
        return null
    }

    // Return true if object to remove
    private fun isNotValidTargetAnnotation(annotation: AnnotationNode): Boolean? = with(annotation) {
        if (values.isNullOrEmpty()) return null

        for (i in 0 until values.size step 2) {
            val name = values[i] as String
            val value = values[i + 1]

            // A two elements String array (for enumeration values)
            if (value !is Array<*> || !value.isArrayOf<String>() || value.size != 2) continue

            types.forEach { type ->
                if (desc == type.annotationDesc && name == type.parameterName && value[0] == "L${type.targetType};") {
                    return value[1] != type.targetValue
                }
            }
        }

        return null
    }
}