package com.xraiassistant.domain.errors

import java.io.IOException
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/**
 * Says whether a failed AI request was the connection going away (the app was
 * backgrounded, the device dozed, or the network dropped) rather than a real
 * error, so it is worth sending again once the app is back in front.
 */
object NetworkInterruption {

    private val interruptionText = listOf(
        "software caused connection abort",
        "connection reset",
        "connection abort",
        "unexpected end of stream",
        "stream was reset",
        "socket closed",
        "broken pipe",
        "network is unreachable"
    )

    fun isInterruption(error: Throwable?): Boolean {
        var current = error
        var depth = 0
        while (current != null && depth < 8) {
            when (current) {
                is UnknownHostException, is ConnectException, is NoRouteToHostException,
                is SocketException, is InterruptedIOException -> return true
                is SSLException -> if (matchesText(current)) return true
                is IOException -> if (matchesText(current)) return true
            }
            current = current.cause
            depth++
        }
        return false
    }

    private fun matchesText(error: Throwable): Boolean {
        val text = error.message?.lowercase() ?: return false
        return interruptionText.any { text.contains(it) }
    }
}

/**
 * Holds one interrupted request and sends it again when the app is in front.
 * Only the latest request is kept, and each is retried once.
 */
class InterruptedRequestQueue {
    private var pending: (() -> Unit)? = null

    val hasPending: Boolean get() = pending != null

    fun hold(retry: () -> Unit) {
        pending = retry
    }

    /** Sends the held request, if any. Call when the app comes to the front. */
    fun resume() {
        val retry = pending ?: return
        pending = null
        retry()
    }
}
