package net.lazz.core.injector

import net.lazz.core.annotation.Inject
import java.lang.reflect.ParameterizedType

object DependencyInjector {

    fun create(clazz: Class<*>, container: IServiceContainer): Any {

        val constructor = clazz.declaredConstructors.firstOrNull()
            ?: throw IllegalStateException("Classe ${clazz.simpleName} sem construtor")

        constructor.isAccessible = true

        val params = constructor.parameters.map { param ->

            val type = param.type

            // SUPORTE A Provider<T> NO CONSTRUCTOR
            if (type == Provider::class.java) {

                val generic = param.parameterizedType

                val actualType = (generic as? ParameterizedType)
                    ?.actualTypeArguments
                    ?.firstOrNull()
                    ?: throw IllegalStateException(
                        "Provider sem tipo genérico em ${clazz.simpleName}"
                    )

                val depClass = Class.forName(actualType.typeName).kotlin

                return@map Provider {
                    container.get(depClass)
                }
            }

            val kClass = type.kotlin

            if (!container.has(kClass)) {
                throw IllegalStateException(
                    "Dependência não encontrada: ${kClass.simpleName} em ${clazz.simpleName}"
                )
            }

            container.get(kClass)
        }.toTypedArray()

        val instance = constructor.newInstance(*params)

        inject(instance, container)

        return instance
    }

    fun inject(target: Any, container: IServiceContainer) {

        val clazz = target.javaClass

        clazz.declaredFields.forEach { field ->

            val annotation = field.getAnnotation(Inject::class.java)
                ?: return@forEach

            val type = field.type

            field.isAccessible = true

            // 🔥 LAZY SUPPORT
            if (type == Provider::class.java) {

                val genericType = field.genericType
                    .toString()
                    .substringAfter("<")
                    .substringBefore(">")

                val depClass = Class.forName(genericType).kotlin

                val provider = Provider {
                    container.get(depClass)
                }

                field.set(target, provider)
                return@forEach
            }

            val kClass = type.kotlin

            val dependency = when {
                container.has(kClass) -> container.get(kClass)
                annotation.optional -> null
                else -> throw IllegalStateException(
                    "Dependência não encontrada: ${kClass.simpleName} em ${clazz.simpleName}"
                )
            }

            field.set(target, dependency)
        }
    }
}