package se.dorne.parser

import com.github.javaparser.ParseProblemException
import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.FieldDeclaration
import com.github.javaparser.ast.body.VariableDeclarator
import com.github.javaparser.ast.expr.StringLiteralExpr
import com.github.javaparser.ast.type.ClassOrInterfaceType
import se.dorne.LOG
import java.io.File
import java.nio.file.Path
import java.util.*

data class Icon(
    val content: ByteArray,
    val relativePath: Path,
)

fun extractBase64Icons(javaFile: File, type: String): List<Icon> {
    try {
        return StaticJavaParser.parse(javaFile)
            .findAll(FieldDeclaration::class.java)
            .filter { it.isPublic && it.isFinal && it.isOfType(type) }
            .mapNotNull {
                val className = it.findAncestor(ClassOrInterfaceDeclaration::class.java)
                if (className.isEmpty) {
                    LOG.warn("ignoring field ${it}, reason: no parent class")
                    null
                } else {
                    it.toBase64Icon(className.get().fullyQualifiedName.get())
                }
            }
    } catch (e: ParseProblemException) {
        LOG.warn("ignoring file ${javaFile}, reason: not a valid Java file")
        return emptyList()
    }
}

private fun FieldDeclaration.toBase64Icon(javaClassFullyQualifiedName: String): Icon? {
    val field = this.variables.first.get()
    when (val initializer = field.initializer.get()) {
        is StringLiteralExpr -> {
            val image = Base64.getDecoder().tryDecode(initializer.asStringLiteralExpr().asString())
            if (image == null) {
                LOG.warn("ignoring field ${field.name}, reason: string literal is not a valid base64 representation")
                return null
            }
            return Icon(
                relativePath = generateLocation(javaClassFullyQualifiedName, field, "png"),
                content = image,
            )
        }
        else -> {
            // TODO: this is used for testing in large scale IntelliJ project, for the final deliverable, we might remove it as it is not part of requirements
            LOG.debug("not generating image for field ${field.name} but will still create a output file with the string representation of its initializer, reason: not a string literal expression")
            return Icon(
                relativePath = generateLocation(javaClassFullyQualifiedName, field, "txt"),
                content = initializer.toString().toByteArray(),
            )
        }
    }
}

private fun generateLocation(
    javaClassFullyQualifiedName: String,
    variable: VariableDeclarator,
    extension: String
): Path =
    Path.of(javaClassFullyQualifiedName.replace(".", "/"), "${variable.nameAsString}.${extension}")

private fun FieldDeclaration.isOfType(type: String): Boolean {
    if (!this.elementType.isClassOrInterfaceType) {
        return false
    }
    val t = this.elementType as ClassOrInterfaceType
    return t.name.toString() == type
}

private fun Base64.Decoder.tryDecode(src: String): ByteArray? = try {
    this.decode(src)
} catch (e: IllegalArgumentException) {
    null
}
