plugins {
    kotlin("jvm") version "2.3.10"
    id("org.jlleitschuh.gradle.ktlint") version "14.0.1"
}

group = "pt.isel.pc"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // logging
    implementation("org.slf4j:slf4j-api:2.0.16")
    runtimeOnly("org.slf4j:slf4j-simple:2.0.16")

    // Kotlin coroutine extensions
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}
ktlint {
    version = "1.5.0"
    enableExperimentalRules = true
}
