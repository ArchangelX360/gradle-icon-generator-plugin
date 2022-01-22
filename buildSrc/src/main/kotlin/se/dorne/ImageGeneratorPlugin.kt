package se.dorne

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.register
import se.dorne.tasks.GeneratePngTask

abstract class GeneratePngExtension {
    abstract val sourceDirectories: ConfigurableFileCollection
    abstract val outputDirectory: DirectoryProperty

    abstract val javaFileIconSuffix: Property<String>
    abstract val iconVariableType: Property<String>
}

class ImageGeneratorPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val sourceExtension = project.extensions.create<GeneratePngExtension>("generatePngForSources")

        project.tasks.register<GeneratePngTask>("generatePngs") {
            group = "icons"

            javaFileIconSuffix.set(sourceExtension.javaFileIconSuffix)
            iconVariableType.set(sourceExtension.iconVariableType)
            sourceFiles.setFrom(sourceExtension.sourceDirectories)
            val output = sourceExtension.outputDirectory.orNull ?: project.layout.buildDirectory.dir("icons").get()
            outputDir.set(output)
            stateOutputDir.set(project.layout.buildDirectory.dir("icon-states").get())
        }
    }
}
