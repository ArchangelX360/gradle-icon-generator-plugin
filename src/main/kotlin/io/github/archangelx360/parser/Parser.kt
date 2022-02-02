package io.github.archangelx360.parser

import com.github.javaparser.JavaParser
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.Node
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

    return findAllFields(compilationUnit).flatMap { it.extractIcons(type) }
}

/**
 * Find all fields of a CompilationUnit in a breadth first fashion
 *
 * It will not go deeper in the AST if encountering a node different from a Class or Interface to prevent unnecessary processing
 */
private fun findAllFields(compilationUnit: CompilationUnit): List<FieldDeclaration> {
    val fields = mutableListOf<FieldDeclaration>()

    val queue = ArrayDeque<Node>()
    queue.addAll(compilationUnit.types)
    while (queue.isNotEmpty()) {
        when (val current = queue.pop()) {
            is FieldDeclaration -> {
                fields.add(current)
                // no need to go deeper in the tree, the field declaration is our expected leaf
            }
            // interfaces
            is ClassOrInterfaceDeclaration -> {
                queue.addAll(current.childNodes)
            }
            else -> {
                // we ignore the node, no need to go deeper in the tree as none of the children of this node will be a
                // field, fields are only declared in Class or Interface in Java
            }
        }
    }

    return fields
}

private fun FieldDeclaration.extractIcons(ofType: String): List<Icon> {
    val parent = this.parentNode.orElse(null)
    if (parent == null || parent !is ClassOrInterfaceDeclaration) {
        LOG.info("ignoring field, illegal state, parent was not a class or interface")
        return emptyList()
    }

    val fullyQualifiedName = parent.fullyQualifiedName.orElse(null)
    if (fullyQualifiedName == null) {
        LOG.warn("ignoring field, local class are unsupported (cannot find fully qualified name)")
        return emptyList()
    }

    if (!this.isPublic || !this.isFinal) {
        LOG.info("ignoring field, plugin only supports public and final fields")
        return emptyList()
    }

    return this.variables.mapNotNull { it.toIcon(ofType, fullyQualifiedName) }
}

private fun VariableDeclarator.toIcon(
    ofType: String,
    fullyQualifiedNameOfParentClassOrInterface: String,
): Icon? {
    if (this.typeAsString != ofType) {
        return null
    }

    val variableName = this.nameAsString
    val initializer = this.initializer.orElse(null)
    if (initializer == null || initializer !is StringLiteralExpr) {
        LOG.info("[${variableName}] ignoring, initializer must be a string literal expression")
        return null
    }
    val image = Base64.getDecoder().tryDecode(initializer.asString())
    if (image == null) {
        LOG.warn("[${variableName}] ignoring, string literal initializer is not a valid base64 representation")
        return null
    }

    return Icon(
        javaClassFullyQualifiedName = fullyQualifiedNameOfParentClassOrInterface,
        fieldName = nameAsString,
        content = image,
    )
}

private fun Base64.Decoder.tryDecode(src: String): ByteArray? = try {
    this.decode(src)
} catch (e: IllegalArgumentException) {
    null
}
