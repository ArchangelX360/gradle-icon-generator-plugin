package io.github.archangelx360.tasks

import io.github.archangelx360.parser.Icon
import io.github.archangelx360.parser.extractBase64Icons
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.work.ChangeType
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import java.io.File
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createDirectories

interface GenerateIconsActionParameters : WorkParameters {
    val changeType: Property<ChangeType>
    val sourceFile: Property<File>
    val outputDirectory: DirectoryProperty
    val stateOutputDirectory: DirectoryProperty
    val projectDirectory: Property<File>
    val fieldType: Property<String>
}

@OptIn(ExperimentalPathApi::class)
abstract class GenerateIconsAction : WorkAction<GenerateIconsActionParameters> {

    override fun execute() {
        val changeType = parameters.changeType.get()
        val sourceFile = parameters.sourceFile.get()
        val outputDirectory = parameters.outputDirectory.get()
        val stateOutputDirectory = parameters.stateOutputDirectory.get()
        val iconFieldType = parameters.fieldType.get()
        val projectDirectory = parameters.projectDirectory.get()

        val stateFile = resolveStateFile(stateOutputDirectory, projectDirectory.toPath(), sourceFile.toPath())
        when (changeType) {
            ChangeType.ADDED, ChangeType.MODIFIED -> {
                val icons = extractBase64Icons(sourceFile, iconFieldType)
                    .associateBy { it.outputPath(outputDirectory) }
                // create/update the added/modified icons
                icons.forEach { (filepath, icon) -> icon.saveTo(filepath) }
                updateStateAndCleanUpStaleOutputs(outputDirectory.asFile.toPath(), stateFile, icons.keys)
            }
            ChangeType.REMOVED -> {
                updateStateAndCleanUpStaleOutputs(outputDirectory.asFile.toPath(), stateFile, emptySet())
            }
        }
    }

    private fun Icon.saveTo(filepath: Path) {
        filepath.parent.createDirectories()
        filepath.toFile().writeBytes(this.content)
    }

    private fun Icon.outputPath(outputDirectory: Directory): Path {
        val prefix = javaClassFullyQualifiedName.replace(".", "/")
        val outputRelativePath = Path.of(prefix, "$fieldName.png")
        return outputDirectory.file(outputRelativePath.toString()).asFile.toPath()
    }
}
