plugins {
    id("io.github.archangelx360.icon-generator") version "1.0.0"
}

generateIconsForSources {
    sources.setFrom(
        project.layout.projectDirectory.dir("src")
    )
}
