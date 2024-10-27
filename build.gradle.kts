plugins {
    id("java")
    id("com.gradleup.shadow") version "8.3.0"
}

group = "me.levitate"
version = "2.1.0"

repositories {
    mavenCentral()

    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.codemc.io/repository/maven-public/")
    maven("https://repo.aikar.co/content/groups/aikar/")
    maven("https://jitpack.io")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT")
    compileOnly("me.clip:placeholderapi:2.11.6")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7")

    compileOnly("org.projectlombok:lombok:1.18.32")
    annotationProcessor("org.projectlombok:lombok:1.18.32")

    implementation("com.fasterxml.jackson.core:jackson-core:2.16.1")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.16.1")
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.16.1")

    implementation("dev.triumphteam:triumph-gui:3.1.10")
    implementation("co.aikar:acf-paper:0.5.1-SNAPSHOT")
    implementation("de.exlll:configlib-yaml:4.5.0")
}

tasks.shadowJar {
    relocate("de.exlll.configlib", "me.levitate.config")
    relocate("co.aikar.commands", "me.levitate.acf")
    relocate("co.aikar.locales", "me.levitate.locales")
    relocate("dev.triumphteam.gui", "me.levitate.gui")
}