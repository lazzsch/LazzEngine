package net.lazz.core.common.file

import net.lazz.core.Core
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.scheduler.BukkitRunnable
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap

class FileManager(private val plugin: Core) {

    private val configs = ConcurrentHashMap<String, Config>()

    fun configExists(name: String): Boolean {
        return File(plugin.dataFolder, name).exists()
    }

    fun getConfigNames(directory: String): List<String> {
        val folder = File(plugin.dataFolder, directory)

        if (!folder.exists() || !folder.isDirectory) return emptyList()

        return folder.listFiles { _, name -> name.endsWith(".yml") }
            ?.map { it.name.removeSuffix(".yml") }
            ?: emptyList()
    }

    fun getConfig(name: String): Config {
        return configs.computeIfAbsent(name) {
            Config(name, plugin)
        }
    }

    fun saveConfig(name: String) {
        getConfig(name).save()
    }

    fun reloadConfig(name: String): Config {
        return getConfig(name).reload()
    }

    fun reloadAllConfigsAsync(after: (() -> Unit)? = null) {
        val total = configs.size
        if (total == 0) {
            after?.invoke()
            return
        }

        var completed = 0

        configs.forEach { (name, config) ->
            object : BukkitRunnable() {
                override fun run() {
                    config.reload()
                    plugin.logger.info("Config $name recarregada.")

                    completed++
                    if (completed >= total) {
                        plugin.logger.info("Todas configs recarregadas.")
                        after?.invoke()
                    }
                }
            }.runTaskAsynchronously(plugin)
        }
    }

    fun deleteConfig(name: String): Boolean {
        val file = File(plugin.dataFolder, name)
        return file.exists() && file.delete()
    }

    // ========================= CONFIG =========================

    class Config(
        private val name: String,
        private val plugin: Core
    ) {

        private val file: File = File(plugin.dataFolder, name)
        private var config: YamlConfiguration = YamlConfiguration()

        init {
            reload()
        }

        fun get(): YamlConfiguration = config

        fun reload(): Config {
            if (!file.exists()) {
                plugin.saveResource(name, false)
            }

            config = YamlConfiguration.loadConfiguration(file)

            val defConfigStream: InputStream? = plugin.getResource(name)
            defConfigStream?.let {
                val defConfig = YamlConfiguration.loadConfiguration(it.reader())
                config.setDefaults(defConfig)
            }

            return this
        }

        fun save(): Config {
            object : BukkitRunnable() {
                override fun run() {
                    try {
                        config.save(file)
                    } catch (e: IOException) {
                        plugin.logger.severe("Erro ao salvar config: $name")
                        e.printStackTrace()
                    }
                }
            }.runTaskAsynchronously(plugin)

            return this
        }

        fun saveDefaultConfig(): Config {
            if (!file.exists()) {
                plugin.saveResource(name, false)
            }
            return this
        }

        fun copyDefaults(force: Boolean): Config {
            config.options().copyDefaults(force)
            return this
        }

        fun set(path: String, value: Any?): Config {
            config.set(path, value)
            return this
        }

        fun getFloat(path: String): Float {
            return (config.get(path) as? Number)?.toFloat() ?: 0f
        }

        fun getAny(path: String): Any? {
            return config.get(path)
        }
    }
}