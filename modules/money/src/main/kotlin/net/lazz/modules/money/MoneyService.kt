package net.lazz.modules.money

import net.lazz.core.service.dependency.annotation.Service
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@Service
class MoneyService {

    private val balances = ConcurrentHashMap<UUID, Double>()

    fun get(uuid: UUID): Double {
        return balances.getOrDefault(uuid, 0.0)
    }

    fun set(uuid: UUID, value: Double) {
        balances[uuid] = value
    }

    fun add(uuid: UUID, value: Double) {
        balances[uuid] = get(uuid) + value
    }

    fun remove(uuid: UUID, value: Double): Boolean {
        val current = get(uuid)

        if (current < value) return false

        balances[uuid] = current - value
        return true
    }
}