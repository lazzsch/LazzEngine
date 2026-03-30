rootProject.name = "LazzPlugins"

include(":core")

// 🔥 auto-detecta módulos dentro de /modules
file("modules").listFiles()
    ?.filter { it.isDirectory }
    ?.forEach { dir ->

        val moduleBuild = file("modules/${dir.name}/build.gradle.kts")

        if (moduleBuild.exists()) {

            val path = ":modules:${dir.name}"

            include(path)

            project(path).projectDir = dir
        }
    }
