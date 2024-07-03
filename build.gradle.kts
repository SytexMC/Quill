plugins {
    id("java")
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.featherservices"
version = "2.0.1"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT")

    compileOnly("org.projectlombok:lombok:1.18.32")
    annotationProcessor("org.projectlombok:lombok:1.18.32")

    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.17.0-rc1")
    implementation("org.apache.commons:commons-lang3:3.14.0")
    implementation("com.googlecode.json-simple:json-simple:1.1.1")
}