package tech.fsev.plugins.k8s.interceptor.models

data class Intercept(
    var name: String = "",
    var localPort: Int = 8000,
    var envPath: String = "",
) {
    override fun toString() = name
}
