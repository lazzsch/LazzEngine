package net.lazz.core.services.module

import  net.lazz.core.Core
import net.lazz.core.services.module.model.ModuleDescription
import net.lazz.core.services.module.service.container.ModuleServiceContainer
import net.lazz.core.services.module.service.scanner.ModuleServiceScanner
import net.lazz.core.services.module.service.listener.ModuleListenerScanner
import net.lazz.core.annotation.OnDisable
import net.lazz.core.annotation.OnEnable
import net.lazz.core.annotation.OnLoad
import net.lazz.core.annotation.Service
import net.lazz.core.services.module.api.ModuleAPI
import net.lazz.core.services.module.command.ModuleCommand
import net.lazz.core.services.module.model.ModuleModel
import net.lazz.core.services.module.service.ModuleService
import net.lazz.core.services.module.service.annotation.Depend
import net.lazz.core.services.module.service.annotation.SoftDepend
import net.lazz.core.services.module.service.command.ModuleCommandScanner
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.net.URLClassLoader
import java.util.concurrent.CompletableFuture
import java.util.jar.JarFile

data class ModuleView(
    val id: String,
    val name: String,
    val version: String,
    val enabled: Boolean,
    val depends: List<String>
)

data class ModuleLoadResult(
    val loaded: Int,
    val errors: Int,
    val found: Int
) {
    val success: Boolean
        get() = loaded > 0 && errors == 0

    val hasNew: Boolean
        get() = loaded > 0

    val empty: Boolean
        get() = found == 0
}

data class ModuleActionResult(
    val success: Boolean,
    val message: String
)

@Service
class ModuleManager(
    val plugin: Core
) {

    // ================= STORAGE =================

    private val classLoaders = mutableMapOf<String, URLClassLoader>()
    private val modules = mutableMapOf<String, net.lazz.core.services.module.model.ModuleModel>()
    private val descriptions = mutableMapOf<String, net.lazz.core.services.module.model.ModuleDescription>()
    private val moduleFiles = mutableMapOf<String, File>()
    val moduleContexts = mutableMapOf<String, net.lazz.core.services.module.ModuleContext>()
    private val enabled = mutableSetOf<String>()

    private val modulesFolder = File(plugin.dataFolder, "modules")

    // ================= LOADING =================

    private val loading = mutableSetOf<String>()

    fun isLoading(id: String): Boolean {
        return loading.contains(id.lowercase())
    }

    fun toggle(id: String) {
        val key = id.lowercase()

        if (loading.contains(key)) return

        if (enabled.contains(key)) {
            disable(id)
        } else {
            enable(id)
        }
    }

    // ================= LOG =================

    private val prefix = "[Module]"
    private var debugLogs = false
    private var verboseLogs = true

    private fun log(msg: String) {
        if (verboseLogs) {
            plugin.logger.info("$prefix $msg")
        }
    }

    private fun debug(msg: String) {
        if (debugLogs) {
            plugin.logger.info("§8[DEBUG] $prefix $msg")
        }
    }

    private fun error(msg: String) {
        plugin.logger.severe("$prefix $msg")
    }

    private fun loadLogSettings() {
        debugLogs = plugin.config.getBoolean("modules.debug", false)
        verboseLogs = plugin.config.getBoolean("modules.verbose", true)
    }

    fun isDebug(): Boolean = debugLogs
    fun isVerbose(): Boolean = verboseLogs

    fun toggleDebug(): Boolean {
        debugLogs = !debugLogs
        plugin.config.set("modules.debug", debugLogs)
        plugin.saveConfig()
        return debugLogs
    }

    fun toggleVerbose(): Boolean {
        verboseLogs = !verboseLogs
        plugin.config.set("modules.verbose", verboseLogs)
        plugin.saveConfig()
        return verboseLogs
    }

    // ================= LIFECYCLE =================

    @OnLoad
    fun onLoad() {
        loadLogSettings()
        log("Preparando sistema de módulos...")

        ModuleAPI.init(this)

        createFolders()
        checkModulesFolder()
        scanModules()
    }

    @OnEnable
    fun onEnable() {
        start()
    }

    @OnDisable
    fun onDisable() {
        shutdown()
    }

    // ================= START/STOP =================

    fun start() {
        log("Inicializando módulos...")

        loadModules()
        enableAll()

        setupCoreFeatures()

        log("Sistema de módulos pronto")
    }

    fun shutdown() {
        disableAll()

        modules.keys.toList().forEach {
            unloadModule(it)
        }
    }

    // ================= BOOTSTRAP =================

    fun setupCoreFeatures() {
        plugin.getCommand("lm")?.setExecutor(ModuleCommand(this))
    }

    // ================= FILE SYSTEM =================

    private fun createFolders() {
        if (!plugin.dataFolder.exists()) {
            plugin.dataFolder.mkdirs()
        }

        if (!modulesFolder.exists()) {
            modulesFolder.mkdirs()
        }
    }

    private fun checkModulesFolder() {
        val jars = modulesFolder.listFiles { it.name.endsWith(".jar") }

        if (jars.isNullOrEmpty()) {
            error("Nenhum módulo encontrado (pasta modules vazia)")
            return
        }

        log("${jars.size} módulo(s) encontrado(s)")
    }

    // ================= SCAN =================

    fun scanModules() {
        val jars = modulesFolder.listFiles { it.name.endsWith(".jar") } ?: return

        debug("Escaneando módulos...")

        jars.forEach { file ->
            try {
                JarFile(file).use { jar ->
                    val entry = jar.entries().asSequence()
                        .firstOrNull { it.name.startsWith("module-") && it.name.endsWith(".yml") }

                    if (entry == null) {
                        error("${file.name} inválido")
                        return@forEach
                    }

                    val yaml = Yaml().load<Map<String, Any>>(jar.getInputStream(entry))

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

                    debug("Detectado: ${desc.id} v${desc.version}")
                }
            } catch (ex: Exception) {
                error("Erro ao escanear ${file.name}: ${ex.message}")
            }
        }
    }

    fun scanModule(id: String): Boolean {
        val key = id.lowercase()

        val file = modulesFolder
            .listFiles { it.name.endsWith(".jar") }
            ?.firstOrNull { it.nameWithoutExtension.equals(key, true) }
            ?: return false.also {
                error("Jar não encontrado para: $id")
            }

        try {
            JarFile(file).use { jar ->
                val entry = jar.entries().asSequence()
                    .firstOrNull { it.name.startsWith("module-") && it.name.endsWith(".yml") }
                    ?: return false.also {
                        error("${file.name} inválido")
                    }

                val yaml = Yaml().load<Map<String, Any>>(jar.getInputStream(entry))

                val desc = ModuleDescription(
                    id = yaml["id"].toString(),
                    name = yaml["name"].toString(),
                    main = yaml["main"].toString(),
                    packageName = yaml["package"].toString(),
                    version = yaml["version"].toString(),
                    depends = (yaml["depends"] as? List<*>)?.map { it.toString() } ?: emptyList()
                )

                val newKey = desc.id.lowercase()

                descriptions[newKey] = desc
                moduleFiles[newKey] = file

                debug("Re-scan: ${desc.id} v${desc.version}")

                return true
            }
        } catch (ex: Exception) {
            error("Erro ao escanear $id: ${ex.message}")
            ex.printStackTrace()
            return false
        }
    }

    // ================= LOAD =================

    fun loadModules() {
        var loaded = 0

        val ordered = try {
            resolveLoadOrder()
        } catch (ex: Exception) {
            error("Erro ao resolver dependências: ${ex.message}")
            return
        }

        ordered.forEach { key ->
            if (modules.containsKey(key)) return@forEach

            val desc = descriptions[key] ?: return@forEach
            val file = moduleFiles[key] ?: return@forEach

            var classLoader: URLClassLoader? = null

            try {
                debug("Carregando: ${desc.id}")

                classLoader = classLoaders.computeIfAbsent(key) {
                    debug("Criando ClassLoader para ${desc.id}")
                    URLClassLoader(
                        arrayOf(file.toURI().toURL()),
                        plugin.javaClass.classLoader
                    )
                }

                val clazz = Class.forName(desc.main, true, classLoader)
                val constructor = clazz.getConstructor(JavaPlugin::class.java)

                val module = constructor.newInstance(plugin) as ModuleModel

                val depend = clazz.getAnnotation(Depend::class.java)

                val softDepend = clazz.getAnnotation(SoftDepend::class.java)

                val hardDepends = depend?.value?.map { it.lowercase() } ?: emptyList()
                val softDepends = softDepend?.value?.map { it.lowercase() } ?: emptyList()

                descriptions[key] = desc.copy(
                    depends = hardDepends,
                    softDepends = softDepends
                )

                val context = (module as AbstractModule).context

                moduleContexts[key] = context
                modules[key] = module

                module.onLoad()

                debug("Dependências ${desc.id}: hard=$hardDepends soft=$softDepends")

                loaded++
            } catch (ex: Exception) {
                error("Erro ao carregar ${desc.id}: ${ex.message}")
                ex.printStackTrace()

                modules.remove(key)
                moduleContexts.remove(key)

                try {
                    classLoader?.close()
                } catch (_: Exception) {
                }

                classLoaders.remove(key)
            }
        }

        log("$loaded módulo(s) carregado(s) (ordenado por dependência)")
    }

    fun loadModule(id: String) {
        val key = id.lowercase()

        if (modules.containsKey(key)) {
            unloadModule(id)
        }

        descriptions.remove(key)
        moduleFiles.remove(key)
        classLoaders.remove(key)
        moduleContexts.remove(key)

        if (!scanModule(id)) {
            error("Falha ao reescanear: $id")
            return
        }

        val desc = descriptions[key] ?: return error("Não encontrado após scan: $id")
        val file = moduleFiles[key] ?: return error("Arquivo não encontrado após scan: $id")

        var classLoader: URLClassLoader? = null

        try {
            debug("Carregando: ${desc.id}")

            classLoader = URLClassLoader(
                arrayOf(file.toURI().toURL()),
                plugin.javaClass.classLoader
            )

            val clazz = Class.forName(desc.main, true, classLoader)
            val constructor = clazz.getConstructor(JavaPlugin::class.java)

            val module = constructor.newInstance(plugin) as ModuleModel

            val depend = clazz.getAnnotation(Depend::class.java)

            val softDepend = clazz.getAnnotation(SoftDepend::class.java)

            val hardDepends = depend?.value?.map { it.lowercase() } ?: emptyList()
            val softDepends = softDepend?.value?.map { it.lowercase() } ?: emptyList()

            descriptions[key] = desc.copy(
                depends = hardDepends,
                softDepends = softDepends
            )

            val context = (module as AbstractModule).context

            classLoaders[key] = classLoader
            moduleContexts[key] = context
            modules[key] = module

            module.onLoad()

            log("Carregado: ${desc.id} v${desc.version}")
            debug("Dependências: hard=$hardDepends soft=$softDepends")
        } catch (ex: Exception) {
            error("Erro ao carregar ${desc.id}: ${ex.message}")
            ex.printStackTrace()

            modules.remove(key)
            moduleContexts.remove(key)

            try {
                classLoader?.close()
            } catch (_: Exception) {
            }

            classLoaders.remove(key)
        }
    }

    fun loadNewModules(): ModuleLoadResult {
        val jars = modulesFolder.listFiles { it.name.endsWith(".jar") }
            ?: return ModuleLoadResult(0, 0, 0)

        debug("Verificando novos módulos...")

        var loaded = 0
        var errors = 0
        val found = jars.size

        jars.forEach { file ->
            try {
                JarFile(file).use { jar ->
                    val entry = jar.entries().asSequence()
                        .firstOrNull { it.name.startsWith("module-") && it.name.endsWith(".yml") }
                        ?: return@forEach

                    val yaml = Yaml()
                        .load<Map<String, Any>>(jar.getInputStream(entry))
                        ?: return@forEach

                    val id = yaml["id"]?.toString()?.lowercase() ?: return@forEach

                    if (modules.containsKey(id) || descriptions.containsKey(id)) {
                        return@forEach
                    }

                    debug("Novo módulo detectado: $id")

                    val desc = ModuleDescription(
                        id = yaml["id"].toString(),
                        name = yaml["name"].toString(),
                        main = yaml["main"].toString(),
                        packageName = yaml["package"].toString(),
                        version = yaml["version"].toString(),
                        depends = (yaml["depends"] as? List<*>)?.map { it.toString() } ?: emptyList()
                    )

                    descriptions[id] = desc
                    moduleFiles[id] = file

                    loadModule(id)
                    loaded++
                }
            } catch (ex: Exception) {
                errors++
                error("Erro ao carregar ${file.name}: ${ex.message}")
            }
        }

        log("$loaded módulo(s) carregado(s) $errors erro(s)")

        return ModuleLoadResult(
            loaded = loaded,
            errors = errors,
            found = found
        )
    }

    fun resolveLoadOrder(): List<String> {
        val visited = mutableSetOf<String>()
        val visiting = mutableSetOf<String>()
        val result = mutableListOf<String>()

        fun visit(id: String) {
            val key = id.lowercase()

            if (key in visited) return

            if (key in visiting) {
                throw IllegalStateException("Dependência circular detectada em: $id")
            }

            visiting.add(key)

            val desc = descriptions[key]

            // 🔥 HARD DEPENDS (obrigatórios)
            desc?.depends?.forEach { dep ->
                val depKey = dep.lowercase()
                if (descriptions.containsKey(depKey)) {
                    visit(depKey)
                }
            }

            // 🔥 SOFT DEPENDS (opcionais)
            desc?.softDepends?.forEach { dep ->
                val depKey = dep.lowercase()
                if (descriptions.containsKey(depKey)) {
                    visit(depKey)
                }
            }

            visiting.remove(key)
            visited.add(key)
            result.add(key)
        }

        descriptions.keys.forEach { visit(it) }

        return result
    }


    // ================= ENABLE =================

    fun enable(id: String, silent: Boolean = false, force: Boolean = false) {

        val key = id.lowercase()

        if (!force && loading.contains(key)) return
        if (enabled.contains(key)) return

        val module = modules[key] ?: return error("Não encontrado: $id")
        val desc = descriptions[key] ?: return

        loading.add(key)

        var success = false

        try {

            // ================= DEPENDÊNCIAS =================

            val missing = mutableListOf<String>()

            for (dep in desc.depends) {
                val depKey = dep.lowercase()

                when {
                    !modules.containsKey(depKey) -> missing.add("$dep (não encontrado)")
                    !enabled.contains(depKey) -> missing.add("$dep (não ativado)")
                }
            }

            if (missing.isNotEmpty()) {
                error("Módulo ${desc.id} NÃO será ativado. Dependências inválidas: ${missing.joinToString(", ")}")
                return
            }

            val context = (module as? AbstractModule)?.context
                ?: throw IllegalStateException("Sem context")

            val file = moduleFiles[key]
                ?: throw IllegalStateException("Arquivo do módulo não encontrado")

            val classLoader = classLoaders[key]
                ?: throw IllegalStateException("ClassLoader não encontrado para ${desc.id}")

            // ================= CONTAINER =================

            val container = ModuleServiceContainer(
                plugin.services,
                this,
                key
            )

            // ================= SCAN SERVICES =================

            ModuleServiceScanner.registerServices(
                plugin,
                this,
                file,
                desc.packageName,
                container,
                classLoader,
                module
            )

            // ================= LIFECYCLE ENABLE =================

            context.namedServices.values.forEach { service ->
                if (service is ModuleService) {
                    service.onEnable()
                }
            }

            // ================= COMMANDS =================

            plugin.server.scheduler.runTask(plugin, Runnable {

                ModuleCommandScanner.registerCommands(
                    plugin,
                    this,
                    file,
                    desc.packageName,
                    context.commandRegistry,
                    container,
                    classLoader
                )

                plugin.server.scheduler.runTaskLater(plugin, Runnable {
                    try {
                        val server = Bukkit.getServer()
                        val method = server.javaClass.getMethod("syncCommands")
                        method.invoke(server)

                        Bukkit.getOnlinePlayers().forEach {
                            it.updateCommands()
                        }
                    } catch (_: Exception) {
                    }
                }, 1L)
            })

            // ================= LISTENERS =================

            ModuleListenerScanner.registerListeners(
                plugin,
                this,
                file,
                desc.packageName,
                context.listenerRegistry,
                container,
                classLoader
            )

            // ================= MODULE ENABLE =================

            module.onEnable()

            success = true

            if (!silent) log("Ativado: ${desc.id}")

        } catch (ex: Exception) {

            error("Erro ao ativar ${desc.id}: ${ex.message}")
            ex.printStackTrace()

        } finally {

            if (success) {
                enabled.add(key)
            } else {
                enabled.remove(key)
            }

            loading.remove(key)
        }
    }

    fun enableAll() {
        val ordered = try {
            resolveLoadOrder()
        } catch (ex: Exception) {
            error("Erro ao resolver dependências para enable: ${ex.message}")
            return
        }

        ordered.forEach { id ->
            enable(id)
        }
    }

    fun enableAsync(id: String): CompletableFuture<ModuleActionResult> {
        val future = CompletableFuture<ModuleActionResult>()
        val key = id.lowercase()

        if (loading.contains(key)) {
            future.complete(ModuleActionResult(false, "§cMódulo já está em carregamento"))
            return future
        }

        if (enabled.contains(key)) {
            future.complete(ModuleActionResult(false, "§eMódulo já está ativado"))
            return future
        }

        val module = modules[key]
            ?: return future.also {
                it.complete(ModuleActionResult(false, "§cMódulo não encontrado"))
            }

        val desc = descriptions[key]
            ?: return future.also {
                it.complete(ModuleActionResult(false, "§cDescrição não encontrada"))
            }

        loading.add(key)

        try {
            val missing = mutableListOf<String>()

            for (dep in desc.depends) {
                val depKey = dep.lowercase()

                when {
                    !modules.containsKey(depKey) -> missing.add("$dep (não encontrado)")
                    !enabled.contains(depKey) -> missing.add("$dep (não ativado)")
                }
            }

            if (missing.isNotEmpty()) {
                val msg = "§cDependências inválidas:\n§7- " + missing.joinToString("\n§7- ")
                future.complete(ModuleActionResult(false, msg))
                return future
            }

            enable(id, silent = true, force = true)

            future.complete(ModuleActionResult(true, "§aMódulo $id ativado com sucesso"))
        } catch (ex: Exception) {
            future.complete(
                ModuleActionResult(false, "§cErro: ${ex.message}")
            )
        } finally {
            loading.remove(key)
        }

        return future
    }

    // ================= DISABLE =================

    fun disable(id: String, silent: Boolean = false, force: Boolean = false) {
        val key = id.lowercase()

        if (!force && loading.contains(key)) return
        if (!enabled.contains(key)) return

        val dependents = getAllDependents(key)
            .filter { enabled.contains(it) }
            .distinct()

        dependents.forEach {
            disable(it, silent = true, force = force)
        }

        val module = modules[key] ?: return

        loading.add(key)

        try {
            val context = (module as? AbstractModule)?.context

            try {
                module.onDisable()
            } catch (ex: Exception) {
                error("Erro no onDisable de $id: ${ex.message}")
                ex.printStackTrace()
            }

            context?.cleanup()

            if (!silent) log("Desativado: $id")
        } catch (ex: Exception) {
            error("Erro ao desativar $id: ${ex.message}")
            ex.printStackTrace()
        } finally {
            enabled.remove(key)
            loading.remove(key)
        }
    }

    fun disableAll() = enabled.toList().reversed().forEach { disable(it) }

    fun disableAsync(id: String): CompletableFuture<ModuleActionResult> {
        val future = CompletableFuture<ModuleActionResult>()
        val key = id.lowercase()

        if (loading.contains(key)) {
            future.complete(ModuleActionResult(false, "§cMódulo está em processamento"))
            return future
        }

        if (!enabled.contains(key)) {
            future.complete(ModuleActionResult(false, "§eMódulo já está desativado"))
            return future
        }

        try {
            val dependents = getAllDependents(key)
                .filter { enabled.contains(it) }
                .distinct()

            disable(id, silent = true, force = true)

            val message = buildString {
                append("§eMódulo §f$id §edesativado")

                if (dependents.isNotEmpty()) {
                    append("\n§cTambém desativado:")
                    dependents.forEach {
                        append("\n§7- §f$it")
                    }
                }
            }

            future.complete(ModuleActionResult(true, message))
        } catch (ex: Exception) {
            future.complete(
                ModuleActionResult(false, "§cErro ao desativar: ${ex.message}")
            )
        }

        return future
    }

    // ================= UNLOAD =================

    fun unloadModule(id: String) {
        val key = id.lowercase()

        val dependents = getAllDependents(key)
            .filter { modules.containsKey(it) }
            .distinct()

        dependents.forEach {
            unloadModule(it)
        }

        if (enabled.contains(key)) {
            disable(id, true, force = true)
        }

        val context = moduleContexts[key]

        context?.cleanup()

        modules.remove(key)
        descriptions.remove(key)

        try {
            classLoaders[key]?.close()
            debug("ClassLoader fechado: $id")
        } catch (ex: Exception) {
            error("Erro ao fechar ClassLoader de $id: ${ex.message}")
        }

        classLoaders.remove(key)
        moduleFiles.remove(key)
        moduleContexts.remove(key)
        enabled.remove(key)
        loading.remove(key)

        System.gc()

        log("Descarregado: $id")
    }

    // ================= RELOAD =================

    fun reload(id: String): CompletableFuture<Boolean> {
        val future = CompletableFuture<Boolean>()
        val key = id.lowercase()

        if (loading.contains(key)) {
            future.complete(false)
            return future
        }

        log("Recarregando módulo: $id")

        val dependents = getAllDependents(key)
            .filter { modules.containsKey(it) }
            .distinct()

        val unloadOrder = dependents + key
        val loadOrder = unloadOrder.reversed()

        val backup = unloadOrder.associateWith {
            descriptions[it] to moduleFiles[it]
        }

        try {
            unloadOrder.forEach {
                disable(it, true, force = true)
                unloadModule(it)
            }

            loadOrder.forEach {
                descriptions.remove(it)
                moduleFiles.remove(it)

                if (!scanModule(it)) {
                    error("Falha ao reescanear: $it")
                    throw IllegalStateException("Scan falhou: $it")
                }
            }

            loadOrder.forEach {
                loadModule(it)
            }

            loadOrder.forEach {
                enable(it, true, force = true)
            }

            log("Módulo $id recarregado com sucesso (cascata)")
            future.complete(true)
        } catch (ex: Exception) {
            error("Erro ao recarregar $id: ${ex.message}")
            ex.printStackTrace()

            try {
                unloadOrder.forEach {
                    disable(it, true, force = true)
                    unloadModule(it)
                }

                backup.forEach { (modId, pair) ->
                    val (desc, file) = pair
                    if (desc != null && file != null) {
                        descriptions[modId] = desc
                        moduleFiles[modId] = file
                    }
                }

                loadOrder.forEach {
                    loadModule(it)
                    enable(it, true, force = true)
                }

                log("Rollback aplicado com sucesso")
            } catch (rollbackEx: Exception) {
                error("Rollback FALHOU: ${rollbackEx.message}")
            }

            future.complete(false)
        }

        return future
    }

    // ================= INFO =================

    fun getModules(): List<ModuleView> {
        return descriptions.values.map {
            val key = it.id.lowercase()

            ModuleView(
                id = it.id,
                name = it.name,
                version = it.version,
                enabled = enabled.contains(key),
                depends = it.depends
            )
        }
    }

    fun getFormattedList(): List<String> {
        if (descriptions.isEmpty()) {
            return listOf("Nenhum módulo encontrado.")
        }

        return descriptions.values.map { desc ->
            val key = desc.id.lowercase()

            val status = if (enabled.contains(key)) "§aATIVO" else "§cDESATIVADO"

            "§7- §f${desc.id} §8(§b${desc.version}§8) §7- $status"
        }
    }

    fun getDependents(id: String): List<String> {
        val key = id.lowercase()

        return descriptions.values
            .filter { desc ->
                desc.depends.any { it.equals(key, true) }
            }
            .map { it.id.lowercase() }
    }

    fun getAllDependents(id: String, visited: MutableSet<String> = mutableSetOf()): List<String> {
        val key = id.lowercase()

        if (!visited.add(key)) return emptyList()

        val direct = getDependents(key)

        return direct + direct.flatMap { getAllDependents(it, visited) }
    }

    fun getDescription(id: String): ModuleDescription? {
        return descriptions[id.lowercase()]
    }

    fun isEnabled(id: String): Boolean {
        return enabled.contains(id.lowercase())
    }

    fun getContext(id: String): ModuleContext? {
        return moduleContexts[id.lowercase()]
    }

    fun hasSoftDepend(moduleId: String, target: String): Boolean {
        val desc = descriptions[moduleId.lowercase()] ?: return false
        return desc.softDepends.any { it.equals(target, true) }
    }
}