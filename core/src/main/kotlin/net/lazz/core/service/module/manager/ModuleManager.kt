package net.lazz.core.service.module.manager

import net.lazz.core.service.module.AbstractModule
import net.lazz.core.service.module.ModuleModel
import net.lazz.core.service.module.model.ModuleDescription
import net.lazz.core.service.module.util.CommandScanner
import net.lazz.core.service.module.util.ServiceScanner
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.net.URLClassLoader
import java.util.jar.JarFile

class ModuleManager(
    private val plugin: JavaPlugin
) {

    private val modules = mutableMapOf<String, ModuleModel>()
    private val descriptions = mutableMapOf<String, ModuleDescription>()
    private val enabled = mutableSetOf<String>()

    // 🔥 NOVO: map de arquivos
    private val moduleFiles = mutableMapOf<String, File>()

    private val modulesFolder = File(plugin.dataFolder, "modules")

    private val prefix = "[Module]"

    private fun log(msg: String) = plugin.logger.info("$prefix $msg")
    private fun error(msg: String) = plugin.logger.severe("$prefix $msg")

    // ================= INIT =================

    fun init() {
        log("Inicializando sistema de módulos...")
        createFolders()
        checkModulesFolder()
    }

    private fun createFolders() {
        if (!plugin.dataFolder.exists()) {
            plugin.dataFolder.mkdirs()
            log("Pasta do plugin criada")
        }

        if (!modulesFolder.exists()) {
            modulesFolder.mkdirs()
            log("Pasta modules criada")
        }
    }

    private fun checkModulesFolder() {
        val jars = modulesFolder.listFiles { f -> f.name.endsWith(".jar") }

        if (jars == null || jars.isEmpty()) {
            error("Nenhum módulo encontrado na pasta modules")
            return
        }

        log("${jars.size} arquivo(s) .jar encontrado(s)")
    }

    // ================= LOAD =================

    fun loadModules() {

        val jars = modulesFolder.listFiles { f -> f.name.endsWith(".jar") } ?: return

        var loaded = 0

        jars.forEach { file ->

            try {
                JarFile(file).use { jar ->

                    val entry = jar.entries().asSequence()
                        .firstOrNull {
                            it.name.startsWith("module-") && it.name.endsWith(".yml")
                        }

                    if (entry == null) {
                        error("${file.name} não possui module.yml")
                        return@forEach
                    }

                    val yaml = org.yaml.snakeyaml.Yaml()
                        .load<Map<String, Any>>(jar.getInputStream(entry))

                    val desc = ModuleDescription(
                        id = yaml["id"].toString(),
                        name = yaml["name"].toString(),
                        main = yaml["main"].toString(),
                        packageName = yaml["package"].toString(),
                        version = yaml["version"].toString(),
                        depends = (yaml["depends"] as? List<*>)?.map { it.toString() } ?: emptyList()
                    )

                    val key = desc.id.lowercase()

                    descriptions[key] = desc
                    moduleFiles[key] = file

                    log("Carregando módulo: ${desc.id}")

                    val classLoader = URLClassLoader(
                        arrayOf(file.toURI().toURL()),
                        plugin.javaClass.classLoader
                    )

                    val clazz = Class.forName(desc.main, true, classLoader)
                    val constructor = clazz.getConstructor(JavaPlugin::class.java)

                    val module = constructor.newInstance(plugin) as ModuleModel

                    modules[key] = module

                    module.onLoad()

                    log("Módulo ${desc.id} carregado")

                    loaded++

                }
            } catch (ex: Exception) {
                error("Erro ao carregar ${file.name}: ${ex.message}")
                ex.printStackTrace()
            }
        }

        if (loaded == 0) {
            error("Nenhum módulo válido foi carregado")
        } else {
            log("$loaded módulo(s) carregado(s)")
        }
    }

    // ================= ENABLE =================

    fun enable(id: String, silent: Boolean = false) {
        val key = id.lowercase()
        val module = modules[key]
        val desc = descriptions[key]

        if (module == null || desc == null) {
            error("Módulo não encontrado: $id")
            return
        }

        if (enabled.contains(key)) {
            if (!silent) log("Módulo já está ativo: $id")
            return
        }

        if (!silent) log("Ativando módulo: ${desc.id}")

        try {
            desc.depends.forEach {
                if (!enabled.contains(it.lowercase())) {
                    error("${desc.id} depende de $it e não está ativo")
                    return
                }
            }

            val context = (module as? AbstractModule)?.context
                ?: throw IllegalStateException("Sem context")

            ServiceScanner.registerServices(
                plugin,
                desc.packageName,
                context.serviceRegistry
            )

            CommandScanner.registerCommands(
                plugin,
                desc.packageName,
                context.commandRegistry,
                context.serviceRegistry
            )

            module.onEnable()
            enabled.add(key)

            if (!silent) log("Módulo ${desc.id} ativado")

        } catch (ex: Exception) {
            error("Erro ao ativar ${desc.id}: ${ex.message}")
            ex.printStackTrace()
        }
    }

    fun enableAll() {
        modules.keys.forEach { enable(it) }
    }

    // ================= LOAD SINGLE =================

    fun loadModule(id: String) {

        val key = id.lowercase()

        if (modules.containsKey(key)) {
            error("Módulo já carregado: $id")
            return
        }

        val file = modulesFolder.listFiles { f ->
            f.name.equals("$key.jar", true) || f.name.startsWith("$key-", true)
        }?.firstOrNull()

        if (file == null) {
            error("Arquivo do módulo não encontrado: $id")
            return
        }

        try {
            JarFile(file).use { jar ->

                val entry = jar.entries().asSequence()
                    .firstOrNull {
                        it.name.startsWith("module-") && it.name.endsWith(".yml")
                    }

                if (entry == null) {
                    error("${file.name} não possui module.yml")
                    return
                }

                val yaml = org.yaml.snakeyaml.Yaml()
                    .load<Map<String, Any>>(jar.getInputStream(entry))

                val desc = ModuleDescription(
                    id = yaml["id"].toString(),
                    name = yaml["name"].toString(),
                    main = yaml["main"].toString(),
                    packageName = yaml["package"].toString(),
                    version = yaml["version"].toString(),
                    depends = (yaml["depends"] as? List<*>)?.map { it.toString() } ?: emptyList()
                )

                val moduleKey = desc.id.lowercase()

                descriptions[moduleKey] = desc
                moduleFiles[moduleKey] = file

                log("Carregando módulo: ${desc.id}")

                val classLoader = URLClassLoader(
                    arrayOf(file.toURI().toURL()),
                    plugin.javaClass.classLoader
                )

                val clazz = Class.forName(desc.main, true, classLoader)
                val constructor = clazz.getConstructor(JavaPlugin::class.java)

                val module = constructor.newInstance(plugin) as ModuleModel

                modules[moduleKey] = module

                module.onLoad()

                log("Módulo ${desc.id} carregado")

                // 🔥 já ativa automaticamente
                enable(moduleKey)

            }

        } catch (ex: Exception) {
            error("Erro ao carregar ${file.name}: ${ex.message}")
            ex.printStackTrace()
        }
    }

    // ================= RELOAD =================

    fun reload(id: String) {

        val key = id.lowercase()

        if (!modules.containsKey(key)) {
            error("Módulo não encontrado: $id")
            return
        }

        log("Recarregando módulo: $id")

        // 🔥 desativa SEM log spam
        disable(id, silent = true)

        plugin.server.scheduler.runTaskLater(plugin, Runnable {

            try {
                // 🔥 ativa SEM duplicar logs
                enable(id, silent = true)

                log("Módulo $id recarregado")

            } catch (ex: Exception) {
                error("Erro ao recarregar $id: ${ex.message}")
                ex.printStackTrace()
            }

        }, 60L)
    }

    // ================= DISABLE =================

    fun disable(id: String, silent: Boolean = false) {
        val key = id.lowercase()
        val module = modules[key]

        if (module == null) {
            error("Módulo não encontrado: $id")
            return
        }

        if (!enabled.contains(key)) {
            if (!silent) log("Módulo já está desativado: $id")
            return
        }

        if (!silent) log("Desativando módulo: $id")

        try {
            module.onDisable()
            enabled.remove(key)

            if (!silent) log("Módulo $id desativado")

        } catch (ex: Exception) {
            error("Erro ao desativar $id: ${ex.message}")
            ex.printStackTrace()
        }
    }

    fun disableAll() {
        enabled.toList().reversed().forEach { disable(it) }
    }

    // ================= DELETE =================

    fun deleteModule(id: String) {

        val key = id.lowercase()

        if (enabled.contains(key)) {
            disable(id)
        }

        val file = moduleFiles[key]

        if (file == null || !file.exists()) {
            error("Arquivo do módulo não encontrado: $id")
            return
        }

        if (file.delete()) {
            log("Módulo $id deletado do disco")
        } else {
            error("Falha ao deletar módulo $id")
        }

        modules.remove(key)
        descriptions.remove(key)
        moduleFiles.remove(key)
    }

    // ================= LIST =================

    fun getFormattedList(): List<String> {
        if (modules.isEmpty()) return listOf("Nenhum módulo carregado.")

        return modules.keys.map {
            val status = if (enabled.contains(it)) "ATIVO" else "DESATIVADO"
            "$it - $status"
        }
    }

    fun isEnabled(id: String): Boolean {
        return enabled.contains(id.lowercase())
    }
}