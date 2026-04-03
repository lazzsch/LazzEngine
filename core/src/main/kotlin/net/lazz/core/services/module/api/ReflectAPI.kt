package net.lazz.core.services.module.api

import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap

class ReflectAPI(private val instance: Any) {

    companion object {
        private val methodCache =
            ConcurrentHashMap<Class<*>, Map<String, List<Method>>>()
    }

    private val methods: Map<String, List<Method>> =
        methodCache.computeIfAbsent(instance.javaClass) { clazz ->
            clazz.methods.groupBy { it.name }
        }

    fun call(method: String, vararg args: Any?): Any? {
        val candidates = methods[method] ?: return null

        val match = candidates
            .mapNotNull { m ->
                val score = matchScore(m.parameterTypes, args)
                if (score >= 0) m to score else null
            }
            .minByOrNull { it.second }
            ?.first ?: return null

        return try {
            match.invoke(instance, *args)
        } catch (e: Exception) {
            throw RuntimeException(
                buildString {
                    append("Erro ao invocar '$method' em ${instance.javaClass.name}\n")
                    append("Args: ${args.joinToString { it?.javaClass?.simpleName ?: "null" }}")
                },
                e
            )
        }
    }

    fun callAuto(method: String, vararg args: Any?): Any? {
        return call(method, *args)
    }

    inline fun <reified T> callAs(method: String, vararg args: Any?): T? {
        return call(method, *args) as? T
    }

    // ================= MATCH =================

    private fun matchScore(types: Array<Class<*>>, args: Array<out Any?>): Int {

        if (types.size != args.size) return -1

        var score = 0

        for (i in types.indices) {
            val type = types[i]
            val arg = args[i]

            val s = scoreType(type, arg) ?: return -1
            score += s
        }

        return score
    }

    private fun scoreType(type: Class<*>, arg: Any?): Int? {

        if (arg == null) {
            return if (!type.isPrimitive) 10 else null
        }

        val argClass = arg.javaClass

        if (type == argClass) return 0
        if (primitiveWrapperMatch(type, argClass)) return 1
        if (type.isAssignableFrom(argClass)) return 2

        return null
    }

    private val primitiveToWrapper = mapOf(
        Int::class.javaPrimitiveType to Int::class.java,
        Double::class.javaPrimitiveType to Double::class.java,
        Float::class.javaPrimitiveType to Float::class.java,
        Long::class.javaPrimitiveType to Long::class.java,
        Boolean::class.javaPrimitiveType to Boolean::class.java,
        Short::class.javaPrimitiveType to Short::class.java,
        Byte::class.javaPrimitiveType to Byte::class.java,
        Char::class.javaPrimitiveType to Char::class.java
    )

    private fun primitiveWrapperMatch(type: Class<*>, arg: Class<*>): Boolean {
        return primitiveToWrapper[type] == arg
    }
}