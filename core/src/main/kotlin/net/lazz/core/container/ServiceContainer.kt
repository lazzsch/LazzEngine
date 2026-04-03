package net.lazz.core.container

import net.lazz.core.registry.ServiceRegistry
import net.lazz.core.injector.IServiceContainer
import kotlin.reflect.KClass

class ServiceContainer(
    private val globalRegistry: ServiceRegistry
) : IServiceContainer {

    private val local = mutableMapOf<KClass<*>, Any>()

    fun register(clazz: KClass<*>, instance: Any) {
        local[clazz] = instance
    }

    override fun get(type: KClass<*>): Any {
        return local[type] ?: globalRegistry.get(type)
    }

    override fun has(type: KClass<*>): Boolean {
        return local.containsKey(type) || globalRegistry.has(type)
    }

    inline fun <reified T : Any> get(): T {
        return get(T::class) as T
    }

    fun getAll(): Map<KClass<*>, Any> {
        return local
    }
}