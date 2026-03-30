package net.lazz.core.util.reflection

import java.util.concurrent.ConcurrentHashMap

object ClassAccessor {

    private val classCache = ConcurrentHashMap<String, Class<*>?>()

    fun getClass(className: String): Class<*>? {
        return classCache.computeIfAbsent(className) {
            try {
                Class.forName(className)
            } catch (_: ClassNotFoundException) {
                null
            } catch (_: NoClassDefFoundError) {
                null
            }
        }
    }

    fun getClass(className: String, classLoader: ClassLoader): Class<*>? {
        val key = "$className@${classLoader.hashCode()}"

        return classCache.computeIfAbsent(key) {
            try {
                Class.forName(className, true, classLoader)
            } catch (_: ClassNotFoundException) {
                null
            } catch (_: NoClassDefFoundError) {
                null
            }
        }
    }

    fun classExists(className: String): Boolean {
        return getClass(className) != null
    }

    fun classExists(className: String, classLoader: ClassLoader): Boolean {
        return getClass(className, classLoader) != null
    }

    fun assignableFrom(superClassName: String, subClassName: String): Boolean {
        val superClass = getClass(superClassName)
        val subClass = getClass(subClassName)
        return assignableFrom(superClass, subClass)
    }

    fun assignableFrom(superClass: Class<*>?, subClass: Class<*>?): Boolean {
        if (superClass == null || subClass == null) return false
        return superClass.isAssignableFrom(subClass)
    }

    fun assignableFrom(superClass: Class<*>, subClassName: String): Boolean {
        val subClass = getClass(subClassName)
        return assignableFrom(superClass, subClass)
    }

    fun assignableFrom(superClassName: String, subClass: Class<*>): Boolean {
        val superClass = getClass(superClassName)
        return assignableFrom(superClass, subClass)
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> newInstance(className: String): T? {
        val clazz = getClass(className) ?: return null

        return try {
            clazz.getDeclaredConstructor().newInstance() as T
        } catch (_: Exception) {
            null
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> newInstance(
        className: String,
        parameterTypes: Array<Class<*>>,
        vararg args: Any?
    ): T? {
        val clazz = getClass(className) ?: return null

        return try {
            val constructor = clazz.getDeclaredConstructor(*parameterTypes)
            constructor.isAccessible = true
            constructor.newInstance(*args) as T
        } catch (_: Exception) {
            null
        }
    }

    fun getSimpleName(className: String): String? {
        return getClass(className)?.simpleName
    }

    fun getPackageName(className: String): String? {
        return getClass(className)?.`package`?.name
    }

    fun clearCache() {
        classCache.clear()
    }

    fun getCacheSize(): Int {
        return classCache.size
    }
}