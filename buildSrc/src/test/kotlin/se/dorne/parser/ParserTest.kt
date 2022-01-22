package se.dorne.parser

import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class ParserTest {
    @Test
    fun `should extract icons`() {
        val javaFile = Path.of("src", "test", "resources", "MultiClassFile.java").toFile()
        val icons = extractBase64Icons(javaFile, "String")
        assertEquals(
            setOf(
                "foo/Parent/AIcon.png",
                "foo/Parent/Nested/BIcon.png",
                "foo/Parent/Nested/CIcon.png",
                "foo/Two/TwoIcon.png",
            ),
            icons.map { it.relativePath.toString() }.toSet()
        )
    }

    @Test
    fun `should ignore invalid file`() {
        val javaFile = Path.of("src", "test", "resources", "Invalid.java").toFile()
        val icons = extractBase64Icons(javaFile, "String")
        assertTrue(icons.isEmpty())
    }
}
