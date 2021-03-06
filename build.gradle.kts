plugins {
    `kotlin-dsl`
    id("java-gradle-plugin")
    id("maven-publish")
    id("com.gradle.plugin-publish") version "0.16.0"
}

group = "io.github.archangelx360"
version = "1.0.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.github.javaparser:javaparser-core:3.17.0")

    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit5"))
    testImplementation(gradleTestKit())
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.2")
}

tasks.test {
    useJUnitPlatform()
}

gradlePlugin {
    plugins {
        register("iconGeneratorPlugin") {
            id = "io.github.archangelx360.icon-generator"
            displayName = "Icon Generator Plugin"
            description = "Generates PNG icons from Java sources base64 fields"
            implementationClass = "io.github.archangelx360.IconGeneratorPlugin"
        }
    }
}

pluginBundle {
    website = "https://github.com/ArchangelX360/gradle-icon-generator-plugin"
    vcsUrl = "https://github.com/ArchangelX360/gradle-icon-generator-plugin"
    tags = listOf("example", "assignment")
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

val compilerArgs = listOf("-Xopt-in=kotlin.RequiresOptIn")
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
    kotlinOptions.freeCompilerArgs += compilerArgs
}
