import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.1.20"
    id("io.ktor.plugin") version "3.1.2"
    kotlin("plugin.serialization") version "2.1.20"
    application
}



java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Ktor
    implementation("io.ktor:ktor-server-core-jvm:3.1.2")
    implementation("io.ktor:ktor-server-cio-jvm:3.1.2")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:3.1.2")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:3.1.2")
    implementation("io.ktor:ktor-serialization-gson-jvm:3.1.2")

    // Logging
    implementation("ch.qos.logback:logback-classic:1.5.13")

    // Security
    implementation("at.favre.lib:bcrypt:0.9.0")

    // Database
    implementation("org.jetbrains.exposed:exposed-core:0.50.1")
    implementation("org.jetbrains.exposed:exposed-dao:0.50.1")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.50.1")

    implementation("com.h2database:h2:2.2.220")
    implementation("org.ktorm:ktorm-core:3.6.0")
    implementation("org.ktorm:ktorm-support-mysql:3.6.0")
    implementation("mysql:mysql-connector-java:8.0.33")


    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("com.google.code.gson:gson:2.12.1")

    // HTTP Client
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Testing
    // https://mvnrepository.com/artifact/io.ktor/ktor-server-test-host-jvm
    implementation("io.ktor:ktor-server-test-host-jvm:3.1.2-eap-1251")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:2.1.20")
}

application {
    mainClass.set("com.example.ApplicationKt")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "17" // Синхронизируем с Java toolchain
        apiVersion = "1.9"
        languageVersion = "1.9"
    }
}

tasks.withType<JavaCompile> {
    sourceCompatibility = "17"
    targetCompatibility = "17"
}