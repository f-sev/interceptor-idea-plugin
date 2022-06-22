package tech.fsev.plugins.k8s.interceptor.actions.connection

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.runBackgroundableTask
import tech.fsev.plugins.k8s.interceptor.services.InterceptorService
import tech.fsev.plugins.k8s.interceptor.utils.createNotification

class DisconnectFromClusterAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {

        val interceptorService = e.project!!.service<InterceptorService>()


        runBackgroundableTask("Quitting Kubernetes cluster", e.project) {
            try {
                val cluster = interceptorService.disconnectFromCluster()

                it.checkCanceled()
                it.fraction = 0.5

                if (cluster != null)
                    NotificationGroupManager.getInstance().createNotification(
                        groupId = "Kubernetes Interceptor Plugin",
                        title = "Disconnected from ${cluster.context}",
                        project = e.project!!
                    ).notify(e.project)

                interceptorService.checkInterceptionStatus()
            } catch (exc: Exception) {
                NotificationGroupManager.getInstance().createNotification(
                    groupId = "Kubernetes Interceptor Plugin",
                    title = "Quitting cluster error!",
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