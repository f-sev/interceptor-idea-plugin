package tech.fsev.plugins.k8s.interceptor.utils

import java.io.File
import java.io.IOException
import java.io.SequenceInputStream
import java.util.concurrent.TimeUnit

fun String.runCommandWithProcessBuilder(workingDir: File): String? {
    return try {
        val parts = this.split("\\s".toRegex())
        val proc = ProcessBuilder(*parts.toTypedArray())
            .directory(workingDir)
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .redirectErrorStream(true)
            .start()

        proc.waitFor(60, TimeUnit.MINUTES)
        proc.inputStream.bufferedReader().readText()
    } catch (e: IOException) {
        e.printStackTrace()
        throw e
    }
}

fun String.runCommandWithRuntime(): String? {
    return try {

        val process = Runtime.getRuntime().exec(this)
        val stream = SequenceInputStream(process.inputStream, process.errorStream)

        stream.bufferedReader().readText()

    } catch (e: IOException) {
        e.printStackTrace()
        throw e
    }
}