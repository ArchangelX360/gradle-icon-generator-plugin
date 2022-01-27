package io.github.archangelx360.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileType
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.work.Incremental
import org.gradle.work.InputChanges
import org.gradle.workers.WorkerExecutor
import javax.inject.Inject

@CacheableTask
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
    abstract val iconFieldType: Property<String>

    @TaskAction
    fun execute(inputChanges: InputChanges) {
        val workQueue = workerExecutor.noIsolation()
        inputChanges.getFileChanges(sourceFiles)
            .forEach { change ->
                if (change.fileType == FileType.DIRECTORY) return@forEach

                // each file has isolated output so can be process in parallel
                workQueue.submit(
                    GenerateIconsAction::class.java
                ) {
                    this.changeType.set(change.changeType)
                    this.sourceFile.set(change.file)
                    this.outputDirectory.set(outputDir)
                    this.stateOutputDirectory.set(stateOutputDir)
                    this.fieldType.set(iconFieldType)
                }
            }
    }
}
