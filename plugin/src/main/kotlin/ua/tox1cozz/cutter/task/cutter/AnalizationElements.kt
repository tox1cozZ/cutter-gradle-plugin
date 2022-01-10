package ua.tox1cozz.cutter.task.cutter

import org.objectweb.asm.tree.ClassNode

abstract class AnalyzationElement(
    var parent: AnalyzationElement? = null,
    val childs: MutableList<AnalyzationElement> = mutableListOf()
) {

    abstract val path: String

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AnalyzationElement) return false
        return path == other.path
    }

    override fun hashCode(): Int = path.hashCode()

    override fun toString(): String = path


//    fun destroy(): List<AnalyzationElement> {
//        val elements = mutableListOf<AnalyzationElement>()
//        var scan = childs
//        while (scan.isNotEmpty()) {
//            scan.forEach {
//
//            }
//        }
//    }
}

class ClassAnalyzationElement(
    val node: ClassNode
) : AnalyzationElement() {

    override val path: String = node.name
}

class MethodAnalyzationElement(
    val owner: ClassAnalyzationElement,
    val name: String,
    val desc: String
) : AnalyzationElement() {

    override val path: String = "${owner.path}.$name$desc"
}

class FieldAnalyzationElement(
    val owner: ClassAnalyzationElement,
    val name: String,
    val desc: String
) : AnalyzationElement() {

    override val path: String = "${owner.path}.field:$name#$desc"
}