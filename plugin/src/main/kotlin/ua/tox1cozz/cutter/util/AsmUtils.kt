package ua.tox1cozz.cutter.util

import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AbstractInsnNode
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
}