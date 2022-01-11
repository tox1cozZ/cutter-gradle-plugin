package ua.tox1cozz.cutter.task.cutter

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileTreeElement
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.file.RelativePath
import org.gradle.api.specs.Spec
import org.gradle.api.tasks.*
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.tree.ClassNode
import ua.tox1cozz.cutter.Constants
import ua.tox1cozz.cutter.configuration.CutterExtension.Companion.cutterExtension
import ua.tox1cozz.cutter.configuration.TargetConfiguration
import ua.tox1cozz.cutter.task.cutter.transform.NewClassTransformer
import ua.tox1cozz.cutter.util.PathExtensions.cleanDirectory
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.net.URI
import java.nio.file.*
import java.nio.file.StandardCopyOption.COPY_ATTRIBUTES
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import java.nio.file.attribute.BasicFileAttributes
import javax.inject.Inject
import kotlin.io.path.*

@OptIn(ExperimentalPathApi::class)
@CacheableTask
abstract class CutterTask @Inject constructor(
    private val target: TargetConfiguration
) : DefaultTask() {

    init {
        description = "Classes transforming for ${target.name} target build"
        group = Constants.GROUP
    }

    @get:InputFile
    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    abstract val archiveFile: RegularFileProperty

    @get:OutputDirectory
    internal abstract val targetClassesDir: DirectoryProperty

    @get:OutputDirectory
    internal abstract val originalFilesDir: DirectoryProperty

    @TaskAction
    fun execute() {
        val archive = archiveFile.get().asFile.toPath()
        val classesDir = targetClassesDir.get().asFile.toPath().also { it.cleanDirectory() }
        val otherFilesDir = originalFilesDir.get().asFile.toPath().also { it.cleanDirectory() }

        val classes = processJar(archive, otherFilesDir).associateTo(mutableMapOf()) {
            val classNode = ClassNode()
            val classReader = ClassReader(it.second)
            classReader.accept(classNode, 0)
            classNode.name to ClassFile(it.first, it.second, classNode)
        }
//        val classTransformer = ClassTransformer(target, classes, cutterExtension)
//        classTransformer.transform()
//        if (cutterExtension.validation.get()) {
//            classTransformer.validate()
//        }

        val classTransformer = NewClassTransformer(target, classes, cutterExtension)
        val transformedClasses = classTransformer.transform()
        transformedClasses.forEach { classFile ->
            if (classFile.changed) {
                val writer = ClassWriter(ClassWriter.COMPUTE_MAXS)
                classFile.classNode.accept(writer)

                val destPath = classesDir.resolve(classFile.path.toString())
                destPath.parent.createDirectories()
                destPath.writeBytes(writer.toByteArray())
            } else {
                val destPath = otherFilesDir.resolve(classFile.path.toString())
                destPath.parent.createDirectories()
                destPath.writeBytes(classFile.originalBytes)
            }
        }
    }

    private fun processJar(archive: Path, otherFilesDir: Path): MutableList<Pair<Path, ByteArray>> {
        val classesSpec = cutterExtension.packages.asSpec
        val classes = mutableListOf<Pair<Path, ByteArray>>()

        FileSystems.newFileSystem(
            URI.create("jar:${archive.toUri()}"),
            mapOf(
                "create" to "false",
                "encoding" to "UTF-8"
            )
        ).use {
            val rootPath = it.getPath("/")
            Files.walkFileTree(rootPath, object : FileVisitor<Path> {
                override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                    return FileVisitResult.CONTINUE
                }

                override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                    val relativeFile = rootPath.relativize(file)
                    if (relativeFile.isTargetClassFile(classesSpec)) {
                        classes.add(Pair(relativeFile, relativeFile.readBytes()))
                    } else {
                        val otherFile = otherFilesDir.resolve(relativeFile.toString())
                        otherFile.parent.createDirectories()
                        Files.copy(relativeFile, otherFile, COPY_ATTRIBUTES, REPLACE_EXISTING)
                    }
                    return FileVisitResult.CONTINUE
                }

                override fun visitFileFailed(file: Path, ex: IOException): FileVisitResult {
                    throw GradleException("Failed to visit file in jar $archive: $file", ex)
                }

                override fun postVisitDirectory(dir: Path, ex: IOException?): FileVisitResult {
                    return FileVisitResult.CONTINUE
                }
            })
        }

        return classes
    }

    private fun Path.isTargetClassFile(classesSpec: Spec<FileTreeElement>): Boolean {
        class JarFileTreeElement(private val path: Path) : FileTreeElement {
            override fun getFile() = null
            override fun isDirectory() = false
            override fun getLastModified() = path.getLastModifiedTime().toMillis()
            override fun getSize() = path.fileSize()
            override fun open() = null
            override fun copyTo(output: OutputStream) = Unit
            override fun copyTo(target: File) = false
            override fun getName() = path.name
            override fun getPath() = path.toString()
            override fun getRelativePath() = RelativePath(true, *getPath().split("/").toTypedArray())
            override fun getMode(): Int = 0
        }

        return extension == "class" && classesSpec.isSatisfiedBy(JarFileTreeElement(this))
    }
}