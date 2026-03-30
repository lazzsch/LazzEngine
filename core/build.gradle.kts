plugins {
    kotlin("jvm") version "2.3.20"
    id("xyz.jpenilla.run-paper") version "2.3.1"
}

group = "net.lazz"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") { name = "spigotmc-repo" }
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:${rootProject.extra["spigotVersion"]}")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
}

tasks {
    runServer {
        minecraftVersion("1.21")
    }
}

kotlin {
    jvmToolchain(21)
}

tasks.jar {
    archiveFileName.set("${rootProject.name}-${project.version}.jar")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    // 🔥 dependências (shade)
    from({
        configurations.runtimeClasspath.get()
            .filter { it.name.endsWith(".jar") }
            .map { zipTree(it) }
    })

    // 🔥 GARANTE QUE OS MODULES ENTREM
    from("src/main/resources") {
        include("modules/**")
    }

    destinationDirectory.set(file("E:\\Principal\\Dev\\clientes\\joao\\Minecraft_Testes - 1.21.8\\plugins"))
}

tasks.processResources {

    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = "UTF-8"

    filesMatching("plugin.yml") {
        expand(props)
    }
}