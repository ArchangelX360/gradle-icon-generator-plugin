package se.dorne.parser

import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

internal class ParserTest {
    @Test
    fun `should extract top level public class icon`() {
        val sourceFile = createTemporarySourceFile(
            """
            package foo;

            public class Example {
                public final String AIcon = "iVBORw0KGgoAAAANSUhEUgAAABgAAAAYCAYAAADgdz34AAAABmJLR0QA/wD/AP+gvaeTAAAAeElEQVRIiWNgGAVDHXTQ2oL/tLbkP60t+c9AY0tghv+G0g34FB9FcxEpOJwYS8g1/D9UfwiSJViDC1kxuQDDEiYKDUQHjEjs7+iSlAZRKJLr67HZfpgCC4iKZHIAScmUEgtwBgu1LKCJ4TALaGY4AwMNwnwUUB8AAGoAZWQIwMYBAAAAAElFTkSuQmCC";
                public final static String BIcon = "iVBORw0KGgoAAAANSUhEUgAAABgAAAAYCAYAAADgdz34AAAABmJLR0QA/wD/AP+gvaeTAAAAeElEQVRIiWNgGAVDHXTQ2oL/tLbkP60t+c9AY0tghv+G0g34FB9FcxEpOJwYS8g1/D9UfwiSJViDC1kxuQDDEiYKDUQHjEjs7+iSlAZRKJLr67HZfpgCC4iKZHIAScmUEgtwBgu1LKCJ4TALaGY4AwMNwnwUUB8AAGoAZWQIwMYBAAAAAElFTkSuQmCC";
            }
        """.trimIndent()
        )
        val icons = extractBase64Icons(sourceFile, "String")
        assertNotNull(icons.find { it.javaClassFullyQualifiedName == "foo.Example" && it.fieldName == "AIcon" })
        assertNotNull(icons.find { it.javaClassFullyQualifiedName == "foo.Example" && it.fieldName == "BIcon" })
        assertEquals(2, icons.size)
    }

    @Test
    fun `should extract multi-variable field declaration icons`() {
        val sourceFile = createTemporarySourceFile(
            """
            package foo;

            public class Example {
                public static final String AIcon = "iVBORw0KGgoAAAANSUhEUgAAABgAAAAYCAYAAADgdz34AAAABmJLR0QA/wD/AP+gvaeTAAAAeElEQVRIiWNgGAVDHXTQ2oL/tLbkP60t+c9AY0tghv+G0g34FB9FcxEpOJwYS8g1/D9UfwiSJViDC1kxuQDDEiYKDUQHjEjs7+iSlAZRKJLr67HZfpgCC4iKZHIAScmUEgtwBgu1LKCJ4TALaGY4AwMNwnwUUB8AAGoAZWQIwMYBAAAAAElFTkSuQmCC",
                                           BIcon = "iVBORw0KGgoAAAANSUhEUgAAABgAAAAYCAYAAADgdz34AAAABmJLR0QA/wD/AP+gvaeTAAAAeElEQVRIiWNgGAVDHXTQ2oL/tLbkP60t+c9AY0tghv+G0g34FB9FcxEpOJwYS8g1/D9UfwiSJViDC1kxuQDDEiYKDUQHjEjs7+iSlAZRKJLr67HZfpgCC4iKZHIAScmUEgtwBgu1LKCJ4TALaGY4AwMNwnwUUB8AAGoAZWQIwMYBAAAAAElFTkSuQmCC";
            }
        """.trimIndent()
        )
        val icons = extractBase64Icons(sourceFile, "String")
        assertNotNull(icons.find { it.javaClassFullyQualifiedName == "foo.Example" && it.fieldName == "AIcon" })
        assertNotNull(icons.find { it.javaClassFullyQualifiedName == "foo.Example" && it.fieldName == "BIcon" })
        assertEquals(2, icons.size)
    }

    @Test
    fun `should extract nested class icon`() {
        val sourceFile = createTemporarySourceFile(
            """
            package foo;

            public class Example {
                class Nested {
                    public final String AIcon = "iVBORw0KGgoAAAANSUhEUgAAABgAAAAYCAYAAADgdz34AAAABmJLR0QA/wD/AP+gvaeTAAAAeElEQVRIiWNgGAVDHXTQ2oL/tLbkP60t+c9AY0tghv+G0g34FB9FcxEpOJwYS8g1/D9UfwiSJViDC1kxuQDDEiYKDUQHjEjs7+iSlAZRKJLr67HZfpgCC4iKZHIAScmUEgtwBgu1LKCJ4TALaGY4AwMNwnwUUB8AAGoAZWQIwMYBAAAAAElFTkSuQmCC";
                }
            }
        """.trimIndent()
        )
        val icons = extractBase64Icons(sourceFile, "String")
        assertNotNull(icons.find { it.javaClassFullyQualifiedName == "foo.Example.Nested" && it.fieldName == "AIcon" })
        assertEquals(1, icons.size)
    }

    @Test
    fun `should ignore non-final String field`() {
        val sourceFile = createTemporarySourceFile(
            """
            package foo;

            public class Example {
                public String IgnoredIcon = "iVBORw0KGgoAAAANSUhEUgAAABgAAAAYCAYAAADgdz34AAAABmJLR0QA/wD/AP+gvaeTAAAAeElEQVRIiWNgGAVDHXTQ2oL/tLbkP60t+c9AY0tghv+G0g34FB9FcxEpOJwYS8g1/D9UfwiSJViDC1kxuQDDEiYKDUQHjEjs7+iSlAZRKJLr67HZfpgCC4iKZHIAScmUEgtwBgu1LKCJ4TALaGY4AwMNwnwUUB8AAGoAZWQIwMYBAAAAAElFTkSuQmCC";
            }
        """.trimIndent()
        )
        val icons = extractBase64Icons(sourceFile, "String")
        assertTrue(icons.isEmpty())
    }

    @Test
    fun `should ignore non-public String field`() {
        val sourceFile = createTemporarySourceFile(
            """
            package foo;

            public class Example {
                final String IgnoredIcon = "iVBORw0KGgoAAAANSUhEUgAAABgAAAAYCAYAAADgdz34AAAABmJLR0QA/wD/AP+gvaeTAAAAeElEQVRIiWNgGAVDHXTQ2oL/tLbkP60t+c9AY0tghv+G0g34FB9FcxEpOJwYS8g1/D9UfwiSJViDC1kxuQDDEiYKDUQHjEjs7+iSlAZRKJLr67HZfpgCC4iKZHIAScmUEgtwBgu1LKCJ4TALaGY4AwMNwnwUUB8AAGoAZWQIwMYBAAAAAElFTkSuQmCC";
            }
        """.trimIndent()
        )
        val icons = extractBase64Icons(sourceFile, "String")
        assertTrue(icons.isEmpty())
    }

    @Test
    fun `should ignore String field with non-string initializer`() {
        val sourceFile = createTemporarySourceFile(
            """
            package foo;

            public class Example {
                  public final static String InvalidIcon = someFunction("invalid_base64_is_ignored");

                  public static String someFunction(String s) {
                    return s;
                  }
            }
        """.trimIndent()
        )
        val icons = extractBase64Icons(sourceFile, "String")
        assertTrue(icons.isEmpty())
    }

    @Test
    fun `should ignore String field with invalid base64 initializer`() {
        val sourceFile = createTemporarySourceFile(
            """
            package foo;

            public class Example {
                 public final static String InvalidIcon = "invalid_base64_is_ignored";
            }
        """.trimIndent()
        )
        val icons = extractBase64Icons(sourceFile, "String")
        assertTrue(icons.isEmpty())
    }

    @Test
    fun `should ignore fields not matching type`() {
        val sourceFile = createTemporarySourceFile(
            """
            package foo;

            public class Example {
                public final static Integer IgnoredInt = 1;
            }
        """.trimIndent()
        )
        val icons = extractBase64Icons(sourceFile, "String")
        assertTrue(icons.isEmpty())
    }

    @Test
    fun `should parse top-level non-public class`() {
        val sourceFile = createTemporarySourceFile(
            """
            package foo;

            class Example {
                public final static String SomeIcon = "iVBORw0KGgoAAAANSUhEUgAAABgAAAAYCAYAAADgdz34AAAABmJLR0QA/wD/AP+gvaeTAAAAeElEQVRIiWNgGAVDHXTQ2oL/tLbkP60t+c9AY0tghv+G0g34FB9FcxEpOJwYS8g1/D9UfwiSJViDC1kxuQDDEiYKDUQHjEjs7+iSlAZRKJLr67HZfpgCC4iKZHIAScmUEgtwBgu1LKCJ4TALaGY4AwMNwnwUUB8AAGoAZWQIwMYBAAAAAElFTkSuQmCC";
            }
        """.trimIndent()
        )
        val icons = extractBase64Icons(sourceFile, "String")
        assertNotNull(icons.find { it.javaClassFullyQualifiedName == "foo.Example" && it.fieldName == "SomeIcon" })
        assertEquals(1, icons.size)
    }

    @Test
    fun `should ignore invalid file`() {
        val sourceFile = createTemporarySourceFile(
            """
                package foo // invalid because missing ;
    
                public class Invalid {
                    public final static String AIcon = "iVBORw0KGgoAAAANSUhEUgAAABgAAAAYCAYAAADgdz34AAAABmJLR0QA/wD/AP+gvaeTAAAAeElEQVRIiWNgGAVDHXTQ2oL/tLbkP60t+c9AY0tghv+G0g34FB9FcxEpOJwYS8g1/D9UfwiSJViDC1kxuQDDEiYKDUQHjEjs7+iSlAZRKJLr67HZfpgCC4iKZHIAScmUEgtwBgu1LKCJ4TALaGY4AwMNwnwUUB8AAGoAZWQIwMYBAAAAAElFTkSuQmCC";
                }
            """.trimIndent(),
            "Invalid.java",
        )
        val icons = extractBase64Icons(sourceFile, "String")
        assertTrue(icons.isEmpty())
    }

    @Test
    fun `should ignore invalid java file with 2 top level public classes`() {
        val sourceFile = createTemporarySourceFile(
            """
            package foo;

            public class Example {
                public final String AIcon = "iVBORw0KGgoAAAANSUhEUgAAABgAAAAYCAYAAADgdz34AAAABmJLR0QA/wD/AP+gvaeTAAAAeElEQVRIiWNgGAVDHXTQ2oL/tLbkP60t+c9AY0tghv+G0g34FB9FcxEpOJwYS8g1/D9UfwiSJViDC1kxuQDDEiYKDUQHjEjs7+iSlAZRKJLr67HZfpgCC4iKZHIAScmUEgtwBgu1LKCJ4TALaGY4AwMNwnwUUB8AAGoAZWQIwMYBAAAAAElFTkSuQmCC";
            }
            public class Example2 {
                public final String AIcon = "iVBORw0KGgoAAAANSUhEUgAAABgAAAAYCAYAAADgdz34AAAABmJLR0QA/wD/AP+gvaeTAAAAeElEQVRIiWNgGAVDHXTQ2oL/tLbkP60t+c9AY0tghv+G0g34FB9FcxEpOJwYS8g1/D9UfwiSJViDC1kxuQDDEiYKDUQHjEjs7+iSlAZRKJLr67HZfpgCC4iKZHIAScmUEgtwBgu1LKCJ4TALaGY4AwMNwnwUUB8AAGoAZWQIwMYBAAAAAElFTkSuQmCC";
            }
        """.trimIndent()
        )
        val icons = extractBase64Icons(sourceFile, "String")
        assertTrue(icons.isEmpty())
    }

    private fun createTemporarySourceFile(content: String, nameSuffix: String = "Example.java"): File {
        val sourceFile = File.createTempFile("ParserTest", nameSuffix)
        sourceFile.deleteOnExit()
        sourceFile.writeText(content)
        return sourceFile
    }
}
