package io.github.archangelx360.tasks

import org.gradle.api.file.Directory
import java.io.File
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists

/**
 * Resolve state file of the source file [inputFile], declaring it in the [stateOutputFolder] directory
 *
 * The state file name is relative to the [projectDirectory] to make state file agnostic of the developer's directory
 * structure before the project directory. This makes the output cache of the plugin sharable amongst the different
 * machines (CI, developers laptop, etc.) where the plugin used.
 *
 * In the case the plugin is used on sources that are before the project directory, the cache may or may not be sharable.
 * For this reason, it is discouraged to use the plugin on files outside the project directory. The cache will still
 * work as expected, state file names will simply include dots.
 */
fun resolveStateFile(stateOutputFolder: Directory, projectDirectory: Path, inputFile: Path): File {
    val stateFilename = projectDirectory.relativize(inputFile).toString().replace("/", "_")
    return stateOutputFolder.file(stateFilename).asFile
}

/**
 * Updates the state of the [stateFile] and cleans up stale icons from previous run.
 *
 * An icon is considered stale if it is in the current state (before the update) but not in the [newOutputs].
 * If all the icons are stale, then the state file will be cleaned up as well to prevent garbage to pile up in the
 * build directory.
 *
 * The format of the state file is the following:
 * ```
 * deepest_common_directory_path_relative_to_the_output_directory
 * output_file_1_path_relative_to_the_deepest_common_directory
 * output_file_2_path_relative_to_the_deepest_common_directory
 * output_file_N_path_relative_to_the_deepest_common_directory
 * ```
 *
 * The format uses relative paths to compact as much as possible the size of the state file.
 */
@OptIn(ExperimentalPathApi::class)
fun updateStateAndCleanUpStaleOutputs(outputDirectory: Path, stateFile: File, newOutputs: Set<Path>) {
    if (!outputDirectory.isAbsolute) error("outputDirectory path must be absolute")
    if (newOutputs.any { !it.isAbsolute }) error("newOutputs must be absolute paths")
    if (newOutputs.any { !it.startsWith(outputDirectory) }) error("newOutputs must be sub-paths of outputDirectory")

    val oldOutputs = deserialize(outputDirectory, stateFile)

    val staleIcons = oldOutputs.subtract(newOutputs)
    staleIcons.forEach { it.deleteIfExists() }

    if (newOutputs.isNotEmpty()) {
        stateFile.toPath().parent.createDirectories()
        stateFile.writeText(serialize(outputDirectory, newOutputs))
    } else {
        stateFile.toPath().deleteIfExists()
    }
}

private fun deserialize(outputDirectory: Path, stateFile: File): Set<Path> {
    if (stateFile.exists()) {
        val lines = stateFile.readLines()
        if (lines.isEmpty()) {
            return emptySet()
        }

        val commonDeepestDirectory = lines.first()
        return (1 until lines.size).map {
            Path.of(outputDirectory.toString(), commonDeepestDirectory, lines[it])
        }.toSet()
    }
    return emptySet()
}

private fun commonDeepestDirectory(paths: List<Path>): Path? {
    var commonDeepestDirectory = paths.first().parent
    (1 until paths.size).forEach {
        if (commonDeepestDirectory == null) return null

        while (commonDeepestDirectory != null && !paths[it].startsWith(commonDeepestDirectory)) {
            commonDeepestDirectory = commonDeepestDirectory.parent
        }
    }
    return commonDeepestDirectory
}

private fun serialize(outputDirectory: Path, outputs: Set<Path>): String {
    val commonDeepestDirectory = commonDeepestDirectory(outputs.toList())
    val relativePaths = outputs.map { commonDeepestDirectory?.relativize(it) ?: it }
    val relativeCommonDeepestDirectory = outputDirectory.relativize(commonDeepestDirectory ?: Path.of(""))
    return "${relativeCommonDeepestDirectory}\n" + relativePaths.joinToString("\n")
}
