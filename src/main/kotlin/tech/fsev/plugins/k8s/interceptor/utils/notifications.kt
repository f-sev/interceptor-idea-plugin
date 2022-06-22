package tech.fsev.plugins.k8s.interceptor.utils

import com.intellij.notification.Notification
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.project.Project

fun NotificationGroupManager.createNotification(
    groupId: String,
    title: String,
    content: String = "",
    type: NotificationType = NotificationType.INFORMATION,
    actions: List<AnAction>? = null,
    project: Project
): Notification {
    val notification = this
        .getNotificationGroup(groupId)
        .createNotification(title, content, type)

    actions?.forEach {
        notification.addAction(it)
    }

    return notification
}
