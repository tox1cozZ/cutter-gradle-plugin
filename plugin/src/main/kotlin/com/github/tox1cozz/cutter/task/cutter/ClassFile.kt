package com.github.tox1cozz.cutter.task.cutter

import org.objectweb.asm.tree.ClassNode
import java.nio.file.Path

internal class ClassFile(val path: Path, val originalBytes: ByteArray, val node: ClassNode) {

    var changed = false
}