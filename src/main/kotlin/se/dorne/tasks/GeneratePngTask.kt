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

    @get:Input
    @get:Optional
    abstract val iconFieldType: Property<String>

    @TaskAction
    fun execute(inputChanges: InputChanges) {
        val outputFolder = outputDir.get()
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
                    this.changeType.set(change.changeType)
                    this.changeFile.set(change.file)
                    this.outputFolder.set(outputFolder)
                    this.iconFieldType.set(iconFieldType)
                    this.projectDir.set(project.projectDir)
                }
            }
        workQueue.await()
    }

    interface GenerateIconsActionParameters : WorkParameters {
        val changeType: Property<ChangeType>
        val changeFile: Property<File>
        val outputFolder: DirectoryProperty
        val iconFieldType: Property<String>
        val projectDir: Property<File>
    }

    abstract class GenerateIconsAction : WorkAction<GenerateIconsActionParameters> {

        override fun execute() {
            val changeType = parameters.changeType.get()
            val changeFile = parameters.changeFile.get()
            val outputFolder = parameters.outputFolder.get()
            val iconFieldType = parameters.iconFieldType.get()
            val projectDir = parameters.projectDir.get()

            val classDirectoryOutput = relativeDirectoryOutput(projectDir, changeFile.toPath())
            when (changeType) {
                ChangeType.ADDED -> {
                    extractBase64Icons(changeFile, iconFieldType)
                        .associateBy { it.outputPath(outputFolder, classDirectoryOutput) }
                        .forEach { (filepath, icon) -> icon.saveTo(filepath) }
                }
                ChangeType.MODIFIED -> {
                    Path.of(outputFolder.toString(), classDirectoryOutput.toString()).toFile().deleteRecursively()
                    extractBase64Icons(changeFile, iconFieldType)
                        .associateBy { it.outputPath(outputFolder, classDirectoryOutput) }
                        .forEach { (filepath, icon) -> icon.saveTo(filepath) }
                }
                ChangeType.REMOVED -> {
                    Path.of(outputFolder.toString(), classDirectoryOutput.toString()).toFile().deleteRecursively()
                }
            }
        }

        private fun relativeDirectoryOutput(projectDir: File, inputFile: Path): Path {
            val relativePath = projectDir.toPath().relativize(inputFile).toString().removeSuffix(".java")
            return Path.of(relativePath)
        }

        private fun Icon.saveTo(filepath: Path) {
            filepath.parent.createDirectories()
            filepath.toFile().writeBytes(this.content)
        }

        private fun Icon.outputPath(outputDirectory: Directory, relativePath: Path): Path {
            val fullyQualifiedNameOfTopLevelClassAsPath = this.fullyQualifiedNameOfTopLevelClass.replace(".", "/")
            val directoryStructure = relativePath.toString().split(fullyQualifiedNameOfTopLevelClassAsPath).first()

            val outputRelativePath = Path.of(
                directoryStructure,
                javaClassFullyQualifiedName.replace(".", "/"),
                "$fieldName.${extension}",
            )

            return outputDirectory.file(outputRelativePath.toString()).asFile.toPath()
        }
    }
}
