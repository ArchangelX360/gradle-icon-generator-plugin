package io.github.archangelx360

import org.apache.xerces.impl.dv.util.Base64
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.testkit.runner.UnexpectedBuildFailure
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import kotlin.test.Ignore
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class IconGeneratorPluginTest {
    private val generateIconTaskName = "generateIcons"

    @Test
    fun `should generate icons with proper content`() {
        val projectDirectory = getTemporaryProjectDirectory(
            "example-project-tiny", ProjectConfiguration(
                sourceDir = "src",
            )
        )

        val initialRun = GradleRunner.create()
            .withProjectDir(projectDirectory)
            .withPluginClasspath() // make `io.github.archangelx360.icon-generator` available
            .withArguments(generateIconTaskName)
            .build()
        assertEquals(TaskOutcome.SUCCESS, initialRun.task(":${generateIconTaskName}")?.outcome)

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
            .withPluginClasspath() // make `io.github.archangelx360.icon-generator` available

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
            .withPluginClasspath() // make `io.github.archangelx360.icon-generator` available

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
            .withPluginClasspath() // make `io.github.archangelx360.icon-generator` available

        val initialRun = runnerBuilder
            .withArguments(generateIconTaskName)
            .build()
        assertEquals(TaskOutcome.SUCCESS, initialRun.task(":${generateIconTaskName}")?.outcome)

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
            .withPluginClasspath() // make `io.github.archangelx360.icon-generator` available

        val initialRun = runnerBuilder
            .withArguments(generateIconTaskName)
            .build()
        assertEquals(TaskOutcome.SUCCESS, initialRun.task(":${generateIconTaskName}")?.outcome)

        val expected = setOf(
            "${projectDirectory}/build/icons/foo/AIcons/AIcon.png",
            "${projectDirectory}/build/icons/foo/bar/AIcons/AIcon.png",
            "${projectDirectory}/build/icons/bar/AIcons/AIcon.png",
            "${projectDirectory}/build/icons/foo/ParentIcons/Nested/DIcon.png",
            "${projectDirectory}/build/icons/foo/ParentIcons/Nested/CIcon.png",
            "${projectDirectory}/build/icons/foo/ParentIcons/Nested/BIcon.png",
            "${projectDirectory}/build/icons/foo/ParentIcons/AIcon.png",
            "${projectDirectory}/build/icons/foo/SiblingIcons/AIcon.png",
            "${projectDirectory}/build/icons/foo/OtherIcons/BIcon.png",
            "${projectDirectory}/build/icons/foo/OtherIcons/CIcon.png",
            "${projectDirectory}/build/icons/foo/OtherIcons/DIcon.png",
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
            .withPluginClasspath() // make `io.github.archangelx360.icon-generator` available

        val initialRun = runnerBuilder
            .withArguments(generateIconTaskName)
            .build()
        assertEquals(TaskOutcome.SUCCESS, initialRun.task(":${generateIconTaskName}")?.outcome)

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
            .withPluginClasspath() // make `io.github.archangelx360.icon-generator` available

        val initialRun = runnerBuilder
            .withArguments(generateIconTaskName)
            .build()
        assertEquals(TaskOutcome.SUCCESS, initialRun.task(":${generateIconTaskName}")?.outcome)
        assertEquals(emptySet<String>(), fetchGeneratedIconsPaths(projectDirectory.toPath()))
    }

    @Test
    fun `should cleanup icons of deleted source file`() {
        val projectDirectory = getTemporaryProjectDirectory(
            "example-project-minimal", ProjectConfiguration(
                sourceDir = "src",
            )
        )

        val runnerBuilder = GradleRunner.create()
            .withProjectDir(projectDirectory)
            .withPluginClasspath() // make `io.github.archangelx360.icon-generator` available

        val initialRun = runnerBuilder
            .withArguments(generateIconTaskName)
            .build()
        assertEquals(TaskOutcome.SUCCESS, initialRun.task(":${generateIconTaskName}")?.outcome)

        val expected = setOf(
            "${projectDirectory}/build/icons/foo/AIcons/AIcon.png",
            "${projectDirectory}/build/icons/foo/bar/AIcons/AIcon.png",
            "${projectDirectory}/build/icons/bar/AIcons/AIcon.png",
            "${projectDirectory}/build/icons/foo/ParentIcons/Nested/DIcon.png",
            "${projectDirectory}/build/icons/foo/ParentIcons/Nested/CIcon.png",
            "${projectDirectory}/build/icons/foo/ParentIcons/Nested/BIcon.png",
            "${projectDirectory}/build/icons/foo/ParentIcons/AIcon.png",
            "${projectDirectory}/build/icons/foo/SiblingIcons/AIcon.png",
            "${projectDirectory}/build/icons/foo/OtherIcons/BIcon.png",
            "${projectDirectory}/build/icons/foo/OtherIcons/CIcon.png",
            "${projectDirectory}/build/icons/foo/OtherIcons/DIcon.png",
        )
        assertEquals(expected, fetchGeneratedIconsPaths(projectDirectory.toPath()))

        // Delete some sources
        val deleted = File("${projectDirectory}/src/main/java/foo/ParentIcons.java").delete()
        assertTrue(deleted)

        val secondRun = runnerBuilder
            .withArguments(generateIconTaskName)
            .build()
        assertEquals(TaskOutcome.SUCCESS, secondRun.task(":${generateIconTaskName}")?.outcome)

        val expectedAfterDeletion = setOf(
            "${projectDirectory}/build/icons/foo/AIcons/AIcon.png",
            "${projectDirectory}/build/icons/foo/bar/AIcons/AIcon.png",
            "${projectDirectory}/build/icons/bar/AIcons/AIcon.png",
            "${projectDirectory}/build/icons/foo/SiblingIcons/AIcon.png",
            "${projectDirectory}/build/icons/foo/OtherIcons/BIcon.png",
            "${projectDirectory}/build/icons/foo/OtherIcons/CIcon.png",
            "${projectDirectory}/build/icons/foo/OtherIcons/DIcon.png",
        )
        assertEquals(expectedAfterDeletion, fetchGeneratedIconsPaths(projectDirectory.toPath()))
    }

    @Test
    fun `should create or remove icons when modifying the source file`() {
        val projectDirectory = getTemporaryProjectDirectory(
            "example-project-tiny", ProjectConfiguration(
                sourceDir = "src",
            )
        )

        val runnerBuilder = GradleRunner.create()
            .withProjectDir(projectDirectory)
            .withPluginClasspath() // make `io.github.archangelx360.icon-generator` available

        val initialRun = runnerBuilder
            .withArguments(generateIconTaskName)
            .build()
        assertEquals(TaskOutcome.SUCCESS, initialRun.task(":${generateIconTaskName}")?.outcome)
        assertEquals(
            setOf(
                "${projectDirectory}/build/icons/foo/AIcons/AIcon.png"
            ),
            fetchGeneratedIconsPaths(projectDirectory.toPath()),
        )

        // add a class to the source file
        val sourceFile = File("${projectDirectory}/src/main/java/foo/AIcons.java")
        sourceFile.writeText(
            """
            package foo;

            public class AIcons {
                public final static String AIcon = "iVBORw0KGgoAAAANSUhEUgAAABgAAAAYCAYAAADgdz34AAAABmJLR0QA/wD/AP+gvaeTAAAAeElEQVRIiWNgGAVDHXTQ2oL/tLbkP60t+c9AY0tghv+G0g34FB9FcxEpOJwYS8g1/D9UfwiSJViDC1kxuQDDEiYKDUQHjEjs7+iSlAZRKJLr67HZfpgCC4iKZHIAScmUEgtwBgu1LKCJ4TALaGY4AwMNwnwUUB8AAGoAZWQIwMYBAAAAAElFTkSuQmCC";
                public final static String BIcon = "iVBORw0KGgoAAAANSUhEUgAAABgAAAAYCAYAAADgdz34AAAABmJLR0QA/wD/AP+gvaeTAAAAeElEQVRIiWNgGAVDHXTQ2oL/tLbkP60t+c9AY0tghv+G0g34FB9FcxEpOJwYS8g1/D9UfwiSJViDC1kxuQDDEiYKDUQHjEjs7+iSlAZRKJLr67HZfpgCC4iKZHIAScmUEgtwBgu1LKCJ4TALaGY4AwMNwnwUUB8AAGoAZWQIwMYBAAAAAElFTkSuQmCC";
            }
        """.trimIndent()
        )

        val secondRun = runnerBuilder
            .withArguments(generateIconTaskName)
            .build()
        assertEquals(TaskOutcome.SUCCESS, secondRun.task(":${generateIconTaskName}")?.outcome)
        assertEquals(
            setOf(
                "${projectDirectory}/build/icons/foo/AIcons/AIcon.png",
                "${projectDirectory}/build/icons/foo/AIcons/BIcon.png", // new icon has been generated
            ),
            fetchGeneratedIconsPaths(projectDirectory.toPath()),
        )

        // renaming class
        val content = sourceFile.readText()
        val modifiedContent = content
            .replace("BIcon", "CIcon")
        sourceFile.writeText(modifiedContent)

        val thirdRun = runnerBuilder
            .withArguments(generateIconTaskName)
            .build()
        assertEquals(TaskOutcome.SUCCESS, thirdRun.task(":${generateIconTaskName}")?.outcome)
        assertEquals(
            setOf(
                "${projectDirectory}/build/icons/foo/AIcons/AIcon.png",
                // BIcon file has been cleaned up
                "${projectDirectory}/build/icons/foo/AIcons/CIcon.png"
            ),
            fetchGeneratedIconsPaths(projectDirectory.toPath()),
        )
    }

    @Test
    @Ignore("Disabled until we find a better way to check for creation time cross-OS and without having to sleep")
    fun `should not cleanup icons that are still in the source file`() {
        val projectDirectory = getTemporaryProjectDirectory(
            "example-project-tiny", ProjectConfiguration(
                sourceDir = "src",
            )
        )

        val runnerBuilder = GradleRunner.create()
            .withProjectDir(projectDirectory)
            .withPluginClasspath() // make `io.github.archangelx360.icon-generator` available

        val initialRun = runnerBuilder
            .withArguments(generateIconTaskName)
            .build()
        assertEquals(TaskOutcome.SUCCESS, initialRun.task(":${generateIconTaskName}")?.outcome)
        val iconAFile = Path.of("${projectDirectory}/build/icons/foo/AIcons/AIcon.png")
        assertTrue(fetchGeneratedIconsPaths(projectDirectory.toPath()).contains(iconAFile.toString()))

        val creationTimeOfAIconOutput = iconAFile.creationTimeInSeconds
        // creation time for files only supports up to the second granularity
        // we are forcing the two runs to be separated by at least 1 second to be able to verify if the icon output file
        // has been re-created or not
        Thread.sleep(1000)

        // change the source file
        val sourceFile = File("${projectDirectory}/src/main/java/foo/AIcons.java")
        sourceFile.appendText("\n\n\n")

        val secondRun = runnerBuilder
            .withArguments(generateIconTaskName)
            .build()
        assertEquals(TaskOutcome.SUCCESS, secondRun.task(":${generateIconTaskName}")?.outcome)
        assertEquals(
            creationTimeOfAIconOutput,
            iconAFile.creationTimeInSeconds,
            "untouched classes icon files are reused, not regenerated"
        )
    }

    @Test
    fun `should fail to configure plugin with restricted output directory`() {
        val projectDirectory = getTemporaryProjectDirectory(
            "example-project-tiny", ProjectConfiguration(
                sourceDir = "src",
                outputDir = "icon-generator/icon-states"
            )
        )

        assertThrows<UnexpectedBuildFailure> {
            GradleRunner.create()
                .withProjectDir(projectDirectory)
                .withPluginClasspath() // make `io.github.archangelx360.icon-generator` available
                .withArguments(generateIconTaskName)
                .build()
        }
    }

    @Test
    fun `should fail to configure plugin with restricted output subdirectory`() {
        val projectDirectory = getTemporaryProjectDirectory(
            "example-project-tiny", ProjectConfiguration(
                sourceDir = "src",
                outputDir = "icon-generator/icon-states/something"
            )
        )

        assertThrows<UnexpectedBuildFailure> {
            GradleRunner.create()
                .withProjectDir(projectDirectory)
                .withPluginClasspath() // make `io.github.archangelx360.icon-generator` available
                .withArguments(generateIconTaskName)
                .build()
        }
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

    private val Path.creationTimeInSeconds
        get() = Files
            .readAttributes(this, BasicFileAttributes::class.java)
            .creationTime()
            .toInstant()
}
