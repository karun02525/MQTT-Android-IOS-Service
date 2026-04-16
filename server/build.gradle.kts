plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ktor)
    kotlin("plugin.serialization") version "1.9.22"
    application
}

group = "org.smarthome"
version = "1.0.0"
application {
    mainClass.set("org.smarthome.MainKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

dependencies {
    implementation(projects.shared)
    implementation(libs.logback)
    implementation(libs.ktor.serverCore)
    implementation(libs.ktor.serverNetty)

    implementation("io.moquette:moquette-broker:0.17")

    // ✅ MQTT Client — Publisher + Subscriber
    implementation("org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.5")

    // ✅ JSON — Build and parse payloads
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")

    // ✅ Coroutines — Non-blocking delays
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

    // ✅ Logging
    implementation("ch.qos.logback:logback-classic:1.4.11")

    implementation(libs.org.eclipse.paho.client.mqttv3)
    implementation(libs.moquette.broker)



    testImplementation(libs.ktor.serverTestHost)
    testImplementation(libs.kotlin.testJunit)
}