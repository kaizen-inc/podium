package inc.kaizen.infra.podium

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import kotlin.random.Random

/**
 * An OkHttp interceptor that retries failed requests with exponential backoff and jitter.
 *
 * Retries are attempted for:
 * - Network failures ([IOException])
 * - Server errors (HTTP 5xx) on idempotent methods (GET, HEAD, OPTIONS, PUT, DELETE)
 *
 * Full jitter is applied to each backoff delay to spread out thundering-herd retries.
 *
 * @property maxRetries Maximum number of retry attempts. Defaults to 3.
 * @property initialBackoffMillis Initial backoff delay in milliseconds. Defaults to 1000ms.
 * @property maxBackoffMillis Upper cap for any single backoff delay in milliseconds. Defaults to 30000ms.
 */
class RetryInterceptor(
    private val maxRetries: Int = 3,
    private val initialBackoffMillis: Long = 1000L,
    private val maxBackoffMillis: Long = 30_000L,
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

    /**
     * Sleeps for a jittered exponential backoff duration.
     * Actual delay = random value in [0, min(initialBackoffMillis * 2^attempt, maxBackoffMillis)].
     */
    private fun sleepForBackoff(attempt: Int) {
        val exponential = minOf(initialBackoffMillis * (1L shl attempt), maxBackoffMillis)
        val jittered = Random.nextLong(0, exponential + 1)
        Thread.sleep(jittered)
    }
}
