plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "LazzEngine"

include(":core")

// garante que a pasta existe
file("modules").mkdirs()

// auto-detect modules dentro de /modules
file("modules").listFiles()
    ?.filter { it.isDirectory }
    ?.forEach { dir ->

        val modulePath = ":modules:${dir.name}"
        val moduleBuild = file("modules/${dir.name}/build.gradle.kts")

        if (moduleBuild.exists()) {

            include(modulePath)

            project(modulePath).projectDir = file("modules/${dir.name}")
        }
    }