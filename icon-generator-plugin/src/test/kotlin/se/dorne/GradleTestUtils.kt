package se.dorne

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertNotNull

data class ProjectConfiguration(
    val sourceDir: String,
    val pattern: String? = null,

    val enabledCaching: Boolean = true,
    val enableParallelTaskExecution: Boolean = true,
)

/**
 * Creates a temporary project directory
 *
 * Temporary project is configured with:
 *  - sources from the test resource directory of name projectName (copied recursively)
 *  - a `settings.gradle.kts` file configured with an isolated cache directory to allow test isolation
 *  - a `build.gradle.kts` file configuring the `icon-generator-plugin` with configuration from ProjectConfiguration configuration
 *  - a `gradle.properties` file configured with some parameters of the ProjectConfiguration configuration
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
    createGradleBuildFile(tmpDirPath, configuration.sourceDir, configuration.pattern)
    createGradlePropertiesFile(
        tmpDirPath,
        enableCaching = configuration.enabledCaching,
        enableParallelExecution = configuration.enableParallelTaskExecution
    )
    return tmpDir
}

private fun createGradleBuildFile(projectDirectory: Path, sourceDir: String, pattern: String? = null) {
    val filepatternConfigurationBlock = if (pattern != null && pattern.isNotBlank()) {
        """
            patternFilterable.set(
                PatternSet()
                    .include("$pattern")
            )
        """.trimIndent()
    } else {
        null
    }

    val content = """
            plugins {
                id("icon-generator-plugin")
                id("java")
            }

            generateIconsForSources {
                sources.setFrom(
                    project.layout.projectDirectory.dir("$sourceDir")
                )
                ${filepatternConfigurationBlock ?: ""}
            }
        """.trimIndent()
    saveInProject(projectDirectory, "build.gradle.kts", content.toByteArray())
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
