package se.dorne.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import se.dorne.parser.Base64Icon
import se.dorne.parser.extractBase64Icons
import java.io.File
import java.nio.file.Path
import java.util.*
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createDirectories

private const val iconSuffix = ".Icons.java"

abstract class GeneratePngTask : DefaultTask() {
    @get:InputFiles
    abstract val sourceDirectories: ListProperty<File> // TODO: should we use ConfigurableFileCollection instead?

    @get:OutputDirectory
    abstract val output: DirectoryProperty

    private val base64Decoder = Base64.getDecoder()

    @TaskAction
    fun deploy() {
        sourceDirectories
            .get()
            .flatMap { scanJavaIconFiles(it) }
            .flatMap { extractBase64Icons(base64Decoder, it) }
            .map { it.saveToPng(output.get().asFile.toPath()) }
    }

    private fun scanJavaIconFiles(directory: File): List<File> = directory
        .walkTopDown()
        .filter { it.path.endsWith(iconSuffix) }
        .toList()

    @OptIn(ExperimentalPathApi::class)
    private fun Base64Icon.saveToPng(outputFolder: Path) {
        val pngFilepath = Path.of(outputFolder.toString(), this.name.toString())
        pngFilepath.parent.createDirectories()
        pngFilepath.toFile().writeBytes(this.image)
    }
}
