package io.github.archangelx360.tasks

import org.gradle.api.file.Directory
import java.io.File
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists

@OptIn(ExperimentalPathApi::class)
data class GenerateIconState(
    private val file: File,
) {
    private val serializingSeparator: String = "\n"

    /**
     * Updates the state of the [file] and cleans up stale icons from previous run.
     *
     * An icon is considered stale if it is in the current state (before the update) but not in the [newOutputs].
     * If all the icons are stale, then the state file will be cleaned up as well to prevent garbage to pile up in the
     * build directory.
     */
    fun updateState(newOutputs: Set<Path>) {
        val oldOutputs = deserialize()

        val staleIcons = oldOutputs.subtract(newOutputs)
        staleIcons.forEach { it.deleteIfExists() }

        if (newOutputs.isNotEmpty()) {
            file.toPath().parent.createDirectories()
            file.writeText(serialize(newOutputs))
        } else {
            file.toPath().deleteIfExists()
        }
    }

    private fun deserialize(): Set<Path> {
        if (file.exists()) {
            val content = file.readText()
            if (content.isBlank()) {
                return emptySet()
            }
            return content.split(serializingSeparator).map { Path.of(it) }.toSet()
        }
        return emptySet()
    }

    private fun serialize(outputs: Set<Path>) = outputs.joinToString(serializingSeparator)

    companion object {

        /**
         * Instantiate a State for the source file [inputFile], declaring it in the [stateOutputFolder] directory
         */
        fun resolve(stateOutputFolder: Directory, inputFile: File): GenerateIconState {
            val stateFileName = generateStateFileName(inputFile)
            val stateFile = stateOutputFolder.file(stateFileName).asFile
            return GenerateIconState(file = stateFile)
        }

        private fun generateStateFileName(inputFile: File): String = inputFile.path.replace("/", "_")
    }
}
