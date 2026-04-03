package net.lazz.core.services.module.api

interface ModuleCallable {
    fun call(method: String, vararg args: Any?): Any?
}