plugins {
    kotlin("jvm")
}

group = "net.lazz"
version = "1.0"

repositories {
    maven("https://jitpack.io") { name = "jitpack" }
}

dependencies {
    compileOnly(project(":core"))
    compileOnly("org.spigotmc:spigot-api:${rootProject.extra["spigotVersion"]}")
}

tasks.jar {
    archiveBaseName.set(project.name)
}

tasks.processResources {

    val props = mapOf(
        "version" to project.version
    )

    inputs.properties(props)
    filteringCharset = "UTF-8"

    filesMatching("*.yml") {
        expand(props)
    }
}

kotlin {
    jvmToolchain(21)
}