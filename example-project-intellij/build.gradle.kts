plugins {
    id("image-generator-plugin")
}

generatePngForSources {
    sourceDirectories.setFrom(
        project.layout.projectDirectory.dir("intellij-community"),
    )
    iconVariableType.set("Icon")
}
