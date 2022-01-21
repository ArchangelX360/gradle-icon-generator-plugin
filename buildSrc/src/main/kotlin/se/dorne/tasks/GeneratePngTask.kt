package se.dorne.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileType
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.api.tasks.Optional
import org.gradle.work.ChangeType
import org.gradle.work.Incremental
import org.gradle.work.InputChanges
import se.dorne.parser.Icon
import se.dorne.parser.extractBase64Icons
import java.nio.file.Path
import java.util.*
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
        val outputPath = outputDir.asFile.get().toPath()
        val suffix = javaFileIconSuffix.orNull ?: defaultJavaFileIconSuffix
        val iconVariableType = iconVariableType.orNull ?: defaultIconType

        inputChanges.getFileChanges(sourceFiles)
            .asSequence()
            .onEach { println("processing ${it.file.path}, ${it.changeType}") }
            .filter { it.fileType != FileType.DIRECTORY }
            .filter { it.normalizedPath.endsWith(suffix) }
            .forEach { change ->
                when (change.changeType) {
                    ChangeType.ADDED -> {
                        val icons = extractBase64Icons(base64Decoder, change.file, iconVariableType)
                        icons.forEach { it.saveToPng(outputPath) }
                    }
                    ChangeType.MODIFIED -> {
                        val icons = extractBase64Icons(base64Decoder, change.file, iconVariableType)
                        // FIXME: delete also the ones that should not be here
                        icons.forEach { it.saveToPng(outputPath) }
                    }
                    ChangeType.REMOVED -> {
                        val icons = extractBase64Icons(base64Decoder, change.file, iconVariableType)
                        icons.forEach { it.name.deleteIfExists() }
                    }
                }
            }
    }

    @OptIn(ExperimentalPathApi::class)
    private fun Icon.saveToPng(outputFolder: Path) {
        val pngFilepath = Path.of(outputFolder.toString(), this.name.toString())
        pngFilepath.parent.createDirectories()
        pngFilepath.toFile().writeBytes(this.content)
    }
}
