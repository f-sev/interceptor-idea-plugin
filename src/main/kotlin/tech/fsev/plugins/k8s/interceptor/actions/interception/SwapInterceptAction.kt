package tech.fsev.plugins.k8s.interceptor.actions.interception

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.runBackgroundableTask
import tech.fsev.plugins.k8s.interceptor.models.Intercept
import tech.fsev.plugins.k8s.interceptor.services.InterceptorService
import tech.fsev.plugins.k8s.interceptor.ui.SwapInterceptDialog
import tech.fsev.plugins.k8s.interceptor.utils.createNotification

class SwapInterceptAction() : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {

        val dialog = SwapInterceptDialog(e.project!!)
        dialog.show()

        val deployment = Intercept(dialog.model.name, dialog.model.localPort, dialog.model.envPath)

        val interceptorService = e.project!!.service<InterceptorService>()

        runBackgroundableTask("Intercepting ${deployment.name} deployment", e.project) {
            try {
                interceptorService.interceptDeployment(deployment)

                NotificationGroupManager.getInstance().createNotification(
                    groupId = "Kubernetes Interceptor Plugin",
                    title = "Deployment ${deployment.name} successfully intercepted!",
                    project = e.project!!
                ).notify(e.project)

            } catch (exc: Exception) {
                NotificationGroupManager.getInstance().createNotification(
                    groupId = "Kubernetes Interceptor Plugin",
                    title = "Intercepting ${deployment.name} error!",
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
            e.presentation.isVisible = interceptorService.isConnectedToCluster
            e.presentation.isEnabled = !interceptorService.isOperationPerforming
        }

        super.update(e)
    }
}