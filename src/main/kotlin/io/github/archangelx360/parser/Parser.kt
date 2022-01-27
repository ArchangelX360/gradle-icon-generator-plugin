package io.github.archangelx360.parser

import com.github.javaparser.JavaParser
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.FieldDeclaration
import com.github.javaparser.ast.body.VariableDeclarator
import com.github.javaparser.ast.expr.StringLiteralExpr
import io.github.archangelx360.LOG
import java.io.File
import java.util.*


data class Icon(
    val content: ByteArray,
    val fieldName: String,
    val javaClassFullyQualifiedName: String,
)

fun extractBase64Icons(javaFile: File, type: String): List<Icon> {
    val parseResult = JavaParser().parse(javaFile)
    if (!parseResult.isSuccessful) {
        LOG.warn("ignoring file ${javaFile}, reason: not a valid Java file:\n  - ${parseResult.problems.joinToString("\n  - ") { it.verboseMessage }}}")
        return emptyList()
    }
    val compilationUnit = parseResult.result.orElse(null)
    if (compilationUnit == null) {
        LOG.warn("ignoring file ${javaFile}, reason: not a valid Java file:\n  - found empty parse result")
        return emptyList()
    }

    return compilationUnit
        .findAll(VariableDeclarator::class.java)
        .filter { it.typeAsString == type }
        .mapNotNull {
            val variableName = it.nameAsString
            val initializer = it.initializer.orElse(null)
            if (initializer == null || initializer !is StringLiteralExpr) {
                LOG.info("[${variableName}] ignoring, initializer must be a string literal expression")
                return@mapNotNull null
            }
            val image = Base64.getDecoder().tryDecode(initializer.asString())
            if (image == null) {
                LOG.warn("[${variableName}] ignoring, string literal initializer is not a valid base64 representation")
                return@mapNotNull null
            }

            val field = it.findAncestor(FieldDeclaration::class.java).orElse(null)
            if (field == null) {
                LOG.warn("[${variableName}] ignoring, no field declaration found")
                return@mapNotNull null
            }
            if (!field.isPublic || !field.isFinal) {
                LOG.info("[${variableName}] ignoring, plugin only supports public and final fields")
                return@mapNotNull null
            }

            val parent = it.findAncestor(ClassOrInterfaceDeclaration::class.java).orElse(null)
            if (parent == null) {
                LOG.warn("[${variableName}] ignoring, no parent class found")
                return@mapNotNull null
            }

            val fullyQualifiedName = parent.fullyQualifiedName.orElse(null)
            if (fullyQualifiedName == null) {
                LOG.warn("[${variableName}] ignoring, local class are unsupported (cannot find fully qualified name)")
                return@mapNotNull null
            }

            Icon(
                javaClassFullyQualifiedName = fullyQualifiedName,
                fieldName = it.nameAsString,
                content = image,
            )
        }
}

private fun Base64.Decoder.tryDecode(src: String): ByteArray? = try {
    this.decode(src)
} catch (e: IllegalArgumentException) {
    null
}
