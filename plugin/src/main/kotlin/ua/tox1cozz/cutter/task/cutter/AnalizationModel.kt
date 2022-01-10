package ua.tox1cozz.cutter.task.cutter

class AnalyzationModel {

    val elements = mutableMapOf<String, AnalyzationElement>()

    fun addElement(child: AnalyzationElement, parent: AnalyzationElement?) {
        if (child == parent) return
        check(child.parent == null) { "Element ${child.path} already has a parent: ${child.parent}" }

        child.parent = parent
        parent?.run {
            childs += child
        }
        elements[child.path] = child
    }



    fun removeElement(element: AnalyzationElement): List<AnalyzationElement> {
        val result = mutableListOf<AnalyzationElement>()
        result.add(element)
        for (child in element.childs) {
            result.addAll(removeElement(child))
        }
        return result
    }
}