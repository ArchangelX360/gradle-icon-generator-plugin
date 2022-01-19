plugins {
    `kotlin-dsl`
}

group = "se.dorne"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    val roasterVersion = "2.23.2.Final"
    implementation("org.jboss.forge.roaster:roaster-api:$roasterVersion")
    implementation("org.jboss.forge.roaster:roaster-jdt:$roasterVersion")

    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit5"))
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
