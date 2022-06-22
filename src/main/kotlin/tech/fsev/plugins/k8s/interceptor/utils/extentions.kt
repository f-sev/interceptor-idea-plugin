package tech.fsev.plugins.k8s.interceptor.utils

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.util.messages.Topic

fun invokeLater(task: () -> Unit) = ApplicationManager.getApplication().invokeLater(task)

fun <L : Any> subscribeToAppTopic(topic: Topic<L>, listener: () -> L) {
    ApplicationManager
        .getApplication()
        .messageBus
        .connect()
        .subscribe(topic, listener())
}

fun <L : Any> subscribeToProjectTopic(project: Project, topic: Topic<L>, listener: () -> L) {
    project
        .messageBus
        .connect()
        .subscribe(topic, listener())
}



