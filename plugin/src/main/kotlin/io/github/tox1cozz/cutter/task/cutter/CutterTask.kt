package io.github.tox1cozz.cutter.task.cutter

import groovy.lang.Closure
import io.github.tox1cozz.cutter.CutterPlugin
import io.github.tox1cozz.cutter.configuration.CutterExtension
import io.github.tox1cozz.cutter.configuration.ReplaceTokensConfiguration
import io.github.tox1cozz.cutter.configuration.TargetConfiguration
import io.github.tox1cozz.cutter.task.TargetTask
import io.github.tox1cozz.cutter.util.cleanDirectory
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileTreeElement
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.file.RelativePath
import org.gradle.api.specs.Spec
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.tree.ClassNode
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.net.URI
import java.nio.file.FileSystems
import java.nio.file.FileVisitResult
import java.nio.file.FileVisitor
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption.COPY_ATTRIBUTES
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import java.nio.file.attribute.BasicFileAttributes
import javax.inject.Inject
import kotlin.io.path.createDirectories
import kotlin.io.path.extension
import kotlin.io.path.name
import kotlin.io.path.readBytes
import kotlin.io.path.writeBytes

@CacheableTask
abstract class CutterTask @Inject constructor(
    @get:Internal final override val target: TargetConfiguration,
    private val extension: CutterExtension
) : DefaultTask(), TargetTask {

    init {
        description = "Classes transforming for ${target.name} target build"
        group = CutterPlugin.GROUP
    }

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val archiveFile: RegularFileProperty

    @get:Nested
    abstract val replaceTokens: ReplaceTokensConfiguration
    fun replaceTokens(config: Action<ReplaceTokensConfiguration>) = config.execute(replaceTokens)
    fun replaceTokens(config: Closure<Unit>): Any = project.configure(replaceTokens, config)

    @get:OutputDirectory
    internal abstract val targetClassesDir: DirectoryProperty

    @get:OutputDirectory
    internal abstract val originalFilesDir: DirectoryProperty

    @TaskAction
    fun execute() {
        val archive = archiveFile.get().asFile.toPath()
        val classesDir = targetClassesDir.get().asFile.toPath().also { it.cleanDirectory() }
        val otherFilesDir = originalFilesDir.get().asFile.toPath().also { it.cleanDirectory() }

        val inputClasses = processJar(archive, otherFilesDir).associateTo(mutableMapOf()) {
            val classNode = ClassNode()
            val classReader = ClassReader(it.second)
            classReader.accept(classNode, 0)
            classNode.name to ClassFile(it.first, it.second, classNode)
        }

        val transformer = ClassTransformer(target, extension, replaceTokens, inputClasses)
        val outputClasses = transformer.process()

        val errors = transformer.validate()
        if (errors.isEmpty()) {
            outputClasses.forEach { classFile ->
                if (classFile.changed) {
                    val writer = ClassWriter(ClassWriter.COMPUTE_MAXS)
                    classFile.node.accept(writer)

                    val destPath = classesDir.resolve(classFile.path.toString())
                    destPath.parent.createDirectories()
                    destPath.writeBytes(writer.toByteArray())
                } else {
                    val destPath = otherFilesDir.resolve(classFile.path.toString())
                    destPath.parent.createDirectories()
                    destPath.writeBytes(classFile.originalBytes)
                }
            }
        } else {
            val details = errors.map { "$it\n" }
            throw GradleException("Validation is failed. Details:\n$details")
        }
    }

    private fun processJar(archive: Path, otherFilesDir: Path): MutableList<Pair<Path, ByteArray>> {
        val classesSpec = extension.packages.asSpec

        return FileSystems.newFileSystem(
            URI.create("jar:${archive.toUri()}"),
            mapOf("create" to "false", "encoding" to "UTF-8")
        ).use {
            val classes = mutableListOf<Pair<Path, ByteArray>>()
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

                override fun visitFileFailed(file: Path, ex: IOException) =
                    throw GradleException("Failed to visit file in jar $archive: $file", ex)

                override fun postVisitDirectory(dir: Path, ex: IOException?) = FileVisitResult.CONTINUE
            })

            classes
        }
    }

    private fun Path.isTargetClassFile(classesSpec: Spec<FileTreeElement>): Boolean {
        class ClassFileChecker(private val path: Path) : FileTreeElement {
            override fun getFile() = error("Not used")
            override fun isDirectory() = false
            override fun getLastModified() = error("Not used")
            override fun getSize() = error("Not used")
            override fun open() = error("Not used")
            override fun copyTo(output: OutputStream) = error("Not used")
            override fun copyTo(target: File) = error("Not used")
            override fun getName() = path.name
            override fun getPath() = path.toString()
            override fun getRelativePath() = RelativePath(true, *getPath().split("/").toTypedArray())
            override fun getMode() = error("Not used")
        }

        return extension == "class" && classesSpec.isSatisfiedBy(ClassFileChecker(this))
    }
}