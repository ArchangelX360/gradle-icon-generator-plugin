pluginManagement {
    repositories {
        mavenLocal() // FIXME: remove, for testing only
        gradlePluginPortal()
    }
}

rootProject.name = "icon-generator"

include("examples:large-generated-project")
include("examples:small-project")
