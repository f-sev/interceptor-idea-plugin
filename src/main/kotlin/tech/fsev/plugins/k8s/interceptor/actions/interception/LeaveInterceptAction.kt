package tech.fsev.plugins.k8s.interceptor.actions.interception

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.runBackgroundableTask
import tech.fsev.plugins.k8s.interceptor.services.InterceptorService
import tech.fsev.plugins.k8s.interceptor.utils.createNotification

class LeaveInterceptAction(label: String) : AnAction(label) {

    override fun actionPerformed(e: AnActionEvent) {

        val interceptorService = e.project!!.service<InterceptorService>()

        runBackgroundableTask("Leaving deployment ${e.presentation.text}", e.project) {
            try {
                val deploymentName = interceptorService.leaveDeployment(e.presentation.text)

                NotificationGroupManager.getInstance().createNotification(
                    groupId = "Kubernetes Interceptor Plugin",
                    title = "Successfully left deployment $deploymentName!",
                    project = e.project!!
                ).notify(e.project)

            } catch (exc: Exception) {
                NotificationGroupManager.getInstance().createNotification(
                    groupId = "Kubernetes Interceptor Plugin",
                    title = "Leaving deployment error!",
                    content = exc.message ?: "",
                    type = NotificationType.ERROR,
                    project = e.project!!
                ).notify(e.project)
            }
        }
    }
}
