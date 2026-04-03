package net.lazz.modules.moneyexample

import net.lazz.core.services.module.api.ModuleCallable
import net.lazz.core.services.module.api.annotation.ModuleAPI
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@ModuleAPI("MoneyexampleService")
class MoneyexampleService : ModuleCallable {

    private val data = ConcurrentHashMap<UUID, Int>()

    fun get(uuid: UUID): Int {
        return data.getOrDefault(uuid, 0)
    }

    fun add(uuid: UUID, value: Int) {
        data[uuid] = get(uuid) + value
    }

    override fun call(method: String, vararg args: Any?): Any? {
        return when (method) {
            "get" -> get(args[0] as UUID)
            "add" -> {
                add(args[0] as UUID, args[1] as Int)
                null
            }
            else -> null
        }
    }
}