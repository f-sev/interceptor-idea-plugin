package tech.fsev.plugins.k8s.interceptor.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import tech.fsev.plugins.k8s.interceptor.models.Cluster
import tech.fsev.plugins.k8s.interceptor.models.Intercept
import tech.fsev.plugins.k8s.interceptor.utils.*
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

private val LOG = logger<InterceptorService>()

/**
 * This service class interacts with Telepresence and stores its actual connection/interception state
 *
 * @property isOperationPerforming the indicator of long-running operation (can be used for UI locking)
 * @property connectedCluster the cluster connected to our local agent
 * @property isConnectedToCluster the helper property which indicates if local agent connected to cluster
 * @property interceptedServices list of intercepted services
 */
@Service
class InterceptorService(private val project: Project) {

    var isTelepresenceInstalled = false
        private set

    var isOperationPerforming = false
        private set

    var connectedCluster: Cluster? = null
        private set

    val isConnectedToCluster
        get() = connectedCluster != null

    val interceptedServices: MutableList<String> = mutableListOf()

    /**
     * Check installation and version of Telepresence
     *
     * @throws TelepresenceVerifyInstallationException if Telepresence not installed
     * @return a boolean expression indicates if right version of Telepresence installed
     */
    fun checkTelepresenceInstallation(): Boolean {

        try {
            val outputResult = "telepresence version".runCommandWithProcessBuilder(File(System.getProperty("user.dir")))

            isTelepresenceInstalled = outputResult?.contains("Client: v", ignoreCase = true) == true
                    && outputResult.contains("(api v3)", ignoreCase = true)

            return isTelepresenceInstalled
        } catch (e: Exception) {
            throw TelepresenceVerifyInstallationException(
                "Telepresence installation verification error. See the logs for details: ${e.message}"
            )
        }

    }

    /**
     * Connects to Kubernetes cluster using Telepresence
     *
     * @throws ClusterConnectionError where cluster configuration changed / other unexpected errors
     * @return a [Cluster] object which stores cluster context name and cluster address
     */
    fun connectToCluster(): Cluster {

        if (connectedCluster != null)
            return connectedCluster as Cluster

        longActionPerforming {

            val outputResult = "telepresence connect".runCommandWithProcessBuilder(File(System.getProperty("user.dir")))

            val successResult =
                outputResult?.let { Regex("Connected to context (.*) \\((.*)\\)").find(it)?.groupValues }

            if (successResult != null) {

                val (_, clusterContext, clusterServerLocation) = successResult
                connectedCluster = Cluster(clusterContext, clusterServerLocation)

                return connectedCluster as Cluster
            } else {
                throw ClusterConnectionError(outputResult ?: "")
            }
        }
    }

    /**
     * Disconnects from Kubernetes cluster using Telepresence
     *
     * @return disconnected cluster name
     */
    fun disconnectFromCluster(): Cluster? {

        longActionPerforming {
            val outputResult = "telepresence quit".runCommandWithProcessBuilder(File(System.getProperty("user.dir")))

            val clusterToDisconnect = connectedCluster?.copy()
            connectedCluster = null

            return clusterToDisconnect
        }
    }

    /**
     * Checking interception status via "telepresence status" command and updates the state if needed
     *
     * @throws UnexpectedTelepresenceError when response message is unknown
     * @return None
     */
    fun checkInterceptionStatus(): Unit {

        longActionPerforming {

            val outputResult = "telepresence status".runCommandWithProcessBuilder(File(System.getProperty("user.dir")))
            val lines = outputResult?.lines() ?: return

            var connectionStatus: String? = null        // "Connected", "Not Connected" or null
            var connectedServerLocation: String? = null // Connected cluster details
            var connectedServerContext: String? = null  // Connected cluster details
            var interceptsStartIndex: Int? = null       // start line index of intercepted services
            var interceptsCount = 0                     // count of intercepted services
            val interceptedServicesStatus: MutableList<String> = mutableListOf()

            lines.forEachIndexed { index, element ->
                with(element.trim()) {
                    when {
                        equals("Root Daemon: Not running") || equals("User Daemon: Not running") ->
                            connectionStatus = "Not Connected"
                        startsWith("Status") ->
                            connectionStatus = split(":").lastOrNull()?.trim()

                        startsWith("Kubernetes server") -> connectedServerLocation =
                            Regex("Kubernetes server.*: (.*)").find(this)!!.groupValues[1]

                        startsWith("Kubernetes context") -> connectedServerContext =
                            Regex("Kubernetes context.*: (.*)").find(this)!!.groupValues[1]

                        startsWith("Intercepts") -> {
                            val interceptCountPattern = "Intercepts.*: (\\d) total"
                            interceptsCount = Regex(interceptCountPattern).find(this)!!.groupValues[1].toInt()
                            interceptsStartIndex = if (interceptsCount > 0) index + 1 else 0
                        }
                    }

                    // If line contains names of service intercepts
                    if (interceptsStartIndex != null) {
                        if (index in interceptsStartIndex!! until interceptsStartIndex!! + interceptsCount)
                            interceptedServicesStatus.add(this.split(":")[0])
                    }
                }
            }

            // if mandatory properties are not exists, throw an error
            if (connectionStatus == null && interceptsStartIndex == null)
                throw UnexpectedTelepresenceError("Parsing error: Unknown response \n\n $outputResult")

            // Assign status results to class properties
            connectedCluster = if (
                connectionStatus == "Connected" && connectedServerLocation != null && connectedServerContext != null
            ) {
                Cluster(connectedServerContext!!, connectedServerLocation!!)
            } else
                null

            interceptedServices.clear()
            interceptedServices.addAll(interceptedServicesStatus)
        }
    }

    /**
     * Get list of deployments
     *
     * @param withoutIntercepted if set to true, only available for interception deployments will be filtered
     * @return list of deployments
     */
    fun getAvailableDeployments(withoutIntercepted: Boolean = true): List<String> {

        longActionPerforming {
            val outputResult = "telepresence list".runCommandWithProcessBuilder(File(System.getProperty("user.dir")))
            val lines = outputResult?.lines() ?: emptyList()

            val deployments = mutableListOf<String>()

            lines
                .filter { it.isNotBlank() }
                .map { it.split(":") }
                .forEach { element ->
                    if (element[1].contains("ready to intercept") ||
                        (element[1].contains("intercepted") && !withoutIntercepted)
                    ) {
                        deployments.add(element[0].trim())
                    }
                }

            return deployments
        }
    }

    /**
     * Intercepts deployment with defined name, port and env path
     *
     * @throws UnknownDeploymentError where telepresence can not recognize deployment name
     * @throws UnexpectedTelepresenceError where function can not parse response message
     * @return added deployment name
     */
    fun interceptDeployment(deployment: Intercept): String {

        longActionPerforming {

            // Here we use different way of new subprocess creation because the Telepresence itself creates
            // "sshfs" subprocess which need to be run out the control of JVM.
            // ProcessBuilder can't do this operation neither Runtime.exec() can
            val outputResult = ("telepresence intercept $deployment " +
                    if (deployment.localPort != 0) "--port ${deployment.localPort} " else "" +
                            if (deployment.envPath != "") "--env-json ${deployment.envPath}" else ""
                    ).runCommandWithRuntime() ?: throw UnexpectedTelepresenceError("Unknown Telepresence error")

            // this complex logical expression indicates the presence of keywords that confirms interception
            val isInterceptionSucceeded: Boolean = outputResult.contains("intercepted") &&
                    Regex("Intercept name.*: ${deployment.name}").containsMatchIn(outputResult) &&
                    Regex("State.*: ACTIVE").containsMatchIn(outputResult)

            // error guards
            when {
                outputResult.contains("error: No interceptable deployment") -> throw UnknownDeploymentError(outputResult)
                !isInterceptionSucceeded -> throw UnexpectedTelepresenceError(outputResult)
            }

            interceptedServices.add(deployment.name)

            return deployment.name
        }
    }


    /**
     * Removes deployment by name
     *
     * @throws UnknownDeploymentError where telepresence can not recognize deployment name
     * @throws UnexpectedTelepresenceError where function can not parse response message
     * @return removed deployment name
     */
    fun leaveDeployment(deployment: String): String {

        longActionPerforming {

            val outputResult =
                "telepresence leave $deployment".runCommandWithProcessBuilder(File(System.getProperty("user.dir")))

            if (!outputResult.isNullOrBlank()) {
                when {
                    Regex("Intercept named \"$deployment\" not found").containsMatchIn(outputResult) -> throw UnknownDeploymentError(
                        outputResult
                    )
                    else -> throw UnexpectedTelepresenceError(outputResult)
                }
            }

            interceptedServices.remove(deployment)

            return deployment
        }
    }


    /**
     * Sets long operation indicator **[isOperationPerforming]** to true until operation ends
     *
     * @param action a closure-style function
     *
     * @return the result of passed function execution
     */
    private inline fun <T> longActionPerforming(action: () -> T): T {
        isOperationPerforming = true
        try {
            return action()
        } finally {
            isOperationPerforming = false
        }
    }
}
