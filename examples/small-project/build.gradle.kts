plugins {
    id("io.github.archangelx360.icon-generator") version "1.0.1"
}

generateIconsForSources {
    sources.setFrom(
        project.layout.projectDirectory.dir("src")
    )
}
