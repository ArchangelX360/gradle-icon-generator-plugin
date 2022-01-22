package se.dorne.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileType
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.api.tasks.Optional
import org.gradle.work.ChangeType
import org.gradle.work.Incremental
import org.gradle.work.InputChanges
import org.gradle.workers.WorkerExecutor
import se.dorne.parser.Icon
import se.dorne.parser.extractBase64Icons
import java.io.File
import java.nio.file.Path
import java.util.*
import javax.inject.Inject
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists

private const val defaultJavaFileIconSuffix = "Icons.java"
private const val defaultIconType = "String"

abstract class GeneratePngTask : DefaultTask() {

    @get:Incremental
    @get:InputFiles
    abstract val sourceFiles: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:OutputDirectory
    abstract val stateOutputDir: DirectoryProperty

    @get:Input
    @get:Optional
    abstract val javaFileIconSuffix: Property<String>

    @get:Input
    @get:Optional
    abstract val iconVariableType: Property<String>

    private val base64Decoder = Base64.getDecoder()

    @OptIn(ExperimentalPathApi::class)
    @TaskAction
    fun execute(inputChanges: InputChanges) {
        val outputFolder = outputDir.get()
        val stateOutputFolder = stateOutputDir.get()
        val suffix = javaFileIconSuffix.orNull ?: defaultJavaFileIconSuffix
        val iconVariableType = iconVariableType.orNull ?: defaultIconType

        inputChanges.getFileChanges(sourceFiles)
            .asSequence()
            .onEach { println("processing ${it.file.path}, ${it.changeType}") }
            .filter { it.fileType != FileType.DIRECTORY }
            .filter { it.normalizedPath.endsWith(suffix) }
            .forEach { change ->
                val stateFile = stateOutputFolder.file(change.file.path.replace("/", "_")).asFile
                val state = State.resolve(stateFile)
                when (change.changeType) {
                    ChangeType.ADDED -> {
                        val iconPaths = saveIcons(change.file, outputFolder, iconVariableType)
                        state.recordOutputs(iconPaths)
                        state.save(stateFile)
                    }
                    ChangeType.MODIFIED -> {
                        state.cleanOutputs() // we are going to reprocess all the outputs of this file
                        val iconPaths = saveIcons(change.file, outputFolder, iconVariableType)
                        state.recordOutputs(iconPaths)
                        state.save(stateFile)
                    }
                    ChangeType.REMOVED -> {
                        state.cleanOutputs()
                        stateFile.toPath().deleteIfExists()
                    }
                }
            }
    }

    private fun saveIcons(ofFile: File, outputFolder: Directory, iconVariableType: String): List<File> {
        val icons = extractBase64Icons(base64Decoder, ofFile, iconVariableType)
        val outputToIcon = icons.associateBy {
            outputFolder.file(it.relativePath.toString()).asFile
        }
        outputToIcon.forEach { (outputFile, icon) -> icon.save(outputFile) }
        return outputToIcon.keys.toList()
    }

    @OptIn(ExperimentalPathApi::class)
    private fun Icon.save(to: File) {
        to.toPath().parent.createDirectories()
        to.writeBytes(this.content)
    }
}

private data class State(
    var producedOutputs: List<String> = listOf(),
) {

    @OptIn(ExperimentalPathApi::class)
    fun cleanOutputs() {
        producedOutputs.forEach { Path.of(it).deleteIfExists() }
    }

    fun recordOutputs(outputs: List<File>) {
        producedOutputs = outputs.map { it.toPath().toString() }
    }

    private fun serialize() = producedOutputs.joinToString(serilizingSeparator)

    fun save(to: File) {
        to.writeText(serialize())
    }

    companion object {
        const val serilizingSeparator: String = "\n"

        private fun deserialize(raw: String): State = State(producedOutputs = raw.split(serilizingSeparator))

        @OptIn(ExperimentalPathApi::class)
        fun resolve(f: File): State = if (f.exists()) {
            deserialize(f.readText())
        } else {
            f.toPath().parent.createDirectories()
            State()
        }
    }
}
