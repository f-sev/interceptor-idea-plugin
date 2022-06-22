package tech.fsev.plugins.k8s.interceptor.actions.interception

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.runBackgroundableTask
import tech.fsev.plugins.k8s.interceptor.services.InterceptorService
import tech.fsev.plugins.k8s.interceptor.utils.createNotification

class UpdateInterceptionStatusAction() : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {

        val interceptorService = e.project!!.service<InterceptorService>()

        runBackgroundableTask("Updating interception status", e.project) {
            try {
                interceptorService.checkInterceptionStatus()

                NotificationGroupManager.getInstance().createNotification(
                    groupId = "Kubernetes Interceptor Plugin",
                    title = "Interception status updated!",
                    project = e.project!!
                ).notify(e.project)

            } catch (exc: Exception) {
                NotificationGroupManager.getInstance().createNotification(
                    groupId = "Kubernetes Interceptor Plugin",
                    title = "Updating interception status error!",
                    content = exc.message ?: "",
                    type = NotificationType.ERROR,
                    project = e.project!!
                ).notify(e.project)
            }

        }
    }

    override fun update(e: AnActionEvent) {
        val interceptorService: InterceptorService? = e.project?.service()

        if (interceptorService != null) {
            e.presentation.isEnabled = !interceptorService.isOperationPerforming
            e.presentation.isVisible = interceptorService.isConnectedToCluster
        }

        super.update(e)
    }
}