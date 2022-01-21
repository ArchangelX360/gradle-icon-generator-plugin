package se.dorne.parser

import org.junit.jupiter.api.Test
import java.nio.file.Path
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// FIXME: test are not running anymore since it is in buildSrc?
internal class ParserTest {
    @Test
    fun `should extract icons`() {
        val javaFile = Path.of("src", "test", "resources", "MultiClassFile.java").toFile()
        val icons = extractBase64Icons(Base64.getDecoder(), javaFile, "String")
        assertEquals(
            setOf(
                "foo/Parent/AIcon.png",
                "foo/Parent/Nested/BIcon.png",
                "foo/Parent/Nested/CIcon.png",
                "foo/Two/TwoIcon.png",
            ),
            icons.map { it.name.toString() }.toSet()
        )
    }

    @Test
    fun `should ignore invalid file`() {
        val javaFile = Path.of("src", "test", "resources", "Invalid.java").toFile()
        val icons = extractBase64Icons(Base64.getDecoder(), javaFile, "String")
        assertTrue(icons.isEmpty())
    }
}
