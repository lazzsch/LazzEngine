package net.lazz.core.util.reflection

import java.lang.reflect.Field
import java.util.concurrent.ConcurrentHashMap

object FieldAccessor {

    private val fieldCache = ConcurrentHashMap<String, Field?>()
    private val firstFieldNameCache = ConcurrentHashMap<String, String?>()

    fun getField(clazz: Class<*>?, fieldName: String?): Field? {
        if (clazz == null || fieldName == null) return null

        val key = "${clazz.name}.$fieldName"

        return fieldCache.computeIfAbsent(key) {
            try {
                val field = clazz.getDeclaredField(fieldName)
                field.isAccessible = true
                field
            } catch (_: NoSuchFieldException) {
                null
            } catch (_: NoSuchFieldError) {
                null
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> getValue(clazz: Class<*>, fieldName: String, instance: Any?): T? {
        val field = getField(clazz, fieldName) ?: return null
        return field.get(instance) as T
    }

    fun setValue(clazz: Class<*>, fieldName: String, instance: Any?, value: Any?) {
        val field = getField(clazz, fieldName)
            ?: throw IllegalArgumentException("Field '$fieldName' not found in class ${clazz.name}")

        field.set(instance, value)
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> getValue(fieldName: String, instance: Any): T? {
        return getValue(instance.javaClass, fieldName, instance)
    }

    fun setValue(fieldName: String, instance: Any, value: Any?) {
        setValue(instance.javaClass, fieldName, instance, value)
    }

    fun getFirstFieldName(clazz: Class<*>, type: Class<*>): String? {
        val key = "${clazz.name}:${type.name}"

        return firstFieldNameCache.computeIfAbsent(key) {
            clazz.declaredFields.firstOrNull { field ->
                ClassAccessor.assignableFrom(type, field.type)
            }?.name
        }
    }

    fun clearCache() {
        fieldCache.clear()
        firstFieldNameCache.clear()
    }

    fun getCacheSize(): Int {
        return fieldCache.size + firstFieldNameCache.size
    }
}