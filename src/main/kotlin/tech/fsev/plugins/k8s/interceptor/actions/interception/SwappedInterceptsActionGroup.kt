package tech.fsev.plugins.k8s.interceptor.actions.interception

import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import tech.fsev.plugins.k8s.interceptor.services.InterceptorService

class SwappedInterceptsActionGroup : ActionGroup() {

    override fun getChildren(e: AnActionEvent?): Array<LeaveInterceptAction> {

        return e?.project?.service<InterceptorService>()?.interceptedServices?.map { it ->
            LeaveInterceptAction(it)
        }?.toTypedArray() ?: emptyArray()
    }

    override fun update(e: AnActionEvent) {
        val interceptorService: InterceptorService? = e.project?.service()

        if (interceptorService != null) {
            e.presentation.isVisible = interceptorService.isConnectedToCluster
            e.presentation.isEnabled = interceptorService.interceptedServices.isNotEmpty() && !interceptorService.isOperationPerforming
        }

        super.update(e)
    }
}