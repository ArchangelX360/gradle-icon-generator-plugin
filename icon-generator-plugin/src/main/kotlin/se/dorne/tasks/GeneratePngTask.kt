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
                    // `it` is unresolved here, due to a bug related to https://youtrack.jetbrains.com/issue/KTIJ-14684
                    // if you are on IntelliJ IDEA, don't mind the red colours, the code actually compiles and does the
                    // right thing
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
                ChangeType.ADDED, ChangeType.MODIFIED -> {
                    val icons = extractBase64Icons(changeFile, iconFieldType)
                        .associateBy { it.outputPath(outputFolder) }
                    // create/update the added/modified icons
                    icons.forEach { (filepath, icon) -> icon.saveTo(filepath) }
                    // update state and cleans up stale icons
                    state.updateState(icons.keys)
                }
                ChangeType.REMOVED -> {
                    state.updateState(emptySet())
                }
            }
        }

        private fun Icon.saveTo(filepath: Path) {
            filepath.parent.createDirectories()
            filepath.toFile().writeBytes(this.content)
        }

        private fun Icon.outputPath(outputDirectory: Directory): Path {
            val prefix = javaClassFullyQualifiedName.replace(".", "/")
            val outputRelativePath = Path.of(prefix, "$fieldName.${extension}")
            return outputDirectory.file(outputRelativePath.toString()).asFile.toPath()
        }
    }
}

@OptIn(ExperimentalPathApi::class)
private data class State(
    private val file: File,
) {
    private val serializingSeparator: String = "\n"

    /**
     * Updates the state of the [file] and cleans up stale icons from previous run.
     *
     * An icon is considered stale if it is in the current state (before the update) but not in the [newOutputs].
     * If all the icons are stale, then the state file will be cleaned up as well to prevent garbage to pile up in the
     * build directory.
     */
    fun updateState(newOutputs: Set<Path>) {
        val oldOutputs = deserialize()
        val staleIcons = oldOutputs.subtract(newOutputs)
        staleIcons.forEach { it.deleteIfExists() }
        if (newOutputs.isNotEmpty()) {
            file.toPath().parent.createDirectories()
            file.writeText(serialize(newOutputs))
        } else {
            file.toPath().deleteIfExists()
        }
    }

    private fun deserialize(): Set<Path> {
        if (file.exists()) {
            val content = file.readText()
            if (content.isBlank()) {
                return emptySet()
            }
            return content.split(serializingSeparator).map { Path.of(it) }.toSet()
        }
        return emptySet()
    }

    private fun serialize(outputs: Set<Path>) = outputs.joinToString(serializingSeparator)

    companion object {

        /**
         * Instantiate a State for the source file [inputFile], declaring it in the [stateOutputFolder] directory
         */
        fun resolve(stateOutputFolder: Directory, inputFile: File): State {
            val stateFileName = generateStateFileName(inputFile)
            val stateFile = stateOutputFolder.file(stateFileName).asFile
            return State(file = stateFile)
        }

        private fun generateStateFileName(inputFile: File): String = inputFile.path.replace("/", "_")
    }
}
