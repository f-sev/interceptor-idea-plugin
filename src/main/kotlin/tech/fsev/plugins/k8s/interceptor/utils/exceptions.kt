package tech.fsev.plugins.k8s.interceptor.utils

class TelepresenceVerifyInstallationException(message: String) : Exception(message)

class ClusterConnectionError(message: String) : Exception(message)

// This error caused when deployment not found while intercepting or leaving
class UnknownDeploymentError(message: String) : Exception(message)

// This error caused when we get unexpected output from Telepresence
class UnexpectedTelepresenceError(message: String) : Exception(message)