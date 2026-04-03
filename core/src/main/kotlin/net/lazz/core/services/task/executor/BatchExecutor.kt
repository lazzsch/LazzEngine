package net.lazz.core.services.task.executor

import net.lazz.core.services.task.TaskManager
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.milliseconds

class BatchExecutor<T>(
    private val taskManager: TaskManager,
    private val name: String,
    private val batchSize: Int = 50,
    private val intervalMs: Long = 50,
    private val consumer: (List<T>) -> Unit
) {

    private val queue = ConcurrentLinkedQueue<T>()
    private val running = AtomicBoolean(false)

    fun submit(item: T) {
        queue.offer(item)
        start()
    }

    private fun start() {
        if (!running.compareAndSet(false, true)) return

        taskManager.runTimerAsync(name, intervalMs.milliseconds) {
            process()
        }
    }

    private fun process() {
        if (queue.isEmpty()) {
            running.set(false)
            taskManager.cancel(name)
            return
        }

        val batch = mutableListOf<T>()

        repeat(batchSize) {
            val item = queue.poll() ?: return@repeat
            batch.add(item)
        }

        if (batch.isNotEmpty()) {
            try {
                consumer(batch)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}