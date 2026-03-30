package net.lazz.core.service.service.annotation

import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Provide(
    val asType: KClass<*> = Any::class
)