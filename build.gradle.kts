import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    application
    idea
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
    implementation(libs.playwright)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.clikt)
    implementation(libs.kotlinresult)
    implementation(project(":gcal-sync-kt"))

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

idea {
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
    }
}
