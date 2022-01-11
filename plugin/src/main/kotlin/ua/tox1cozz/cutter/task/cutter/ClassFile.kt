package ua.tox1cozz.cutter.task.cutter

import org.objectweb.asm.tree.ClassNode
import java.nio.file.Path

internal class ClassFile(
    val path: Path,
    val originalBytes: ByteArray,
    val classNode: ClassNode,
    var changed: Boolean = false
)