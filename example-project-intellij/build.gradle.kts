plugins {
    id("icon-generator-plugin")
}

generateIconsForSources {
    sources.setFrom(
        project.layout.projectDirectory.dir("intellij-community"),
    )
    iconFieldType.set("Icon")
}
