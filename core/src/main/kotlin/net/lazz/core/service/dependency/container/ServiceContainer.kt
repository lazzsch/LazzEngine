package net.lazz.core.service.dependency.container

import net.lazz.core.service.service.ServiceRegistry

class ServiceContainer(
    private val globalRegistry: ServiceRegistry
) {

    private val local = mutableMapOf<Class<*>, Any>()

    fun register(clazz: Class<*>, instance: Any) {
        local[clazz] = instance
    }

    fun <T : Any> get(clazz: Class<T>): T? {
        return local[clazz] as? T
            ?: globalRegistry.get(clazz)
    }

    inline fun <reified T : Any> get(): T? {
        return get(T::class.java)
    }

    fun getAll(): Map<Class<*>, Any> {
        return local
    }
}