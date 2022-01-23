package se.dorne

import org.apache.xerces.impl.dv.util.Base64
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Path
import kotlin.test.assertEquals

internal class IconGeneratorPluginTest {
    private val generateIconTaskName = "generateIcons"

    @Test
    fun `should generate icons with proper content`() {
        val projectDirectory = getTemporaryProjectDirectory(
            "example-project-tiny", ProjectConfiguration(
                sourceDir = "src",
            )
        )

        val result = GradleRunner.create()
            .withProjectDir(projectDirectory)
            .withPluginClasspath() // make `icon-generator-plugin` available
            .withArguments(generateIconTaskName)
            .build()
        assertEquals(TaskOutcome.SUCCESS, result.task(":${generateIconTaskName}")?.outcome)

        val expected = mapOf(
            "${projectDirectory}/build/icons/foo/AIcons/AIcon.png" to "iVBORw0KGgoAAAANSUhEUgAAABgAAAAYCAYAAADgdz34AAAABmJLR0QA/wD/AP+gvaeTAAAAeElEQVRIiWNgGAVDHXTQ2oL/tLbkP60t+c9AY0tghv+G0g34FB9FcxEpOJwYS8g1/D9UfwiSJViDC1kxuQDDEiYKDUQHjEjs7+iSlAZRKJLr67HZfpgCC4iKZHIAScmUEgtwBgu1LKCJ4TALaGY4AwMNwnwUUB8AAGoAZWQIwMYBAAAAAElFTkSuQmCC",
        )
        assertEquals(expected, fetchGeneratedIconsWithContent(projectDirectory.toPath()))
    }

    @Test
    fun `should retrieve icons from cache`() {
        val projectDirectory = getTemporaryProjectDirectory(
            "example-project-tiny", ProjectConfiguration(
                sourceDir = "src",
            )
        )

        val runnerBuilder = GradleRunner.create()
            .withProjectDir(projectDirectory)
            .withPluginClasspath() // make `icon-generator-plugin` available

        val initialRun = runnerBuilder
            .withArguments(generateIconTaskName)
            .build()
        assertEquals(TaskOutcome.SUCCESS, initialRun.task(":${generateIconTaskName}")?.outcome)

        val expected = setOf(
            "${projectDirectory}/build/icons/foo/AIcons/AIcon.png",
        )
        assertEquals(expected, fetchGeneratedIconsPaths(projectDirectory.toPath()))

        // Delete previously generated output
        fetchGeneratedIcons(projectDirectory.toPath()).forEach { it.delete() }

        val cachedRun = runnerBuilder
            .withArguments(generateIconTaskName)
            .build()
        assertEquals(TaskOutcome.FROM_CACHE, cachedRun.task(":${generateIconTaskName}")?.outcome)
        assertEquals(expected, fetchGeneratedIconsPaths(projectDirectory.toPath()))
    }

    @Test
    fun `should re-run icons generation if cache is disabled`() {
        val projectDirectory = getTemporaryProjectDirectory(
            "example-project-tiny", ProjectConfiguration(
                sourceDir = "src",
                enabledCaching = false
            )
        )

        val runnerBuilder = GradleRunner.create()
            .withProjectDir(projectDirectory)
            .withPluginClasspath() // make `icon-generator-plugin` available

        val initialRun = runnerBuilder
            .withArguments(generateIconTaskName)
            .build()
        assertEquals(TaskOutcome.SUCCESS, initialRun.task(":${generateIconTaskName}")?.outcome)

        val expected = setOf(
            "${projectDirectory}/build/icons/foo/AIcons/AIcon.png",
        )
        assertEquals(expected, fetchGeneratedIconsPaths(projectDirectory.toPath()))

        // Delete previously generated output
        fetchGeneratedIcons(projectDirectory.toPath()).forEach { it.delete() }

        val uncachedRun = runnerBuilder
            .withArguments(generateIconTaskName)
            .build()
        assertEquals(TaskOutcome.SUCCESS, uncachedRun.task(":${generateIconTaskName}")?.outcome)
        assertEquals(expected, fetchGeneratedIconsPaths(projectDirectory.toPath()))
    }


    @Test
    fun `should have up-to-date icons generation if nothing changed`() {
        val projectDirectory = getTemporaryProjectDirectory(
            "example-project-tiny", ProjectConfiguration(
                sourceDir = "src",
            )
        )

        val runnerBuilder = GradleRunner.create()
            .withProjectDir(projectDirectory)
            .withPluginClasspath() // make `icon-generator-plugin` available

        val result = runnerBuilder
            .withArguments(generateIconTaskName)
            .build()
        assertEquals(TaskOutcome.SUCCESS, result.task(":${generateIconTaskName}")?.outcome)

        val expected = setOf(
            "${projectDirectory}/build/icons/foo/AIcons/AIcon.png",
        )
        assertEquals(expected, fetchGeneratedIconsPaths(projectDirectory.toPath()))

        val nothingChangedRun = runnerBuilder
            .withArguments(generateIconTaskName)
            .build()
        assertEquals(TaskOutcome.UP_TO_DATE, nothingChangedRun.task(":${generateIconTaskName}")?.outcome)
        assertEquals(expected, fetchGeneratedIconsPaths(projectDirectory.toPath()))
    }

    @Test
    fun `should generate icons for more complex project structure`() {
        val projectDirectory = getTemporaryProjectDirectory(
            "example-project-minimal", ProjectConfiguration(
                sourceDir = "src",
            )
        )

        val runnerBuilder = GradleRunner.create()
            .withProjectDir(projectDirectory)
            .withPluginClasspath() // make `icon-generator-plugin` available

        val result = runnerBuilder
            .withArguments(generateIconTaskName)
            .build()
        assertEquals(TaskOutcome.SUCCESS, result.task(":${generateIconTaskName}")?.outcome)

        val expected = setOf(
            "${projectDirectory}/build/icons/foo/OtherIcons/DIcon.png",
            "${projectDirectory}/build/icons/foo/OtherIcons/CIcon.png",
            "${projectDirectory}/build/icons/foo/OtherIcons/BIcon.png",
            "${projectDirectory}/build/icons/foo/AIcons/AIcon.png",
            "${projectDirectory}/build/icons/foo/bar/AIcons/AIcon.png",
            "${projectDirectory}/build/icons/bar/AIcons/AIcon.png",
            "${projectDirectory}/build/icons/foo/ParentIcons/Nested/DIcon.png",
            "${projectDirectory}/build/icons/foo/ParentIcons/Nested/CIcon.png",
            "${projectDirectory}/build/icons/foo/ParentIcons/Nested/BIcon.png",
            "${projectDirectory}/build/icons/foo/ParentIcons/AIcon.png",
            "${projectDirectory}/build/icons/foo/SiblingIcons/AIcon.png",
        )
        assertEquals(expected, fetchGeneratedIconsPaths(projectDirectory.toPath()))
    }

    @Test
    fun `should generate icons only for filtered file patterns`() {
        val projectDirectory = getTemporaryProjectDirectory(
            "example-project-minimal", ProjectConfiguration(
                sourceDir = "src",
                pattern = "**/*AIcons.java",
            )
        )

        val runnerBuilder = GradleRunner.create()
            .withProjectDir(projectDirectory)
            .withPluginClasspath() // make `icon-generator-plugin` available

        val result = runnerBuilder
            .withArguments(generateIconTaskName)
            .build()
        assertEquals(TaskOutcome.SUCCESS, result.task(":${generateIconTaskName}")?.outcome)

        val expected = setOf(
            "${projectDirectory}/build/icons/foo/AIcons/AIcon.png",
            "${projectDirectory}/build/icons/foo/bar/AIcons/AIcon.png",
            "${projectDirectory}/build/icons/bar/AIcons/AIcon.png",
        )
        assertEquals(expected, fetchGeneratedIconsPaths(projectDirectory.toPath()))
    }


    @Test
    fun `should generate 0 icon if no file is matched by the specified pattern`() {
        val projectDirectory = getTemporaryProjectDirectory(
            "example-project-minimal", ProjectConfiguration(
                sourceDir = "src",
                pattern = "**/*some-unmatching-pattern.java",
            )
        )

        val runnerBuilder = GradleRunner.create()
            .withProjectDir(projectDirectory)
            .withPluginClasspath() // make `icon-generator-plugin` available

        val result = runnerBuilder
            .withArguments(generateIconTaskName)
            .build()
        assertEquals(TaskOutcome.SUCCESS, result.task(":${generateIconTaskName}")?.outcome)
        assertEquals(emptySet<String>(), fetchGeneratedIconsPaths(projectDirectory.toPath()))
    }

    private fun fetchGeneratedIconsPaths(projectDirectory: Path) = fetchGeneratedIcons(projectDirectory)
        .map { it.path }
        .toSet()

    private fun fetchGeneratedIconsWithContent(projectDirectory: Path) = fetchGeneratedIcons(projectDirectory)
        .associate { it.path to Base64.encode(it.readBytes()) }

    private fun fetchGeneratedIcons(projectDirectory: Path): Set<File> {
        val iconOutputDirectory = Path.of(projectDirectory.toString(), "build", "icons").toFile()
        return iconOutputDirectory.walkTopDown()
            .filter { it.isFile }
            .toSet()
    }
}
