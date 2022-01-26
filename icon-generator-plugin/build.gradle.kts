plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
}

group = "se.dorne"

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
            id = "icon-generator-plugin"
            implementationClass = "se.dorne.IconGeneratorPlugin"
        }
    }
}

val compilerArgs = listOf("-Xopt-in=kotlin.RequiresOptIn")
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.freeCompilerArgs += compilerArgs
}
