package se.dorne

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.register
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import se.dorne.tasks.GeneratePngTask

abstract class GeneratePngExtension {
    abstract val sources: ConfigurableFileCollection
    abstract val outputDirectory: DirectoryProperty

    abstract val javaFileIconSuffix: Property<String>
    abstract val iconFieldType: Property<String>
}

class IconGeneratorPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val sourceExtension = project.extensions.create<GeneratePngExtension>("generateIconsForSources")

        project.tasks.register<GeneratePngTask>("generateIcons") {
            group = "icons"

            javaFileIconSuffix.set(sourceExtension.javaFileIconSuffix)
            iconFieldType.set(sourceExtension.iconFieldType)
            sourceFiles.setFrom(sourceExtension.sources)
            val output = sourceExtension.outputDirectory.orNull ?: project.layout.buildDirectory.dir("icons").get()
            outputDir.set(output)
            stateOutputDir.set(project.layout.buildDirectory.dir("icon-states").get())
        }
    }
}

val LOG: Logger = LoggerFactory.getLogger("icon-generator-logger")
