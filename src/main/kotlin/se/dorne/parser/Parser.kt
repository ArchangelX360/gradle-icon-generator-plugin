package se.dorne.parser

import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.FieldDeclaration
import com.github.javaparser.ast.type.ClassOrInterfaceType
import java.io.File
import java.nio.file.Path
import java.util.*

data class Base64Icon(
    val image: ByteArray,
    val name: Path,
)

fun extractBase64Icons(base64Decoder: Base64.Decoder, javaFile: File): List<Base64Icon> {
    val file = StaticJavaParser.parse(javaFile)
    return file.findAll(FieldDeclaration::class.java)
        .filter { it.isPublic && it.isStatic && it.isFinal && it.isString() }
        .mapNotNull {
            val className = it.findAncestor(ClassOrInterfaceDeclaration::class.java)
            if (className.isEmpty) {
                // if no parent class, we ignore the field for now
                // TODO: should we support fields outside of classes?
                null
            } else {
                it.toBase64Icon(base64Decoder, className.get().fullyQualifiedName.get())
            }
        }
}

private fun FieldDeclaration.toBase64Icon(base64Decoder: Base64.Decoder, javaClassFullyQualifiedName: String): Base64Icon? {
    val variable = this.variables.first.get()
    val image = base64Decoder.tryDecode(variable.initializer.get().asStringLiteralExpr().asString())
        ?: return null // ignore String variables that are not valid base64 representation

    return Base64Icon(
        name = Path.of(javaClassFullyQualifiedName.replace(".", "/"), "${variable.nameAsString}.png"),
        image = image,
    )
}

private fun FieldDeclaration.isString(): Boolean {
    if (!this.elementType.isClassOrInterfaceType) {
        return false
    }
    val type = this.elementType as ClassOrInterfaceType
    return type.name.toString() == "String"
}

private fun Base64.Decoder.tryDecode(src: String): ByteArray? = try {
    this.decode(src)
} catch (e: IllegalArgumentException) {
    null
}
