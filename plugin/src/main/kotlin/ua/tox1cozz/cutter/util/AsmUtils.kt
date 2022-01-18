package ua.tox1cozz.cutter.util

import kotlinx.metadata.jvm.KotlinClassHeader
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.InsnNode

object AsmUtils {

    fun createZero(typeDescriptor: String) = createZero(Type.getType(typeDescriptor))

    fun createZero(type: Type): AbstractInsnNode = when (type) {
        Type.VOID_TYPE -> throw IllegalArgumentException("Type void not allowed here")
        Type.BOOLEAN_TYPE, Type.BYTE_TYPE, Type.CHAR_TYPE, Type.SHORT_TYPE, Type.INT_TYPE -> {
            InsnNode(Opcodes.ICONST_0)
        }
        Type.LONG_TYPE -> {
            InsnNode(Opcodes.LCONST_0)
        }
        Type.FLOAT_TYPE -> {
            InsnNode(Opcodes.FCONST_0)
        }
        Type.DOUBLE_TYPE -> {
            InsnNode(Opcodes.DCONST_0)
        }
        else -> InsnNode(Opcodes.ACONST_NULL)
    }

    fun InsnList.subList(
        start: AbstractInsnNode,
        end: AbstractInsnNode,
        reverseDirection: Boolean = false
    ): List<AbstractInsnNode> {
        val list = mutableListOf<AbstractInsnNode>()
        if (reverseDirection) {
            var insn = end
            while (insn != start) {
                list.add(insn)
                insn = insn.previous
            }
        } else {
            var insn = start
            while (insn != end) {
                list.add(insn)
                insn = insn.next
            }
        }
        return list
    }

    @Suppress("UNCHECKED_CAST")
    fun AnnotationNode.getKotlinClassHeader(): KotlinClassHeader {
        var kind: Int? = null
        var metadataVersion: IntArray? = null
        var bytecodeVersion: IntArray? = null
        var data1: Array<String>? = null
        var data2: Array<String>? = null
        var extraString: String? = null
        var packageName: String? = null
        var extraInt: Int? = null

        var i = 0
        while (i < values.size) {
            val name = values[i++]
            val value = values[i++]

            when (name) {
                "k" -> kind = value as? Int
                "mv" -> metadataVersion = (value as List<Int>).toIntArray()
                "bv" -> bytecodeVersion = (value as List<Int>).toIntArray()
                "xs" -> extraString = value as? String
                "xi" -> extraInt = value as? Int
                "pn" -> packageName = value as? String
                "d1" -> data1 = (value as List<String>).toTypedArray()
                "d2" -> data2 = (value as List<String>).toTypedArray()
            }
        }

        return KotlinClassHeader(kind, metadataVersion, /*bytecodeVersion, */data1, data2, extraString, packageName, extraInt)
    }
}