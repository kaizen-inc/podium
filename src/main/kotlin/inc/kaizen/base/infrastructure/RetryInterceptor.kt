package inc.kaizen.base.infrastructure

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * An OkHttp interceptor that retries failed requests with exponential backoff.
 *
 * Retries are attempted for:
 * - Network failures ([IOException])
 * - Server errors (HTTP 5xx) on idempotent methods (GET, HEAD, OPTIONS, PUT, DELETE)
 *
 * @property maxRetries Maximum number of retry attempts. Defaults to 3.
 * @property initialBackoffMillis Initial backoff delay in milliseconds. Doubles after each retry. Defaults to 1000ms.
 */
class RetryInterceptor(
    private val maxRetries: Int = 3,
    private val initialBackoffMillis: Long = 1000L
) : Interceptor {

    companion object {
        private val IDEMPOTENT_METHODS = setOf("GET", "HEAD", "OPTIONS", "PUT", "DELETE")
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var lastException: IOException? = null
        var lastResponse: Response? = null

        for (attempt in 0..maxRetries) {
            try {
                // Close the previous unsuccessful response body before retrying
                lastResponse?.close()

                val response = chain.proceed(request)

                if (response.isSuccessful || !isRetryable(response, request.method)) {
                    return response
                }

                lastResponse = response

                if (attempt < maxRetries) {
                    sleepForBackoff(attempt)
                }
            } catch (e: IOException) {
                lastException = e
                if (attempt >= maxRetries) {
                    throw e
                }
                sleepForBackoff(attempt)
            }
        }

        // If we exhausted all retries with a server error response, return the last response
        lastResponse?.let { return it }

        // Should not reach here, but just in case
        throw lastException ?: IOException("Retry exhausted with no response")
    }

    private fun isRetryable(response: Response, method: String): Boolean {
        return response.code in 500..599 && method.uppercase() in IDEMPOTENT_METHODS
    }

    private fun sleepForBackoff(attempt: Int) {
        val backoff = initialBackoffMillis * (1L shl attempt)
        Thread.sleep(backoff)
    }
}

