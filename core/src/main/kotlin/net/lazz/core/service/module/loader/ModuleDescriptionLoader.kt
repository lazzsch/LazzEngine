package net.lazz.core.service.module.loader

import net.lazz.core.service.module.model.ModuleDescription
import org.bukkit.plugin.java.JavaPlugin
import org.yaml.snakeyaml.Yaml
import java.util.jar.JarFile

object ModuleDescriptionLoader {

    fun load(plugin: JavaPlugin): List<ModuleDescription> {

        val result = mutableListOf<ModuleDescription>()

        val jar = plugin.javaClass.protectionDomain.codeSource.location
        val file = java.io.File(jar.toURI())

        JarFile(file).use { jarFile ->

            val entries = jarFile.entries()

            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()

                if (!entry.name.startsWith("module-")) continue
                if (!entry.name.endsWith(".yml")) continue

                val stream = jarFile.getInputStream(entry)

                val yaml = Yaml().load<Map<String, Any>>(stream)

                val desc = ModuleDescription(
                    id = yaml["id"] as String,
                    name = yaml["name"] as String,
                    main = yaml["main"] as String,
                    packageName = yaml["package"] as String,
                    version = yaml["version"] as String,
                    depends = (yaml["depends"] as? List<*>)?.map { it.toString() } ?: emptyList()
                )

                result.add(desc)
            }
        }

        return result
    }
}