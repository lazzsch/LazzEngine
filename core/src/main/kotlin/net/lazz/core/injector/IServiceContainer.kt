package net.lazz.core.injector

import kotlin.reflect.KClass

interface IServiceContainer {

    fun get(type: KClass<*>): Any

    fun has(type: KClass<*>): Boolean
}