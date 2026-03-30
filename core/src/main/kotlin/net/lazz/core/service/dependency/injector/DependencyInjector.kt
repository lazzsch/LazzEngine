package net.lazz.core.service.dependency.injector

import net.lazz.core.service.dependency.annotation.Inject
import net.lazz.core.service.dependency.container.ServiceContainer

object DependencyInjector {

    fun inject(target: Any, container: ServiceContainer) {

        val clazz = target.javaClass

        clazz.declaredFields.forEach { field ->

            if (!field.isAnnotationPresent(Inject::class.java)) return@forEach

            val dependency = container.get(field.type)
                ?: return@forEach

            field.isAccessible = true
            field.set(target, dependency)
        }
    }
}