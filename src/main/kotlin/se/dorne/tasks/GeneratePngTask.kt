package se.dorne.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.jboss.forge.roaster.Roaster
import org.jboss.forge.roaster.model.JavaClass
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

    @TaskAction
    fun deploy() {
        val outputFolder = project.mkdir(output.get())

        sourceDirectories
            .get()
            .flatMap { scanJavaIconFiles(it) }
            .flatMap { extractBase64Icons(outputFolder, it) }
            .onEach { println(it) }
            .map { it.saveToPng() }
    }

    private fun scanJavaIconFiles(directory: File): List<File> = directory
        .walkTopDown()
        .filter { it.path.endsWith(iconSuffix) }
        .toList()

    // TODO: this function needs to be re-organised
    private fun extractBase64Icons(outputFolder: File, javaFile: File): List<Base64Icon> {
        val file = Roaster.parse(javaFile)
        if (file.isClass) {
            val classFile = file as JavaClass
            return classFile.fields
                .filter { it.type.isType(String::class.java) }
                .map {
                    Base64Icon(
                        data = it.stringInitializer,
                        // FIXME: revise this
                        destination = Path.of(
                            outputFolder.toString(),
                            file.getPackage(),
                            file.getName(),
                            "${it.name}.png"
                        ),
                    )
                }
            // TODO: support nested classes with `classFile.nestedTypes`
        } else {
            // TODO: rework this error message
            error("Java File must have a class")
        }
    }

    @OptIn(ExperimentalPathApi::class)
    private fun Base64Icon.saveToPng() {
        // FIXME: for now, every string will be tried, but we need to either filter the base64 ones, or handle failure properly here
        val image = Base64.getDecoder().decode(this.data)
        this.destination.parent.createDirectories()
        val f = this.destination.toFile()
        f.writeBytes(image)
    }
}

private data class Base64Icon(
    val data: String,
    val destination: Path,
)
