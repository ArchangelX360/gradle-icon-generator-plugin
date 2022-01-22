buildscript {
    dependencies {
        classpath("se.dorne:icon-generator-plugin")
    }
}

plugins {
    id("icon-generator-plugin")
}

generateIconsForSources {
    sources.setFrom(
        project.layout.projectDirectory.dir("intellij-community")
    )
    iconFieldType.set("Icon")
}
