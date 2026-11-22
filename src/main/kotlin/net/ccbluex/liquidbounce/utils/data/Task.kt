/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */

package net.ccbluex.liquidbounce.utils.data

import kotlinx.coroutines.Job
import java.util.concurrent.ConcurrentHashMap

/**
 * An asynchronous task with progress tracking.
 *
 * Kept as a generic progress model for marketplace downloads.
 */
open class Task(val name: String) {

    @Volatile
    var progress: Float = 0f
        get() = if (subTasks.isNotEmpty()) calculateProgress() else field
    @Volatile
    var isCompleted: Boolean = false
        get() = field && if (subTasks.isNotEmpty()) areAllSubTasksCompleted() else true
    var startTime: Long = System.currentTimeMillis()
    @Volatile
    var job: Job? = null
    val subTasks = ConcurrentHashMap<String, Task>()

    fun getOrCreateTask(subTaskName: String): Task {
        return subTasks.computeIfAbsent(subTaskName, ::Task)
    }

    fun getOrCreateFileTask(subTaskName: String): ResourceTask {
        return subTasks.computeIfAbsent(subTaskName, ::ResourceTask) as ResourceTask
    }

    fun calculateProgress(): Float {
        if (subTasks.isEmpty()) return progress
        return subTasks.values.sumOf { it.progress.toDouble() }.toFloat() / subTasks.size
    }

    fun areAllSubTasksCompleted(): Boolean = subTasks.isEmpty() ||
        subTasks.values.all { task -> task.isCompleted }

}

/**
 * A task with IO progress tracking.
 */
class ResourceTask(name: String) : Task(name) {

    var bytesRead = 0L
        private set
    var contentLength = 0L
        private set

    val speed: Long
        get() {
            val elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000.0
            return if (elapsedSeconds > 0) {
                (bytesRead / elapsedSeconds).toLong()
            } else {
                0
            }
        }

    fun update(bytesRead: Long, contentLength: Long) {
        this.bytesRead = bytesRead
        this.contentLength = contentLength
        this.progress = if (contentLength > 0) {
            bytesRead.toFloat() / contentLength.toFloat()
        } else {
            0f
        }
    }

}
