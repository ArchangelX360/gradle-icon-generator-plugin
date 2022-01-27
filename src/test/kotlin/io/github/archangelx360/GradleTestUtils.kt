package io.github.archangelx360

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertNotNull

data class ProjectConfiguration(
    val sourceDir: String,
    val outputDir: String? = null,
    val pattern: String? = null,

    val enabledCaching: Boolean = true,
    val enableParallelTaskExecution: Boolean = true,
)

/**
 * Creates a temporary project directory
 *
 * Temporary project is configured with:
 *  - sources from the test resource directory of name [projectName] (copied recursively)
 *  - a `settings.gradle.kts` file configured with an isolated cache directory to allow test isolation
 *  - a `build.gradle.kts` file configuring the `icon-generator-plugin` with configuration from [configuration]
 *  - a `gradle.properties` file configured with some parameters of the [configuration]
 */
fun getTemporaryProjectDirectory(projectName: String, configuration: ProjectConfiguration): File {
    val projectDirectory = getProjectDirectory(projectName)

    val tmpDirPath = Files.createTempDirectory(projectName)
    val tmpDir = tmpDirPath.toFile()
    tmpDir.deleteOnExit()
    projectDirectory.copyRecursively(tmpDir, overwrite = true)

    val cacheDirPath = Path.of(tmpDirPath.toString(), "build-cache")
    cacheDirPath.toFile().deleteOnExit()

    createGradleSettingsFile(tmpDirPath, cacheDirPath, projectName)
    createGradleBuildFile(tmpDirPath, configuration.sourceDir, configuration.pattern, configuration.outputDir)
    createGradlePropertiesFile(
        tmpDirPath,
        enableCaching = configuration.enabledCaching,
        enableParallelExecution = configuration.enableParallelTaskExecution
    )
    return tmpDir
}

private fun createGradleBuildFile(
    projectDirectory: Path,
    sourceDir: String,
    pattern: String? = null,
    outputDir: String? = null,
) {
    val filePatternConfigurationBlock = optionalConfigurationBlock(pattern) {
        """
            patternFilterable.set(
                PatternSet()
                    .include("$it")
            )
        """.trimIndent()
    }
    val outputDirConfigurationBlock = optionalConfigurationBlock(outputDir) {
        """
            outputDirectory.set(
                project.layout.buildDirectory.dir("$it")
            )
        """.trimIndent()
    }

    val content = """
            plugins {
                id("io.github.archangelx360.icon-generator")
                id("java")
            }

            generateIconsForSources {
                sources.setFrom(
                    project.layout.projectDirectory.dir("$sourceDir")
                )
                $filePatternConfigurationBlock
                $outputDirConfigurationBlock
            }
        """.trimIndent()
    saveInProject(projectDirectory, "build.gradle.kts", content.toByteArray())
}

private fun optionalConfigurationBlock(value: String?, block: (value: String) -> String) =
    if (value != null && value.isNotBlank()) {
        block(value)
    } else {
        ""
    }

private fun createGradlePropertiesFile(
    projectDirectory: Path,
    enableCaching: Boolean,
    enableParallelExecution: Boolean
) {
    val content = """
           org.gradle.caching=${enableCaching}
           org.gradle.parallel=${enableParallelExecution}
        """.trimIndent()
    saveInProject(projectDirectory, "gradle.properties", content.toByteArray())
}

private fun createGradleSettingsFile(projectDirectory: Path, buildCacheDirectory: Path, projectName: String) {
    val content = """
          rootProject.name = "$projectName"

          buildCache {
              local {
                  directory = "${buildCacheDirectory.toUri()}"
              }
          }
        """.trimIndent()
    saveInProject(projectDirectory, "settings.gradle.kts", content.toByteArray())
}

private fun saveInProject(projectDirectory: Path, filename: String, content: ByteArray) {
    val gradleBuildKts = Path.of(projectDirectory.toString(), filename).toFile()
    gradleBuildKts.writeBytes(content)
}

private fun getProjectDirectory(projectName: String): File {
    val project = Path.of("src", "test", "resources", projectName).toFile()
    assertNotNull(project, "could not find project '$project' in test resources")
    return project
}
