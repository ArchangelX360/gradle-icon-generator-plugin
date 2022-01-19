package se.dorne.parser

import org.junit.jupiter.api.Test
import java.nio.file.Path
import java.util.*
import kotlin.test.assertEquals

internal class ParserTest {

    @Test
    fun `should extract icons`() {
        val javaFile = Path.of("src", "test", "resources", "MultiClassFile.java").toFile()
        val icons = extractBase64Icons(Base64.getDecoder(), javaFile)
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
}
