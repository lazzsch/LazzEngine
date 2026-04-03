package net.lazz.core.reflection

import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap

object MethodAccessor {

    private val methodCache = ConcurrentHashMap<String, Method?>()

    fun getMethod(clazz: Class<*>, methodName: String, vararg parameterTypes: Class<*>): Method? {
        val key = buildMethodKey(clazz, methodName, parameterTypes)

        return methodCache.computeIfAbsent(key) {
            try {
                val method = clazz.getDeclaredMethod(methodName, *parameterTypes)
                method.isAccessible = true
                method
            } catch (_: NoSuchMethodException) {
                null
            } catch (_: NoSuchMethodError) {
                null
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> invoke(
        clazz: Class<*>,
        methodName: String,
        instance: Any?,
        parameterTypes: Array<Class<*>>,
        vararg args: Any?
    ): T {
        val method = getMethod(clazz, methodName, *parameterTypes)
            ?: throw IllegalArgumentException("Method '$methodName' not found in class ${clazz.name}")

        return method.invoke(instance, *args) as T
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> invoke(
        methodName: String,
        instance: Any,
        parameterTypes: Array<Class<*>>,
        vararg args: Any?
    ): T {
        return invoke(instance.javaClass, methodName, instance, parameterTypes, *args)
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> invoke(clazz: Class<*>, methodName: String, instance: Any?): T {
        return invoke(clazz, methodName, instance, emptyArray())
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> invoke(methodName: String, instance: Any): T {
        return invoke(instance.javaClass, methodName, instance, emptyArray())
    }

    fun findMethodByName(clazz: Class<*>, methodName: String): Method? {
        val key = "${clazz.name}.$methodName.byName"

        return methodCache.computeIfAbsent(key) {
            try {
                clazz.declaredMethods.firstOrNull {
                    it.name == methodName
                }?.apply { isAccessible = true }
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun buildMethodKey(
        clazz: Class<*>,
        methodName: String,
        parameterTypes: Array<out Class<*>>
    ): String {
        val params = if (parameterTypes.isEmpty()) {
            "()"
        } else {
            parameterTypes.joinToString(",", "(", ")") { it.name }
        }

        return "${clazz.name}.$methodName$params"
    }

    fun clearCache() {
        methodCache.clear()
    }

    fun getCacheSize(): Int {
        return methodCache.size
    }
}