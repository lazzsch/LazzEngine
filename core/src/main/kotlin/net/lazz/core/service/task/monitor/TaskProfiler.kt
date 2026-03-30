package net.lazz.core.service.task.monitor

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

class TaskProfiler {

    data class Stats(
        val executions: AtomicLong = AtomicLong(0),
        val totalTime: AtomicLong = AtomicLong(0),
        val maxTime: AtomicLong = AtomicLong(0)
    )

    private val stats = ConcurrentHashMap<String, Stats>()

    fun record(name: String, timeNs: Long) {
        val stat = stats.computeIfAbsent(name) { Stats() }

        stat.executions.incrementAndGet()
        stat.totalTime.addAndGet(timeNs)

        stat.maxTime.updateAndGet { current ->
            if (timeNs > current) timeNs else current
        }

        if (stats.size > 1000) {
            stats.clear()
        }
    }

    fun snapshot(): Map<String, Stats> = stats

    fun clear() = stats.clear()
}