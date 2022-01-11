package ua.tox1cozz.cutter.task.cutter

import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode

abstract class AnalyzationElement(
    val childs: MutableList<AnalyzationElement> = mutableListOf()
) {

    var parent: AnalyzationElement? = null

    abstract val path: String

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AnalyzationElement) return false

//        if (parent != other.parent) return false
        if (path != other.path) return false

        return true
    }

    override fun hashCode(): Int {
        return path.hashCode()
//        var result = parent?.hashCode() ?: 0
//        result = 31 * result + path.hashCode()
//        return result
    }

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
    val name: String
) : AnalyzationElement() {

    override val path: String = name
}

class MethodAnalyzationElement(
    val owner: String,
    val name: String,
    val desc: String
) : AnalyzationElement() {

    constructor(owner: String, method: MethodNode) : this(owner, method.name, method.desc)

    override val path: String = "$owner.$name$desc"
}

class FieldAnalyzationElement(
    val owner: String,
    val name: String,
    val desc: String
) : AnalyzationElement() {

    constructor(owner: String, field: FieldNode) : this(owner, field.name, field.desc)

    override val path: String = "$owner.field:$name#$desc"
}