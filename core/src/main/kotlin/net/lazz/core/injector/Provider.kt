package net.lazz.core.injector

class Provider<T>(
    private val supplier: () -> T
) {
    fun get(): T = supplier()
}