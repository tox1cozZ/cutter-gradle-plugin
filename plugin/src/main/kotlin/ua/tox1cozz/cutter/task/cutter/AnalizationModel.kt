package ua.tox1cozz.cutter.task.cutter

import ua.tox1cozz.cutter.task.cutter.analyzers.ClassMembersAnalyzer
import ua.tox1cozz.cutter.task.cutter.analyzers.InnerClassesAnalyzer
import ua.tox1cozz.cutter.task.cutter.analyzers.LambdaAnalyzer

internal class AnalyzationModel(private val classes: List<ClassFile>) {

    val elements = mutableMapOf<String, AnalyzationElement>()
    private val analyzers = listOf(
        ClassMembersAnalyzer(this),
        InnerClassesAnalyzer(this),
        LambdaAnalyzer(this)
    )

    fun analyze() {
        for (classFile in classes) {
            for (analyzer in analyzers) {
                analyzer.process(classFile.classNode)
            }
        }
    }

    fun addElement(child: AnalyzationElement, parent: AnalyzationElement?) {
        if (child == parent) return
        val element = elements.computeIfAbsent(child.path) { child }
        parent?.run {
            element.parent = parent
            childs += element
        }
    }

    fun removeElement(element: AnalyzationElement): List<AnalyzationElement> {
        val root = elements[element.path] ?: return emptyList()
        val result = mutableListOf<AnalyzationElement>()
        result.add(root)
        for (child in root.childs) {
            result.addAll(removeElement(child))
        }
        return result
    }
}