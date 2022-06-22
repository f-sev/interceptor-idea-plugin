package tech.fsev.plugins.k8s.interceptor.ui

import com.intellij.openapi.components.service
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.dsl.builder.bindIntText
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import tech.fsev.plugins.k8s.interceptor.models.Intercept
import tech.fsev.plugins.k8s.interceptor.services.InterceptorService
import javax.swing.JComponent


class SwapInterceptDialog(val project: Project) :
    DialogWrapper(project, true) {
    var model: Intercept = Intercept()

    init {
        init()
        title = "Intercept a Service"
    }

    override fun createCenterPanel(): JComponent {

        val interceptorService = project.service<InterceptorService>()
        val comboBoxChoices = interceptorService.getAvailableDeployments().toTypedArray()

        return panel {
            row("Service name:") {
                comboBox(comboBoxChoices).bindItem(model::name)
            }
            row("Port:") {
                intTextField().bindIntText(model::localPort)
            }
            row("Path to env variables:") {
                textFieldWithBrowseButton(
                    "Choose ENV File",
                    project,
                    FileChooserDescriptor(true, false, false, false, false, false)
                ).bindText(model::envPath)
                contextHelp(
                    "If file chosen, it will be used to store ENV variables. " +
                            "If directory, plugin will create new default ENV file there",
                    "Choose the directory or file"
                )
            }
        }
    }
}

