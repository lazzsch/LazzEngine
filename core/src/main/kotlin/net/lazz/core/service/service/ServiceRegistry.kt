package net.lazz.core.service.service

class ServiceRegistry {
    private val services = mutableMapOf<Class<*>, Any>()

    fun register(type: Class<*>, instance: Any) {
        services[type] = instance
    }

    fun <T : Any> get(type: Class<T>): T? {
        return services[type] as? T
    }

    fun unregister(type: Class<*>) {
        services.remove(type)
    }

    fun clear() {
        services.clear()
    }
}