package net.lazz.modules.teste

import net.lazz.core.service.dependency.annotation.Service
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Service
class TesteService {

    private val data = ConcurrentHashMap<UUID, Int>()

    fun get(uuid: UUID): Int {
        return data.getOrDefault(uuid, 0)
    }

    fun set(uuid: UUID, value: Int) {
        data[uuid] = value
    }

    fun add(uuid: UUID, value: Int) {
        data[uuid] = get(uuid) + value
    }

    fun hello(): String {
        return "Service funcionando!"
    }
}