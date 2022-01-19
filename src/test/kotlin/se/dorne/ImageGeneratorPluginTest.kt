package se.dorne

import org.gradle.kotlin.dsl.the
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Path
import java.util.*
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.exists
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

internal class ImageGeneratorPluginTest {
    @OptIn(ExperimentalPathApi::class)
    @Test
    fun `should generate png for minimal project`() {
        val projectName = "example-projects/minimal-project"
        val minimalProjectDirectory = getProjectDirectory(projectName)
        assertNotNull(minimalProjectDirectory, "could not find project '$projectName' in test resources")

        val project = ProjectBuilder.builder()
            .withProjectDir(minimalProjectDirectory)
            .build()

        project.pluginManager.apply(ImageGeneratorPlugin::class.java)
        project.the(SourceSetExtension::class).directories.set(
            listOf(
                project.layout.projectDirectory.dir("src/main/java").asFile
            )
        )

        val task = project.tasks.getByName("generatePngs")
        assertNotNull(task)

        task.actions.forEach { it.execute(task) }
        val expectedPngPath = Path.of(project.buildDir.path, "foo", "A", "AIcon.png")
        assertTrue(expectedPngPath.exists())
        val actualBase64 = Base64.getEncoder().encodeToString(expectedPngPath.toFile().readBytes())
        assertEquals(
            "iVBORw0KGgoAAAANSUhEUgAAABgAAAAYCAYAAADgdz34AAAABmJLR0QA/wD/AP+gvaeTAAAAeElEQVRIiWNgGAVDHXTQ2oL/tLbkP60t+c9AY0tghv+G0g34FB9FcxEpOJwYS8g1/D9UfwiSJViDC1kxuQDDEiYKDUQHjEjs7+iSlAZRKJLr67HZfpgCC4iKZHIAScmUEgtwBgu1LKCJ4TALaGY4AwMNwnwUUB8AAGoAZWQIwMYBAAAAAElFTkSuQmCC",
            actualBase64
        )
    }

    private fun getProjectDirectory(projectName: String): File =
        Path.of("src", "test", "resources", projectName).toFile()
}
