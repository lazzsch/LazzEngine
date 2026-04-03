package net.lazz.core.services.task.manager

import net.lazz.core.services.task.model.Task
import net.lazz.core.services.task.monitor.TaskProfiler
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

class AsyncTaskManager(
    private val profiler: TaskProfiler,
    private val isLogging: () -> Boolean,
    threads: Int = Runtime.getRuntime().availableProcessors() * 2
) {

    private val executor = Executors.newScheduledThreadPool(threads)
    private val tasks = ConcurrentHashMap<String, Task>()

    fun run(name: String, task: () -> Unit) {
        cancel(name)

        val t = Task(name, task)

        val future = executor.schedule({
            safeRun(t)
            tasks.remove(name)
        }, 0, TimeUnit.MILLISECONDS)

        t.setFuture(future)
        tasks[name] = t
    }

    fun runLater(name: String, delayMs: Long, task: () -> Unit) {
        cancel(name)

        val t = Task(name, task)

        val future = executor.schedule({
            safeRun(t)
            tasks.remove(name)
        }, delayMs, TimeUnit.MILLISECONDS)

        t.setFuture(future)
        tasks[name] = t
    }

    fun runTimer(name: String, delayMs: Long, periodMs: Long, task: () -> Unit) {
        cancel(name)

        val t = Task(name, task)

        val future = executor.scheduleAtFixedRate({
            safeRun(t)
        }, delayMs, periodMs, TimeUnit.MILLISECONDS)

        t.setFuture(future)
        tasks[name] = t
    }

    private fun safeRun(task: Task) {
        val start = System.nanoTime()

        try {
            task.run()
        } catch (e: Exception) {
            println("§c[Task ERROR] ${task.name}")
            e.printStackTrace()
        }

        val duration = System.nanoTime() - start
        val ms = duration / 1_000_000

        profiler.record(task.name, duration)

        // 🔥 LOG REAL
        if (isLogging()) {
            println("§7[Task] ${task.name} executou em ${ms}ms")
        }

        // 🔥 LAG DETECT
        if (ms > 50) {
            println("§c[LAG] Task ${task.name} demorou ${ms}ms")
        }
    }

    fun cancel(name: String) {
        tasks.remove(name)?.cancel()
    }

    fun cancelAll() {
        tasks.values.forEach { it.cancel() }
        tasks.clear()
    }

    fun getQueueSize(): Int = tasks.size

    fun getActiveThreads(): Int =
        (executor as ScheduledThreadPoolExecutor).activeCount

    fun getTaskNames(): List<String> {
        return tasks.keys().toList()
    }

    fun shutdown() {
        cancelAll()
        executor.shutdownNow()
    }
}