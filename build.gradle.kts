import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.0.0"
    kotlin("plugin.serialization") version "2.0.0"
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
    implementation("com.microsoft.playwright:playwright:1.46.0")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")

    implementation("com.github.ajalt.clikt:clikt:3.4.0")

    implementation(project(":gcal-sync-kt"))

    implementation("com.michael-bull.kotlin-result:kotlin-result:1.1.14")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "21"
}