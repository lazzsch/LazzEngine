package net.lazz.core.registry

import kotlin.reflect.KClass

class ServiceRegistry {

    @PublishedApi
    internal val services = mutableMapOf<KClass<*>, Any>()

    fun register(type: KClass<*>, instance: Any) {
        services[type] = instance
    }

    fun get(type: KClass<*>): Any {
        return services[type]
            ?: error("Service ${type.simpleName} não registrado")
    }

    inline fun <reified T : Any> get(): T {
        return get(T::class) as T
    }

    fun has(type: KClass<*>): Boolean {
        return services.containsKey(type)
    }

    inline fun <reified T : Any> has(): Boolean {
        return has(T::class)
    }

    fun clear() {
        services.clear()
    }
}