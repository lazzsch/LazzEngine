plugins {
    kotlin("jvm") version "2.3.20" apply false
}

allprojects {
    group = "net.lazz"
    version = "1.0-SNAPSHOT"

    repositories {
        mavenCentral()
        maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") { name = "spigotmc-repo" }
        maven("https://jitpack.io")
    }
}

extra["spigotVersion"] = "1.21.1-R0.1-SNAPSHOT"


// ================= BUILD MODULES =================

tasks.register("buildModules") {

    val modules = subprojects.filter { it.path.startsWith(":modules:") }

    // 1️⃣ builda todos jars
    dependsOn(modules.map { it.tasks.named("jar") })

    doLast {

        val targetDir = file("core/src/main/resources/modules")

        targetDir.deleteRecursively()
        targetDir.mkdirs()

        modules.forEach { module ->

            val jarFile = module.layout.buildDirectory
                .file("libs/${module.name}-${module.version}.jar")
                .get()
                .asFile

            if (!jarFile.exists()) {
                throw GradleException("❌ Jar não encontrado: ${jarFile.path}")
            }

            println("🔥 Copiando módulo: ${jarFile.name}")

            jarFile.copyTo(
                target = file("$targetDir/${jarFile.name}"),
                overwrite = true
            )
        }
    }
}

// ================= CREATE MODULE =================

tasks.register("createModule") {

    doLast {

        val inputName = (project.findProperty("moduleName") ?: project.findProperty("module") ?: project.findProperty("m"))?.toString()
            ?: throw GradleException("Use: -Pm=NomeDoModulo")

        val moduleName = inputName.replaceFirstChar { it.uppercase() }
        val pkg = inputName.lowercase()

        val baseDir = file("modules/$pkg")

        if (baseDir.exists()) {
            throw GradleException("Módulo já existe: $pkg")
        }

        println("Criando módulo: $moduleName")

        val kotlinDir = File(baseDir, "src/main/kotlin/net/lazz/modules/$pkg")
        val resourceDir = File(baseDir, "src/main/resources")

        kotlinDir.mkdirs()
        resourceDir.mkdirs()

        // build.gradle.kts do módulo
        File(baseDir, "build.gradle.kts").writeText("""
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
        """.trimIndent())

        // Classe do módulo
        File(kotlinDir, "${moduleName}Module.kt").writeText("""
            package net.lazz.modules.$pkg

            import net.lazz.core.service.module.AbstractModule
            import net.lazz.core.service.module.ModuleContext
            import org.bukkit.plugin.java.JavaPlugin

            class ${moduleName}Module(
                plugin: JavaPlugin
            ) : AbstractModule(
                id = "$pkg",
                context = ModuleContext(plugin)
            ) {

                override fun onEnable() {
                    plugin.logger.info("[${moduleName}] Módulo ativado")
                }
            
                override fun onDisable() {
                    super.onDisable()
                    plugin.logger.info("[${moduleName}] Módulo desativado")
                }
            }
        """.trimIndent())

        // ================= COMMAND 1 (SIMPLES) =================

                File(kotlinDir, "${moduleName}SimpleCommand.kt").writeText("""
            package net.lazz.modules.$pkg
        
            import net.lazz.core.command.annotation.CommandInfo
            import org.bukkit.command.Command
            import org.bukkit.command.CommandExecutor
            import org.bukkit.command.CommandSender
            import org.bukkit.entity.Player
        
            @CommandInfo(
                name = "${pkg}simple",
                aliases = ["${pkg}s"]
            )
            class ${moduleName}SimpleCommand : CommandExecutor {
        
                override fun onCommand(
                    sender: CommandSender,
                    command: Command,
                    label: String,
                    args: Array<String>
                ): Boolean {
        
                    if (sender !is Player) {
                        sender.sendMessage("Apenas jogadores.")
                        return true
                    }
        
                    sender.sendMessage("§aComando simples funcionando!")
                    return true
                }
            }
        """.trimIndent())

        // ================= COMMAND 2 (COM SERVICE) =================

                File(kotlinDir, "${moduleName}Command.kt").writeText("""
            package net.lazz.modules.$pkg
        
            import net.lazz.core.command.annotation.CommandInfo
            import net.lazz.core.service.dependency.annotation.Inject
            import org.bukkit.command.Command
            import org.bukkit.command.CommandExecutor
            import org.bukkit.command.CommandSender
            import org.bukkit.entity.Player
        
            @CommandInfo(
                name = "$pkg",
                aliases = ["${pkg}test"]
            )
            class ${moduleName}Command : CommandExecutor {
        
                @Inject
                lateinit var service: ${moduleName}Service
        
                override fun onCommand(
                    sender: CommandSender,
                    command: Command,
                    label: String,
                    args: Array<String>
                ): Boolean {
        
                    if (sender !is Player) {
                        sender.sendMessage("Apenas jogadores.")
                        return true
                    }
        
                    sender.sendMessage("§a${moduleName} funcionando!")
                    sender.sendMessage("§7Service: " + service.hello())
        
                    return true
                }
            }
        """.trimIndent())

        // ================= SERVICE =================

        File(kotlinDir, "${moduleName}Service.kt").writeText("""
            package net.lazz.modules.$pkg
        
            import net.lazz.core.service.dependency.annotation.Service
            import java.util.UUID
            import java.util.concurrent.ConcurrentHashMap
        
            @Service
            class ${moduleName}Service {
        
                private val data = ConcurrentHashMap<UUID, Int>()
        
                fun get(uuid: UUID): Int {
                    return data.getOrDefault(uuid, 0)
                }
        
                fun set(uuid: UUID, value: Int) {
                    data[uuid] = value
                }
        
                fun add(uuid: UUID, value: Int) {
                    data[uuid] = get(uuid) + value
                }
        
                fun hello(): String {
                    return "Service funcionando!"
                }
            }
        """.trimIndent())

        // ================= LISTENER =================

                File(kotlinDir, "${moduleName}Listener.kt").writeText("""
            package net.lazz.modules.$pkg
        
            import net.lazz.core.listener.annotation.AutoListener
            import org.bukkit.event.EventHandler
            import org.bukkit.event.Listener
            import org.bukkit.event.player.PlayerJoinEvent
        
            @AutoListener
            class ${moduleName}Listener : Listener {
        
                @EventHandler
                fun onJoin(event: PlayerJoinEvent) {
                    event.player.sendMessage("§e[$moduleName] Bem-vindo!")
                }
            }
        """.trimIndent())

        // YAML
        File(resourceDir, "module-$pkg.yml").writeText("""
            id: $pkg
            name: $moduleName
            main: net.lazz.modules.$pkg.${moduleName}Module
            package: net.lazz.modules.$pkg
            version: "1.0"
            depends: []
        """.trimIndent())

        println("module-$pkg.yml criado")
        println("Módulo criado com sucesso: $pkg")
    }
}

// ================= BUILD COMPLETO =================

tasks.register("fullBuild") {
    dependsOn("buildModules")
    dependsOn(":core:build")
}