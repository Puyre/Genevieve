plugins {
    kotlin("jvm") version "2.2.21"
    kotlin("plugin.serialization") version "2.2.21"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("org.jetbrains.skiko:skiko-awt-runtime-macos-arm64:0.7.93")
}

kotlin {
    jvmToolchain(17)
}

tasks.test {
    useJUnitPlatform()
}