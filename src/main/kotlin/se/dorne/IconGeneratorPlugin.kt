package se.dorne

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.util.PatternFilterable
import org.gradle.api.tasks.util.PatternSet
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.register
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import se.dorne.tasks.GeneratePngTask

abstract class GeneratePngExtension {
    abstract val sources: ConfigurableFileCollection
    abstract val patternFilterable: Property<PatternFilterable>
    abstract val outputDirectory: DirectoryProperty
    abstract val iconFieldType: Property<String>

    abstract val internalStateDirectory: DirectoryProperty
}

private val defaultFilePattern = PatternSet().include("**/*Icons.java")
private const val defaultIconDirectoryOutput = "icons"
private const val defaultStateStoreDirectory = "icon-generator/icon-states"
private const val defaultIconType = "String"

class IconGeneratorPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val sourceExtension = project.extensions.create<GeneratePngExtension>("generateIconsForSources")

        val generateTasks = project.tasks.register<GeneratePngTask>("generateIcons") {
            group = "icons"
            description = "generate icons of sources configured through `generateIconsForSources` extension"

            // the pattern filtering here avoid time-consuming "fingerprinting" step for the very first build
            val pattern = sourceExtension.patternFilterable.orNull ?: defaultFilePattern
            sourceFiles.setFrom(sourceExtension.sources.asFileTree.matching(pattern))

            val fieldType = sourceExtension.iconFieldType.orNull ?: defaultIconType
            iconFieldType.set(fieldType)

            val output = sourceExtension.outputDirectory.orNull
                ?: project.layout.buildDirectory.dir(defaultIconDirectoryOutput).get()
            outputDir.set(output)

            val stateOutput = sourceExtension.internalStateDirectory.orNull
                ?: project.layout.buildDirectory.dir(defaultStateStoreDirectory).get()
            stateOutputDir.set(stateOutput)

            if (output.toString().startsWith(stateOutput.toString())) {
                error("`outputDirectory` cannot use a directory $stateOutput or any of its subdirectories, please choose output directory or set `internalStateDirectory`")
            }
        }

        project.tasks.register<Delete>("cleanIcons") {
            group = "icons"
            description = "cleanup the outputs of `generateIcons` task"

            delete(generateTasks.get().outputs)
        }
    }
}

val LOG: Logger = LoggerFactory.getLogger("icon-generator-logger")
