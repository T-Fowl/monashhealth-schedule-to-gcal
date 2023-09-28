import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.20"
    kotlin("plugin.serialization") version "1.7.20"
    application
}

group = "com.tfowl.monashhealth"
version = "1.0-SNAPSHOT"

application {
    mainClass.set("com.tfowl.monashhealth.cli.Main")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.microsoft.playwright:playwright:1.38.0")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")

    implementation("com.github.ajalt.clikt:clikt:3.4.0")

    implementation(project(":gcal-sync-kt"))

    implementation("com.michael-bull.kotlin-result:kotlin-result:1.1.14")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}