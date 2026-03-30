package net.lazz.core.service.module.util

import net.lazz.core.service.service.ServiceRegistry
import net.lazz.core.service.service.annotation.Provide
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.util.jar.JarFile

object ServiceScanner {

    fun registerServices(
        plugin: JavaPlugin,
        basePackage: String,
        registry: ServiceRegistry
    ) {

        val jarFile = File(plugin.javaClass.protectionDomain.codeSource.location.toURI())
        val jar = JarFile(jarFile)

        val path = basePackage.replace(".", "/")

        val entries = jar.entries()

        while (entries.hasMoreElements()) {
            val entry = entries.nextElement()

            if (!entry.name.endsWith(".class")) continue
            if (!entry.name.startsWith(path)) continue

            val className = entry.name
                .replace("/", ".")
                .removeSuffix(".class")

            try {
                val clazz = Class.forName(className)

                val provide = clazz.getAnnotation(Provide::class.java) ?: continue

                val instance = clazz.getDeclaredConstructor().newInstance()

                val type = if (provide.asType == Any::class) clazz else provide.asType.java

                registry.register(type, instance)

                plugin.logger.info("[Service] Registrado: ${clazz.simpleName}")

            } catch (_: Exception) {}
        }

        jar.close()
    }
}