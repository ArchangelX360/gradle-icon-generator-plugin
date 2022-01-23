package se.dorne.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileType
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.work.ChangeType
import org.gradle.work.Incremental
import org.gradle.work.InputChanges
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import se.dorne.LOG
import se.dorne.parser.Icon
import se.dorne.parser.extractBase64Icons
import java.io.File
import java.nio.file.Path
import javax.inject.Inject
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists

private const val defaultIconType = "String"

@CacheableTask
@OptIn(ExperimentalPathApi::class)
abstract class GeneratePngTask @Inject constructor(private val workerExecutor: WorkerExecutor) : DefaultTask() {

    @get:Incremental
    @get:InputFiles
    @get:PathSensitive(value = PathSensitivity.ABSOLUTE)
    abstract val sourceFiles: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:OutputDirectory
    abstract val stateOutputDir: DirectoryProperty

    @get:Input
    @get:Optional
    abstract val iconFieldType: Property<String>

    @TaskAction
    fun execute(inputChanges: InputChanges) {
        val outputFolder = outputDir.get()
        val stateOutputFolder = stateOutputDir.get()
        val iconFieldType = iconFieldType.orNull ?: defaultIconType

        val workQueue = workerExecutor.noIsolation()
        inputChanges.getFileChanges(sourceFiles)
            .asSequence()
            .onEach { LOG.debug("processing ${it.file.path}, ${it.changeType}") }
            .filter { it.fileType != FileType.DIRECTORY }
            .forEach { change ->
                // each file has isolated output so can be process in parallel
                workQueue.submit(
                    GenerateIconsAction::class.java
                ) {
                    this.stateOutputFolder.set(stateOutputFolder)
                    this.changeType.set(change.changeType)
                    this.changeFile.set(change.file)
                    this.outputFolder.set(outputFolder)
                    this.iconFieldType.set(iconFieldType)
                }
            }
        workQueue.await()
    }

    interface GenerateIconsActionParameters : WorkParameters {
        val stateOutputFolder: DirectoryProperty
        val changeType: Property<ChangeType>
        val changeFile: Property<File>
        val outputFolder: DirectoryProperty
        val iconFieldType: Property<String>
    }

    abstract class GenerateIconsAction : WorkAction<GenerateIconsActionParameters> {

        override fun execute() {
            val stateOutputFolder = parameters.stateOutputFolder.get()
            val changeType = parameters.changeType.get()
            val changeFile = parameters.changeFile.get()
            val outputFolder = parameters.outputFolder.get()
            val iconFieldType = parameters.iconFieldType.get()

            val state = State.resolve(stateOutputFolder, changeFile)
            when (changeType) {
                ChangeType.ADDED -> {
                    val iconPaths = changeFile.saveIcons(outputFolder, iconFieldType)
                    state.recordOutputs(iconPaths)
                }
                ChangeType.MODIFIED -> {
                    state.cleanOutputs() // we are going to reprocess all the outputs of this file
                    val iconPaths = changeFile.saveIcons(outputFolder, iconFieldType)
                    state.recordOutputs(iconPaths)
                }
                ChangeType.REMOVED -> {
                    state.cleanOutputs(cleanupStateFile = true)
                }
            }
        }

        private fun File.saveIcons(to: Directory, iconFieldType: String): List<File> {
            val icons = extractBase64Icons(this, iconFieldType)
            val outputToIcon = icons.associateBy {
                to.file(it.relativePath.toString()).asFile
            }
            outputToIcon.forEach { (outputFile, icon) -> icon.save(outputFile) }
            return outputToIcon.keys.toList()
        }

        private fun Icon.save(to: File) {
            to.toPath().parent.createDirectories()
            to.writeBytes(this.content)
        }
    }
}

@OptIn(ExperimentalPathApi::class)
private data class State(
    val file: File,
    var producedOutputs: List<String>,
) {

    fun cleanOutputs(cleanupStateFile: Boolean = false) {
        producedOutputs.forEach { Path.of(it).deleteIfExists() }
        if (cleanupStateFile) {
            file.toPath().deleteIfExists()
        } else {
            recordOutputs(emptyList())
        }
    }

    fun recordOutputs(outputs: List<File>) {
        producedOutputs = outputs.map { it.toPath().toString() }
        file.writeText(serialize())
    }

    private fun serialize() = producedOutputs.joinToString(serilizingSeparator)

    companion object {
        const val serilizingSeparator: String = "\n"

        private fun deserialize(stateFile: File): State = State(
            file = stateFile,
            producedOutputs = stateFile.readText().split(serilizingSeparator)
        )

        fun resolve(stateOutputFolder: Directory, inputFile: File): State {
            val stateFileName = generateStateFileName(inputFile)
            val stateFile = stateOutputFolder.file(stateFileName).asFile
            return if (stateFile.exists()) {
                deserialize(stateFile)
            } else {
                stateFile.toPath().parent.createDirectories()
                State(
                    file = stateFile,
                    producedOutputs = emptyList(),
                )
            }
        }

        private fun generateStateFileName(inputFile: File): String = inputFile.path.replace("/", "_")
    }
}
