plugins {
    `kotlin-dsl`
}

group = "se.dorne"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

gradlePlugin {
    plugins {
        register("imageGeneratorPlugin") {
            id = "image-generator-plugin"
            implementationClass = "se.dorne.ImageGeneratorPlugin"
        }
    }
}
