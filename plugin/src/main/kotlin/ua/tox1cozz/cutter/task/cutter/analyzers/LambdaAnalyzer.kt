package ua.tox1cozz.cutter.task.cutter.analyzers

import org.objectweb.asm.Handle
import org.objectweb.asm.Opcodes.H_INVOKESTATIC
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.InvokeDynamicInsnNode
import org.objectweb.asm.tree.MethodNode
import ua.tox1cozz.cutter.task.cutter.AnalyzationModel
import ua.tox1cozz.cutter.task.cutter.MethodAnalyzationElement

internal class LambdaAnalyzer(context: AnalyzationModel) : Analyzer(context) {

    override fun process(classNode: ClassNode) {
        for (method in classNode.methods) {
            for (insn in method.instructions) {
                if (insn is InvokeDynamicInsnNode) {
                    processInvokeDynamic(classNode, method, insn)
                }
            }
        }
    }

    private fun processInvokeDynamic(classNode: ClassNode, method: MethodNode, insn: InvokeDynamicInsnNode) {
        if (insn.bsm.owner != "java/lang/invoke/LambdaMetafactory") return
        if (insn.bsm.name != "metafactory" && insn.bsm.name != "altMetafactory") return

        val handle = insn.bsmArgs[1] as Handle
        if (handle.owner != classNode.name) return
        if (handle.tag != H_INVOKESTATIC) return

        val lambdaMethod = classNode.methods.first { it.name == handle.name && it.desc == handle.desc }
        context.addElement(
            MethodAnalyzationElement(classNode.name, lambdaMethod),
            MethodAnalyzationElement(classNode.name, method)
        )
    }
}