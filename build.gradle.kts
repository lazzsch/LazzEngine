import org.gradle.api.Project
import org.gradle.api.tasks.bundling.Jar
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.File

plugins {
    kotlin("jvm") version "2.3.20"
}

group = "net.lazz"

// ===== CONFIG GLOBAL =====
extra.apply {
    set("javaVersion", 21)
    set("spigotVersion", "1.21.1-R0.1-SNAPSHOT")
    set("apiVersion", "1.21")
    set("coreDir", "E:/Principal/Dev/clientes/joao/Minecraft_Testes - 1.21.8/plugins")
    set("moduleDir", "E:/Principal/Dev/voxy/lazzengine/modules")
}

// ===== VERSIONAMENTO =====

fun getVersionFile(project: Project): File {
    val file = File(project.projectDir, "version.txt")
    if (!file.exists()) file.writeText("0.0.1")
    return file
}

fun readCurrentVersion(project: Project): String {
    return getVersionFile(project).readText().trim().ifBlank { "0.0.1" }
}

fun bumpPatch(version: String): String {
    val parts = version.split(".")
    require(parts.size >= 3) { "Versão inválida: $version" }

    val major = parts[0].toInt()
    val minor = parts[1].toInt()
    val patch = parts[2].toInt()

    return "$major.$minor.${patch + 1}"
}

// ===== CONFIG SUBPROJECTS =====

subprojects {

    if (project.path == ":modules") {
        tasks.configureEach { enabled = false }
        return@subprojects
    }

    apply(plugin = "org.jetbrains.kotlin.jvm")

    version = readCurrentVersion(project)

    repositories {
        mavenCentral()
        maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    }

    extensions.configure<KotlinJvmProjectExtension> {
        jvmToolchain(rootProject.extra["javaVersion"].toString().toInt())
    }

    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
    }

    dependencies {
        add("compileOnly", "org.spigotmc:spigot-api:${rootProject.extra["spigotVersion"]}")
        add("implementation", "org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    }

    tasks.withType<Jar>().configureEach {

        outputs.upToDateWhen { false }

        archiveFileName.set(
            when {
                project.path == ":core" -> "${rootProject.name}-${project.version}.jar"
                project.path.startsWith(":modules:") -> "${project.name}.jar"
                else -> "${project.name}.jar"
            }
        )

        destinationDirectory.set(
            if (project.path == ":core") {
                file(rootProject.extra["coreDir"] as String)
            } else {
                file(rootProject.extra["moduleDir"] as String)
            }
        )
    }

    if (project.path.startsWith(":modules:")) {

        dependencies {
            add("implementation", project(":core"))
        }

        tasks.named("compileKotlin").configure {
            dependsOn(project(":core").tasks.named("compileKotlin"))
        }
    }

    tasks.withType<ProcessResources>().configureEach {

        val props = mapOf(
            "version" to project.version.toString(),
            "apiVersion" to rootProject.extra["apiVersion"].toString()
        )

        inputs.properties(props)
        filteringCharset = "UTF-8"

        filesMatching(listOf("plugin.yml", "*.yml")) {
            expand(props)
        }
    }
}

// ===== VERSIONAMENTO GLOBAL (FIX FINAL) =====

tasks.register("resolveAllVersions") {

    doFirst {

        val noVersion = gradle.startParameter.projectProperties["v"] == "true"
        val processed = mutableSetOf<String>()

        subprojects
            .filter { it.path.startsWith(":modules:") || it.path == ":core" }
            .forEach { project ->

                if (processed.contains(project.path)) return@forEach
                processed.add(project.path)

                val file = getVersionFile(project)
                val current = file.readText().trim().ifBlank { "0.0.1" }

                if (noVersion) {
                    project.version = current
                    println("${project.name} versao mantida -> $current")
                    return@forEach
                }

                val next = bumpPatch(current)

                file.writeText(next)
                project.version = next

                println("${project.name} versao -> $next")
            }
    }
}

gradle.projectsEvaluated {
    tasks.matching { it.name == "build" }.configureEach {
        dependsOn("resolveAllVersions")
    }
}

// ================= BUILD MODULES =================

tasks.register("buildModules") {

    dependsOn(subprojects.map { it.tasks.matching { t -> t.name == "jar" } })

    doLast {

        subprojects
            .filter { it.path.startsWith(":modules:") }
            .forEach { project ->

                val jarTask = project.tasks.named("jar").get() as Jar
                val jarFile = jarTask.archiveFile.get().asFile

                if (!jarFile.exists()) {
                    throw GradleException("Jar nao encontrado: ${jarFile.absolutePath}")
                }

                println("✔ Modulo OK: ${jarFile.name}")
            }
    }
}

// ================= VERSION TASK =================

tasks.register("bumpVersion") {
    doLast {
        val newVersion = bumpVersionHelper(project)
        println("Nova versao: $newVersion")
    }
}

// ================= CREATE MODULE =================

tasks.register("createModuleApi") {

    doLast {

        val inputName = (project.findProperty("moduleName")
            ?: project.findProperty("module")
            ?: project.findProperty("m"))?.toString()
            ?: throw GradleException("Use: -Pm=NomeDoModulo")

        val moduleName = inputName.replaceFirstChar { it.uppercase() }
        val pkg = inputName.lowercase()

        val baseDir = file("modules/$pkg")

        if (baseDir.exists()) {
            throw GradleException("Modulo ja existe: $pkg")
        }

        println("Criando módulo API: $moduleName")

        val kotlinDir = File(baseDir, "src/main/kotlin/net/lazz/modules/$pkg")
        val resourceDir = File(baseDir, "src/main/resources")

        kotlinDir.mkdirs()
        resourceDir.mkdirs()

        File(baseDir, "version.txt").writeText("0.0.1")

        File(baseDir, "build.gradle.kts").writeText(
            """
            dependencies {
                implementation(project(":core"))
            }
            """.trimIndent()
        )

        // MODULE
        File(kotlinDir, "${moduleName}Module.kt").writeText(
            """
            package net.lazz.modules.$pkg

            import net.lazz.core.services.module.AbstractModule
            import org.bukkit.plugin.java.JavaPlugin

            class ${moduleName}Module(
                plugin: JavaPlugin
            ) : AbstractModule(
                plugin = plugin,
                id = "$pkg"
            ) {

                override fun onEnable() {
                    plugin.logger.info("[${moduleName}] Módulo ativado")
                }
            
                override fun onDisable() {
                    super.onDisable()
                    plugin.logger.info("[${moduleName}] Módulo desativado")
                }
            }
            """.trimIndent()
        )

        // SERVICE API
        File(kotlinDir, "${moduleName}Service.kt").writeText(
            """
            package net.lazz.modules.$pkg

            import net.lazz.core.services.module.api.ModuleCallable
            import net.lazz.core.services.module.api.annotation.ModuleAPI
            import java.util.UUID
            import java.util.concurrent.ConcurrentHashMap

            @ModuleAPI("${moduleName}Service")
            class ${moduleName}Service : ModuleCallable {

                private val data = ConcurrentHashMap<UUID, Int>()

                fun get(uuid: UUID): Int {
                    return data.getOrDefault(uuid, 0)
                }

                fun add(uuid: UUID, value: Int) {
                    data[uuid] = get(uuid) + value
                }

                override fun call(method: String, vararg args: Any?): Any? {
                    return when (method) {
                        "get" -> get(args[0] as UUID)
                        "add" -> {
                            add(args[0] as UUID, args[1] as Int)
                            null
                        }
                        else -> null
                    }
                }
            }
            """.trimIndent()
        )

        // COMMAND
        File(kotlinDir, "${moduleName}Command.kt").writeText(
            """
            package net.lazz.modules.$pkg

            import net.lazz.core.services.module.service.command.annotation.ModuleCommand
            import org.bukkit.command.Command
            import org.bukkit.command.CommandExecutor
            import org.bukkit.command.CommandSender
            import org.bukkit.entity.Player

            @ModuleCommand(name = "$pkg", aliases = ["${pkg}test"])
            class ${moduleName}Command : CommandExecutor {

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

                    sender.sendMessage("§eteste")

                    return true
                }
            }
            """.trimIndent()
        )

        // Listener
        File(kotlinDir, "${moduleName}Listener.kt").writeText(
            """
            package net.lazz.modules.$pkg
        
            import net.lazz.core.services.module.service.listener.ModuleListener
            import org.bukkit.event.EventHandler
            import org.bukkit.event.Listener
            import org.bukkit.event.player.PlayerJoinEvent
        
            @ModuleListener
            class ${moduleName}Listener : Listener {
        
                @EventHandler
                fun onJoin(event: PlayerJoinEvent) {
                    event.player.sendMessage("§e[$moduleName] Bem-vindo!")
                }
            }
            """.trimIndent()
        )

        // YAML
        File(resourceDir, "module-$pkg.yml").writeText(
            $$"""
            id: $$pkg
            name: $$moduleName
            main: net.lazz.modules.$$pkg.$${moduleName}Module
            package: net.lazz.modules.$$pkg
            version: ${version}
            """.trimIndent()
        )

        println("Modulo API criado: $pkg")
    }
}

tasks.register("createModuleDepend") {

    doLast {

        val inputName = (project.findProperty("moduleName")
            ?: project.findProperty("module")
            ?: project.findProperty("m"))?.toString()
            ?: throw GradleException("Use: -Pm=NomeDoModulo")

        val moduleName = inputName.replaceFirstChar { it.uppercase() }
        val pkg = inputName.lowercase()

        val baseDir = file("modules/$pkg")

        if (baseDir.exists()) {
            throw GradleException("Modulo ja existe: $pkg")
        }

        println("Criando modulo exemplo dependente: $moduleName")

        val kotlinDir = File(baseDir, "src/main/kotlin/net/lazz/modules/$pkg")
        val resourceDir = File(baseDir, "src/main/resources")

        kotlinDir.mkdirs()
        resourceDir.mkdirs()

        File(baseDir, "version.txt").writeText("0.0.1")

        File(baseDir, "build.gradle.kts").writeText(
            """
            dependencies {
                implementation(project(":core"))
            }
            """.trimIndent()
        )

        // MODULE
        File(kotlinDir, "${moduleName}Module.kt").writeText(
            """
            package net.lazz.modules.$pkg

            import net.lazz.core.services.module.AbstractModule
            import net.lazz.core.services.module.service.annotation.Depend
            import org.bukkit.plugin.java.JavaPlugin

            @Depend("teste") //não esquecer de mudar para dependencia correta.
            class ${moduleName}Module(
                plugin: JavaPlugin
            ) : AbstractModule(
                plugin = plugin,
                id = "$pkg"
            ) {

                override fun onEnable() {
                    plugin.logger.info("[${moduleName}] Módulo ativado")
                }
            
                override fun onDisable() {
                    super.onDisable()
                    plugin.logger.info("[${moduleName}] Módulo desativado")
                }
            }
            """.trimIndent()
        )

        // COMMAND (CONSUME API)
        File(kotlinDir, "${moduleName}Command.kt").writeText(
            $$"""
            package net.lazz.modules.$$pkg

            import net.lazz.core.services.module.api.ModuleAPI
            import net.lazz.core.services.module.service.command.annotation.ModuleCommand
            import org.bukkit.command.Command
            import org.bukkit.command.CommandExecutor
            import org.bukkit.command.CommandSender
            import org.bukkit.entity.Player

            @ModuleCommand(name = "$$pkg", aliases = ["$${pkg}test"])
            class $${moduleName}Command : CommandExecutor {

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

                    val uuid = sender.uniqueId

                    val value = ModuleAPI.callAs<Int>("dependencia", "get", uuid) ?: 0

                    sender.sendMessage("§aValor atual: $value")

                    ModuleAPI.call("dependencia", "add", uuid, 50)

                    val after = ModuleAPI.callAs<Int>("dependencia", "get", uuid)

                    sender.sendMessage("§eDepois +50: $after")

                    return true
                }
            }
            """.trimIndent()
        )

        // Listener
        File(kotlinDir, "${moduleName}Listener.kt").writeText(
            """
            package net.lazz.modules.$pkg
        
            import net.lazz.core.services.module.service.listener.ModuleListener
            import org.bukkit.event.EventHandler
            import org.bukkit.event.Listener
            import org.bukkit.event.player.PlayerJoinEvent
        
            @ModuleListener
            class ${moduleName}Listener : Listener {
        
                @EventHandler
                fun onJoin(event: PlayerJoinEvent) {
                    event.player.sendMessage("§e[$moduleName] Bem-vindo!")
                }
            }
            """.trimIndent()
        )

        // YAML
        File(resourceDir, "module-$pkg.yml").writeText(
            $$"""
            id: $$pkg
            name: $$moduleName
            main: net.lazz.modules.$$pkg.$${moduleName}Module
            package: net.lazz.modules.$$pkg
            version: ${version}
            """.trimIndent()
        )

        println("Modulo dependente criado: $pkg")
    }
}

// ================= HELPER =================

fun runWithLoading(title: String, command: List<String>) {
    println("\n$title...\n")

    val process = ProcessBuilder(command)
        .redirectErrorStream(true)
        .start()

    val start = System.currentTimeMillis()

    process.inputStream.bufferedReader().use { reader ->
        reader.lineSequence().forEach { println(it) }
    }

    val result = process.waitFor()
    val time = (System.currentTimeMillis() - start) / 1000.0

    if (result == 0) {
        println("\n$title finalizado com sucesso (${time}s)\n")
        return
    }

    throw GradleException("Erro ao executar $title (code $result) (${time}s)")
}

fun gradleCommand(vararg args: String): List<String> {
    val isWindows = System.getProperty("os.name").lowercase().contains("win")

    return if (isWindows) {
        listOf("cmd", "/c", "gradlew.bat", *args)
    } else {
        listOf("./gradlew", *args)
    }
}

fun ask(q: String): String {
    val console = System.console()
    return if (console != null) {
        console.readLine(q)?.trim().orEmpty()
    } else {
        print(q)
        System.out.flush()
        val reader = System.`in`.bufferedReader()
        reader.readLine()?.trim().orEmpty()
    }
}

fun choose(title: String, options: List<String>): Int {
    println("\n$title\n")
    options.forEachIndexed { i, opt -> println("${i + 1}. $opt") }
    return ask("\nEscolha: ").toIntOrNull() ?: -1
}

// ================= EXEC =================

fun execTask(task: Task) {
    task.actions.forEach { it.execute(task) }
}

// ================= VERSION =================

fun bumpVersionHelper(project: Project): String {
    val file = getVersionFile(project)

    val (maj, min, pat) = file.readText().split(".").map { it.toInt() }
    val newVersion = "$maj.$min.${pat + 1}"

    file.writeText(newVersion)
    return newVersion
}

// ================= CHANGELOG =================

fun updateGlobalChangelog(
    version: String,
    type: String,
    desc: String,
    isCore: Boolean,
    module: String?
) {
    val file = File("CHANGELOG.md")
    if (!file.exists()) file.writeText("# Changelog\n")

    val content = file.readText()

    val header = "## v$version"
    val section = if (isCore) "### Core" else "### Modules"

    val entry = if (isCore) {
        "- $type: $desc"
    } else {
        "- ${module}: $type $desc"
    }

    val newContent = if (!content.contains(header)) {
        "$content\n$header\n\n$section\n$entry\n"
    } else {
        content.replace(header, "$header\n\n$section\n$entry")
    }

    file.writeText(newContent)
}

// ================= COMMIT =================

fun getChangedFiles(): List<String> {
    val process = ProcessBuilder("git", "status", "--porcelain")
        .redirectErrorStream(true)
        .start()

    val files = mutableListOf<String>()

    process.inputStream.bufferedReader().useLines { lines ->
        lines.forEach { line ->
            val file = line.substring(3)
            files.add(file)
        }
    }

    process.waitFor()
    return files
}

fun selectFiles(files: List<String>): List<String> {
    val selected = mutableListOf<String>()

    println("\nArquivos modificados:\n")

    files.forEachIndexed { i, file ->
        val input = ask("[${i + 1}] $file (y/n): ").lowercase()
        if (input == "y" || input == "s") {
            selected.add(file)
        }
    }

    return selected
}

fun commit() {

    val files = getChangedFiles()

    if (files.isEmpty()) {
        println("Nenhuma alteração encontrada.")
        return
    }

    val selected = selectFiles(files)

    if (selected.isEmpty()) {
        println("Nenhum arquivo selecionado.")
        return
    }

    val types = listOf("feat", "fix", "refactor", "perf", "docs", "style", "test", "chore")

    val type = types.getOrNull(choose("Tipo:", types) - 1) ?: return
    val desc = ask("Descricao: ")

    if (desc.isBlank()) return println("Descricao obrigatoria")

    val isCore = selected.any { it.startsWith("core") }

    val moduleName = selected
        .find { it.startsWith("modules/") }
        ?.split("/")
        ?.getOrNull(1)

    // VERSION
    val projectDir = when {
        isCore -> File("core")
        moduleName != null -> File("modules/$moduleName")
        else -> File(".")
    }

    val versionFile = File(projectDir, "version.txt")
    if (!versionFile.exists()) versionFile.writeText("0.0.1")

    val (maj, min, pat) = versionFile.readText().split(".").map { it.toInt() }
    val newVersion = "$maj.$min.${pat + 1}"
    versionFile.writeText(newVersion)

    println("Nova versao: $newVersion")

    // CHANGELOG AUTO
    val changes = selected.joinToString("\n") { "- $it" }
    updateGlobalChangelog(newVersion, type, changes, isCore, moduleName)

    val finalMsg = if (moduleName != null) {
        "$type($moduleName): $desc"
    } else {
        "$type: $desc"
    }

    // ADD APENAS SELECIONADOS
    selected.forEach {
        runWithLoading("Git add $it", listOf("git", "add", it))
    }

    runWithLoading("Git commit", listOf("git", "commit", "-m", finalMsg))
    runWithLoading("Git push", listOf("git", "push"))

    println("Commit realizado com sucesso")
}

// ================= RELEASE =================

fun release(project: Project) {

    val coreVersionFile = File("core/version.txt")
    if (!coreVersionFile.exists()) {
        println("Core version.txt nao encontrado")
        return
    }

    val version = coreVersionFile.readText().trim()

    println("\nCriando release v$version\n")

    runWithLoading("Criando tag", listOf("git", "tag", "v$version"))
    runWithLoading("Enviando tag", listOf("git", "push", "origin", "v$version"))

    println("Release criada com sucesso")
}

// ================= CREATE MODULE =================

fun createModule(project: Project) {

    println("Tipo de modulo:")
    println("1 - API (fornece servico)")
    println("2 - Dependente (usa outro modulo)")

    val type = ask("Escolha (1/2): ").trim()

    val name = ask("Nome do modulo: ").trim()
    if (name.isBlank()) return println("Nome invalido")

    project.extensions.extraProperties.set("module", name)

    when (type) {

        "1" -> {
            val task = project.tasks.getByName("createModuleApi")
            execTask(task)

            println("Modulo API criado com sucesso: $name")
        }

        "2" -> {
            val task = project.tasks.getByName("createModuleDepend")
            execTask(task)

            println("Modulo dependente criado com sucesso: $name")
        }

        else -> {
            println("Tipo invalido")
        }
    }
}


// ================= BUMP =================

fun bumpCore(project: Project) {
    val core = project.findProject(":core") ?: return
    val version = bumpVersionHelper(core)
    println("Nova versao core: $version")
}

fun bumpModule(project: Project) {
    val name = ask("Modulo: ")
    val module = project.findProject(":modules:${name.lowercase()}") ?: return
    val version = bumpVersionHelper(module)
    println("Nova versao modulo: $version")
}

// ================= MENU =================

fun runHelper(project: Project) {

    println("\n[LazzEngine Helper]\n")

    when (choose("Selecione uma opcao:", listOf(
        "Commit",
        "Criar modulo",
        "Build completo",
        "Build apenas core",
        "Build modulo especifico",
    ))) {

        1 -> commit()
        2 -> createModule(project)

        3 -> {
            val noVersion = ask("Sem versionamento? (s/n): ").lowercase() == "s"
            val versionFlag = if (noVersion) listOf("-Pv=true") else emptyList()

            runWithLoading(
                "Build completo",
                gradleCommand("build", "-x", "test", *versionFlag.toTypedArray())
            )
        }

        4 -> {
            val noVersion = ask("Sem versionamento? (s/n): ").lowercase() == "s"
            val versionFlag = if (noVersion) listOf("-Pv=true") else emptyList()

            runWithLoading(
                "Build core",
                gradleCommand(":core:jar", *versionFlag.toTypedArray())
            )
        }

        5 -> {
            val name = ask("Modulo: ")
            if (name.isBlank()) {
                println("Nome invalido")
                return
            }

            val noVersion = ask("Sem versionamento? (s/n): ").lowercase() == "s"
            val versionFlag = if (noVersion) listOf("-Pv=true") else emptyList()

            runWithLoading(
                "Build modulo $name",
                gradleCommand(":modules:${name.lowercase()}:jar", *versionFlag.toTypedArray())
            )
        }

        else -> println("Opcao invalida")
    }
}

tasks.register("helper") {
    doLast {
        runHelper(project)
    }
}
