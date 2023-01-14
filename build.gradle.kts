import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("java")
    kotlin("jvm") version "1.8.0"
    `maven-publish`
}

group = "info.skyblond"
version = "v1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("net.java.dev.jna:jna:5.12.1")
    implementation("net.java.dev.jna:jna-platform:5.12.1")

    testImplementation(kotlin("test"))
    testImplementation("org.pcap4j:pcap4j-core:1.8.2")
    testImplementation("org.pcap4j:pcap4j-packetfactory-static:1.8.2")
    testImplementation("ch.qos.logback:logback-classic:1.4.5")
}

tasks.test {
    useJUnitPlatform()
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

publishing {
    publications {
        create<MavenPublication>("maven"){
            from(components["java"])
        }
    }
}
