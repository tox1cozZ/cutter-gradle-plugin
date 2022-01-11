package ua.tox1cozz.cutter.task.cutter.analyzers

import org.objectweb.asm.tree.ClassNode
import ua.tox1cozz.cutter.task.cutter.AnalyzationModel
import ua.tox1cozz.cutter.task.cutter.ClassAnalyzationElement
import ua.tox1cozz.cutter.task.cutter.FieldAnalyzationElement
import ua.tox1cozz.cutter.task.cutter.MethodAnalyzationElement

internal class ClassMembersAnalyzer(context: AnalyzationModel) : Analyzer(context) {

    override fun process(classNode: ClassNode) {
        val root = ClassAnalyzationElement(classNode.name)
        context.addElement(
            root,
            ClassAnalyzationElement(classNode.superName)
        )
        for (field in classNode.fields) {
            context.addElement(
                FieldAnalyzationElement(classNode.name, field),
                root
            )
        }
        for (method in classNode.methods) {
            context.addElement(
                MethodAnalyzationElement(classNode.name, method),
                root
            )
        }
    }
}