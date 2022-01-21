plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.github.javaparser:javaparser-symbol-solver-core:3.24.0")

    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit5"))
    // testImplementation(gradleTestKit())
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.2")
}

tasks.test {
    useJUnitPlatform()
}

gradlePlugin {
    plugins {
        register("imageGeneratorPlugin") {
            id = "image-generator-plugin"
            implementationClass = "se.dorne.ImageGeneratorPlugin"
        }
    }
}

val compilerArgs = listOf("-Xopt-in=kotlin.RequiresOptIn")
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.freeCompilerArgs += compilerArgs
}
