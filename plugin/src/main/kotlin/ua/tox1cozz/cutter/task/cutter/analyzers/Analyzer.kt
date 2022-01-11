package ua.tox1cozz.cutter.task.cutter.analyzers

import org.objectweb.asm.tree.ClassNode
import ua.tox1cozz.cutter.task.cutter.AnalyzationModel

internal abstract class Analyzer(protected val context: AnalyzationModel) {

    abstract fun process(classNode: ClassNode)
}