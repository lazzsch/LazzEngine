package net.lazz.core.services.module.service.injector

import net.lazz.core.services.module.model.ModuleModel
import net.lazz.core.services.module.service.container.ModuleServiceContainer

object ModuleDependencyInjector {

    fun <T : Any> create(
        clazz: Class<T>,
        container: ModuleServiceContainer,
        module: ModuleModel? = null
    ): T {

        // ================= LOG CONTROL =================

        val manager = container.manager
        val moduleId = container.moduleId

        fun debug(msg: String) {
            if (manager.isDebug()) {
                manager.plugin.logger.info("§8[DEBUG] [Injector:$moduleId] $msg")
            }
        }

        fun error(msg: String): Nothing {
            manager.plugin.logger.severe("[Injector:$moduleId] $msg")
            throw IllegalStateException(msg)
        }

        // ================= INJECTION =================

        val constructor = clazz.declaredConstructors.first()

        debug("Criando: ${clazz.simpleName}")

        val params = constructor.parameterTypes.map { param ->

            when {

                param == ModuleModel::class.java -> {
                    debug("Injetando ModuleModel em ${clazz.simpleName}")

                    module ?: error("ModuleModel não disponível para ${clazz.simpleName}")
                }

                else -> {
                    val kClass = param.kotlin

                    if (!container.has(kClass)) {
                        error("Dependência não encontrada: ${kClass.simpleName} em ${clazz.simpleName}")
                    }

                    debug("Injetando ${kClass.simpleName} em ${clazz.simpleName}")

                    container.get(kClass)
                }
            }

        }.toTypedArray()

        constructor.isAccessible = true

        val instance = constructor.newInstance(*params) as T

        debug("Instanciado: ${clazz.simpleName}")

        return instance
    }
}