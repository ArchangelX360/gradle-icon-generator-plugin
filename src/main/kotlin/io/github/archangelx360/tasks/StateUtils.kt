package io.github.archangelx360.tasks

import org.gradle.api.file.Directory
import java.io.File
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists

/**
 * Instantiate a State for the source file [inputFile], declaring it in the [stateOutputFolder] directory
 */
fun resolveStateFile(stateOutputFolder: Directory, inputFile: File): File =
    stateOutputFolder.file(generateStateFileName(inputFile)).asFile

/**
 * Updates the state of the [file] and cleans up stale icons from previous run.
 *
 * An icon is considered stale if it is in the current state (before the update) but not in the [newOutputs].
 * If all the icons are stale, then the state file will be cleaned up as well to prevent garbage to pile up in the
 * build directory.
 */
@OptIn(ExperimentalPathApi::class)
fun updateStateAndCleanUpStaleOutputs(stateFile: File, newOutputs: Set<Path>) {
    val oldOutputs = deserialize(stateFile)

    val staleIcons = oldOutputs.subtract(newOutputs)
    staleIcons.forEach { it.deleteIfExists() }

    if (newOutputs.isNotEmpty()) {
        stateFile.toPath().parent.createDirectories()
        stateFile.writeText(serialize(newOutputs))
    } else {
        stateFile.toPath().deleteIfExists()
    }
}

private fun generateStateFileName(inputFile: File): String = inputFile.path.replace("/", "_")

private fun deserialize(stateFile: File): Set<Path> {
    if (stateFile.exists()) {
        val content = stateFile.readText()
        if (content.isBlank()) {
            return emptySet()
        }
        return content.split(serializingSeparator).map { Path.of(it) }.toSet()
    }
    return emptySet()
}

private fun serialize(outputs: Set<Path>) = outputs.joinToString(serializingSeparator)

private const val serializingSeparator: String = "\n"
