package tech.fsev.plugins.k8s.interceptor.listeners

import com.intellij.ide.AppLifecycleListener
import com.intellij.openapi.ui.Messages

internal class K8sInterceptorAppLifecycleListener : AppLifecycleListener {

    override fun appClosing() {
//        val result = Messages.showYesNoDialog(
//            "Do you want to leave the services?",
//            "Some Services Are Intercepted",
//            Messages.getWarningIcon(),
//        )
//
//        if (result == Messages.YES)
//            println("Left the services away")
    }
}
