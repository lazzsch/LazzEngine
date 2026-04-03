package net.lazz.core.services.task.monitor

import java.util.concurrent.TimeUnit

object TaskProfilerFormatter {

    fun format(profiler: TaskProfiler): List<String> {
        return profiler.snapshot()
            .map { (name, stat) ->

                val executions = stat.executions.get()
                val totalMs = TimeUnit.NANOSECONDS.toMillis(stat.totalTime.get())
                val maxMs = TimeUnit.NANOSECONDS.toMillis(stat.maxTime.get())
                val avg = if (executions == 0L) 0 else totalMs / executions

                Triple(name, avg, maxMs)
            }
            .sortedByDescending { it.third } // ordenar por lag (max)
            .take(10)
            .mapIndexed { index, (name, avg, max) ->
                "#${index + 1} $name | avg=${avg}ms | max=${max}ms"
            }
    }
}