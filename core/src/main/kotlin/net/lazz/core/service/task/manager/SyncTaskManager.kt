package net.lazz.core.service.task.manager

import net.lazz.core.Core
import net.lazz.core.service.task.monitor.TaskProfiler
import org.bukkit.Bukkit
import org.bukkit.scheduler.BukkitTask
import java.util.concurrent.ConcurrentHashMap

class SyncTaskManager(
    private val plugin: Core,
    private val profiler: TaskProfiler,
    private val isLogging: () -> Boolean
) {

    private val tasks = ConcurrentHashMap<String, BukkitTask>()

    fun run(name: String, task: () -> Unit) {
        cancel(name)

        val bukkitTask = Bukkit.getScheduler().runTask(plugin, Runnable {
            safeRun(name, task)
            tasks.remove(name)
        })

        tasks[name] = bukkitTask
    }

    fun runLater(name: String, delayTicks: Long, task: () -> Unit) {
        cancel(name)

        val bukkitTask = Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            safeRun(name, task)
            tasks.remove(name)
        }, delayTicks)

        tasks[name] = bukkitTask
    }

    fun runTimer(name: String, delayTicks: Long, periodTicks: Long, task: () -> Unit) {
        cancel(name)

        val bukkitTask = Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
            safeRun(name, task)
        }, delayTicks, periodTicks)

        tasks[name] = bukkitTask
    }

    private fun safeRun(name: String, task: () -> Unit) {
        val start = System.nanoTime()

        try {
            task()
        } catch (e: Exception) {
            println("§c[SyncTask ERROR] $name")
            e.printStackTrace()
        }

        val duration = System.nanoTime() - start
        val ms = duration / 1_000_000

        profiler.record(name, duration)

        if (isLogging()) {
            println("§7[SyncTask] $name executou em ${ms}ms")
        }

        if (ms > 50) {
            println("§c[LAG][SYNC] $name demorou ${ms}ms")
        }
    }

    fun getTaskNames(): List<String> {
        return tasks.keys().toList()
    }

    fun cancel(name: String) {
        tasks.remove(name)?.cancel()
    }

    fun cancelAll() {
        tasks.values.forEach { it.cancel() }
        tasks.clear()
    }
}