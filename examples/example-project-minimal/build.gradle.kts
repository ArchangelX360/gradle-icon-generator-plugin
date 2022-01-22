buildscript {
    dependencies {
        classpath("se.dorne:icon-generator-plugin")
    }
}

plugins {
    id("icon-generator-plugin")
    id("java")
}

generateIconsForSources {
    sources.setFrom(
        project.layout.projectDirectory.dir("src")
    )
}
