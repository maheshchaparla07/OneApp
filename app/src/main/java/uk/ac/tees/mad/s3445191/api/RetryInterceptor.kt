package uk.ac.tees.mad.s3445191.api


import okhttp3.Interceptor
import java.io.IOException
import okhttp3.Response
import java.net.SocketTimeoutException
import javax.net.ssl.SSLHandshakeException


class RetryInterceptor(
    private val maxRetries: Int,
    private val initialDelayMs: Long,
    private val maxDelayMs: Long
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        var currentDelay = initialDelayMs
        var attempt = 0

        while (true) {
            try {
                return chain.proceed(chain.request())
            } catch (e: IOException) {
                if (++attempt > maxRetries) throw e

                when (e) {
                    is SocketTimeoutException,
                    is SSLHandshakeException -> {
                        Thread.sleep(currentDelay)
                        currentDelay = (currentDelay * 2).coerceAtMost(maxDelayMs)
                    }
                    else -> throw e
                }
            }
        }
    }
}