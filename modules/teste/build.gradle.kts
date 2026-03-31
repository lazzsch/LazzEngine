plugins {
    kotlin("jvm")
}

group = "net.lazz"
version = "1.0"

repositories {
    //maven("example.com.br") { name = "name" }
}

dependencies {
    compileOnly(project(":core"))
    compileOnly("org.spigotmc:spigot-api:1.21.1-R0.1-SNAPSHOT")
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