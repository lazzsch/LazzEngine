package net.lazz.core.services.module.api

import net.lazz.core.services.module.ModuleContext
import net.lazz.core.services.module.ModuleManager
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

object ModuleAPI {

    private var _manager: ModuleManager? = null

    val manager: ModuleManager
        get() = _manager ?: error("ModuleAPI não foi inicializada")

    fun init(manager: ModuleManager) {
        this._manager = manager
    }

    private fun normalize(api: String) = api.lowercase(Locale.ROOT)

    private val reflectCache = ConcurrentHashMap<Any, ReflectAPI>()

    private fun reflectOf(instance: Any): ReflectAPI {
        return reflectCache.computeIfAbsent(instance) {
            ReflectAPI(it)
        }
    }

    // ================= CONTEXT =================

    fun getModule(id: String): ModuleContext? {
        return manager.getContext(id)
    }

    fun requireModule(id: String): ModuleContext {
        return getModule(id)
            ?: error("Módulo '$id' não encontrado")
    }

    fun hasModule(id: String): Boolean {
        return getModule(id) != null
    }

    // ================= POR NOME =================

    fun get(id: String, api: String = id): Any? {
        val context = getModule(id) ?: return null
        return context.namedServices[normalize(api)]
    }

    inline fun <reified T> getAs(id: String, api: String = id): T? {
        return get(id, api) as? T
    }

    fun require(id: String, api: String = id): Any {
        return get(id, api)
            ?: error("API '$api' do módulo '$id' não encontrada")
    }

    inline fun <reified T> requireAs(id: String, api: String = id): T {
        return getAs<T>(id, api)
            ?: error("API '$api' não é do tipo ${T::class.simpleName}")
    }

    fun has(id: String, api: String = id): Boolean {
        return get(id, api) != null
    }

    // ================= POR TIPO =================

    fun <T : Any> getFromModule(id: String, type: KClass<T>): T? {
        val context = getModule(id) ?: return null
        val registry = context.serviceRegistry

        return if (registry.has(type)) {
            type.java.cast(registry.get(type))
        } else null
    }

    inline fun <reified T : Any> getFromModule(id: String): T? {
        return getFromModule(id, T::class)
    }

    fun <T : Any> requireFromModule(id: String, type: KClass<T>): T {
        return getFromModule(id, type)
            ?: error("Service ${type.simpleName} não encontrado no módulo '$id'")
    }

    inline fun <reified T : Any> requireFromModule(id: String): T {
        return requireFromModule(id, T::class)
    }

    // ================= CALL =================

    fun call(id: String, method: String, vararg args: Any?): Any? {

        val instance = get(id)
            ?: throw IllegalStateException("API '$id' não encontrada")

        // prioridade: callable (mais rápido)
        if (instance is ModuleCallable) {
            return try {
                instance.call(method, *args)
            } catch (e: Exception) {
                throw RuntimeException(
                    "Erro no ModuleCallable: $id -> $method",
                    e
                )
            }
        }

        // fallback reflection
        val reflect = reflectOf(instance)
        return reflect.call(method, *args)
    }

    inline fun <reified T> callAs(id: String, method: String, vararg args: Any?): T? {
        val result = call(id, method, *args)
        return result as? T
    }

    // ================= DEBUG =================

    fun listAPIs(id: String): Set<String> {
        val context = getModule(id) ?: return emptySet()
        return context.namedServices.keys
    }
}