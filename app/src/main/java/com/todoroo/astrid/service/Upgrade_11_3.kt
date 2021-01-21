@file:Suppress("ClassName")

package com.todoroo.astrid.service

import org.tasks.caldav.iCalendar
import org.tasks.caldav.iCalendar.Companion.apply
import org.tasks.data.OpenTaskDao
import org.tasks.data.TaskDao
import org.tasks.data.UpgraderDao
import javax.inject.Inject

class Upgrade_11_3 @Inject constructor(
        private val upgraderDao: UpgraderDao,
        private val openTaskDao: OpenTaskDao,
        private val taskDao: TaskDao
) {
    internal suspend fun applyiCalendarStartDates() {
        val (hasStartDate, noStartDate) =
                upgraderDao.tasksWithVtodos().partition { it.startDate > 0 }
        for (task in noStartDate) {
            task.vtodo?.let { iCalendar.fromVtodo(it) }?.dtStart?.let {
                it.apply(task.task)
                upgraderDao.setStartDate(task.id, task.startDate)
            }
        }
        hasStartDate
                .map { it.id }
                .let { taskDao.touch(it) }
    }

    internal suspend fun applyOpenTaskStartDates() {
        openTaskDao.getLists().forEach { list ->
            val (hasStartDate, noStartDate) =
                    upgraderDao
                            .getOpenTasksForList(list.account!!, list.url!!)
                            .partition { it.startDate > 0 }
            for (task in noStartDate) {
                openTaskDao
                        .getTask(list.id, task.remoteId!!)
                        ?.dtStart
                        ?.let {
                            it.apply(task.task)
                            upgraderDao.setStartDate(task.id, task.startDate)
                        }
            }
            hasStartDate
                    .map { it.id }
                    .let { taskDao.touch(it) }
        }
    }

    companion object {
        const val VERSION = 110300
    }
}