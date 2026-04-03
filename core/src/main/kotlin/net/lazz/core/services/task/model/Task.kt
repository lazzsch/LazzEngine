package net.lazz.core.services.task.model

import java.util.concurrent.ScheduledFuture
import java.util.concurrent.atomic.AtomicBoolean

class Task(
    val name: String,
    private val action: () -> Unit
) {

    private var future: ScheduledFuture<*>? = null
    private val running = AtomicBoolean(true)

    fun run() {
        if (!running.get()) return
        action()
    }

    fun setFuture(future: ScheduledFuture<*>) {
        this.future = future
    }

    fun cancel() {
        running.set(false)
        future?.cancel(true)
    }

    fun isRunning(): Boolean = running.get()
}