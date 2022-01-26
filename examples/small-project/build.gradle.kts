plugins {
    id("se.dorne.icon-generator") version "0.0.1"
    id("java")
}

generateIconsForSources {
    sources.setFrom(
        project.layout.projectDirectory.dir("src")
    )
}
