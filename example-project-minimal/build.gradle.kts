plugins {
    id("image-generator-plugin")
    id("java")
}

generatePngForSources {
    sourceDirectories.setFrom(
        project.layout.projectDirectory.dir("src")
    )
}
