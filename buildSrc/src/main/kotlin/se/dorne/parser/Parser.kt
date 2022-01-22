package se.dorne.parser

import com.github.javaparser.ParseProblemException
import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.FieldDeclaration
import com.github.javaparser.ast.body.VariableDeclarator
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.expr.StringLiteralExpr
import com.github.javaparser.ast.type.ClassOrInterfaceType
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
                    // if no parent class, we ignore the field
                    // TODO: log warning?
                    null
                } else {
                    it.toBase64Icon(className.get().fullyQualifiedName.get())
                }
            }
    } catch (e: ParseProblemException) {
        // TODO: log warning
        return emptyList()
    }
}

private fun FieldDeclaration.toBase64Icon(
    javaClassFullyQualifiedName: String
): Icon? {
    val variable = this.variables.first.get()
    when (val initializer = variable.initializer.get()) {
        is StringLiteralExpr -> {
            val image = Base64.getDecoder().tryDecode(initializer.asStringLiteralExpr().asString())
                ?: return null // ignore String variables that are not valid base64 representation
            return Icon(
                relativePath = generateLocation(javaClassFullyQualifiedName, variable, "png"),
                content = image,
            )
        }
        // TODO: this is used for testing in large scale IntelliJ project, it should probably be removed in the future
        is MethodCallExpr -> {
            return Icon(
                relativePath = generateLocation(javaClassFullyQualifiedName, variable, "txt"),
                content = initializer.asMethodCallExpr().toString().toByteArray(),
            )
        }
        else -> return null
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
