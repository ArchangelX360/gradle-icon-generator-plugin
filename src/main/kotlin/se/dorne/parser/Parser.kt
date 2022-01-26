package se.dorne.parser

import com.github.javaparser.ParseProblemException
import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.Node.TreeTraversal
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.FieldDeclaration
import com.github.javaparser.ast.body.VariableDeclarator
import com.github.javaparser.ast.expr.StringLiteralExpr
import se.dorne.LOG
import java.io.File
import java.util.*


data class Icon(
    val content: ByteArray,
    val fieldName: String,
    val javaClassFullyQualifiedName: String,
    val extension: String = "png",
    val fullyQualifiedNameOfTopLevelClass: String,
)

fun extractBase64Icons(javaFile: File, type: String): List<Icon> {
    try {
        val compilationUnit = StaticJavaParser.parse(javaFile)

        val (publicTopLevelClasses, ignoredTopLevelClasses) = compilationUnit
            .findDirectChildren(ClassOrInterfaceDeclaration::class.java)
            .filter { it.isTopLevelType && !it.isInterface }
            .partition { it.isPublic }
        if (ignoredTopLevelClasses.isNotEmpty()) {
            LOG.warn("ignoring variables of classes ${ignoredTopLevelClasses.joinToString(", ")}, plugin only supports *public* top level class fields")
        }
        if (publicTopLevelClasses.isEmpty()) {
            LOG.warn("ignoring file ${javaFile}, reason: no top level public class")
            return emptyList()
        }
        if (publicTopLevelClasses.size > 1) {
            LOG.warn("ignoring file ${javaFile}, reason: java file invalid, only one top level public class is allowed")
            return emptyList()
        }
        val topLevelClass = publicTopLevelClasses.first()
        val fullyQualifiedNameOfTopLevelClass = topLevelClass.fullyQualifiedName.orElse(null)
        if (fullyQualifiedNameOfTopLevelClass == null) {
            LOG.warn("ignoring file ${javaFile}, reason: could not get fully qualified name of top level public class")
            return emptyList()
        }

        return topLevelClass
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
                if ((parent.isTopLevelType && !parent.isPublic) || parent.isInterface) {
                    LOG.warn("[${variableName}] ignoring, plugin only supports public top level class fields")
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
                    fullyQualifiedNameOfTopLevelClass = fullyQualifiedNameOfTopLevelClass,
                )
            }
    } catch (e: ParseProblemException) {
        LOG.warn("ignoring file ${javaFile}, reason: not a valid Java file:\n  - ${e.problems.joinToString("\n  - ") { it.verboseMessage }}}")
        return emptyList()
    }
}

// in higher version of javaparser, this function is available as `public <T extends Node> List<T> findAll(Class<T> nodeType, TreeTraversal traversal)`
// however, Gradle uses javaparser in version `3.17.0`, so to avoid dependency conflict, we are also using this version
private fun <T : Node?> Node.findDirectChildren(nodeType: Class<T>): List<T> {
    val found: MutableList<T> = ArrayList()
    walk(TreeTraversal.DIRECT_CHILDREN) { node ->
        if (nodeType.isAssignableFrom(node.javaClass)) {
            found.add(nodeType.cast(node))
        }
    }
    return found
}

private fun Base64.Decoder.tryDecode(src: String): ByteArray? = try {
    this.decode(src)
} catch (e: IllegalArgumentException) {
    null
}
