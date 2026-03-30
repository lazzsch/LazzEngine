package net.lazz.core.service.task.enum

enum class TaskState(val key: String) {
    STABLE("stable"),
    MODERATE("moderate"),
    UNDER_LOAD("under_load"),
    CRITICAL("critical")
}