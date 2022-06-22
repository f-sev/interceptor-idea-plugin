package tech.fsev.plugins.k8s.interceptor

import com.intellij.notification.BrowseNotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import tech.fsev.plugins.k8s.interceptor.services.InterceptorService
import tech.fsev.plugins.k8s.interceptor.utils.K8sInterceptorBundle
import tech.fsev.plugins.k8s.interceptor.utils.TelepresenceVerifyInstallationException
import tech.fsev.plugins.k8s.interceptor.utils.createNotification

class InterceptorStartupActivity : StartupActivity, DumbAware {

    override fun runActivity(project: Project) {

        val interceptorService = project.service<InterceptorService>()
        var notificationContent: String? = null

        try {
            if (!interceptorService.checkTelepresenceInstallation()) {
                notificationContent =
                    "Unknown version of Telepresence. Please, use the v2.* vesion compatible with api 3"
            }
        } catch (e: Exception) {
            notificationContent = K8sInterceptorBundle.message("telepresence.not.installed.message")
        } finally {
            if (notificationContent != null) {
                NotificationGroupManager.getInstance().createNotification(
                    groupId = "Kubernetes Interceptor Plugin",
                    title = "Telepresence is not installed!",
                    content = notificationContent,
                    type = NotificationType.WARNING,
                    actions = listOf(
                        BrowseNotificationAction(
                            "Install it manually...",
                            "https://www.telepresence.io/docs/latest/install/"
                        )
                    ),
                    project = project
                ).notify(project)
            }
        }

        interceptorService.checkInterceptionStatus()
    }
}