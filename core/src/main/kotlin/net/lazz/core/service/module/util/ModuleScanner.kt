package net.lazz.core.service.module.util

import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.util.jar.JarFile

object ModuleScanner {

    fun findModules(plugin: JavaPlugin, basePackage: String): List<Class<out Module>> {

        val classes = mutableListOf<Class<out Module>>()

        val jarFile = File(plugin.javaClass.protectionDomain.codeSource.location.toURI())
        val jar = JarFile(jarFile)

        val entries = jar.entries()

        val path = basePackage.replace(".", "/")

        while (entries.hasMoreElements()) {
            val entry = entries.nextElement()

            if (!entry.name.endsWith(".class")) continue
            if (!entry.name.startsWith(path)) continue

            val className = entry.name
                .replace("/", ".")
                .removeSuffix(".class")

            try {
                val clazz = Class.forName(className)

                if (!Module::class.java.isAssignableFrom(clazz)) continue
                if (clazz.isInterface) continue
                if (java.lang.reflect.Modifier.isAbstract(clazz.modifiers)) continue

                @Suppress("UNCHECKED_CAST")
                classes.add(clazz as Class<out Module>)

            } catch (_: Throwable) {}
        }

        jar.close()

        return classes
    }
}