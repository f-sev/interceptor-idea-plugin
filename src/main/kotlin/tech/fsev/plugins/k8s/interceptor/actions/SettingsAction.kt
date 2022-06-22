package tech.fsev.plugins.k8s.interceptor.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class SettingsAction: AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        println("Settings menu opened!")
    }
}