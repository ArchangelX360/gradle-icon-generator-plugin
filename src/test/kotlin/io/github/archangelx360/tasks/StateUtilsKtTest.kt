package io.github.archangelx360.tasks

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals

internal class StateUtilsKtTest {
    @Test
    fun `should find common directory`() {
        val buildDirectory = Files.createTempDirectory("build")
        val files = listOf(
            "${buildDirectory}/foo/bar/Icons/A.png",
            "${buildDirectory}/foo/bar/Icons/Nested/C.png",
            "${buildDirectory}/foo/bar/Icons/B.png",
        )
            .map { Path.of(it) }
            .toSet()

        val stateFile = Files.createTempFile(buildDirectory, "test", "state").toFile()

        updateStateAndCleanUpStaleOutputs(buildDirectory, stateFile, files)

        val content = stateFile.readText()

        assertEquals(
            """
            foo/bar/Icons
            A.png
            Nested/C.png
            B.png
        """.trimIndent(), content
        )
    }

    @Test
    fun `should find right common directory even if two files have a deeper common one`() {
        val buildDirectory = Files.createTempDirectory("build")
        val files = listOf(
            "${buildDirectory}/foo/bar/Icons/Nested/Nested2/Nested3/A.png",
            "${buildDirectory}/foo/bar/Icons/Nested/Nested2/B.png",
            "${buildDirectory}/foo/bar/Icons/C.png",
        )
            .map { Path.of(it) }
            .toSet()

        val stateFile = Files.createTempFile(buildDirectory, "test", "state").toFile()

        updateStateAndCleanUpStaleOutputs(buildDirectory, stateFile, files)

        val content = stateFile.readText()

        assertEquals(
            """
            foo/bar/Icons
            Nested/Nested2/Nested3/A.png
            Nested/Nested2/B.png
            C.png
        """.trimIndent(), content
        )
    }

    @Test
    fun `should have no common directory`() {
        val buildDirectory = Files.createTempDirectory("build")
        val files = listOf(
            "${buildDirectory}/one/bar/Icons/A.png",
            "${buildDirectory}/two/bar/Nested/C.png",
            "${buildDirectory}/three/bar/B.png",
        )
            .map { Path.of(it) }
            .toSet()

        val stateFile = Files.createTempFile(buildDirectory, "test", "state").toFile()

        updateStateAndCleanUpStaleOutputs(buildDirectory, stateFile, files)

        val content = stateFile.readText()

        assertEquals(
            """
            
            one/bar/Icons/A.png
            two/bar/Nested/C.png
            three/bar/B.png
        """.trimIndent(), content
        )
    }

    @Test
    fun `should have no common directory when it is only files`() {
        val buildDirectory = Files.createTempDirectory("build")
        val files = listOf(
            "${buildDirectory}/A.png",
            "${buildDirectory}/C.png",
            "${buildDirectory}/B.png",
        )
            .map { Path.of(it) }
            .toSet()

        val stateFile = Files.createTempFile(buildDirectory, "test", "state").toFile()

        updateStateAndCleanUpStaleOutputs(buildDirectory, stateFile, files)

        val content = stateFile.readText()

        assertEquals(
            """
            
            A.png
            C.png
            B.png
        """.trimIndent(), content
        )
    }

    @Test
    fun `should fail if outputDirectory is not absolute`() {
        val buildDirectory = Path.of("foo", "bar")
        val files = listOf(
            "${buildDirectory}/A.png",
        )
            .map { Path.of(it) }
            .toSet()

        val stateFile = Files.createTempFile("test", "state").toFile()

        val exception = assertThrows<IllegalStateException> {
            updateStateAndCleanUpStaleOutputs(buildDirectory, stateFile, files)
        }
        assertEquals("outputDirectory path must be absolute", exception.message)
    }

    @Test
    fun `should fail if at least one file is not absolute`() {
        val buildDirectory = Files.createTempDirectory("build")
        val files = listOf(
            "${buildDirectory}/A.png",
            "foo/B.png",
        )
            .map { Path.of(it) }
            .toSet()

        val stateFile = Files.createTempFile("test", "state").toFile()

        val exception = assertThrows<IllegalStateException> {
            updateStateAndCleanUpStaleOutputs(buildDirectory, stateFile, files)
        }
        assertEquals("newOutputs must be absolute paths", exception.message)
    }

    @Test
    fun `should fail if at least one file is not a sub path of the output directory`() {
        val buildDirectory = Files.createTempDirectory("build")
        val files = listOf(
            "${buildDirectory}/A.png",
            "/foo/B.png",
        )
            .map { Path.of(it) }
            .toSet()

        val stateFile = Files.createTempFile("test", "state").toFile()

        val exception = assertThrows<IllegalStateException>("fsdfs") {
            updateStateAndCleanUpStaleOutputs(buildDirectory, stateFile, files)
        }
        assertEquals("newOutputs must be sub-paths of outputDirectory", exception.message)
    }
}
