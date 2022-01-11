package ua.tox1cozz.cutter.task.cutter.analyzers

import org.objectweb.asm.tree.ClassNode
import ua.tox1cozz.cutter.task.cutter.AnalyzationModel
import ua.tox1cozz.cutter.task.cutter.ClassAnalyzationElement
import ua.tox1cozz.cutter.task.cutter.MethodAnalyzationElement

internal class InnerClassesAnalyzer(context: AnalyzationModel) : Analyzer(context) {

    override fun process(classNode: ClassNode) {
        val current = ClassAnalyzationElement(classNode.name)
        if (classNode.outerClass != null) {
            if (classNode.outerMethod != null) {
                context.addElement(
                    current,
                    MethodAnalyzationElement(classNode.name, classNode.outerMethod, classNode.outerMethodDesc)
                )
            } else {
                context.addElement(
                    current,
                    ClassAnalyzationElement(classNode.outerClass)
                )
            }
        } else {
            for (innerClass in classNode.innerClasses) {
                if (innerClass.outerName == classNode.name) {
                    context.addElement(
                        ClassAnalyzationElement(innerClass.name),
                        current
                    )
                }
            }
        }
    }
}