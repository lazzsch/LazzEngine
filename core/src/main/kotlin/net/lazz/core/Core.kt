package net.lazz.core

import net.lazz.core.registry.ServiceRegistry
import net.lazz.core.container.ServiceContainer
import net.lazz.core.scanner.ServiceScanner
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

enum class CoreState {
    STARTING,
    RUNNING,
    STOPPING,
    STOPPED
}

class Core : JavaPlugin() {

    @Volatile
    var state = CoreState.STARTING
        private set

    lateinit var services: ServiceRegistry
        private set

    lateinit var container: ServiceContainer
        private set

    private lateinit var scanner: ServiceScanner

    // ================= CONFIG =================

    private fun setupConfig() {

        if (!dataFolder.exists()) {
            dataFolder.mkdirs()
        }

        val configFile = File(dataFolder, "config.yml")

        if (!configFile.exists()) {
            saveResource("config.yml", false)
            logger.info("config.yml criado automaticamente")
        }

        reloadConfig()
    }

    // ================= LIFECYCLE =================

    override fun onLoad() {

        setupConfig()

        services = ServiceRegistry()
        container = ServiceContainer(services)

        services.register(Core::class, this)
        services.register(JavaPlugin::class, this)
        services.register(ServiceRegistry::class, services)
        services.register(ServiceContainer::class, container)

        scanner = ServiceScanner(
            plugin = this,
            registry = services,
            container = container
        )

        scanner.scan()
    }

    override fun onEnable() {

        logger.info("Iniciando LazzEngine...")

        state = CoreState.STARTING

        try {

            scanner.enableAll()

            state = CoreState.RUNNING

            logger.info("LazzEngine iniciado com sucesso!")

        } catch (ex: Exception) {

            logger.severe("Erro ao iniciar o Core: ${ex.message}")
            ex.printStackTrace()

            state = CoreState.STOPPED
            server.pluginManager.disablePlugin(this)
        }
    }

    override fun onDisable() {

        if (state == CoreState.STOPPING || state == CoreState.STOPPED) return

        state = CoreState.STOPPING

        try {

            scanner.shutdown()

        } catch (ex: Exception) {

            logger.severe("Erro no shutdown: ${ex.message}")
            ex.printStackTrace()
        }

        services.clear()

        state = CoreState.STOPPED

        logger.info("LazzEngine desligado com sucesso!")
    }
}