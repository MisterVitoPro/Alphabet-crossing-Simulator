import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.10"
    id("org.jlleitschuh.gradle.ktlint") version "11.0.0"
}

group = "com.aronagames"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.0")
    implementation("ch.qos.logback:logback-classic:1.4.1")
    implementation("ch.qos.logback:logback-core:1.4.1")
    implementation("org.slf4j:slf4j-api:2.0.1")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "15"
}
