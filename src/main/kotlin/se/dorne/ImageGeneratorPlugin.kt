package se.dorne

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.ListProperty
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.register
import se.dorne.tasks.GeneratePngTask
import java.io.File

abstract class SourceSetExtension {
    abstract val directories: ListProperty<File>
}

class ImageGeneratorPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val sourceExtension = project.extensions.create<SourceSetExtension>("generatePngForSources")

        project.tasks.register<GeneratePngTask>("generatePngs") {
            group = "generate"

            sourceDirectories.set(sourceExtension.directories)
            output.set(project.buildDir) // FIXME: should be another path
        }
    }
}
