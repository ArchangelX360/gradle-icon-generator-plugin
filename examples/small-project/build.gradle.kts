plugins {
    id("io.github.archangelx360.icon-generator") version "0.0.8"
}

generateIconsForSources {
    sources.setFrom(
        project.layout.projectDirectory.dir("src")
    )
}
