package net.lazz.core.scanner

import net.lazz.core.Core
import net.lazz.core.registry.ServiceRegistry
import net.lazz.core.annotation.*
import net.lazz.core.injector.DependencyInjector
import net.lazz.core.injector.IServiceContainer
import java.lang.reflect.Method
import java.util.jar.JarFile
import kotlin.reflect.KClass

class ServiceScanner(
    private val plugin: Core,
    private val registry: ServiceRegistry,
    private val container: IServiceContainer
) {

    private val basePackage = plugin.javaClass.packageName.replace(".", "/") + "/services"

    private val loadMethods = mutableListOf<Pair<Any, Method>>()
    private val enableMethods = mutableListOf<Pair<Any, Method>>()
    private val disableMethods = mutableListOf<Pair<Any, Method>>()

    fun scan() {

        val jarUrl = plugin.javaClass.protectionDomain.codeSource.location
        val jar = JarFile(jarUrl.toURI().path)

        val classes = jar.entries().asSequence()
            .filter { it.name.endsWith(".class") }
            .filter { it.name.startsWith(basePackage) }
            .map { it.name.replace("/", ".").removeSuffix(".class") }
            .mapNotNull { loadServiceClass(it) }
            .toList()

        val sorted = sortByDependencies(classes)

        sorted.forEach { clazz ->

            val instance = DependencyInjector.create(clazz, container)
            val kClass = clazz.kotlin

            registry.register(kClass, instance)
            registerLifecycle(instance)

            plugin.logger.info("Service carregado: ${clazz.simpleName}")
        }

        runLoadLifecycle()
    }

    fun enableAll() {
        runEnableLifecycle()
    }

    fun shutdown() {
        runDisableLifecycle()
    }

    private fun loadServiceClass(name: String): Class<*>? {
        return try {
            val clazz = Class.forName(name)

            if (
                clazz.isAnnotationPresent(Service::class.java) &&
                !clazz.isInterface &&
                !clazz.isEnum &&
                !clazz.isAnnotation &&
                !clazz.isSynthetic
            ) clazz else null

        } catch (_: Exception) {
            null
        }
    }

    private fun sortByDependencies(classes: List<Class<*>>): List<Class<*>> {

        val sorted = mutableListOf<Class<*>>()
        val visited = mutableSetOf<Class<*>>()

        fun visit(clazz: Class<*>) {

            if (visited.contains(clazz)) return
            visited.add(clazz)

            val depends = clazz.getAnnotation(DependsOn::class.java)

            depends?.value?.forEach { dep: KClass<*> ->

                val dependency = classes.find { it == dep.java }

                if (dependency != null) {
                    plugin.logger.info("-> ${clazz.simpleName} depende de ${dependency.simpleName}")
                    visit(dependency)
                } else {
                    plugin.logger.warning("Dependência não encontrada: ${dep.simpleName} em ${clazz.simpleName}")
                }
            }

            sorted.add(clazz)
        }

        classes.forEach { visit(it) }

        return sorted
    }

    private fun registerLifecycle(instance: Any) {

        instance::class.java.declaredMethods.forEach { method ->

            if (method.isAnnotationPresent(OnLoad::class.java)) {
                method.isAccessible = true
                loadMethods.add(instance to method)
            }

            if (method.isAnnotationPresent(OnEnable::class.java)) {
                method.isAccessible = true
                enableMethods.add(instance to method)
            }

            if (method.isAnnotationPresent(OnDisable::class.java)) {
                method.isAccessible = true
                disableMethods.add(instance to method)
            }
        }
    }

    private fun runLoadLifecycle() {
        loadMethods.forEach { (instance, method) ->
            try {
                method.invoke(instance)
            } catch (ex: Exception) {
                plugin.logger.severe("Erro no OnLoad de ${instance::class.java.simpleName}: ${ex.message}")
                ex.printStackTrace()
            }
        }
    }

    private fun runEnableLifecycle() {
        enableMethods.forEach { (instance, method) ->
            try {
                method.invoke(instance)
            } catch (ex: Exception) {
                plugin.logger.severe("Erro no OnEnable de ${instance::class.java.simpleName}: ${ex.message}")
                ex.printStackTrace()
            }
        }
    }

    private fun runDisableLifecycle() {
        disableMethods.reversed().forEach { (instance, method) ->
            try {
                method.invoke(instance)
            } catch (ex: Exception) {
                plugin.logger.severe("Erro no OnDisable de ${instance::class.java.simpleName}: ${ex.message}")
                ex.printStackTrace()
            }
        }
    }
}